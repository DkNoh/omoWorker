/**
 * @fileoverview modal-manager.js — 업무 모달의 lifecycle과 검증 인스턴스 관리자.
 *
 * 폼/검증 hook과 CoreUI Modal 인스턴스를 modalId 기준으로 중앙 관리한다.
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
 *
 * 생명주기 순서:
 *   init → onMount(1회)
 *   open → beforeOpen → onOpen → 저장/삭제 동작 → close → onClose → form/validator 정리
 *
 * 주의:
 *   - 같은 modalId에 init을 반복 호출하면 DOM 이벤트 listener도 반복 등록되므로 화면 초기화 시 1회만 호출한다.
 *   - validateRules는 JustValidate 인스턴스에 field 규칙을 추가하는 함수이며, 실제 저장은 검증 성공 후 실행한다.
 */
(function () {
    'use strict';

    /* modalId를 공통 key로 사용해 framework 인스턴스, 업무 hook, validator를 함께 관리한다. */
    const instances = {};
    const hooks = {};
    const validators = {};

    /** modalId에 대응하는 컨테이너를 찾는다. */
    const resolveEl = (modalId) => document.querySelector('#' + modalId);

    /**
     * 현재 페이지에 로드된 modal framework의 인스턴스를 구한다.
     * CoreUI를 우선하고 Bootstrap 5를 호환 fallback으로 사용한다.
     * @param {Element|null} el modal root
     * @returns {Object|null} framework Modal 인스턴스
     */
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
     * @param {function} [options.onSubmit]      저장 버튼 클릭 시 (검증 통과 후)
     * @param {function} [options.onDelete]      삭제 버튼 클릭 시
     * @param {function} [options.onClose]       hidden.coreui.modal 이후
     * @param {function} [options.validateRules] JustValidate 사용 시 validator => { ... } 형태로 룰 등록
     * @returns {object|null} CoreUI Modal 인스턴스 (없으면 null)
     */
    const init = (modalId, options = {}) => {
        const modalEl = resolveEl(modalId);
        if (!modalEl) return null;

        /* 생략한 hook은 no-op으로 정규화해 이후 생명주기에서 분기 없이 호출한다. */
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

        /* show 이전 hook이 false를 반환하면 framework의 modal open을 취소한다. */
        modalEl.addEventListener('show.coreui.modal', (e) => {
            if (hooks[modalId].beforeOpen(e) === false) {
                e.preventDefault();
            }
        });

        /* 표시 완료 후 현재 form DOM을 기준으로 validator를 만들고 onOpen을 실행한다. */
        modalEl.addEventListener('shown.coreui.modal', () => {
            const formEl = modalEl.querySelector('form');
            if (formEl && options.validateRules && window.JustValidate) {
                /* 열릴 때마다 현재 form 기준 validator를 생성하고 닫힐 때 파괴한다. */
                validators[modalId] = new JustValidate(formEl, {
                    errorFieldCssClass: 'is-invalid',
                    errorLabelCssClass: 'invalid-feedback',
                    focusInvalidField: true
                });
                options.validateRules(validators[modalId]);
            }
            hooks[modalId].onOpen();
        });

        /* onClose가 마지막 form 값을 확인할 수 있도록 hook 실행 후 reset/destroy한다. */
        modalEl.addEventListener('hidden.coreui.modal', () => {
            hooks[modalId].onClose();
            destroyForm(modalId, modalEl);
        });

        const saveBtn = modalEl.querySelector('#' + modalId + '-btn-save');
        if (saveBtn) {
            saveBtn.addEventListener('click', () => {
                const validator = validators[modalId];
                if (validator) {
                    /* 저장 버튼이 form 밖에 있으므로 validation을 명시적으로 실행한다. */
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

    /**
     * 초기화된 모달을 연다. 캐시된 인스턴스가 없으면 DOM에서 framework 인스턴스를 지연 획득한다.
     * @param {string} modalId modal DOM id
     * @returns {void}
     */
    const open = (modalId) => {
        const inst = instances[modalId] || (resolveEl(modalId) && getFrameworkModal(resolveEl(modalId)));
        if (inst) inst.show();
    };

    /**
     * 초기화된 모달을 닫는다. 실제 form 정리는 framework의 hidden 이벤트에서 수행한다.
     * @param {string} modalId modal DOM id
     * @returns {void}
     */
    const close = (modalId) => {
        const inst = instances[modalId] || (resolveEl(modalId) && getFrameworkModal(resolveEl(modalId)));
        if (inst) inst.hide();
    };

    /**
     * 닫힌 모달의 form 값과 JustValidate 인스턴스를 정리한다.
     * validator.destroy()로 내부 DOM/listener를 해제한 뒤 저장소에서도 제거한다.
     */
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
