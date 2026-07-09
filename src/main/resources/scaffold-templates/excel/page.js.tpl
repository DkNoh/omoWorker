// Scaffold 생성(EXCEL). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const API = {
        excel: '@@SCREEN_URL@@/excel'
    };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '@@SCREEN_URL@@/data',
        searchInputs: [@@SEARCH_INPUTS@@],
        searchDefaults: {@@SEARCH_DEFAULTS@@},
        rowHeaders: ['rowNum'],
        columns: [
@@GRID_COLUMNS@@
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
