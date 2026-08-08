/**
 * @fileoverview notify.js — 화면 공통 toast, alert, confirm 및 아이콘 갱신 모듈.
 *
 * alert/confirm은 FIFO queue 기반으로 순차 처리한다.
 * common-utils.js에서 분리됨 (Phase 1)
 *
 * 의존: SweetAlert2 (window.Swal), CoreUI 또는 Bootstrap 전역 (showModal/hideModal 도우미)
 * 노출: window.Notify = { toast, alert, confirm, refreshIcons, showModal, hideModal }
 *
 * 하위 호환 — common-utils.js가 로드된 경우 CommonUtils.toast/alert/confirm이 Notify로 연결된다.
 *
 * 공개 API 호출 형태:
 *   - Notify.toast(message, type)
 *   - Notify.alert(message, title, callback)
 *   - Notify.confirm(message, onConfirm, title, onCancel)
 *   - Notify.refreshIcons(), showModal(), hideModal(), getFrameworkModal()
 *
 * 처리 계약:
 *   - alert/confirm은 하나의 FIFO queue로 직렬화해 팝업이 겹치지 않게 한다.
 *   - 메시지는 SweetAlert2의 text 옵션으로 전달해 HTML로 해석하지 않는다.
 *   - SweetAlert2가 없으면 보안상 native confirm으로 우회하지 않고 confirm의 onCancel만 실행한다.
 */
(function () {
    'use strict';

    /* --------------------------------------------------------------------------
     * Lucide 아이콘
     * -------------------------------------------------------------------------- */
    /** 현재 DOM의 모든 `[data-lucide]` 요소를 프로젝트 로컬 SVG 아이콘으로 변환한다. */
    const refreshIcons = () => {
        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    };

    /* 정적 화면의 메뉴와 버튼 아이콘을 최초 DOM 구성 직후 렌더링한다. */
    document.addEventListener('DOMContentLoaded', refreshIcons);

    /* --------------------------------------------------------------------------
     * Framework Modal 호환 API
     * -------------------------------------------------------------------------- */
    /** CoreUI 우선, Bootstrap 호환 순서로 기존/신규 Modal 인스턴스를 반환한다. */
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

    /**
     * framework가 있으면 공식 show API를 사용하고, 없으면 최소 DOM/class 상태로 표시한다.
     * @param {Element} el modal root
     */
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

    /** framework가 있으면 공식 hide API를 사용하고, 없으면 fallback 표시 상태를 원복한다. */
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

    /* --------------------------------------------------------------------------
     * Alert/Confirm FIFO Queue
     * -------------------------------------------------------------------------- */
    const _queue = [];
    let _showing = false;

    /** queue가 비어 있지 않고 표시 중이 아닐 때 다음 알림 하나를 꺼내 렌더링한다. */
    const _processQueue = () => {
        if (_showing || _queue.length === 0) return;
        const item = _queue.shift();
        _showing = true;
        _render(item);
    };

    /**
     * queue item 하나를 SweetAlert2에 표시하고 사용자 선택에 맞는 callback을 안전하게 호출한다.
     * callback이 Promise를 반환하면 settle될 때까지 기다린 뒤 다음 queue item으로 이동한다.
     */
    const _render = ({ type, msg, title, onConfirm, onCancel }) => {
        /* SweetAlert2가 없으면 native confirm으로 우회하지 않고 fail-closed 처리한다. */
        if (!window.Swal || typeof window.Swal.fire !== 'function') {
            _showing = false;
            if (type === 'confirm' && typeof onCancel === 'function') {
                try { onCancel(); } catch (e) { console.error('[Notify] onCancel error:', e); }
            }
            _processQueue();
            return;
        }

        const isConfirm = type === 'confirm';

        /* callback의 동기 예외와 비동기 reject를 기록하고 queue 진행은 유지한다. */
        const safeInvoke = (cb) => {
            if (typeof cb !== 'function') return Promise.resolve();
            try {
                const result = cb();
                if (result && typeof result.then === 'function') {
                    return result.catch(e => console.error('[Notify] callback rejected:', e));
                }
                return Promise.resolve();
            } catch (e) {
                console.error('[Notify] callback threw:', e);
                return Promise.resolve();
            }
        };

        Swal.fire({
            title: title || '알림',
            text: msg || '',
            /* 프로젝트 공통 팝업은 별도 상태 아이콘 없이 텍스트 중심으로 표시한다. */
            icon: undefined,
            showCancelButton: isConfirm,
            confirmButtonText: '확인',
            cancelButtonText: '취소',
            buttonsStyling: false,
            allowOutsideClick: true,
            allowEscapeKey: true,
            returnFocus: true,
            showCloseButton: false,
            /* 버튼 기본 styling 대신 admin-ui-bridge.css의 프로젝트 token class를 사용한다. */
            customClass: {
                container: 'sms-swal-container',
                popup: 'sms-swal-popup',
                title: 'sms-swal-title',
                htmlContainer: 'sms-swal-message',
                actions: 'sms-swal-actions',
                confirmButton: 'sms-swal-confirm',
                cancelButton: 'sms-swal-cancel'
            }
        }).then((result) => {
            if (result.isConfirmed) {
                /* alert callback과 confirm onConfirm은 확인 동작에서 실행한다. */
                return safeInvoke(onConfirm);
            }
            /* 취소, backdrop, ESC dismiss는 confirm에 한해 onCancel을 실행한다. */
            if (isConfirm) {
                return safeInvoke(onCancel);
            }
            return Promise.resolve();
        }).finally(() => {
            _showing = false;
            _processQueue();
        });
    };

    /** 공개 wrapper에서 받은 인자를 queue item으로 정규화하고 처리기를 깨운다. */
    const _enqueue = (type, msg, title, onConfirm, onCancel) => {
        _queue.push({ type, msg, title, onConfirm, onCancel });
        _processQueue();
    };

    /* --------------------------------------------------------------------------
     * Toast
     * -------------------------------------------------------------------------- */
    /**
     * 우측 상단에 자동으로 사라지는 비차단 toast를 표시한다.
     * @param {string} msg 표시할 메시지
     * @param {'info'|'success'|'error'|'warning'} [type='info'] 배경색과 아이콘을 정하는 종류
     * @returns {void}
     */
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

        /* CoreUI, Bootstrap, 수동 class toggle 순으로 사용 가능한 구현을 선택한다. */
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

    /* --------------------------------------------------------------------------
     * 공개 API
     * -------------------------------------------------------------------------- */
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
