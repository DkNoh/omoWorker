// Scaffold 생성(LIST). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
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
});
