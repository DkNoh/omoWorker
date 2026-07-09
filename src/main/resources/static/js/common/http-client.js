/**
 * http-client.js
 * 전역 HTTP 인프라 — axios interceptors + spinner + 세션 만료 처리 + 얇은 호출 래퍼
 * common-utils.js에서 분리됨 (Phase 1)
 *
 * 노출: window.HttpClient = { get, post, put, delete, remove }
 * 호환: window.ApiClient = HttpClient (기존 api-client.js 제거 대체)
 *
 * 주의: Notify 모듈이 먼저 로드되어 있어야 에러 알림이 동작한다.
 */
(function () {
    'use strict';

    if (typeof axios === 'undefined') {
        console.error('[http-client] axios가 로드되지 않았습니다.');
        return;
    }

    // ════════════════════════════════════════════════════
    // 글로벌 스피너 — ajaxCount 기반 다중 요청 추적
    // ════════════════════════════════════════════════════
    let ajaxCount = 0;

    const showSpinner = () => {
        ajaxCount++;
        const overlay = document.getElementById('global-spinner-overlay');
        if (overlay) overlay.classList.add('active');
    };

    const hideSpinner = () => {
        ajaxCount--;
        if (ajaxCount <= 0) {
            ajaxCount = 0;
            const overlay = document.getElementById('global-spinner-overlay');
            if (overlay) overlay.classList.remove('active');
        }
    };

    document.addEventListener('DOMContentLoaded', () => {
        if (!document.getElementById('global-spinner-overlay')) {
            const overlay = document.createElement('div');
            overlay.id = 'global-spinner-overlay';
            overlay.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';
            document.body.appendChild(overlay);
        }
    });

    // ════════════════════════════════════════════════════
    // 세션 만료 감지/전환
    //
    // SecurityConfig는 formLogin(loginPage="/login")만 두고 커스텀 EntryPoint가 없어,
    // 미인증(세션 만료) 요청을 Spring Security가 /login 으로 302 리다이렉트한다.
    // axios는 이를 투명하게 따라가 "로그인 페이지 HTML(200, text/html)"을 받으므로,
    // API 호출(JSON 예상)이 HTML을 받으면 세션 만료로 판정해 로그인 페이지로 전환한다.
    // ════════════════════════════════════════════════════
    const LOGIN_MARKERS = /login-shell|login-form|SMS V3 로그인/;
    const SESSION_REDIRECTING = { done: false };

    const isSessionExpiredResponse = (response) => {
        if (!response) return false;
        const ct = (response.headers && response.headers['content-type']) || '';
        if (ct.indexOf('text/html') !== -1) return true;
        return typeof response.data === 'string' && LOGIN_MARKERS.test(response.data);
    };

    const redirectToLogin = () => {
        const onLogin = location.pathname.replace(/\/+$/, '').endsWith('/login');
        if (onLogin || SESSION_REDIRECTING.done) return false;
        SESSION_REDIRECTING.done = true;
        console.warn('[session] 만료 감지 — /login 으로 전환');
        location.replace('/login');
        return true;
    };

    const haltChain = () => new Promise(() => {});

    // ════════════════════════════════════════════════════
    // 전역 Axios 요청 인터셉터 — spinner on + CSRF 헤더
    // ════════════════════════════════════════════════════
    axios.interceptors.request.use(
        config => {
            showSpinner();
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

    // ════════════════════════════════════════════════════
    // 전역 Axios 응답 인터셉터 — ApiResponse 언래핑 + 글로벌 예외 처리
    // ════════════════════════════════════════════════════
    axios.interceptors.response.use(
        response => {
            hideSpinner();

            if (isSessionExpiredResponse(response)) {
                redirectToLogin();
                return haltChain();
            }

            // ApiResponse 규격(code 필드 존재)인 경우 알맹이(data)만 덮어씀
            if (response.data && response.data.code !== undefined) {
                if (response.data.code === 200) {
                    response.data = response.data.data;
                } else {
                    // HTTP 200이지만 비즈니스 에러
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

                // @Valid 실패 시 필드별 에러(errors[]) 목록 포맷팅
                if (apiErr.errors && Array.isArray(apiErr.errors) && apiErr.errors.length > 0) {
                    const errorList = apiErr.errors.map(e => `• ${e.message}`).join('\n');
                    displayMsg = `${apiErr.message}\n\n${errorList}`;

                    apiErr.errors.forEach(e => {
                        const fieldEl = document.querySelector(`[data-field="${e.field}"]`)
                            || document.querySelector(`#${e.field}`)
                            || document.querySelector(`[name="${e.field}"]`);
                        if (fieldEl) {
                            fieldEl.classList.add('is-invalid');
                            const clearInvalid = () => fieldEl.classList.remove('is-invalid');
                            fieldEl.addEventListener('input', clearInvalid, { once: true });
                            fieldEl.addEventListener('change', clearInvalid, { once: true });
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

    // ════════════════════════════════════════════════════
    // 얇은 호출 래퍼 — response.data를 바로 반환 (인터셉터가 이미 언래핑)
    // ════════════════════════════════════════════════════
    const get = async (url, params = {}, config = {}) => {
        const response = await axios.get(url, Object.assign({}, config, { params }));
        return response.data;
    };

    const post = async (url, data = {}, config = {}) => {
        const response = await axios.post(url, data, config);
        return response.data;
    };

    const put = async (url, data = {}, config = {}) => {
        const response = await axios.put(url, data, config);
        return response.data;
    };

    const deleteFn = async (url, config = {}) => {
        const response = await axios.delete(url, config);
        return response.data;
    };

    // remove는 기존 api-client.js 호환 — POST + params (스캐폴드 delete 엔드포인트가 POST 기반)
    const remove = async (url, params = {}, config = {}) => {
        const response = await axios.post(url, null, Object.assign({}, config, { params }));
        return response.data;
    };

    const HttpClient = { get, post, put, delete: deleteFn, remove };

    window.HttpClient = HttpClient;
    // 기존 api-client.js 호환
    window.ApiClient = HttpClient;
})();
