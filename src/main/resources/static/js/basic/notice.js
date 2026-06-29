// Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/basic/notice/data',
        searchInputs: ['searchKeyword', 'noticeType', 'useYn', 'startDate', 'endDate'],
        searchDefaults: {},
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'NOTICE_ID', name: 'noticeId', align: 'center', width: 150 },
            { header: 'TITLE', name: 'title', align: 'center', width: 150, editable: true },
            { header: 'NOTICE_TYPE', name: 'noticeType', align: 'center', width: 150, editable: true },
            { header: 'USE_YN', name: 'useYn', align: 'center', width: 150, editable: true },
            { header: 'START_DT', name: 'startDt', align: 'center', width: 150, editable: true, formatter: TuiCommon.fmt.date },
            { header: 'END_DT', name: 'endDt', align: 'center', width: 150, editable: true, formatter: TuiCommon.fmt.date },
            { header: 'VIEW_CNT', name: 'viewCnt', align: 'center', width: 150, editable: true },
            { header: 'REG_DTTM', name: 'regDttm', align: 'center', width: 150, formatter: TuiCommon.fmt.date }
        ]
    });
});
