// Scaffold 생성(DETAIL). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const MODAL_ID = 'department-modal';

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/basic/department/data',
        searchInputs: ['searchkeyword', 'useyn'],
        searchDefaults: {},
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'ID', name: 'noticeId', align: 'center', width: 100 },
            { header: '제목', name: 'title', align: 'left', width: 250 },
            { header: '유형', name: 'noticeType', align: 'center', width: 100 },
            { header: '사용', name: 'useYn', align: 'center', width: 80 },
            { header: '조회수', name: 'viewCnt', align: 'right', width: 100 },
            { header: '등록자', name: 'regId', align: 'center', width: 120 },
            { header: '등록일시', name: 'regDttm', align: 'center', width: 170, formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm') }

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
