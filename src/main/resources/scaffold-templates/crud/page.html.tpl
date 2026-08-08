<!DOCTYPE html>
<!-- Scaffold 생성(CRUD). 생성 후 개발자가 직접 수정해 소유한다. -->
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

    <!-- QuerySpec 검색조건과 권한 기반 등록/조회 동작 영역. -->
    <div class="card mb-4 shadow-sm border-0 scaffold-search-card">
        <div class="card-body">
            <div class="row align-items-center g-3 flex-wrap">
[# th:each="searchParam : ${model.searchParams()}"][# th:if="${searchParam.isBetweenRangeEnd()}"]                <div class="col-auto scaffold-date-range-separator" aria-hidden="true">~</div>
[/]                <div class="col-auto"><label for="[( ${searchParam.name()} )]" class="col-form-label fw-bold">[( ${model.htmlEscape(searchParam.label())} )]</label></div>
                <div class="col-auto">[# th:if="${searchParam.isSelect()}"]<select id="[( ${searchParam.name()} )]" class="form-select scaffold-search-control" name="[( ${searchParam.name()} )]" aria-label="[( ${model.htmlEscape(searchParam.label())} )]"><option value="">전체</option>[# th:each="option : ${searchParam.options()}"]<option value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</option>[/]</select>[/][# th:if="${searchParam.isRadio()}"]<div id="[( ${searchParam.name()} )]" class="d-flex align-items-center gap-2 scaffold-radio-group" role="radiogroup" aria-label="[( ${model.htmlEscape(searchParam.label())} )]"><label class="form-check-label"><input class="form-check-input me-1" type="radio" name="[( ${searchParam.name()} )]" value="" checked>전체</label>[# th:each="option : ${searchParam.options()}"]<label class="form-check-label"><input class="form-check-input me-1" type="radio" name="[( ${searchParam.name()} )]" value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</label>[/]</div>[/][# th:if="${searchParam.isDate()}"]<div class="scaffold-date-field scaffold-date-field-md"><div class="tui-datepicker-input tui-datetime-input scaffold-datepicker-input"><input type="text" id="[( ${searchParam.name()} )]" data-search-type="date" autocomplete="off" aria-label="[( ${model.htmlEscape(searchParam.label())} )]"><span class="tui-ico-date" aria-hidden="true"></span></div><div id="[( ${searchParam.name()} )]PickerLayer" class="scaffold-date-picker-layer"></div></div>[/][# th:if="${!searchParam.isSelect() and !searchParam.isRadio() and !searchParam.isDate()}"]<input type="text" id="[( ${searchParam.name()} )]" class="form-control scaffold-search-control" aria-label="[( ${model.htmlEscape(searchParam.label())} )]">[/]</div>
[/]                <div class="col-auto ms-auto d-flex gap-2">
                    <button type="button" id="btn-create" th:if="${pageAuth.create}" class="btn btn-outline-primary px-3" aria-label="신규 등록"><i data-lucide="plus" aria-hidden="true"></i><span>등록</span></button>
                    <button type="button" id="btn-search" class="btn btn-primary px-4" aria-label="조회"><i data-lucide="search" aria-hidden="true"></i><span>조회</span></button>
                    <button type="button" id="btn-reset" class="btn btn-secondary px-3" aria-label="검색 조건 초기화"><i data-lucide="rotate-ccw" aria-hidden="true"></i><span>초기화</span></button>
                </div>
            </div>
        </div>
    </div>

    <!-- 행 선택은 화면 JS에서 등록/수정 모달의 update 모드로 연결한다. -->
    <div th:replace="~{fragments/toast-grid :: gridCard(null, null, null, null)}"></div>

    <!-- 등록과 수정을 공유하는 상세 모달. PK·잠금 스냅샷은 hidden, 비수정 컬럼은 읽기 전용으로 유지한다. -->
    <th:block th:replace="~{fragments/modal-base :: layout(
        modalId='[( ${model.domainId()} )]-modal',
        title='[( ${model.domainName()} )]',
        size='modal-lg',
        bodyContent=~{::#modal-body},
        footerContent=null
    )}">
        <div id="modal-body">
            <form id="detail-form" autocomplete="off" novalidate>
[# th:each="pkField : ${model.pkFieldNames()}"]                <input type="hidden" name="[( ${pkField} )]">
[/][# th:if="${!model.lockColumn().isEmpty()}"]                <input type="hidden" name="[( ${model.beforeLockFieldName()} )]">
[/][# th:each="row : ${model.modalRows()}"]                <div class="row g-0 form-detail-row">
[# th:each="column : ${row}"]                    <div class="col-12 col-sm-2 form-detail-label">
[# th:if="${column.editable()}"]                        <label[# th:if="${column.isRequired()}"] class="form-detail-required"[/] for="f-[( ${column.fieldName()} )]">[( ${model.htmlEscape(column.headerName())} )]</label>
[/][# th:if="${!column.editable()}"]                        <span>[( ${model.htmlEscape(column.headerName())} )]</span>
[/]                    </div>
                    <div class="col-12 col-sm-[( ${row.size() == 1 ? '10' : '4'} )] form-detail-control">
[# th:if="${!column.editable()}"]                        <div class="form-control-plaintext" data-readonly-field="[( ${column.fieldName()} )]"></div>
[/][# th:if="${column.editable() and column.hasOptions()}"]                        <select id="f-[( ${column.fieldName()} )]" class="form-select" name="[( ${column.fieldName()} )]" aria-label="[( ${model.htmlEscape(column.headerName())} )]"[# th:if="${column.hasInputMask()}"] data-mask="[( ${model.htmlEscape(column.inputMask())} )]"[/][# th:if="${column.hasValidate()}"] data-validate="[( ${model.htmlEscape(column.validate())} )]"[/]>
                            <option value="">선택</option>
[# th:each="option : ${column.options()}"]                            <option value="[( ${model.htmlEscape(option.value())} )]">[( ${model.htmlEscape(option.label())} )]</option>
[/]                        </select>
[/][# th:if="${column.editable() and !column.hasOptions()}"]                        <input type="[( ${column.isNumeric() ? 'number' : 'text'} )]" class="form-control" id="f-[( ${column.fieldName()} )]" name="[( ${column.fieldName()} )]"[# th:if="${column.hasInputMask()}"] data-mask="[( ${model.htmlEscape(column.inputMask())} )]"[/][# th:if="${column.hasValidate()}"] data-validate="[( ${model.htmlEscape(column.validate())} )]"[/]>
[/]                    </div>
[/]                </div>
[/]            </form>
        </div>
    </th:block>

</main>
<th:block layout:fragment="script">
    <!-- 공통 FormBinder/ModalManager/FieldFormat 로드 이후 화면별 CRUD 흐름을 초기화한다. -->
    <script th:src="@{/js/[( ${model.moduleName()} )]/[( ${model.domainId()} )].js}"></script>
</th:block>
</body>
</html>
