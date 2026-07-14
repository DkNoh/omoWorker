// 수동 커스텀: notice 팝업 수정 화면 (window.open 예제). 개발자가 직접 관리한다.
(function () {
    'use strict';

    const API = {
        detail: '/basic/notice/detail',
        update: '/basic/notice/update',
        create: '/basic/notice/create'
    };

    let editor = null;
    let isCreateMode = true;

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        const noticeId = getQueryParam('noticeId');
        isCreateMode = !noticeId;

        initEditor();
        bindEvents();

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
    }

    function bindEvents() {
        document.getElementById('btn-save').addEventListener('click', save);
        document.getElementById('btn-cancel').addEventListener('click', () => window.close());
        document.getElementById('btn-close').addEventListener('click', () => window.close());
    }

    async function loadDetail(noticeId) {
        try {
            const data = await ApiClient.get(API.detail, { noticeId });
            if (data) {
                fillForm(data);
            }
        } catch (err) {
            console.error('상세 조회 실패', err);
        }
    }

    function fillForm(data) {
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
        const payload = collectFormData();
        if (!payload.title) {
            CommonUtils.toast('제목을 입력하세요.', 'warning');
            return;
        }

        const url = isCreateMode ? API.create : API.update;
        try {
            await ApiClient.post(url, payload);
            notifyParentAndClose();
        } catch (err) {
            console.error('저장 실패', err);
        }
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

    function notifyParentAndClose() {
        if (window.opener && !window.opener.closed) {
            window.opener.postMessage({ action: 'noticeSaved' }, window.location.origin);
        }
        window.close();
    }

    function refreshIcons() {
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
    }
})();
