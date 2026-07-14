// Scaffold 생성(LIST). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '[( ${model.screenUrl()} )]/data',
        searchInputs: [[# th:each="searchParam, iter : ${model.searchParams()}"]'[( ${model.jsEscape(searchParam.name())} )]'[# th:if="${!iter.last}"], [/][/]],
        searchDefaults: {[# th:each="searchParam, iter : ${model.searchParamsWithDefaults()}"][( ${searchParam.name()} )]: '[( ${model.jsEscape(searchParam.defaultValue())} )]'[# th:if="${!iter.last}"], [/][/]},
        rowHeaders: ['rowNum'],
        columns: [
[# th:each="column, iter : ${model.columnConfigs()}"]            { header: '[( ${model.jsEscape(column.headerName())} )]', name: '[( ${column.fieldName()} )]', align: '[( ${column.align()} )]', width: [( ${column.width()} )][# th:if="${!column.visible()}"], hidden: true[/][# th:if="${column.hasMask()}"], formatter: ({ value }) => TuiCommon.maskValue(value, '[( ${column.maskType()} )]')[/][# th:if="${!column.hasMask() and column.hasOptions()}"], formatter: TuiCommon.badgeByValue({ labels: { [# th:each="option, optionIter : ${column.options()}"][( ${option.value()} )]: '[( ${model.jsEscape(option.label())} )]'[# th:if="${!optionIter.last}"], [/][/] } })[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'DATE'}"], formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD')[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'DATETIME'}"], formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'AUTO' and column.isDateColumn()}"], formatter: TuiCommon.fmt.date[/] }[# th:if="${!iter.last}"],[/]
[/]
        ]
    });
});
