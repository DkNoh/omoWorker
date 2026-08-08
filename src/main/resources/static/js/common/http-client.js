/**
 * @fileoverview http-client.js — axios 기반 전역 HTTP 요청·응답 인프라.
 *
 * axios interceptors + spinner + 세션 만료 처리 + 얇은 호출 래퍼.
 * common-utils.js에서 분리됨 (Phase 1)
 *
 * 노출: window.HttpClient = { get, post, put, delete, remove }
 * 호환: window.ApiClient = HttpClient (기존 api-client.js 제거 대체)
 *
 * 로드/의존 계약:
 *   - axios가 이 파일보다 먼저 로드되어야 한다. 없으면 전역 설정 없이 종료한다.
 *   - Notify가 먼저 로드되어 있어야 오류 alert가 표시된다. 없더라도 Promise reject는 유지한다.
 *   - defaultLayout의 `_csrf`, `_csrf_header` meta 값을 변경 요청 헤더에 자동 주입한다.
 *
 * 응답 계약:
 *   - `{code, data, message}` ApiResponse에서 code=200이면 response.data를 실제 data로 교체한다.
 *   - HTTP 오류와 code!=200 비즈니스 오류는 알림 후 reject하여 호출부 catch 흐름을 보존한다.
 *   - Spring Security가 반환한 로그인 HTML은 세션 만료로 판단하고 `/login`으로 이동한다.
 */
(function () {
    'use strict';

    if (typeof axios === 'undefined') {
        console.error('[http-client] axios가 로드되지 않았습니다.');
        return;
    }

    /* --------------------------------------------------------------------------
     * 전역 요청 스피너
     * -------------------------------------------------------------------------- */
    let ajaxCount = 0;

    /** 진행 중 요청 수를 증가시키고 전역 스피너를 표시한다. */
    const showSpinner = () => {
        ajaxCount++;
        const overlay = document.getElementById('global-spinner-overlay');
        if (overlay) overlay.classList.add('active');
    };

    /**
     * 완료된 요청 수만큼 카운터를 감소시키고, 모든 요청이 끝났을 때만 스피너를 숨긴다.
     * 요청/응답 인터셉터 양쪽 오류에서도 호출되므로 카운터는 0 미만으로 내려가지 않게 보정한다.
     */
    const hideSpinner = () => {
        ajaxCount--;
        if (ajaxCount <= 0) {
            ajaxCount = 0;
            const overlay = document.getElementById('global-spinner-overlay');
            if (overlay) overlay.classList.remove('active');
        }
    };

    /* body가 준비된 뒤 스피너 DOM을 한 번만 만들고 CSS `.active`로 표시를 제어한다. */
    document.addEventListener('DOMContentLoaded', () => {
        if (!document.getElementById('global-spinner-overlay')) {
            const overlay = document.createElement('div');
            overlay.id = 'global-spinner-overlay';
            overlay.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';
            document.body.appendChild(overlay);
        }
    });

    /* --------------------------------------------------------------------------
     * 세션 만료 감지
     *
     * SecurityConfig에는 formLogin(loginPage="/login")만 있고 custom EntryPoint가 없다.
     * Spring Security의 302 redirect를 axios가 따라가면 API 호출 결과로 로그인 HTML 200이
     * 도착하므로, content-type과 화면 marker를 검사해 로그인 페이지로 전환한다.
     * -------------------------------------------------------------------------- */
    const LOGIN_MARKERS = /login-shell|login-form|SMS V3 로그인/;
    const SESSION_REDIRECTING = { done: false };

    /** 정상 JSON 대신 로그인 HTML이 반환됐는지 content-type과 화면 marker로 이중 확인한다. */
    const isSessionExpiredResponse = (response) => {
        if (!response) return false;
        const ct = (response.headers && response.headers['content-type']) || '';
        if (ct.indexOf('text/html') !== -1) return true;
        return typeof response.data === 'string' && LOGIN_MARKERS.test(response.data);
    };

    /**
     * 중복 redirect를 방지하면서 로그인 화면으로 현재 history entry를 교체한다.
     * @returns {boolean} 이번 호출에서 실제 redirect를 시작했으면 true
     */
    const redirectToLogin = () => {
        const onLogin = location.pathname.replace(/\/+$/, '').endsWith('/login');
        if (onLogin || SESSION_REDIRECTING.done) return false;
        SESSION_REDIRECTING.done = true;
        console.warn('[session] 만료 감지 — /login 으로 전환');
        location.replace('/login');
        return true;
    };

    /* redirect 이후 호출부의 then/catch가 기존 화면 DOM을 변경하지 못하도록 Promise chain을 중단한다. */
    const haltChain = () => new Promise(() => {});

    /* --------------------------------------------------------------------------
     * Axios 요청 인터셉터
     * -------------------------------------------------------------------------- */
    axios.interceptors.request.use(
        config => {
            showSpinner();
            /* 토큰과 헤더 이름이 모두 있을 때만 CSRF 헤더를 주입한다. */
            const csrfToken = document.querySelector('meta[name="_csrf"]');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
            if (csrfToken && csrfHeader && csrfToken.content && csrfHeader.content) {
                config.headers[csrfHeader.content] = csrfToken.content;
            }
            return config;
        },
        error => {
            hideSpinner();
            return Promise.reject(error);
        }
    );

    /**
     * CSS selector 문자열에 서버 field명을 직접 보간하지 않고 속성값을 strict 비교한다.
     * `]`, 따옴표 등이 포함된 비정상 field명으로 selector 문법이 깨지는 것을 방지한다.
     */
    const findExactAttribute = (root, selector, attribute, value) =>
        Array.from(root.querySelectorAll(selector))
            .find(element => element.getAttribute(attribute) === value);

    /** 한 DOM 범위에서 data-field → id → name 우선순위로 검증 대상 필드를 찾는다. */
    const findValidationField = (root, field) =>
        findExactAttribute(root, '[data-field]', 'data-field', field)
        || findExactAttribute(root, '[id]', 'id', field)
        || findExactAttribute(root, '[name]', 'name', field);

    /**
     * 서버 검증 오류 field를 실제 입력 요소로 해석한다.
     * 화면 뒤쪽에 같은 name이 있어도 사용자가 작업 중인 열린 모달을 가장 먼저 탐색한다.
     */
    const resolveValidationField = (field) => {
        const activeModal = document.querySelector('.modal.show');
        return (activeModal && findValidationField(activeModal, field))
            || findExactAttribute(document, '[data-field]', 'data-field', field)
            || document.getElementById(field)
            || findExactAttribute(document, '[name]', 'name', field);
    };

    /* 필드별 오류 해제 listener를 기억해 같은 서버 오류가 반복돼도 이벤트 중첩을 막는다. */
    const invalidClearListeners = new WeakMap();

    /** 기존 input/change 오류 해제 listener를 제거하고 WeakMap 상태도 정리한다. */
    const removeInvalidClearListeners = (fieldEl) => {
        const listener = invalidClearListeners.get(fieldEl);
        if (!listener) return;
        fieldEl.removeEventListener('input', listener);
        fieldEl.removeEventListener('change', listener);
        invalidClearListeners.delete(fieldEl);
    };

    /**
     * 필드에 Bootstrap/CoreUI `is-invalid`를 표시하고 사용자의 다음 입력/변경 시 한 번 해제한다.
     * @param {Element} fieldEl 서버 오류와 연결된 form control
     */
    const markFieldInvalid = (fieldEl) => {
        removeInvalidClearListeners(fieldEl);
        fieldEl.classList.add('is-invalid');
        const clearInvalid = () => {
            fieldEl.classList.remove('is-invalid');
            removeInvalidClearListeners(fieldEl);
        };
        invalidClearListeners.set(fieldEl, clearInvalid);
        fieldEl.addEventListener('input', clearInvalid);
        fieldEl.addEventListener('change', clearInvalid);
    };

    /* --------------------------------------------------------------------------
     * Axios 응답 인터셉터
     * -------------------------------------------------------------------------- */
    axios.interceptors.response.use(
        response => {
            hideSpinner();

            if (isSessionExpiredResponse(response)) {
                redirectToLogin();
                return haltChain();
            }

            /* axios response 객체는 유지하고 ApiResponse의 실제 payload만 response.data로 언래핑한다. */
            if (response.data && response.data.code !== undefined) {
                if (response.data.code === 200) {
                    response.data = response.data.data;
                } else {
                    /* HTTP 200 비즈니스 오류가 성공 then으로 진행되지 않도록 reject한다. */
                    const notify = window.Notify;
                    if (notify) notify.alert(response.data.message, '오류');
                    return Promise.reject(new Error(response.data.message));
                }
            }
            return response;
        },
        error => {
            hideSpinner();

            if (error.response
                && (error.response.status === 401 || error.response.status === 403)
                && redirectToLogin()) {
                return haltChain();
            }

            const notify = window.Notify;
            if (error.response && error.response.data) {
                const apiErr = error.response.data;
                let displayMsg = apiErr.message || '오류가 발생했습니다.';

                /* @Valid 오류 메시지를 한 알림으로 합치고 대응하는 form control을 강조한다. */
                if (apiErr.errors && Array.isArray(apiErr.errors) && apiErr.errors.length > 0) {
                    const errorList = apiErr.errors.map(e => `• ${e.message}`).join('\n');
                    displayMsg = `${apiErr.message}\n\n${errorList}`;

                    apiErr.errors.forEach(e => {
                        const fieldEl = resolveValidationField(e.field);
                        if (fieldEl) {
                            markFieldInvalid(fieldEl);
                        }
                    });
                }

                if (notify) notify.alert(displayMsg, '오류');
            } else if (notify) {
                notify.alert('서버와 통신 중 알 수 없는 오류가 발생했습니다.', '시스템 오류');
            }
            return Promise.reject(error);
        }
    );

    /* --------------------------------------------------------------------------
     * HTTP 호출 공개 API
     * -------------------------------------------------------------------------- */
    /** GET query params와 추가 axios config를 병합하고 언래핑된 payload를 반환한다. */
    const get = async (url, params = {}, config = {}) => {
        const response = await axios.get(url, Object.assign({}, config, { params }));
        return response.data;
    };

    /** POST body를 전송하고 언래핑된 payload를 반환한다. */
    const post = async (url, data = {}, config = {}) => {
        const response = await axios.post(url, data, config);
        return response.data;
    };

    /** PUT body를 전송하고 언래핑된 payload를 반환한다. */
    const put = async (url, data = {}, config = {}) => {
        const response = await axios.put(url, data, config);
        return response.data;
    };

    /** HTTP DELETE를 호출한다. `delete` 예약어와 구분하기 위해 내부 이름은 deleteFn을 사용한다. */
    const deleteFn = async (url, config = {}) => {
        const response = await axios.delete(url, config);
        return response.data;
    };

    /**
     * 기존 api-client.js의 삭제 호출 호환 메서드.
     * 스캐폴드 delete endpoint 계약에 맞춰 HTTP DELETE가 아니라 POST + query params를 사용한다.
     */
    const remove = async (url, params = {}, config = {}) => {
        const response = await axios.post(url, null, Object.assign({}, config, { params }));
        return response.data;
    };

    const HttpClient = { get, post, put, delete: deleteFn, remove };

    window.HttpClient = HttpClient;
    /* 제거된 api-client.js를 사용하던 화면의 전역 이름을 유지한다. */
    window.ApiClient = HttpClient;
})();
