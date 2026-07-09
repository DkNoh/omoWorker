/**
 * modal-manager.js
 * 개발자 수동 모달 lifecycle 관리자 — 폼/검증 훅과 CoreUI Modal 인스턴스 중앙화
 * Phase 1 신규 도입
 *
 * DOM id 계약 (fragments/modal-base.html과 일치):
 *   - 모달 컨테이너: id="${modalId}"
 *   - 제목:        id="${modalId}-title"
 *   - 저장 버튼:   id="${modalId}-btn-save"
 *   - 삭제 버튼:   id="${modalId}-btn-delete"
 *
 * 의존: window.Notify (CoreUI Modal 인스턴스 도우미 + JustValidate는 옵션)
 * 노출: window.ModalManager = { init, open, close }
 */
(function () {
    'use strict';

    const instances = {};   // modalId -> CoreUI Modal 인스턴스
    const hooks = {};       // modalId -> { onMount, beforeOpen, onOpen, onSubmit, onDelete, onClose }
    const validators = {};  // modalId -> JustValidate 인스턴스 (메모리 누수 방지용 저장소)

    const resolveEl = (modalId) => document.querySelector('#' + modalId);

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
     * 모달 초기화 — Lifecycle 훅 연결 + 저장/삭제 버튼 바인딩
     * @param {string} modalId            모달 DOM id
     * @param {object} [options]
     * @param {function} [options.onMount]       init 직후 1회 호출
     * @param {function} [options.beforeOpen]    show.coreui.modal 이전. false 반환 시 열기 취소
     * @param {function} [options.onOpen]        shown.coreui.modal 이후
     * @param {function} [options.onSubmit]     저장 버튼 클릭 시 (검증 통과 후)
     * @param {function} [options.onDelete]     삭제 버튼 클릭 시
     * @param {function} [options.onClose]      hidden.coreui.modal 이후
     * @param {function} [options.validateRules] JustValidate 사용 시 validator => { ... } 형태로 룰 등록
     * @returns {object|null} CoreUI Modal 인스턴스 (없으면 null)
     */
    const init = (modalId, options = {}) => {
        const modalEl = resolveEl(modalId);
        if (!modalEl) return null;

        hooks[modalId] = {
            onMount: options.onMount || (() => {}),
            beforeOpen: options.beforeOpen || (() => true),
            onOpen: options.onOpen || (() => {}),
            onSubmit: options.onSubmit || (() => {}),
            onDelete: options.onDelete || (() => {}),
            onClose: options.onClose || (() => {})
        };

        const instance = getFrameworkModal(modalEl);
        if (instance) {
            instances[modalId] = instance;
        }

        modalEl.addEventListener('show.coreui.modal', (e) => {
            if (hooks[modalId].beforeOpen(e) === false) {
                e.preventDefault();
            }
        });

        modalEl.addEventListener('shown.coreui.modal', () => {
            const formEl = modalEl.querySelector('form');
            if (formEl && options.validateRules && window.JustValidate) {
                // 모달이 열릴 때마다 폼을 기준으로 Validator 새로 바인딩 (JustValidate 메모리 누수 방지)
                validators[modalId] = new JustValidate(formEl, {
                    errorFieldCssClass: 'is-invalid',
                    errorLabelCssClass: 'invalid-feedback',
                    focusInvalidField: true
                });
                options.validateRules(validators[modalId]);
            }
            hooks[modalId].onOpen();
        });

        modalEl.addEventListener('hidden.coreui.modal', () => {
            hooks[modalId].onClose();
            destroyForm(modalId, modalEl);
        });

        const saveBtn = modalEl.querySelector('#' + modalId + '-btn-save');
        if (saveBtn) {
            saveBtn.addEventListener('click', () => {
                const validator = validators[modalId];
                if (validator) {
                    // 폼 바깥 버튼이므로 검증을 수동으로 강제 실행
                    validator.revalidate().then(isValid => {
                        if (isValid) hooks[modalId].onSubmit();
                    });
                } else {
                    hooks[modalId].onSubmit();
                }
            });
        }

        const deleteBtn = modalEl.querySelector('#' + modalId + '-btn-delete');
        if (deleteBtn) {
            deleteBtn.addEventListener('click', () => hooks[modalId].onDelete());
        }

        hooks[modalId].onMount();
        return instance;
    };

    const open = (modalId) => {
        const inst = instances[modalId] || (resolveEl(modalId) && getFrameworkModal(resolveEl(modalId)));
        if (inst) inst.show();
    };

    const close = (modalId) => {
        const inst = instances[modalId] || (resolveEl(modalId) && getFrameworkModal(resolveEl(modalId)));
        if (inst) inst.hide();
    };

    // 폼 리셋 + JustValidate 인스턴스 파괴 (메모리 누수 방지)
    const destroyForm = (modalId, modalEl) => {
        const formEl = modalEl.querySelector('form');
        if (formEl) formEl.reset();

        const validator = validators[modalId];
        if (validator) {
            if (typeof validator.destroy === 'function') validator.destroy();
            delete validators[modalId];
        }
    };

    window.ModalManager = { init, open, close };
})();
