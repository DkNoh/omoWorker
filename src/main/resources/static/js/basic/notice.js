// Scaffold 생성(CRUD_PANEL). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const API = {
        create: '/basic/notice/create',
        update: '/basic/notice/update',
        delete: '/basic/notice/delete'
    };

    const SELECTOR = {
        panel: '#detail-panel',
        emptyPanel: '#detail-empty-panel',
        form: '#detail-form',
        panelTitle: '#detail-panel-title',
        selectedKey: '#detail-selected-key',
        btnCreate: '#btn-create',
        btnSave: '#btn-save',
        btnDelete: '#btn-delete'
    };

    const DEFAULT_FORM = { noticeId: '', title: '', noticeType: '', useYn: '', startDt: '', endDt: '', viewCnt: '', beforeUpdDttm: '' };
    const PK_FIELDS = ['noticeId'];
    const LOCK = { field: 'updDttm', beforeField: 'beforeUpdDttm' };

    const state = {
        mode: 'create',
        selectedRow: null
    };

    const $ = (selector, root = document) => root.querySelector(selector);

    const els = {
        panel: $(SELECTOR.panel),
        emptyPanel: $(SELECTOR.emptyPanel),
        form: $(SELECTOR.form),
        panelTitle: $(SELECTOR.panelTitle),
        selectedKey: $(SELECTOR.selectedKey),
        btnCreate: $(SELECTOR.btnCreate),
        btnSave: $(SELECTOR.btnSave),
        btnDelete: $(SELECTOR.btnDelete)
    };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/basic/notice/data',
        searchInputs: ['noticetype', 'useyn', 'searchkeyword', 'startdt', 'enddt'],
        searchDefaults: {},
        btnCreate: 'crud-panel-auto-create-disabled',
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'NOTICE_ID', name: 'noticeId', align: 'center', width: 150 },
            { header: 'TITLE', name: 'title', align: 'center', width: 150 },
            { header: 'NOTICE_TYPE', name: 'noticeType', align: 'center', width: 150 },
            { header: 'USE_YN', name: 'useYn', align: 'center', width: 150 },
            { header: 'START_DT', name: 'startDt', align: 'center', width: 150, formatter: TuiCommon.fmt.date },
            { header: 'END_DT', name: 'endDt', align: 'center', width: 150, formatter: TuiCommon.fmt.date },
            { header: 'VIEW_CNT', name: 'viewCnt', align: 'center', width: 150 },
            { header: 'REG_ID', name: 'regId', align: 'center', width: 150 },
            { header: 'REG_DTTM', name: 'regDttm', align: 'center', width: 150, formatter: TuiCommon.fmt.date },
            { header: 'UPD_ID', name: 'updId', align: 'center', width: 150 },
            { header: 'UPD_DTTM', name: 'updDttm', align: 'center', width: 150, formatter: TuiCommon.fmt.date }

        ]
    });

    const grid = pageBuilder.getGrid();
    grid.on('click', (ev) => {
        if (ev.rowKey === null || ev.rowKey === undefined) return;
        openDetail(grid.getRow(ev.rowKey));
    });

    const showPanel = (show) => {
        els.panel.hidden = !show;
        els.emptyPanel.hidden = show;
    };

    const canSave = () => {
        const auth = window.PAGE_AUTH || {};
        return (state.mode === 'create' && auth.create === true)
            || (state.mode === 'update' && auth.update === true);
    };

    const syncActionButtons = () => {
        if (els.btnSave) els.btnSave.hidden = !canSave();
        if (els.btnDelete) els.btnDelete.hidden = state.mode !== 'update' || !(window.PAGE_AUTH || {}).delete;
    };

    const rowLabel = (row) => PK_FIELDS.map(field => row[field]).filter(value => value !== null && value !== undefined && value !== '').join(' / ');

    const applyLockSnapshot = (row) => {
        if (!LOCK) return;
        const beforeLock = els.form.querySelector(`[name="${LOCK.beforeField}"]`);
        if (beforeLock) beforeLock.value = row[LOCK.field] || '';
    };

    const bindReadonlyFields = (row) => {
        els.form.querySelectorAll('[data-readonly-field]').forEach(field => {
            const value = row[field.dataset.readonlyField];
            field.textContent = value === null || value === undefined || value === '' ? '-' : value;
        });
    };

    const openDetail = (row) => {
        state.mode = 'update';
        state.selectedRow = row;
        FormBinder.bind(SELECTOR.form, row);
        bindReadonlyFields(row);
        applyLockSnapshot(row);
        els.panelTitle.textContent = '공지사항 수정';
        els.selectedKey.textContent = rowLabel(row) || '(선택됨)';
        syncActionButtons();
        showPanel(true);
    };

    const startCreate = () => {
        state.mode = 'create';
        state.selectedRow = null;
        els.form.reset();
        FormBinder.bind(SELECTOR.form, DEFAULT_FORM);
        bindReadonlyFields(DEFAULT_FORM);
        els.panelTitle.textContent = '공지사항 등록';
        els.selectedKey.textContent = '(신규)';
        syncActionButtons();
        showPanel(true);
        const firstInput = els.form.querySelector('input:not([type="hidden"]), select, textarea');
        if (firstInput) firstInput.focus();
    };

    const save = async () => {
        if (!canSave()) return;
        if (typeof FieldFormat !== 'undefined' && !await FieldFormat.validateForm(els.form)) return;
        const payload = FormBinder.toObject(SELECTOR.form);
        if (state.mode === 'create') {
            await ApiClient.post(API.create, payload);
            CommonUtils.toast('등록되었습니다.', 'success');
        } else {
            await ApiClient.post(API.update, payload);
            CommonUtils.toast('수정되었습니다.', 'success');
        }
        await pageBuilder.searchData(pageBuilder.currentPage || 1);
    };

    const pkParams = () => {
        const params = {};
        PK_FIELDS.forEach(field => {
            const input = els.form.querySelector(`[name="${field}"]`);
            params[field] = input ? input.value : null;
        });
        return params;
    };

    const remove = () => {
        if (state.mode !== 'update') return;
        CommonUtils.confirm('선택한 데이터를 삭제하시겠습니까?', async () => {
            await ApiClient.remove(API.delete, pkParams());
            CommonUtils.toast('삭제되었습니다.', 'success');
            state.selectedRow = null;
            showPanel(false);
            await pageBuilder.searchData(pageBuilder.currentPage || 1);
        });
    };

    if (els.btnCreate) els.btnCreate.addEventListener('click', startCreate);
    if (els.btnSave) els.btnSave.addEventListener('click', save);
    if (els.btnDelete) els.btnDelete.addEventListener('click', remove);
    if (typeof FieldFormat !== 'undefined') FieldFormat.applyFieldFormats(els.form);
    syncActionButtons();
});
