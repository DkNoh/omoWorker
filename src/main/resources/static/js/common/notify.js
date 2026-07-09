/**
 * notify.js
 * 화면 공통 알림 모듈 — toast / alert / confirm (큐 기반 순차 처리)
 * common-utils.js에서 분리됨 (Phase 1)
 *
 * 의존: CoreUI 또는 Bootstrap 전역, lucide
 * 노출: window.Notify = { toast, alert, confirm, refreshIcons, showModal, hideModal }
 *
 * 하위 호환 — common-utils.js가 로드된 경우 CommonUtils.toast/alert/confirm이 Notify로 연결된다.
 */
(function () {
    'use strict';

    // ════════════════════════════════════════════════════
    // Lucide 아이콘 갱신 (전역 도우미)
    // ════════════════════════════════════════════════════
    const refreshIcons = () => {
        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    };

    // ════════════════════════════════════════════════════
    // Framework Modal 도우미 — CoreUI 우선, Bootstrap 5 호환 폴백
    // ════════════════════════════════════════════════════
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

    const showModal = (el) => {
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

    const hideModal = (el) => {
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

    // ════════════════════════════════════════════════════
    // 커스텀 알림/확인 모달 DOM 초기화
    // ════════════════════════════════════════════════════
    document.addEventListener('DOMContentLoaded', () => {
        if (!document.getElementById('custom-modal-overlay')) {
            const overlay = document.createElement('div');
            overlay.id = 'custom-modal-overlay';
            overlay.className = 'modal fade';
            overlay.tabIndex = -1;
            overlay.setAttribute('aria-hidden', 'true');
            overlay.setAttribute('aria-labelledby', 'custom-modal-title');
            overlay.innerHTML = `
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
            document.body.appendChild(overlay);
        }
        refreshIcons();
    });

    // ════════════════════════════════════════════════════
    // 큐 기반 모달 처리 — 동시 다중 호출 시 순차 표시
    // ════════════════════════════════════════════════════
    const _queue = [];
    let _showing = false;

    const _processQueue = () => {
        if (_showing || _queue.length === 0) return;
        const item = _queue.shift();
        _showing = true;
        _render(item);
    };

    const _render = ({ type, msg, title, onConfirm, onCancel }) => {
        const overlay = document.getElementById('custom-modal-overlay');
        if (!overlay) {
            _showing = false;
            window.alert(msg);
            _processQueue();
            return;
        }

        const titleEl = document.querySelector('#custom-modal-title');
        const msgEl = document.querySelector('#custom-modal-msg');
        const cancelBtn = document.querySelector('#custom-modal-btn-cancel');
        const confirmBtn = document.querySelector('#custom-modal-btn-confirm');

        titleEl.textContent = title || '알림';
        msgEl.textContent = msg || '';

        if (type === 'confirm') {
            cancelBtn.classList.remove('d-none');
        } else {
            cancelBtn.classList.add('d-none');
        }

        // 기존 리스너 제거용 clone
        const newConfirm = confirmBtn.cloneNode(true);
        confirmBtn.parentNode.replaceChild(newConfirm, confirmBtn);
        const newCancel = cancelBtn.cloneNode(true);
        cancelBtn.parentNode.replaceChild(newCancel, cancelBtn);

        const closeAndNext = (callback) => {
            hideModal(overlay);
            _showing = false;
            if (typeof callback === 'function') callback();
            _processQueue();
        };

        newConfirm.addEventListener('click', () => closeAndNext(onConfirm));
        newCancel.addEventListener('click', () => closeAndNext(onCancel));

        // ESC 닫기
        const escHandler = (e) => {
            if (e.key === 'Escape') {
                e.stopPropagation();
                document.removeEventListener('keydown', escHandler);
                closeAndNext(onCancel);
            }
        };
        document.addEventListener('keydown', escHandler);

        showModal(overlay);
        refreshIcons();

        // 모달이 열릴 때 확인 버튼에 포커스 — 엔터/스페이스로 바로 닫기 가능
        setTimeout(() => {
            newConfirm.focus({ preventScroll: true });
        }, 50);
    };

    const _enqueue = (type, msg, title, onConfirm, onCancel) => {
        _queue.push({ type, msg, title, onConfirm, onCancel });
        _processQueue();
    };

    // ════════════════════════════════════════════════════
    // Toast — 우측 상단 팝업
    // ════════════════════════════════════════════════════
    const toast = (msg, type = 'info') => {
        let container = document.querySelector('#toast-container');
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
        toastEl.innerHTML = `
            <div class="d-flex">
                <div class="toast-body fw-bold d-flex align-items-center">
                    <i data-lucide="${icon}" class="toast-icon"></i><span>${msg}</span>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-coreui-dismiss="toast" aria-label="Close"></button>
            </div>
        `;

        container.appendChild(toastEl);
        refreshIcons();

        if (typeof coreui !== 'undefined' && coreui.Toast) {
            const instance = new coreui.Toast(toastEl, { delay: 3000 });
            instance.show();
            toastEl.addEventListener('hidden.coreui.toast', () => toastEl.remove());
        } else if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
            const instance = new bootstrap.Toast(toastEl, { delay: 3000 });
            instance.show();
            toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
        } else {
            toastEl.classList.add('show');
            setTimeout(() => {
                toastEl.classList.remove('show');
                setTimeout(() => toastEl.remove(), 300);
            }, 3000);
        }
    };

    // ════════════════════════════════════════════════════
    // 공개 API
    // ════════════════════════════════════════════════════
    const Notify = {
        toast,
        alert: (msg, title, callback) => _enqueue('alert', msg, title, callback),
        confirm: (msg, callback, title, onCancel) => _enqueue('confirm', msg, title, callback, onCancel),
        refreshIcons,
        showModal,
        hideModal,
        getFrameworkModal
    };

    window.Notify = Notify;
})();
