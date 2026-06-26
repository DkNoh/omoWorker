/**
 * common-utils.js
 * 시스템 전역 공통 유틸리티 모듈 (그리드 외 UI 렌더링, 공통 코드, 모달 등)
 */

// ════════════════════════════════════════════════════
// 글로벌 로딩 스피너 및 커스텀 모달 DOM 초기화
// ════════════════════════════════════════════════════
let ajaxCount = 0;
const refreshLucideIcons = () => {
    if (window.lucide && typeof window.lucide.createIcons === 'function') {
        window.lucide.createIcons();
    }
};

const getFrameworkModal = (el) => {
    if (!el) return null;
    if (window.coreui && window.coreui.Modal) {
        return window.coreui.Modal.getOrCreateInstance(el);
    }
    if (window.bootstrap && window.bootstrap.Modal) {
        return window.bootstrap.Modal.getOrCreateInstance(el);
    }
    return null;
};

const showModalElement = (el) => {
    const instance = getFrameworkModal(el);
    if (instance) {
        instance.show();
        return;
    }
    el.style.display = 'block';
    el.removeAttribute('aria-hidden');
    el.setAttribute('aria-modal', 'true');
    el.classList.add('show');
    document.body.classList.add('modal-open');
};

const hideModalElement = (el) => {
    const instance = getFrameworkModal(el);
    if (instance) {
        instance.hide();
        return;
    }
    el.classList.remove('show');
    el.setAttribute('aria-hidden', 'true');
    el.removeAttribute('aria-modal');
    el.style.display = 'none';
    document.body.classList.remove('modal-open');
};

document.addEventListener("DOMContentLoaded", () => {
    // 스피너 초기화
    if (!document.getElementById('global-spinner-overlay')) {
        const overlay = document.createElement('div');
        overlay.id = 'global-spinner-overlay';
        overlay.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';
        document.body.appendChild(overlay);
    }

    // 커스텀 모달 초기화
    if (!document.getElementById('custom-modal-overlay')) {
        const modalOverlay = document.createElement('div');
        modalOverlay.id = 'custom-modal-overlay';
        modalOverlay.className = 'modal fade';
        modalOverlay.tabIndex = -1;
        modalOverlay.setAttribute('aria-hidden', 'true');
        modalOverlay.setAttribute('aria-labelledby', 'custom-modal-title');
        modalOverlay.innerHTML = `
            <div class="modal-dialog modal-dialog-centered modal-sm">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="custom-modal-title">알림</h5>
                    </div>
                    <div class="modal-body text-center" id="custom-modal-msg" style="white-space: pre-line;"></div>
                    <div class="modal-footer justify-content-center">
                        <button type="button" class="btn btn-outline-secondary d-none" id="custom-modal-btn-cancel">
                            <i data-lucide="x"></i><span>취소</span>
                        </button>
                        <button type="button" class="btn btn-primary" id="custom-modal-btn-confirm">
                            <i data-lucide="check"></i><span>확인</span>
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modalOverlay);
    }
    refreshLucideIcons();
});

// ════════════════════════════════════════════════════
// 커스텀 알림/확인 모달 제어 로직 (큐 기반: 동시 다중 호출 시 순차 표시)
// ════════════════════════════════════════════════════
const _modalQueue = [];
let _isModalShowing = false;

const _processModalQueue = () => {
    if (_isModalShowing || _modalQueue.length === 0) return;
    const item = _modalQueue.shift();
    _isModalShowing = true;
    _renderCustomModal(item);
};

const _showCustomModal = (type, msg, title, onConfirm, onCancel) => {
    _modalQueue.push({ type, msg, title, onConfirm, onCancel });
    _processModalQueue();
};

const _renderCustomModal = ({ type, msg, title, onConfirm, onCancel }) => {
    const overlay = document.getElementById('custom-modal-overlay');
    if (!overlay) {
        _isModalShowing = false;
        alert(msg);
        _processModalQueue();
        return;
    }

    const titleEl = document.getElementById('custom-modal-title');
    const msgEl = document.getElementById('custom-modal-msg');
    const cancelBtn = document.getElementById('custom-modal-btn-cancel');
    const confirmBtn = document.getElementById('custom-modal-btn-confirm');

    titleEl.textContent = title || '알림';
    msgEl.textContent = msg || '';

    if (type === 'confirm') {
        cancelBtn.classList.remove('d-none');
    } else {
        cancelBtn.classList.add('d-none');
    }

    // 기존 이벤트 리스너 제거용 clone
    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);

    const _closeAndNext = (callback) => {
        hideModalElement(overlay);
        _isModalShowing = false;
        if (callback) callback();
        _processModalQueue();
    };

    newConfirmBtn.addEventListener('click', () => _closeAndNext(onConfirm));
    newCancelBtn.addEventListener('click', () => _closeAndNext(onCancel));

    // ESC 키 닫기
    const _escHandler = (e) => {
        if (e.key === 'Escape') {
            e.stopPropagation();
            document.removeEventListener('keydown', _escHandler);
            _closeAndNext(onCancel);
        }
    };
    document.addEventListener('keydown', _escHandler);

    showModalElement(overlay);
    refreshLucideIcons();

    // 모달이 열릴 때 확인 버튼에 포커스를 강제로 주어 엔터/스페이스로 바로 닫을 수 있게 처리
    setTimeout(() => {
        newConfirmBtn.focus({ preventScroll: true });
    }, 50);
};

// ════════════════════════════════════════════════════
// Bootstrap 5 / CoreUI Toast 알림 (우측 상단 팝업)
// ════════════════════════════════════════════════════
const _showToast = (msg, type = 'info') => {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }

    let bgClass = 'bg-primary';
    let icon = 'info';
    if (type === 'success') { bgClass = 'bg-success'; icon = 'circle-check'; }
    if (type === 'error') { bgClass = 'bg-danger'; icon = 'circle-x'; }
    if (type === 'warning') { bgClass = 'bg-warning text-dark'; icon = 'triangle-alert'; }

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white ${bgClass} border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    // 닫기 버튼 렌더링
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body fw-bold d-flex align-items-center">
                <i data-lucide="${icon}" class="toast-icon"></i><span>${msg}</span>
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-coreui-dismiss="toast" aria-label="Close"></button>
        </div>
    `;

    container.appendChild(toastEl);
    refreshLucideIcons();

    // CoreUI (또는 Bootstrap) Toast API 사용
    if (typeof coreui !== 'undefined' && coreui.Toast) {
        const toast = new coreui.Toast(toastEl, { delay: 3000 });
        toast.show();
        toastEl.addEventListener('hidden.coreui.toast', () => toastEl.remove());
    } else if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
        toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
    } else {
        // Fallback: 프레임워크가 로드되지 않은 경우 CSS 강제 표시
        toastEl.classList.add('show');
        setTimeout(() => {
            toastEl.classList.remove('show');
            setTimeout(() => toastEl.remove(), 300);
        }, 3000);
    }
};

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

// 세션 만료 감지/전환.
// SecurityConfig는 formLogin(loginPage="/login")만 두고 커스텀 EntryPoint가 없어,
// 미인증(세션 만료) 요청을 Spring Security가 /login 으로 302 리다이렉트한다.
// axios는 이를 투명하게 따라가 "로그인 페이지 HTML(200, text/html)"을 받으므로,
// API 호출(JSON 예상)이 HTML을 받으면 세션 만료로 판정해 로그인 페이지로 전환한다.
const _LOGIN_MARKERS = /login-shell|login-form|SMS V3 로그인/;
const _SESSION_REDIRECTING = { done: false };

const _isSessionExpiredResponse = (response) => {
    if (!response) return false;
    const ct = (response.headers && response.headers['content-type']) || '';
    if (ct.indexOf('text/html') !== -1) return true;
    return typeof response.data === 'string' && _LOGIN_MARKERS.test(response.data);
};

const _redirectToLogin = () => {
    const onLogin = location.pathname.replace(/\/+$/, '').endsWith('/login');
    if (onLogin || _SESSION_REDIRECTING.done) return false;
    _SESSION_REDIRECTING.done = true;
    console.warn('[session] 만료 감지 — /login 으로 전환');
    location.replace('/login');
    return true;
};

const _haltChain = () => new Promise(() => {});

// 전역 Axios 요청 인터셉터 (로딩 on)
axios.interceptors.request.use(
    config => {
        showSpinner();
        // CSRF: 서버 csrf 활성화 대응. <meta>의 토큰을 요청 헤더로 싣는다 (GET 등 비변경 요청은 서버가 무시)
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


// 전역 Axios 인터셉터 (ApiResponse 택배 상자 언래핑 및 글로벌 예외 처리)
axios.interceptors.response.use(
    response => {
        hideSpinner();

        if (_isSessionExpiredResponse(response)) {
            _redirectToLogin();
            return _haltChain();
        }

        // 백엔드에서 온 데이터가 우리가 만든 ApiResponse 규격(code가 존재)인 경우
        if (response.data && response.data.code !== undefined) {
            if (response.data.code === 200) {
                // 성공: 껍데기(ApiResponse)를 까서 알맹이(data)만 response.data에 덮어씌움
                // 이렇게 하면 개별 JS 파일(history.js 등)은 코드를 전혀 수정할 필요가 없음!
                response.data = response.data.data;
            } else {
                // 실패: HTTP 상태 코드는 200이지만, 비즈니스 에러인 경우 (예: code 400)
                _showCustomModal('alert', response.data.message, '오류');
                return Promise.reject(new Error(response.data.message));
            }
        }
        return response;
    },
    error => {
        hideSpinner();

        if (error.response
            && (error.response.status === 401 || error.response.status === 403)
            && _redirectToLogin()) {
            return _haltChain();
        }

        // HTTP 상태 코드가 4xx, 5xx 인 경우 (GlobalExceptionHandler 통과)
        if (error.response && error.response.data) {
            const apiErr = error.response.data;
            let displayMsg = apiErr.message || '오류가 발생했습니다.';

            // @Valid 실패 시 필드별 에러(errors[])가 있으면 목록 형태로 포맷팅
            if (apiErr.errors && Array.isArray(apiErr.errors) && apiErr.errors.length > 0) {
                const errorList = apiErr.errors.map(e => `• ${e.message}`).join('\n');
                displayMsg = `${apiErr.message}\n\n${errorList}`;

                // 각 필드에 에러 스타일 적용 (data-field 속성 기반)
                apiErr.errors.forEach(e => {
                    const fieldEl = document.querySelector(`[data-field="${e.field}"]`)
                        || document.querySelector(`#${e.field}`)
                        || document.querySelector(`[name="${e.field}"]`);
                    if (fieldEl) {
                        fieldEl.classList.add('is-invalid');
                        // 값 변경 시 에러 스타일 제거
                        const _clearInvalid = () => fieldEl.classList.remove('is-invalid');
                        fieldEl.addEventListener('input', _clearInvalid, { once: true });
                        fieldEl.addEventListener('change', _clearInvalid, { once: true });
                    }
                });
            }

            _showCustomModal('alert', displayMsg, '오류');
        } else {
            _showCustomModal('alert', '서버와 통신 중 알 수 없는 오류가 발생했습니다.', '시스템 오류');
        }
        return Promise.reject(error);
    }
);

const CommonUtils = (() => {

    // ════════════════════════════════════════════════════
    //  공통 코드 (콤보박스) 자동 생성
    // ════════════════════════════════════════════════════
    const initCombos = async () => {
        const comboList = document.querySelectorAll('.common-combo');
        if (comboList.length === 0) return;

        // 동일한 코드를 여러 콤보박스에서 요청할 수 있으므로, 중복 요청 방지용 캐시
        const cache = {};

        for (const selectEl of comboList) {
            const type = selectEl.getAttribute('data-code-type');
            if (!type) continue;

            if (!cache[type]) {
                try {
                    const res = await axios.get(`/api/common-code/${type}`);
                    cache[type] = res.data;
                } catch (e) {
                    console.error(`공통 코드 조회 실패 [${type}]`, e);
                    cache[type] = [];
                }
            }

            // 기존 옵션이 '전체' 등으로 세팅되어 있을 수 있으므로 보존하면서 추가
            const existingOptions = selectEl.innerHTML;
            let newOptions = '';
            cache[type].forEach(item => {
                newOptions += `<option value="${item.code}">${item.name}</option>`;
            });
            selectEl.innerHTML = existingOptions + newOptions;
        }
    };


    // ════════════════════════════════════════════════════
    //  날짜 / 시간 검색 폼 초기화 유틸
    // ════════════════════════════════════════════════════
    const setDefaultDateTime = (forceReset = false) => {
        const now = dayjs();
        const todayStr = now.format('YYYY-MM-DD');
        const fromTime = now.subtract(1, 'hour').format('HH:mm'); // 현재 -1시간
        const toTime = now.add(1, 'hour').format('HH:mm');      // 현재 +1시간

        // ① 분리형: #startDate / #startTime / #endDate / #endTime
        const startDate = document.querySelector('#startDate');
        const endDate = document.querySelector('#endDate');
        const startTime = document.querySelector('#startTime');
        const endTime = document.querySelector('#endTime');

        if (startDate && (forceReset || !startDate.value)) startDate.value = todayStr;
        if (endDate && (forceReset || !endDate.value)) endDate.value = todayStr;
        if (startTime && (forceReset || !startTime.value)) startTime.value = fromTime;
        if (endTime && (forceReset || !endTime.value)) endTime.value = toTime;

        // ② 통합형: datetime-local input (#startDateTime / #endDateTime 또는 data-default-date)
        const startDT = document.querySelector('#startDateTime');
        const endDT = document.querySelector('#endDateTime');

        if (startDT && (forceReset || !startDT.value)) startDT.value = `${todayStr}T00:00`;
        if (endDT && (forceReset || !endDT.value)) endDT.value = `${todayStr}T23:59`;

        // ③ 단순 date type input (날짜만, 시간 없음)
        document.querySelectorAll('input[type="date"]').forEach(el => {
            if (forceReset || !el.value) el.value = todayStr;
        });
    };

    const getSearchParams = () => {
        const startDate = document.querySelector('#startDate')?.value || '';
        const startTime = document.querySelector('#startTime')?.value || '00:00';
        const endDate = document.querySelector('#endDate')?.value || '';
        const endTime = document.querySelector('#endTime')?.value || '23:59';

        return {
            startDateTime: startDate ? `${startDate}T${startTime}:00` : '',
            endDateTime: endDate ? `${endDate}T${endTime}:59` : '',
            receiverNo: document.querySelector('#receiverNo')?.value.trim() || '',
            sendType: document.querySelector('#sendType')?.value || '',
        };
    };

    const resetFields = () => {
        const receiverNo = document.querySelector('#receiverNo');
        const sendType = document.querySelector('#sendType');

        if (receiverNo) receiverNo.value = '';
        if (sendType) sendType.value = '';

        setDefaultDateTime(true);
    };


    // ════════════════════════════════════════════════════
    //  모달 처리 로직
    // ════════════════════════════════════════════════════
    const openDetail = (url, fillFn) => {
        axios.get(url)
            .then(res => {
                fillFn(res.data);
                document.querySelector('#detailModal')?.classList.add('open');
            })
            .catch(err => {
                // axios interceptor에서 이미 알림창을 띄우므로 여기서는 별도로 alert 안함
                console.error("모달 데이터 로드 실패", err);
            });
    };

    const closeModal = () =>
        document.querySelector('#detailModal')?.classList.remove('open');

    const closeModalOnOverlay = e => {
        if (e.target.id === 'detailModal') closeModal();
    };

    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

    // ════════════════════════════════════════════════════
    //  데이터 포매터 (Grid 등에서 자주 사용)
    // ════════════════════════════════════════════════════
    const fmt = {
        /**
         * 숫자에 콤마 추가 (10000 -> 10,000)
         */
        money: (val) => {
            if (val == null || val === '') return '0';
            const num = Number(val);
            if (isNaN(num)) return val;
            return num.toLocaleString();
        },
        /**
         * 전화번호 하이픈 자동 추가 (01012345678 -> 010-1234-5678)
         */
        phone: (val) => {
            if (!val) return '';
            const clean = String(val).replace(/[^0-9]/g, '');
            if (clean.length === 9) return clean.replace(/(\d{2})(\d{3})(\d{4})/, '$1-$2-$3');
            if (clean.length === 10) {
                if (clean.startsWith('02')) return clean.replace(/(\d{2})(\d{4})(\d{4})/, '$1-$2-$3');
                return clean.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
            }
            if (clean.length === 11) return clean.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
            return clean;
        }
    };

    // 자동 실행 등록
    document.addEventListener('DOMContentLoaded', () => {
        initCombos();
    });

    // ════════════════════════════════════════════════════
    //  Autocomplete (말풍선 자동완성) 공통 초기화
    //  사용 예)
    //  CommonUtils.initAutocomplete({
    //      inputEl:   '#bankCdText',
    //      balloonEl: '#bankBalloon',
    //      apiUrl:    '/api/common-code/bank',
    //      syncCombo: '#bankCdCombo',   // 선택사항
    //      minLength:  1,
    //      debounceMs: 200,
    //      onSelect: (item) => console.log(item) // 선택사항
    //  });
    // ════════════════════════════════════════════════════
    const initAutocomplete = ({
        inputEl, balloonEl, apiUrl,
        syncCombo = null,
        paramName = 'keyword',
        minLength = 1,
        debounceMs = 200,
        renderItem = null,
        onSelect = null
    } = {}) => {
        const input = typeof inputEl === 'string' ? document.querySelector(inputEl) : inputEl;
        const balloon = typeof balloonEl === 'string' ? document.querySelector(balloonEl) : balloonEl;
        const combo = syncCombo ? (typeof syncCombo === 'string' ? document.querySelector(syncCombo) : syncCombo) : null;

        if (!input || !balloon) {
            console.warn('[Autocomplete] inputEl 또는 balloonEl을 찾을 수 없습니다.', { inputEl, balloonEl });
            return;
        }

        let debounceTimer;

        // 기본 아이템 렌더링
        const _renderItem = renderItem || ((item) =>
            `<div class="autocomplete-item" data-code="${item.code}" data-name="${item.name}">
                <span class="ac-code">${item.code}</span>
                <span class="ac-name">${item.name}</span>
             </div>`
        );

        const _close = () => { balloon.style.display = 'none'; };

        const _select = (code, name) => {
            input.value = code;
            // 코드로 값을 직접 설정하면 브라우저 input 이벤트가 발생하지 않으므로 수동 dispatch
            // → 외부에서 input 이벤트를 감지하는 syncBankCd 등의 핸들러가 정상 동작
            input.dispatchEvent(new Event('input', { bubbles: true }));

            if (combo) {
                combo.value = code;
                combo.dispatchEvent(new Event('change', { bubbles: true }));
            }
            _close();
            if (onSelect) onSelect({ code, name });
        };

        // 콤보 → 텍스트 동기화
        if (combo) {
            combo.addEventListener('change', () => {
                input.value = combo.value;
                _close();
            });
        }

        // 텍스트 입력 → 검색
        input.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            const kw = input.value.trim();

            if (kw.length < minLength) {
                _close();
                if (combo) combo.value = '';
                return;
            }

            debounceTimer = setTimeout(() => {
                axios.get(apiUrl, { params: { [paramName]: kw } })
                    .then(res => {
                        // 전역 인터셉터가 ApiResponse를 언래핑하므로 res.data가 곧 목록이다
                        const list = res.data || [];
                        if (!list.length) {
                            balloon.innerHTML = '<div class="ac-empty">검색 결과 없음</div>';
                            balloon.style.display = 'block';
                            return;
                        }
                        balloon.innerHTML = list.map(_renderItem).join('');
                        balloon.style.display = 'block';

                        balloon.querySelectorAll('.autocomplete-item').forEach(item => {
                            item.addEventListener('click', () =>
                                _select(item.dataset.code, item.dataset.name));
                        });
                    })
                    .catch(() => _close());
            }, debounceMs);
        });

        // 외부 클릭 시 닫기
        document.addEventListener('click', e => {
            if (!input.contains(e.target) && !balloon.contains(e.target)) _close();
        });

        // 컨트롤러 반환 (필요 시 외부에서 제어)
        return { close: _close, select: _select };
    };

    return {
        initCombos,
        initAutocomplete,
        refreshIcons: refreshLucideIcons,
        setDefaultDateTime,
        getSearchParams,
        resetFields,
        openDetail,
        closeModal,
        closeModalOnOverlay,
        fmt,
        toast: (msg, type) => _showToast(msg, type),
        alert: (msg, title, callback) => _showCustomModal('alert', msg, title, callback),
        confirm: (msg, callback, title, onCancel) => _showCustomModal('confirm', msg, title, callback, onCancel)
    };
})();
