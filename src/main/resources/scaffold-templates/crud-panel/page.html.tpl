<!DOCTYPE html>
<!-- Scaffold 생성(CRUD_PANEL). 생성 후 개발자가 직접 수정해 소유한다. -->
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{defaultLayout}">
<head>
    <title>[( ${model.domainName()} )]</title>
</head>
<body>
<main layout:fragment="content">

    <div class="content-header">
        <h2>[( ${model.domainName()} )]</h2>
    </div>

        <div class="card mb-4 shadow-sm border-0 scaffold-search-card">
        <div class="card-body">
            <div class="row align-items-center g-3 flex-wrap">
[# th:each="searchParam : ${model.searchParams()}"][# th:if="${searchParam.isBetweenRangeEnd()}"]                <div class="col-auto scaffold-date-range-separator" aria-hidden="true">~</div>
[/]                <div class="col-auto"><label for="[( ${searchParam.name()} )]" class="col-form-label fw-bold">[( ${model.htmlEscape(searchParam.name())} )]</label></div>
                <div class="col-auto">[# th:if="${searchParam.isSelect()}"]<select id="[( ${searchParam.name()} )]" class="form-select scaffold-search-control" name="[( ${searchParam.name()} )]" aria-label="[( ${model.htmlEscape(searchParam.name())} )]"><option value="">전체</option>[# th:each="option : ${searchParam.options()}"]<option value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</option>[/]</select>[/][# th:if="${searchParam.isRadio()}"]<div id="[( ${searchParam.name()} )]" class="d-flex align-items-center gap-2 scaffold-radio-group" role="radiogroup" aria-label="[( ${model.htmlEscape(searchParam.name())} )]"><label class="form-check-label"><input class="form-check-input me-1" type="radio" name="[( ${searchParam.name()} )]" value="" checked>전체</label>[# th:each="option : ${searchParam.options()}"]<label class="form-check-label"><input class="form-check-input me-1" type="radio" name="[( ${searchParam.name()} )]" value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</label>[/]</div>[/][# th:if="${searchParam.isDate()}"]<div class="scaffold-date-field scaffold-date-field-md"><div class="tui-datepicker-input tui-datetime-input scaffold-datepicker-input"><input type="text" id="[( ${searchParam.name()} )]" data-search-type="date" autocomplete="off" aria-label="[( ${model.htmlEscape(searchParam.name())} )]"><span class="tui-ico-date" aria-hidden="true"></span></div><div id="[( ${searchParam.name()} )]PickerLayer" class="scaffold-date-picker-layer"></div></div>[/][# th:if="${!searchParam.isSelect() and !searchParam.isRadio() and !searchParam.isDate()}"]<input type="text" id="[( ${searchParam.name()} )]" class="form-control scaffold-search-control" aria-label="[( ${model.htmlEscape(searchParam.name())} )]">[/]</div>
[/]                <div class="col-auto ms-auto d-flex gap-2">
                    <button type="button" id="btn-create" th:if="${pageAuth.create}" class="btn btn-outline-primary px-3" aria-label="신규 등록"><i data-lucide="plus" aria-hidden="true"></i><span>등록</span></button>
                    <button type="button" id="btn-search" class="btn btn-primary px-4" aria-label="조회"><i data-lucide="search" aria-hidden="true"></i><span>조회</span></button>
                    <button type="button" id="btn-reset" class="btn btn-secondary px-3" aria-label="검색 조건 초기화"><i data-lucide="rotate-ccw" aria-hidden="true"></i><span>초기화</span></button>
                </div>
            </div>
        </div>
    </div>

    <div th:replace="~{fragments/toast-grid :: gridCard}"></div>

    <section class="card surface-card mb-4" id="detail-panel" hidden>
        <header class="card-header bg-transparent d-flex justify-content-between align-items-center flex-wrap gap-2">
            <div class="d-flex align-items-center gap-2"><i data-lucide="panel-right" aria-hidden="true"></i><span class="fw-semibold" id="detail-panel-title">[( ${model.htmlEscape(model.domainName())} )] 상세</span><code id="detail-selected-key"></code></div>
            <div class="d-flex gap-2 flex-wrap">
                <button type="button" id="btn-save" th:if="${pageAuth.create or pageAuth.update}" class="btn btn-primary"><i data-lucide="save" aria-hidden="true"></i><span>저장</span></button>
                <button type="button" id="btn-delete" th:if="${pageAuth.delete}" class="btn btn-danger"><i data-lucide="trash-2" aria-hidden="true"></i><span>삭제</span></button>
            </div>
        </header>
        <div class="card-body"><form id="detail-form" autocomplete="off" novalidate>
[# th:each="pkField : ${model.pkFieldNames()}"]            <input type="hidden" name="[( ${pkField} )]">
[/][# th:if="${!model.lockColumn().isEmpty()}"]            <input type="hidden" name="[( ${model.beforeLockFieldName()} )]">
[/]            <div class="row g-3">
[# th:each="column : ${model.columnConfigs()}"][# th:if="${!column.editable() and column.modalVisible()}"]                <div class="col-12 col-md-6"><div class="form-label">[( ${model.htmlEscape(column.headerName())} )]</div><div class="form-control-plaintext" data-readonly-field="[( ${column.fieldName()} )]"></div></div>
[/][# th:if="${column.editable()}"]                <div class="col-12 col-md-6"><label class="form-label" for="f-[( ${column.fieldName()} )]">[( ${model.htmlEscape(column.headerName())} )]</label>[# th:if="${column.hasOptions()}"]<select id="f-[( ${column.fieldName()} )]" class="form-select" name="[( ${column.fieldName()} )]" aria-label="[( ${model.htmlEscape(column.fieldName())} )]"[# th:if="${column.hasInputMask()}"] data-mask="[( ${model.htmlEscape(column.inputMask())} )]"[/][# th:if="${column.hasValidate()}"] data-validate="[( ${model.htmlEscape(column.validate())} )]"[/]><option value="">선택</option>[# th:each="option : ${column.options()}"]<option value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</option>[/]</select>[/][# th:if="${!column.hasOptions()}"]<input type="[( ${column.isNumeric() ? 'number' : 'text'} )]" class="form-control" id="f-[( ${column.fieldName()} )]" name="[( ${column.fieldName()} )]"[# th:if="${column.hasInputMask()}"] data-mask="[( ${model.htmlEscape(column.inputMask())} )]"[/][# th:if="${column.hasValidate()}"] data-validate="[( ${model.htmlEscape(column.validate())} )]"[/]>[/]</div>
[/][/]            </div>
        </form></div>
    </section>
    <section class="card surface-card mb-4" id="detail-empty-panel">
        <div class="empty-state"><i data-lucide="mouse-pointer-click" class="empty-state-icon" aria-hidden="true"></i><p class="empty-state-title">행을 선택하세요</p></div>
    </section>

</main>
<th:block layout:fragment="script">
    <script th:src="@{/js/[( ${model.moduleName()} )]/[( ${model.domainId()} )].js}"></script>
</th:block>
</body>
</html>
