// Scaffold 생성(EXCEL). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const API = {
        excel: '/basic/audit-log/excel'
    };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/basic/audit-log/data',
        searchInputs: ['startdate', 'enddate', 'regid'],
        searchDefaults: {startdate: 'RECENT_7_DAYS', enddate: 'TODAY'},
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'ID', name: 'noticeId', align: 'center', width: 100 },
            { header: '제목', name: 'title', align: 'left', width: 250 },
            { header: '유형', name: 'noticeType', align: 'center', width: 100 },
            { header: '사용', name: 'useYn', align: 'center', width: 80 },
            { header: '조회수', name: 'viewCnt', align: 'right', width: 100 },
            { header: '등록자', name: 'regId', align: 'center', width: 120 },
            { header: '등록일시', name: 'regDttm', align: 'center', width: 170, formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm') },
            { header: '수정자', name: 'updId', align: 'center', width: 120 },
            { header: '수정일시', name: 'updDttm', align: 'center', width: 170, formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm') }

        ]
    });

    const btnExcel = document.querySelector('#btn-excel');
    if (btnExcel) {
        btnExcel.addEventListener('click', () => {
            if (window.PAGE_AUTH && window.PAGE_AUTH.download !== true) {
                CommonUtils.toast('엑셀 다운로드 권한이 없습니다.', 'warning');
                return;
            }
            const params = new URLSearchParams(pageBuilder.getSearchParams());
            window.location.href = API.excel + '?' + params.toString();
        });
    }
});
