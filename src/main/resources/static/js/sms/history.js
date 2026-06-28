// Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/sms/history/data',
        searchInputs: ['sendType', 'sendStatus', 'sentAt', 'receiverNo'],
        searchDefaults: {},
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'SMS_HISTORY_ID', name: 'smsHistoryId', align: 'center', width: 150 },
            { header: 'REQUEST_ID', name: 'requestId', align: 'center', width: 150 },
            { header: 'SENT_AT', name: 'sentAt', align: 'center', width: 150, editable: true, formatter: TuiCommon.fmt.date },
            { header: 'RECEIVER_NO', name: 'receiverNo', align: 'center', width: 150, editable: true },
            { header: 'SENDER_NO', name: 'senderNo', align: 'center', width: 150, editable: true },
	            { header: 'SEND_TYPE', name: 'sendType', align: 'center', width: 150, editable: true,
	              formatter: TuiCommon.badgeByValue({
	                  labels: { SMS: 'SMS', LMS: 'LMS', MMS: 'MMS', ALIMTALK: '알림톡' },
	                  tones: {
	                      SMS: 'bg-primary-subtle text-primary-emphasis border border-primary-subtle',
	                      LMS: 'bg-info-subtle text-info-emphasis border border-info-subtle',
	                      MMS: 'bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle',
	                      ALIMTALK: 'bg-warning-subtle text-warning-emphasis border border-warning-subtle'
	                  }
	              }) },
            { header: 'SEND_STATUS', name: 'sendStatus', align: 'center', width: 150, editable: true,
              formatter: TuiCommon.badgeByValue({
                  labels: { SUCCESS: '성공', FAIL: '실패', WAIT: '대기' },
                  tones: { SUCCESS: 'bg-success', FAIL: 'bg-danger', WAIT: 'bg-warning text-dark' }
              }) },
            { header: 'RESULT_CD', name: 'resultCd', align: 'center', width: 150, editable: true },
            { header: 'RESULT_MSG', name: 'resultMsg', align: 'center', width: 150, editable: true }
        ]
    });
});
