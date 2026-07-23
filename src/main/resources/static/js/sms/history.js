// Scaffold 생성(EXCEL). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const API = {
        excel: '/sms/history/excel'
    };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/sms/history/data',
        searchInputs: ['sendType', 'sendStatus', 'startDt', 'endDt', 'receiverNo'],
        searchDefaults: {},
        rowHeaders: [],
        columns: [
            { header: 'SMS_HISTORY_ID', name: 'smsHistoryId', align: 'center', width: 150 },
            { header: 'REQUEST_ID', name: 'requestId', align: 'center', width: 150 },
            { header: 'SENT_AT', name: 'sentAt', align: 'center', width: 150, formatter: TuiCommon.fmt.date },
            { header: 'RECEIVER_NO', name: 'receiverNo', align: 'center', width: 150 },
            { header: 'SENDER_NO', name: 'senderNo', align: 'center', width: 150 },
            { header: 'SEND_TYPE', name: 'sendType', align: 'center', width: 150 },
            { header: 'SEND_STATUS', name: 'sendStatus', align: 'center', width: 150 },
            { header: 'RESULT_CD', name: 'resultCd', align: 'center', width: 150 },
            { header: 'RESULT_MSG', name: 'resultMsg', align: 'center', width: 150 }

        ]
    });

    const btnExcel = document.querySelector('#btn-excel');
    if (btnExcel) {
        btnExcel.addEventListener('click', () => {
            if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) {
                CommonUtils.toast('엑셀 다운로드 권한이 없습니다.', 'warning');
                return;
            }
            const params = new URLSearchParams(pageBuilder.getSearchParams());
            window.location.href = API.excel + '?' + params.toString();
        });
    }
});
