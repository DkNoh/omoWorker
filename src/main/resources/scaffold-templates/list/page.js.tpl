/**
 * @fileoverview [( ${model.domainClass()} )] LIST 화면 초기화.
 * QuerySpec 검색조건과 컬럼 옵션을 TuiPageBuilder 계약으로 연결한다.
 * 최초 생성 후에는 개발자가 직접 수정해 소유한다.
 */
document.addEventListener('DOMContentLoaded', function () {
    /* Builder가 Grid 생성, 검색 이벤트, 날짜 입력, 서버 페이징과 최초 조회를 일괄 처리한다. */
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '[( ${model.screenUrl()} )]/data',
        searchInputs: [[# th:each="searchParam, iter : ${model.searchParams()}"]'[( ${model.jsEscape(searchParam.name())} )]'[# th:if="${!iter.last}"], [/][/]],
        searchDefaults: {[# th:each="searchParam, iter : ${model.searchParamsWithDefaults()}"][( ${searchParam.name()} )]: '[( ${model.jsEscape(searchParam.defaultValue())} )]'[# th:if="${!iter.last}"], [/][/]},
        rowHeaders: [( ${rowHeaders} )],
        columns: [
[# th:each="column, iter : ${model.columnConfigs()}"]            { header: '[( ${model.jsEscape(column.headerName())} )]', name: '[( ${column.fieldName()} )]', align: '[( ${column.align()} )]', [# th:if="${model.isLastVisibleColumn(column)}"]minWidth[/][# th:unless="${model.isLastVisibleColumn(column)}"]width[/]: [( ${column.width()} )][# th:if="${!column.visible()}"], hidden: true[/][# th:if="${!column.hasMask() and column.hasOptions()}"], formatter: TuiCommon.badgeByValue({ labels: { [# th:each="option, optionIter : ${column.options()}"][( ${option.value()} )]: '[( ${model.jsEscape(option.label())} )]'[# th:if="${!optionIter.last}"], [/][/] } })[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'DATE'}"], formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD')[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'DATETIME'}"], formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')[/][# th:if="${!column.hasMask() and !column.hasOptions() and column.dateFormat() == 'AUTO' and column.isDateColumn()}"], formatter: TuiCommon.fmt.date[/] }[# th:if="${!iter.last}"],[/]
[/]
        ]
    });
});
