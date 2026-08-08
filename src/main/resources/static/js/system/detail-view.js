(function () {
    'use strict';

    /* 정적 샘플용 목데이터 — 실제 화면에서는 ApiClient.get() 응답으로 바인딩한다. */
    const MOCK_DETAIL = {
        sendNo: 'SND-20260728-0042',
        sendDttm: '2026-07-28 09:30:12',
        receiveTelNo: '010-1234-5678',
        callbackTelNo: '1588-0000',
        messageType: 'LMS',
        sendStatus: 'SUCCESS',
        messageTitle: '7월 카드 청구금액 안내',
        messageContent: '[SC은행] 홍길동 고객님,\n7월(2026년) 신용카드 청구금액은 350,000원이며 납부일은 2026년 7월 31일입니다.\n\n· 납부계좌: 우리은행 1002-123-456789\n· 문의: 1588-0000',
        regEmpNm: '김영희',
        regDttm: '2026-07-27 14:05:33'
    };

    const SEND_STATUS = {
        SUCCESS: { label: '발송성공', className: 'is-success' },
        FAIL: { label: '발송실패', className: 'is-fail' },
        WAIT: { label: '발송대기', className: 'is-wait' }
    };

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        bindDetail(MOCK_DETAIL);
        bindEvents();
        refreshAuthButtons();
        CommonUtils.refreshIcons();
    }

    function bindDetail(detail) {
        setFieldValue('sendNo', detail.sendNo);
        setFieldValue('sendDttm', detail.sendDttm);
        setFieldValue('receiveTelNo', detail.receiveTelNo);
        setFieldValue('callbackTelNo', detail.callbackTelNo);
        setFieldValue('messageType', detail.messageType);
        setFieldValue('messageTitle', detail.messageTitle);
        setFieldValue('messageContent', detail.messageContent);
        setFieldValue('regEmpNm', detail.regEmpNm);
        setFieldValue('regDttm', detail.regDttm);
        setSendStatus(detail.sendStatus);
    }

    function setFieldValue(id, value) {
        const field = document.getElementById(id);
        if (!field) {
            return;
        }

        const text = value == null ? '' : String(value).trim();
        field.textContent = text || '—';
        field.classList.toggle('is-empty', !text);
    }

    function setSendStatus(status) {
        const badge = document.getElementById('sendStatus');
        if (!badge) {
            return;
        }

        const info = SEND_STATUS[status] || SEND_STATUS.WAIT;
        badge.textContent = info.label;
        badge.className = 'detail-status ' + info.className;
    }

    function bindEvents() {
        document.getElementById('btn-back').addEventListener('click', () => history.back());
        document.getElementById('btn-edit').addEventListener('click', () => {
            CommonUtils.toast('정적 샘플입니다. 실제 화면에서는 수정 화면으로 이동합니다.', 'info');
        });
    }

    function refreshAuthButtons() {
        const editButton = document.getElementById('btn-edit');
        const editable = window.PAGE_AUTH && PAGE_AUTH.update;
        editButton.classList.toggle('d-none', !editable);
    }
})();
