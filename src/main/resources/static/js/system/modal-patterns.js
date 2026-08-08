// 모달 패턴 정적 샘플 (samples/modal-patterns.html)
//
// 화면 동작 요약
// 1. DOMContentLoaded 후 ModalManager가 폼 모달 2개(form-modal, large-modal)의 lifecycle을 초기화한다.
// 2. 카드 위 버튼으로 모달을 열거나, Notify.confirm 으로 확인 다이얼로그를 띄운다.
// 3. 저장 버튼은 ModalManager의 onSubmit 훅(JustValidate 검증 통과 후)에서 toast를 띄우고 모달을 닫는다.
// 4. 삭제 버튼은 Notify.confirm 으로 한 번 더 확인받은 뒤 toast + 모달 닫기를 수행한다.
// 5. 확인 다이얼로그는 별도 모달 마크업 없이 Notify.confirm(msg, callback) 만으로 구성한다.
(function () {
    'use strict';

    // fragments/modal-base.html 규약에 따른 모달 DOM id — ModalManager가 이 값으로
    // 모달 컨테이너와 ${modalId}-btn-save / ${modalId}-btn-delete 버튼을 찾는다.
    const FORM_MODAL_ID = 'form-modal';
    const LARGE_MODAL_ID = 'large-modal';

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        // ── 폼 모달 (modal-md) ─────────────────────────────────────────────
        ModalManager.init(FORM_MODAL_ID, {
            // 모달이 열릴 때마다 #notice-form 에 JustValidate가 새로 바인딩되고 이 훅이 호출된다.
            // 룰은 필드의 data-validate 선언과 동일하게 유지한다.
            validateRules: (validator) => {
                validator.addField('#noticeTitle', [
                    { rule: 'required', errorMessage: '제목을 입력하세요.' },
                    { rule: 'maxLength', value: 100, errorMessage: '100자 이내로 입력하세요.' }
                ]);
                validator.addField('#noticeWriter', [
                    { rule: 'required', errorMessage: '작성자를 입력하세요.' },
                    { rule: 'maxLength', value: 50, errorMessage: '50자 이내로 입력하세요.' }
                ]);
                validator.addField('#noticeContent', [
                    { rule: 'required', errorMessage: '내용을 입력하세요.' }
                ]);
            },
            // 모달이 열린 직후 바로 입력할 수 있도록 첫 번째 입력 요소에 포커스를 둔다.
            onOpen: () => {
                const firstInput = document.querySelector('#notice-form input:not([type="hidden"])');
                if (firstInput) firstInput.focus();
            },
            // form-modal-btn-save 클릭 — ModalManager가 검증을 통과시킨 뒤에 이 훅을 호출한다.
            onSubmit: () => {
                Notify.toast('저장되었습니다.', 'success');
                ModalManager.close(FORM_MODAL_ID);
            },
            // form-modal-btn-delete 클릭 — 확인 다이얼로그를 거쳐 삭제 결과를 알린다.
            onDelete: () => {
                Notify.confirm('선택한 항목을 삭제하시겠습니까?', () => {
                    Notify.toast('삭제되었습니다.', 'success');
                    ModalManager.close(FORM_MODAL_ID);
                });
            }
        });

        // ── 큰 모달 (modal-lg) ─────────────────────────────────────────────
        ModalManager.init(LARGE_MODAL_ID, {
            validateRules: (validator) => {
                validator.addField('#configCode', [
                    { rule: 'required', errorMessage: '코드를 입력하세요.' },
                    { rule: 'maxLength', value: 20, errorMessage: '20자 이내로 입력하세요.' }
                ]);
                validator.addField('#configName', [
                    { rule: 'required', errorMessage: '명칭을 입력하세요.' },
                    { rule: 'maxLength', value: 50, errorMessage: '50자 이내로 입력하세요.' }
                ]);
                validator.addField('#configType', [
                    { rule: 'required', errorMessage: '구분을 선택하세요.' }
                ]);
                // 선택 입력 — 빈 값은 number 룰을 타지 않으므로 필수로 전환되지 않는다.
                validator.addField('#sortOrd', [
                    { rule: 'number', errorMessage: '숫자만 입력하세요.' }
                ]);
            },
            // large-modal-btn-save 클릭 — 검증 통과 후 저장 결과를 알리고 모달을 닫는다.
            onSubmit: () => {
                Notify.toast('저장되었습니다.', 'success');
                ModalManager.close(LARGE_MODAL_ID);
            }
        });

        // data-mask 필드(적용일자)에 IMask를 부착한다.
        // 검증은 ModalManager의 validateRules가 담당하므로 applyMasks만 사용한다.
        if (typeof FieldFormat !== 'undefined') {
            FieldFormat.applyMasks(document.querySelector('#config-form'));
        }

        // ── 열기 버튼 연결 ─────────────────────────────────────────────────
        document.getElementById('btn-open-form-modal')
            .addEventListener('click', () => ModalManager.open(FORM_MODAL_ID));
        document.getElementById('btn-open-large-modal')
            .addEventListener('click', () => ModalManager.open(LARGE_MODAL_ID));

        // 확인 다이얼로그 — 커스텀 모달 없이 Notify.confirm(msg, onConfirm)만으로 구성한다.
        document.getElementById('btn-open-confirm').addEventListener('click', () => {
            Notify.confirm('삭제하시겠습니까?', () => {
                Notify.toast('삭제되었습니다.', 'success');
            });
        });

        CommonUtils.refreshIcons();
    }
})();
