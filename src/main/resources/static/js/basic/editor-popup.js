(function () {
    'use strict';

    /*
     * 에디터 팝업 샘플 JS — Toast UI Editor 연동
     * 모든 데이터는 내장 mock — 서버 없이 file://에서 완전 동작한다.
     * 실전 참고: js/basic/notice-popup.js (동일한 에디터 패턴)
     */

    const API = {
        detail: '/basic/notice/detail',
        create: '/basic/notice/create',
        update: '/basic/notice/update',
        remove: '/basic/notice/delete'
    };

    // 본문 라벨의 안정적인 id — 에디터 contenteditable 표면에 aria-labelledby 를 부여할 때 참조
    const CONTENT_LABEL_ID = 'content-label';

    // 샘플용 mock 본문 (수정 모드 진입 시 에디터에 채운다)
    const MOCK_CONTENT = [
        '## 시스템 점검 안내',
        '',
        '안녕하세요. **SC은행 SMS 시스템** 운영팀입니다.',
        '',
        '아래 시간대에 시스템 점검이 예정되어 있습니다.',
        '',
        '- 일시: 2026-08-01 02:00 ~ 04:00',
        '- 영향: SMS 발송 지연 가능',
        '',
        '> 점검 중 긴급 문의는 운영팀 내선 1234 로 연락 바랍니다.'
    ].join('\n');

    let editor = null;
    let isCreateMode = true;

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        const noticeId = getQueryParam('noticeId');
        isCreateMode = !noticeId;

        initEditor();
        bindEvents();
        applyMode();

        if (isCreateMode) {
            document.getElementById('popup-title').firstChild.textContent = '공지사항 등록 ';
        } else {
            document.getElementById('popup-title').firstChild.textContent = '공지사항 수정 ';
            document.getElementById('noticeId').value = noticeId;
            loadDetailMock(noticeId);
        }
        CommonUtils.refreshIcons();
    }

    function getQueryParam(name) {
        const value = new URLSearchParams(window.location.search).get(name);
        return value && value !== '' ? value : null;
    }

    // ─── 에디터 초기화 ────────────────────────────────────────────────
    function initEditor() {
        if (typeof toastui === 'undefined' || !toastui.Editor) {
            console.warn('Toast UI Editor가 로드되지 않았습니다. static/lib/toastui-editor/ 경로를 확인하세요.');
            return;
        }
        editor = new toastui.Editor({
            el: document.querySelector('#content-editor'),
            height: '400px',
            initialEditType: 'wysiwyg',
            previewStyle: 'vertical',
            placeholder: '본문을 입력하세요'
        });
        // 생성 직후에만 접근 가능한 DOM 표면에 접근성 속성을 부여한다.
        applyEditorA11y();
    }

    // 에디터가 #content-editor 안에 동적으로 만드는 contenteditable/그룹 표면에
    // aria-labelledby / role / aria-multiline 을 설정한다.
    function applyEditorA11y() {
        const root = document.getElementById('content-editor');
        if (!root || typeof root.querySelectorAll !== 'function') return;

        root.querySelectorAll('.toastui-editor-contents[contenteditable="true"]').forEach(el => {
            el.setAttribute('aria-labelledby', CONTENT_LABEL_ID);
            el.setAttribute('aria-label', '본문');
            el.setAttribute('role', 'textbox');
            el.setAttribute('aria-multiline', 'true');
        });

        root.querySelectorAll('.toastui-editor-defaultUI, .toastui-editor').forEach(el => {
            el.setAttribute('role', 'group');
            el.setAttribute('aria-labelledby', CONTENT_LABEL_ID);
        });
    }

    // ─── 모드별 버튼 노출 ─────────────────────────────────────────────
    function applyMode() {
        const auth = window.PAGE_AUTH || {};
        const btnDelete = document.getElementById('btn-delete');
        const btnSave = document.getElementById('btn-save');

        // 삭제 버튼은 수정 모드 + delete 권한일 때만 노출
        btnDelete.classList.toggle('d-none', !(isCreateMode === false && auth.delete === true));
        // 저장 버튼은 등록/수정 권한에 따라 노출
        const canSave = isCreateMode ? auth.create === true : auth.update === true;
        btnSave.classList.toggle('d-none', !canSave);
    }

    // ─── 이벤트 ───────────────────────────────────────────────────────
    function bindEvents() {
        document.getElementById('btn-save').addEventListener('click', save);
        document.getElementById('btn-delete').addEventListener('click', remove);
        document.getElementById('btn-cancel').addEventListener('click', () => {
            Notify.confirm('작성 중인 내용을 취소하시겠습니까?', () => {
                Notify.toast('취소되었습니다. (샘플)', 'info');
            });
        });
        document.getElementById('btn-close').addEventListener('click', () => {
            Notify.toast('팝업을 닫습니다. (샘플)', 'info');
        });
    }

    async function save() {
        const form = document.getElementById('notice-form');
        if (typeof FieldFormat !== 'undefined' && !(await FieldFormat.validateForm(form))) {
            return;
        }

        // 에디터 본문(markdown)을 숨겨진 textarea 에 동기화 — 실제 전송되는 값
        const markdown = editor ? editor.getMarkdown() : '';
        document.getElementById('content').value = markdown;

        const payload = FormBinder.toObject('#notice-form');
        console.log('[샘플] 전송 페이로드', payload);

        Notify.toast(isCreateMode ? '등록되었습니다. (샘플 데이터)' : '수정되었습니다. (샘플 데이터)', 'success');
    }

    function remove() {
        Notify.confirm('선택한 공지사항을 삭제하시겠습니까?', () => {
            Notify.toast('삭제되었습니다. (샘플 데이터)', 'success');
        });
    }

    // ─── 수정 모드: 기존 본문 로드 (mock) ─────────────────────────────
    function loadDetailMock(noticeId) {
        document.getElementById('title').value = '시스템 점검 안내';
        document.getElementById('noticeType').value = 'URGENT';
        document.getElementById('useYn').value = 'Y';
        document.getElementById('startDt').value = '2026-08-01';

        if (editor) {
            editor.setMarkdown(MOCK_CONTENT);
        }
        document.getElementById('content').value = MOCK_CONTENT;
    }
})();
