// 개발자 소유 수동 참조: scaffold 복사 후 커스터마이즈한 window.open CRUD 예제. 재생성하지 않고 직접 수정한다.
(function () {
    'use strict';

    const API = {
        detail: '/basic/notice/detail',
        update: '/basic/notice/update',
        create: '/basic/notice/create',
        delete: '/basic/notice/delete'
    };

    // 본문 라벨의 안정적인 id. Toast UI Editor 가 #content-editor 안에 동적으로 만드는
    // contenteditable 표면에 접근 가능한 이름(aria-labelledby)을 부여할 때 참조한다.
    const CONTENT_LABEL_ID = 'content-label';

    // fail-closed 전제: defaultLayout이 noticePageAuth를 주입하지 않은 경우
    // { create: false, update: false, delete: false } 로 폴백한다.
    // 이후 pageAuth.create/update/delete 접근이 모두 falsy 로 평가되어 쓰기가 차단된다.
    const pageAuth = window.noticePageAuth || { create: false, update: false, delete: false };

    let editor = null;
    let isCreateMode = true;
    // 상세 조회/바인딩이 실패하면 true. applyMode 가 이 값을 보고 저장·삭제를 fail-closed 한다.
    let detailLoadFailed = false;

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        const noticeId = getQueryParam('noticeId');
        isCreateMode = !noticeId;

        initEditor();
        bindEvents();
        applyMode();

        if (isCreateMode) {
            document.getElementById('popup-title').textContent = '공지사항 등록';
            refreshIcons();
        } else {
            loadDetail(noticeId);
        }
    }

    function getQueryParam(name) {
        const params = new URLSearchParams(window.location.search);
        const value = params.get(name);
        return value && value !== '' ? value : null;
    }

    function initEditor() {
        if (typeof toastui === 'undefined' || !toastui.Editor) {
            console.warn('Toast UI Editor가 로드되지 않았습니다. static/lib/toastui-editor.min.js를 추가하세요.');
            return;
        }
        editor = new toastui.Editor({
            el: document.querySelector('#content-editor'),
            height: '400px',
            initialEditType: 'wysiwyg',
            previewStyle: 'vertical',
            placeholder: '본문을 입력하세요'
        });
        // 생성 직후에만 접근 가능한 DOM 표면이 존재한다.
        applyEditorA11y();
    }

    // Toast UI Editor 가 #content-editor 안에 동적으로 만드는 contenteditable/그룹 표면에
    // 접근 가능한 이름을 부여한다. 원본 <label id=content-label>을 가리키는 aria-labelledby 와
    // 보조 aria-label, role=textbox, aria-multiline 을 설정한다.
    function applyEditorA11y() {
        const root = document.getElementById('content-editor');
        if (!root || typeof root.querySelectorAll !== 'function') return;

        const surfaces = root.querySelectorAll('.toastui-editor-contents[contenteditable="true"]');
        surfaces.forEach(function (el) {
            el.setAttribute('aria-labelledby', CONTENT_LABEL_ID);
            el.setAttribute('aria-label', '본문');
            el.setAttribute('role', 'textbox');
            el.setAttribute('aria-multiline', 'true');
        });

        const groups = root.querySelectorAll('.toastui-editor-defaultUI, .toastui-editor');
        groups.forEach(function (el) {
            el.setAttribute('role', 'group');
            el.setAttribute('aria-labelledby', CONTENT_LABEL_ID);
        });
    }

    // btn-save/btn-delete는 Thymeleaf th:if 권한 가드로 렌더링되지 않을 수 있으므로
    // null 가드 하에만 addEventListener 를 건다.
    function on(id, event, handler) {
        const el = document.getElementById(id);
        if (el) el.addEventListener(event, handler);
    }

    function bindEvents() {
        on('btn-save', 'click', save);
        on('btn-delete', 'click', removeNotice);
        on('btn-cancel', 'click', () => window.close());
        on('btn-close', 'click', () => window.close());
    }

    function applyMode() {
        // btn-save/btn-delete는 Thymeleaf th:if 권한 가드로 누락될 수 있으므로 null 가드 필수.
        const saveBtn = document.getElementById('btn-save');
        const delBtn = document.getElementById('btn-delete');

        // 부분 상세 로드 실패: 폼이 온전하지 않으므로 저장·삭제를 모두 차단한다(fail-closed).
        if (detailLoadFailed) {
            if (saveBtn) saveBtn.disabled = true;
            if (delBtn) delBtn.style.display = 'none';
            return;
        }

        // CREATE 모드 저장은 pageAuth.create, UPDATE 모드 저장은 pageAuth.update 로 가드 (fail-closed).
        if (saveBtn) {
            const canSave = isCreateMode ? pageAuth.create : pageAuth.update;
            saveBtn.disabled = !canSave;
        }

        // 삭제는 UPDATE 모드 + pageAuth.delete 일때만 노출. CREATE 모드는 PK 미존재로 항상 숨김.
        if (delBtn) {
            const canDelete = !isCreateMode && pageAuth.delete;
            delBtn.style.display = canDelete ? '' : 'none';
        }
    }

    async function loadDetail(noticeId) {
        // HTTP 실패와 DOM/에디터 바인딩 실패를 분리한다.
        // HTTP 실패는 공통 인터셉터가 중앙 에러 다이얼로그를 띄우므로 여기서 중복 알림하지 않는다.
        // 바인딩 실패는 인터셉터가 잡지 않으므로 로컬 warning 을 정확히 한 번 띄운다.
        // 두 경우 모두 detailLoadFailed=true 로 applyMode 가 저장·삭제를 fail-closed 한다.
        let data;
        try {
            data = await ApiClient.get(API.detail, { noticeId });
        } catch (err) {
            detailLoadFailed = true;
            console.error('[notice-popup] 상세 조회 실패 — 인터셉터가 오류를 표시합니다.', err);
            applyMode();
            return;
        }
        if (!data) return;
        try {
            fillForm(data);
        } catch (err) {
            detailLoadFailed = true;
            console.error('[notice-popup] 상세 폼 바인딩 실패 — 저장·삭제를 차단합니다.', err);
            CommonUtils.toast('상세 정보를 표시할 수 없습니다. 저장/삭제가 차단됩니다.', 'warning');
            applyMode();
        }
    }

    function fillForm(data) {
        // DOM/에디터 바인딩 오류를 삼키지 않고 loadDetail 로 던진다.
        // loadDetail 이 detailLoadFailed + applyMode 로 부분 폼 저장·삭제를 차단한다.
        document.getElementById('noticeId').value = data.noticeId || '';
        document.getElementById('beforeUpdDttm').value = data.updDttm || '';
        document.getElementById('title').value = data.title || '';
        document.getElementById('noticeType').value = data.noticeType || '';
        document.getElementById('useYn').value = data.useYn || 'Y';
        document.getElementById('viewCnt').value = data.viewCnt != null ? data.viewCnt : 0;
        document.getElementById('startDt').value = formatDate(data.startDt);
        document.getElementById('endDt').value = formatDate(data.endDt);

        if (editor && data.content) {
            editor.setMarkdown(data.content);
        } else if (!editor) {
            document.getElementById('content').value = data.content || '';
        }
        refreshIcons();
    }

    function formatDate(dtValue) {
        if (!dtValue) return '';
        const dateStr = typeof dtValue === 'string' ? dtValue.substring(0, 10) : '';
        return dateStr;
    }

    async function save() {
        // 모드별 필요 권한을 명시적으로 검사한다 (fail-closed).
        const allowed = isCreateMode ? pageAuth.create : pageAuth.update;
        if (!allowed) {
            CommonUtils.toast('쓰기 권한이 없습니다.', 'warning');
            return;
        }

        const url = isCreateMode ? API.create : API.update;
        try {
            const payload = collectFormData();
            if (!payload.title) {
                CommonUtils.toast('제목을 입력하세요.', 'warning');
                return;
            }
            await ApiClient.post(url, payload);
            notifyParentAndClose(isCreateMode ? 'created' : 'updated');
        } catch (err) {
            console.error('[notice-popup] 저장 실패 — 인터셉터가 오류를 표시합니다.', err);
        }
    }

    function removeNotice() {
        // 삭제 권한 명시 검사 (fail-closed).
        if (!pageAuth.delete) {
            CommonUtils.toast('삭제 권한이 없습니다.', 'warning');
            return;
        }
        // CREATE 모드 또는 PK 미확정 상태에서는 삭제하지 않는다.
        if (isCreateMode) return;

        CommonUtils.confirm('해당 공지사항을 삭제하시겠습니까?', async () => {
            try {
                const noticeId = document.getElementById('noticeId').value;
                if (!noticeId) return;
                await ApiClient.remove(API.delete, { noticeId });
                notifyParentAndClose('deleted');
            } catch (err) {
                console.error('[notice-popup] 삭제 실패 — 인터셉터가 오류를 표시합니다.', err);
            }
        });
    }

    function collectFormData() {
        const form = document.getElementById('notice-form');
        const data = {};
        new FormData(form).forEach((value, key) => {
            data[key] = value;
        });
        if (editor) {
            data.content = editor.getMarkdown();
        }
        data.startDt = toLocalDateTime(data.startDt);
        data.endDt = toLocalDateTime(data.endDt);
        return data;
    }

    function toLocalDateTime(value) {
        return value ? `${value}T00:00:00` : null;
    }

    // 성공 경로에서만 호출: 부모에게 동일 origin으로 noticeChanged 와 bounded operation 을 보내고 닫는다.
    // bounded operation: 'created' | 'updated' | 'deleted'. catch/취소 에서는 호출하지 않는다.
    function notifyParentAndClose(operation) {
        if (window.opener && !window.opener.closed) {
            window.opener.postMessage({ action: 'noticeChanged', operation: operation }, window.location.origin);
        }
        window.close();
    }

    function refreshIcons() {
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
    }
})();
