// Scaffold 생성(DETAIL). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const MODAL_ID = '@@DOMAIN_ID@@-modal';

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

    const grid = pageBuilder.getGrid();
    grid.on('click', (ev) => {
        if (ev.rowKey === null || ev.rowKey === undefined) return;
        openDetail(grid.getRow(ev.rowKey));
    });

    const bindReadonlyFields = (row) => {
        document.querySelectorAll('#detail-form [data-readonly-field]').forEach(field => {
            const value = row[field.dataset.readonlyField];
            field.textContent = value === null || value === undefined || value === '' ? '-' : value;
        });
    };

    const openDetail = (row) => {
        FormBinder.bind('#detail-form', row);
        bindReadonlyFields(row);
        ModalManager.open(MODAL_ID);
    };

    ModalManager.init(MODAL_ID, {
        onOpen: () => {
            const modal = document.querySelector('#' + MODAL_ID);
            const saveBtn = modal.querySelector('#' + MODAL_ID + '-btn-save');
            const deleteBtn = modal.querySelector('#' + MODAL_ID + '-btn-delete');
            if (saveBtn) saveBtn.hidden = true;
            if (deleteBtn) deleteBtn.hidden = true;
        }
    });
});
