document.addEventListener('DOMContentLoaded', function () {
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/basic/notice/data',
        searchInputs: ['searchKeyword', 'noticeType', 'useYn', 'startDt'],
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
        ],
        autoModal: true,
        autoModalTitle: '공지사항관리 상세',
        modalActions: {
            createUrl: '/basic/notice/create',
            updateUrl: '/basic/notice/update',
            deleteUrl: '/basic/notice/delete',
            pkFields: ['noticeId'],
            editable: true
        }
    });
});
