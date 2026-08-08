/**
 * @fileoverview [( ${model.domainClass()} )] EXCEL 화면 초기화.
 * 목록과 다운로드가 동일한 검색조건을 사용하도록 TuiPageBuilder의 직렬화 결과를 재사용한다.
 * 최초 생성 후에는 개발자가 직접 수정해 소유한다.
 */
document.addEventListener('DOMContentLoaded', function () {
    /* 화면 전용 API는 한곳에서 관리해 경로 변경 시 호출부 누락을 막는다. */
    const API = {
        excel: '[( ${model.screenUrl()} )]/excel'
    };

    /* 목록 조회와 검색 UI의 상태는 공통 Builder가 소유한다. */
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

    /* 서버 렌더링과 클라이언트 상태를 모두 확인하고 현재 검색조건으로 다운로드한다. */
    const btnExcel = document.querySelector('#btn-excel');
    if (btnExcel) {
        btnExcel.addEventListener('click', () => {
            if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) {
                CommonUtils.toast('엑셀 다운로드 권한이 없습니다.', 'warning');
                return;
            }
            const params = new URLSearchParams(pageBuilder.getSearchParams());
            window.location.href = API.excel + '?' + params.toString();
        });
    }
});
