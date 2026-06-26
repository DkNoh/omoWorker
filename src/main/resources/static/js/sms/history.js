document.addEventListener('DOMContentLoaded', function () {
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/sms/history/data',
        searchInputs: ['sentAt', 'receiverNo', 'sendStatus', 'sendType'],
        searchDefaults: {},
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'SMS_HISTORY_ID', name: 'smsHistoryId', align: 'center', width: 150 },
            { header: 'REQUEST_ID', name: 'requestId', align: 'center', width: 150 },
            { header: 'SENT_AT', name: 'sentAt', align: 'center', width: 150, editable: true, formatter: TuiCommon.fmt.date },
            { header: 'RECEIVER_NO', name: 'receiverNo', align: 'center', width: 150, editable: true },
            { header: 'SENDER_NO', name: 'senderNo', align: 'center', width: 150, editable: true },
            { header: 'SEND_TYPE', name: 'sendType', align: 'center', width: 150, editable: true },
            { header: 'SEND_STATUS', name: 'sendStatus', align: 'center', width: 150, editable: true },
            { header: 'RESULT_CD', name: 'resultCd', align: 'center', width: 150, editable: true },
            { header: 'RESULT_MSG', name: 'resultMsg', align: 'center', width: 150, editable: true },
            { header: 'UPD_DTTM', name: 'updDttm', align: 'center', width: 150, formatter: TuiCommon.fmt.date }
        ],
        autoModal: true,
        autoModalTitle: '발송이력조회 상세',
        modalActions: {
            createUrl: '/sms/history/create',
            updateUrl: '/sms/history/update',
            deleteUrl: '/sms/history/delete',
            pkFields: ['smsHistoryId'],
            lockField: 'updDttm',
            beforeLockField: 'beforeUpdDttm',
            editable: true
        }
    });
});
