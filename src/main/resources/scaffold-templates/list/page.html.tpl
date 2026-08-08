<!DOCTYPE html>
<!-- Scaffold 생성(LIST). 생성 후 개발자가 직접 수정해 소유한다. -->
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{defaultLayout}">
<head>
    <title>[( ${model.domainName()} )]</title>
</head>
<body>
<main layout:fragment="content">

    <!-- 화면 제목: 메뉴명과 같은 업무 화면명을 사용한다. -->
    <div class="content-header">
        <h2>[( ${model.domainName()} )]</h2>
    </div>

    <!-- QuerySpec의 $검색변수에서 생성된 검색영역. id는 SearchRequestDTO 필드명과 일치한다. -->
    <div class="card mb-4 shadow-sm border-0 scaffold-search-card">
        <div class="card-body">
            <div class="row align-items-center g-3 flex-wrap">
[# th:each="searchParam : ${model.searchParams()}"][# th:if="${searchParam.isBetweenRangeEnd()}"]                <div class="col-auto scaffold-date-range-separator" aria-hidden="true">~</div>
[/]                <div class="col-auto"><label for="[( ${searchParam.name()} )]" class="col-form-label fw-bold">[( ${model.htmlEscape(searchParam.label())} )]</label></div>
                <div class="col-auto">[# th:if="${searchParam.isSelect()}"]<select id="[( ${searchParam.name()} )]" class="form-select scaffold-search-control" name="[( ${searchParam.name()} )]" aria-label="[( ${model.htmlEscape(searchParam.label())} )]"><option value="">전체</option>[# th:each="option : ${searchParam.options()}"]<option value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</option>[/]</select>[/][# th:if="${searchParam.isRadio()}"]<div id="[( ${searchParam.name()} )]" class="d-flex align-items-center gap-2 scaffold-radio-group" role="radiogroup" aria-label="[( ${model.htmlEscape(searchParam.label())} )]"><label class="form-check-label"><input class="form-check-input me-1" type="radio" name="[( ${searchParam.name()} )]" value="" checked>전체</label>[# th:each="option : ${searchParam.options()}"]<label class="form-check-label"><input class="form-check-input me-1" type="radio" name="[( ${searchParam.name()} )]" value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</label>[/]</div>[/][# th:if="${searchParam.isDate()}"]<div class="scaffold-date-field scaffold-date-field-md"><div class="tui-datepicker-input tui-datetime-input scaffold-datepicker-input"><input type="text" id="[( ${searchParam.name()} )]" data-search-type="date" autocomplete="off" aria-label="[( ${model.htmlEscape(searchParam.label())} )]"><span class="tui-ico-date" aria-hidden="true"></span></div><div id="[( ${searchParam.name()} )]PickerLayer" class="scaffold-date-picker-layer"></div></div>[/][# th:if="${!searchParam.isSelect() and !searchParam.isRadio() and !searchParam.isDate()}"]<input type="text" id="[( ${searchParam.name()} )]" class="form-control scaffold-search-control" aria-label="[( ${model.htmlEscape(searchParam.label())} )]">[/]</div>
[/]                <div class="col-auto ms-auto d-flex gap-2">
                    <button type="button" id="btn-search" class="btn btn-primary px-4" aria-label="조회"><i data-lucide="search" aria-hidden="true"></i><span>조회</span></button>
                    <button type="button" id="btn-reset" class="btn btn-secondary px-3" aria-label="검색 조건 초기화"><i data-lucide="rotate-ccw" aria-hidden="true"></i><span>초기화</span></button>
                </div>
            </div>
        </div>
    </div>

    <!-- 공통 Grid/건수/페이지 크기/페이지네이션 골격. 실제 컬럼과 조회는 화면 JS가 설정한다. -->
    <div th:replace="~{fragments/toast-grid :: gridCard(null, null, null, null)}"></div>

</main>
<th:block layout:fragment="script">
    <!-- 공통 라이브러리는 defaultLayout이 먼저 로드하고 화면별 초기화 코드만 여기서 불러온다. -->
    <script th:src="@{/js/[( ${model.moduleName()} )]/[( ${model.domainId()} )].js}"></script>
</th:block>
</body>
</html>
