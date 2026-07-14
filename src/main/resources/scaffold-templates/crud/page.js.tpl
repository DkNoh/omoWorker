// Scaffold 생성(CRUD). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const MODAL_ID = '[( ${model.domainId()} )]-modal';
    const API = {
        create: '[( ${model.screenUrl()} )]/create',
        update: '[( ${model.screenUrl()} )]/update',
        delete: '[( ${model.screenUrl()} )]/delete'
    };

    const DEFAULT_FORM = { [# th:each="field, iter : ${model.defaultFormFieldNames()}"][( ${field} )]: ''[# th:if="${!iter.last}"], [/][/] };
    const PK_FIELDS = [[# th:each="field, iter : ${model.pkFieldNames()}"]'[( ${field} )]'[# th:if="${!iter.last}"], [/][/]];
    const LOCK = [# th:if="${model.hasLockColumn()}"]{ field: '[( ${model.lockFieldName()} )]', beforeField: '[( ${model.beforeLockFieldName()} )]' }[/][# th:unless="${model.hasLockColumn()}"]null[/];

    const state = {
        mode: 'create',
        selectedRow: null
    };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '[( ${model.screenUrl()} )]/data',
        searchInputs: [[# th:each="searchParam, iter : ${model.searchParams()}"]'[( ${model.jsEscape(searchParam.name())} )]'[# th:if="${!iter.last}"], [/][/]],
        searchDefaults: {[# th:each="searchParam, iter : ${model.searchParamsWithDefaults()}"][( ${searchParam.name()} )]: '[( ${model.jsEscape(searchParam.defaultValue())} )]'[# th:if="${!iter.last}"], [/][/]},
        btnCreate: 'crud-modal-auto-create-disabled',
        rowHeaders: ['rowNum'],
        columns: [
[# th:each="column, iter : ${model.columnConfigs()}"]            { header: '[( ${model.jsEscape(column.headerName())} )]', name: '[( ${column.fieldName()} )]', align: '[( ${column.align()} )]', width: [( ${column.width()} )][# th:if="${!column.visible()}"], hidden: true[/][# th:if="${column.hasMask()}"], formatter: ({ value }) => TuiCommon.maskValue(value, '[( ${column.maskType()} )]')[/][# th:if="${!column.hasMask() and column.hasOptions()}"], formatter: TuiCommon.badgeByValue({ labels: { [# th:each="option, optionIter : ${column.options()}"][( ${option.value()} )]: '[( ${model.jsEscape(option.label())} )]'[# th:if="${!optionIter.last}"], [/][/] } })[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'DATE'}"], formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD')[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'DATETIME'}"], formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'AUTO' and column.isDateColumn()}"], formatter: TuiCommon.fmt.date[/] }[# th:if="${!iter.last}"],[/]
[/]
        ]
    });

    const grid = pageBuilder.getGrid();
    grid.on('click', (ev) => {
        if (ev.rowKey === null || ev.rowKey === undefined) return;
        openEdit(grid.getRow(ev.rowKey));
    });

    const canSave = () => {
        const auth = window.PAGE_AUTH || {};
        return (state.mode === 'create' && auth.create === true)
            || (state.mode === 'update' && auth.update === true);
    };

    const syncActionButtons = () => {
        const modal = document.querySelector('#' + MODAL_ID);
        const saveBtn = modal.querySelector('#' + MODAL_ID + '-btn-save');
        const deleteBtn = modal.querySelector('#' + MODAL_ID + '-btn-delete');
        if (saveBtn) saveBtn.hidden = !canSave();
        if (deleteBtn) deleteBtn.hidden = state.mode !== 'update' || !(window.PAGE_AUTH || {}).delete;
    };

    const applyLockSnapshot = (row) => {
        if (!LOCK) return;
        const beforeLock = document.querySelector('#detail-form [name="' + LOCK.beforeField + '"]');
        if (beforeLock) beforeLock.value = row[LOCK.field] || '';
    };

    const openEdit = (row) => {
        state.mode = 'update';
        state.selectedRow = row;
        FormBinder.bind('#detail-form', row);
        applyLockSnapshot(row);
        syncActionButtons();
        ModalManager.open(MODAL_ID);
    };

    const openCreate = () => {
        state.mode = 'create';
        state.selectedRow = null;
        const form = document.querySelector('#detail-form');
        form.reset();
        FormBinder.bind('#detail-form', DEFAULT_FORM);
        syncActionButtons();
        ModalManager.open(MODAL_ID);
        const firstInput = form.querySelector('input:not([type="hidden"]), select, textarea');
        if (firstInput) firstInput.focus();
    };

    const save = async () => {
        if (!canSave()) return;
        const form = document.querySelector('#detail-form');
        if (typeof FieldFormat !== 'undefined' && !await FieldFormat.validateForm(form)) return;
        const payload = FormBinder.toObject('#detail-form');
        if (state.mode === 'create') {
            await ApiClient.post(API.create, payload);
            CommonUtils.toast('등록되었습니다.', 'success');
        } else {
            await ApiClient.post(API.update, payload);
            CommonUtils.toast('수정되었습니다.', 'success');
        }
        ModalManager.close(MODAL_ID);
        await pageBuilder.searchData(pageBuilder.currentPage || 1);
    };

    const pkParams = () => {
        const params = {};
        PK_FIELDS.forEach(field => {
            const input = document.querySelector('#detail-form [name="' + field + '"]');
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
            ModalManager.close(MODAL_ID);
            await pageBuilder.searchData(pageBuilder.currentPage || 1);
        });
    };

    ModalManager.init(MODAL_ID, {
        onSubmit: save,
        onDelete: remove
    });

    const btnCreate = document.querySelector('#btn-create');
    if (btnCreate) btnCreate.addEventListener('click', openCreate);
    if (typeof FieldFormat !== 'undefined') FieldFormat.applyFieldFormats(document.querySelector('#detail-form'));
});
