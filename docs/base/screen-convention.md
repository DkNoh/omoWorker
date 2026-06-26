# 화면 규약 (Screen Convention)

v3 업무 화면은 v2 운영 화면과 동일한 구조로 생성한다. 이 문서는 v2의 화면 규약을 v3 기준으로 고정한 것이다.

화면을 새로 만들 때 이 문서의 골격을 그대로 사용한다. 화면마다 새로운 레이아웃이나 그리드 방식을 임의로 만들지 않는다.

## 기술 스택

| 항목 | 기준 |
|---|---|
| 레이아웃 | Thymeleaf Layout Dialect + `defaultLayout.html` |
| UI 프레임워크 | CoreUI (Bootstrap 기반) — `static/vendor/coreui` 로컬 참조 |
| 그리드/페이징 | TUI Grid + TUI Pagination — `static/lib` 로컬 참조 |
| 날짜 입력 | Toast UI DatePicker — `static/lib/tui-date-picker.min.js`, `static/lib/tui-date-picker.css` |
| HTTP 클라이언트 | axios — `static/lib/axios.min.js` |
| 엑셀 | xlsx — `static/lib/xlsx.full.min.js` |
| 아이콘 | lucide 로컬 번들 — `static/lib/lucide.js`, `data-lucide` 속성으로 렌더링 |
| 공통 CSS | `static/css/admin-common.css` |
| 공통 JS | `static/js/common/common-utils.js`, `tui-common.js`, `tui-page-builder.js` |
| 날짜 라이브러리 | day.js — `static/lib/dayjs.min.js` + `ko.js` (한국어 locale 전역 활성화: `dayjs.locale('ko')`) |

폐쇄망 기준이므로 CDN 참조를 금지한다. 모든 라이브러리는 `static/lib`, `static/vendor` 로컬 파일만 사용한다.
아이콘도 CDN 대신 `lucide.js` 로컬 파일만 사용한다. 버튼/메뉴/모달 아이콘은 `data-lucide="search"`처럼 이름으로 지정하고, 공통 `CommonUtils.refreshIcons()`가 렌더링을 갱신한다.

## 공통 레이아웃 구조

- 모든 업무 화면은 `layout:decorate="~{defaultLayout}"`를 사용한다.
- 본문은 `<main layout:fragment="content">` 안에 작성한다.
- 화면 전용 스크립트는 `<th:block layout:fragment="script">` 안에 둔다.
- 좌측 사이드바(접기 가능, 256px/64px), 상단 헤더는 `fragments/sidebar.html`, `fragments/header.html`로 분리한다.
- 공통 라이브러리/CSS/JS 로드는 `defaultLayout.html`에서만 한다. 개별 화면에서 중복 로드하지 않는다.

## 목록 화면 표준 골격

목록 화면은 아래 3개 영역 순서를 고정한다.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{defaultLayout}">
<head>
    <title>화면명</title>
</head>
<body>
<main layout:fragment="content">

    <!-- 1. 화면 제목 + 메뉴 경로 -->
    <div class="content-header">
        <h2>화면명 <small>(대메뉴 &gt; 화면명)</small></h2>
    </div>

    <!-- 2. 검색조건 카드 -->
    <div class="card mb-4 shadow-sm border-0">
        <div class="card-body">
            <div class="row align-items-center g-3">
                <div class="col-auto"><label for="검색조건ID" class="col-form-label fw-bold">조건명</label></div>
                <div class="col-auto"><input type="text" id="검색조건ID" class="form-control"></div>
                <!-- 검색조건 반복 -->
                <div class="col-auto ms-auto d-flex gap-2">
                    <button type="button" id="btn-search" class="btn btn-primary px-4">조회</button>
                    <button type="button" id="btn-reset" class="btn btn-secondary px-3">초기화</button>
                    <button type="button" id="btn-excel" class="btn btn-success ms-1">엑셀</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 3. 그리드 카드 -->
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-header bg-white d-flex justify-content-between align-items-center py-3">
            <span class="fs-6 fw-medium">총 <strong class="text-primary" id="total-count">0</strong>건</span>
            <div class="d-flex align-items-center gap-2">
                <select id="pageSizeSelect" class="form-select form-select-sm" style="width:80px;">
                    <option value="10">10건</option><option value="20">20건</option><option value="50">50건</option>
                </select>
            </div>
        </div>
        <div class="card-body p-0"><div id="grid" style="width:100%;"></div></div>
        <div class="card-footer bg-white border-0 py-3"><div id="pagination" class="d-flex justify-content-center"></div></div>
    </div>

</main>
<th:block layout:fragment="script">
    <script th:src="@{/js/도메인/화면명.js}"></script>
</th:block>
</body>
</html>
```

## ID / 파일 명명 규칙

| 대상 | 규칙 |
|---|---|
| 조회 버튼 | `btn-search` |
| 초기화 버튼 | `btn-reset` |
| 엑셀 버튼 | `btn-excel` |
| 그리드 컨테이너 | `grid` |
| 페이지네이션 | `pagination` |
| 총 건수 | `total-count` |
| 페이지 크기 | `pageSizeSelect` |
| 화면 JS | `static/js/<도메인>/<화면명>.js` (화면 HTML 파일명과 동일) |
| 검색조건 input id | SearchRequestDTO 필드명과 동일한 camelCase |

## 그리드 초기화 규칙

목록 화면 그리드는 `TuiPageBuilder`로만 초기화한다. 화면별로 TUI Grid를 직접 생성하지 않는다.

```javascript
const pageBuilder = new TuiPageBuilder({
    el:           'grid',
    apiUrl:       '/도메인/data',
    searchInputs: ['검색조건ID1', '검색조건ID2'],
    rowHeaders:   ['rowNum'],
    columns: [
        { header: '컬럼명', name: 'voFieldName', width: 110, align: 'center' }
    ]
});
```

- `columns.name`은 VO 필드명과 1:1로 일치해야 한다.
- `searchInputs`는 SearchRequestDTO 필드명과 1:1로 일치해야 한다.

## 공통 JS 유틸 규칙

화면 JS에서 아래 기능은 직접 구현하지 않고 공통 유틸을 사용한다.

| 기능 | 사용법 |
|---|---|
| 알림 | `CommonUtils.toast('저장되었습니다.', 'success')` |
| 확인 모달 | `CommonUtils.confirm('삭제하시겠습니까?', callback)` |
| 금액/전화번호 포맷 | `CommonUtils.fmt.money(...)`, `CommonUtils.fmt.phone(...)` |
| 공통코드 콤보 | `<select data-code-type="...">` + `CommonUtils.initCombos()` |
| 자동완성 | `CommonUtils.initAutocomplete({...})` |
| 공통코드 API | `/api/common-code/{type}` — 지원 타입: `dept`(SMS.DEP), `role`(TB_ROLE). 추가 절차는 `common-code-api-implementation.md` |

`alert()`, `confirm()` 브라우저 기본 다이얼로그는 사용하지 않는다.

## TuiPageBuilder / TuiCommon 계약

공통 그리드 자산의 암묵 규약을 명문화한 것이다. 화면 JS는 이 계약을 전제로 작성한다.

| 항목 | 계약 |
|---|---|
| 그리드 공통 옵션 | `TuiCommon.gridDefaults`가 단일 통제점 (rowHeight 42, minBodyHeight 300, scroll 없음). 화면별 예외는 `config.gridOptions`로 넘긴다 |
| 총 건수 표시 | `id="total-count"` 요소 기준. PageBuilder가 자동 갱신한다 |
| 날짜 전송 형식 | Toast UI DatePicker 검색 input(`data-search-type="date"`) 값은 `-`가 제거된 `YYYYMMDD` 문자열로 전송된다 (datetime-local은 `YYYYMMDDHHMMSS`). DATE/TIMESTAMP 컬럼과 비교하는 SQL은 `TO_DATE(#{변수}, 'YYYYMMDD')`로 감싼다 |
| 기간 검증 | input id가 정확히 `startDate`/`endDate`일 때만 시작일>종료일 검증이 동작한다 |
| 날짜 컬럼 표시 | LocalDate/LocalDateTime 컬럼은 `formatter: TuiCommon.fmt.date`를 붙인다 (scaffold가 자동 부착) |
| 상태/유형 badge | `TuiCommon.fmt.sendStatus`(SUCCESS/FAIL/WAIT), `fmt.sendType`(SMS/LMS/ALIMTALK), `fmt.resendYn` 사용. 상태값이 추가되면 `tui-common.js`도 함께 갱신한다 |
| 데이터 통신 | **모든 HTTP 호출은 axios로 통일한다** (2026-06-12, fetch 사용처 제거). 전역 인터셉터가 스피너/언래핑/오류 모달을 일괄 담당하므로 화면 JS에서 fetch를 쓰지 않는다 |

## 상세폼 화면 규약 (조회 -> 수정 -> 저장)

목록형(그리드)과 함께 BASE의 두 번째 표준 화면 유형이다.

```text
화면 진입 -> 검색조건 입력 -> 조회
 -> GET /domain/detail (단건 조회, ApiResponse<VO 또는 DetailDTO>)
 -> FormBinder.bind('#detailForm', res.data)   // name 기준 자동 바인딩
 -> 사용자 수정 -> 저장
 -> POST /domain/update (FormBinder.toObject 결과를 UpdateRequestDTO로 수신)
 -> Service 검증/트랜잭션 -> MyBatis update -> 성공/실패
```

### name 일치 계약

수정 가능한 form 필드의 `name` = 응답 JSON 필드명 = `UpdateRequestDTO` 프로퍼티명을 일치시킨다.
상세 모달에 표시만 하는 읽기전용 필드는 `name`을 두지 않거나 `disabled` 처리해 update payload에 들어가지 않게 한다.

```html
<form id="detailForm">
    <input type="hidden" name="msgId">
    <input type="hidden" name="beforeUpdateDttm">  <!-- 낙관적 잠금용 -->
    <input type="text" name="msgNm">
    <input type="checkbox" name="useYn">
    <div class="readonly-field" data-field="regDttm">2026-06-18 10:00</div>
    <textarea name="msgCont"></textarea>
</form>
```

### FormBinder (`static/js/common/form-binder.js`)

| API | 동작 |
|---|---|
| `FormBinder.bind(selector, data)` | `data[name]`이 있는 필드만 채움. checkbox는 'Y'/true -> checked, radio는 value 일치 항목 checked. 없는 필드는 건드리지 않음 |
| `FormBinder.toObject(selector)` | name 필드를 객체로 수집. disabled 제외, checkbox -> 'Y'/'N', **빈 문자열 -> null 전송 (확정 정책)** |

```javascript
// 조회
const res = await axios.get('/system/message/detail', { params: { msgId } });
FormBinder.bind('#detailForm', res.data);

// 저장
await axios.post('/system/message/update', FormBinder.toObject('#detailForm'));
CommonUtils.toast('저장되었습니다.', 'success');
```

### 보안 기준 (자동 바인딩 != 자동 업데이트)

- 조회 응답은 VO(전체 컬럼) 가능. 그러나 **수정 요청은 반드시 `*UpdateRequestDTO`(화이트리스트)로만 받는다.**
- `UpdateRequestDTO`에는 수정 가능한 필드만 선언한다. `REG_ID`/`REG_DTTM`, 시스템 필드, 권한 필드는 선언하지 않는다 — 선언하지 않으면 Jackson이 버리므로 DTO 자체가 화이트리스트다.
- 상세 모달은 표시 필드와 수정 필드를 분리한다. 사용자는 여러 값을 볼 수 있지만, `editable=true` 필드만 input `name`을 갖고 update payload에 포함된다.
- 프론트는 편의 장치일 뿐이다. 화면에서 실수로 많은 값을 보내더라도 서버는 `*UpdateRequestDTO`에 선언된 필드만 수신하고, Mapper XML도 DTO의 수정 허용 필드만 `UPDATE SET`에 사용한다.
- VO를 `@RequestBody`로 받지 않는다 (`ConventionTest`가 자동 검출).
- 소유자 키(EMP_ID/DEP_ID)를 hidden으로 받더라도 서버에서 principal과 재검증한다.

### 낙관적 잠금

- 조회 시 `UPDATE_DTTM`을 `beforeUpdateDttm` hidden에 보관하고 저장 시 함께 전송한다.
- update SQL은 `WHERE PK = #{pk} AND UPDATE_DTTM = #{beforeUpdateDttm}`.
- Service는 update 결과 0건이면 `CustomException(ErrorCode.UPDATE_CONFLICT)`(409, C004)로 실패 처리한다.

## 날짜 처리 규약 (day.js)

화면 JS의 날짜 처리는 day.js를 사용한다. 수기 문자열 조작(substring/replace 체인)으로 날짜를 다루지 않는다.

| 용도 | 형식 | 처리 주체 |
|---|---|---|
| 그리드 표시 | 날짜만 `YYYY-MM-DD`, 일시 `YYYY-MM-DD HH:mm` | `TuiCommon.fmt.date` (컬럼 formatter) |
| 서버 전송 | DatePicker 검색 input(`data-search-type="date"`) -> `YYYYMMDD`, datetime-local -> `YYYYMMDDHHmm` | `TuiPageBuilder`가 자동 변환 |
| 검색조건 기본값 | 오늘 날짜 자동 세팅 | `CommonUtils.setDefaultDateTime` (PageBuilder가 호출) |
| 화면별 날짜 계산 | `dayjs().subtract(1, 'month').format('YYYY-MM-DD')` 형태 | 화면 JS에서 dayjs 직접 사용 |

## axios 응답 처리 규약 (중요)

`common-utils.js`의 전역 인터셉터가 모든 axios 응답을 가공한다. 화면 JS는 이 규약을 전제로 작성한다.

- 성공(code 200): `ApiResponse` 껍데기를 벗긴다. **`response.data`가 곧 알맹이(data)다.** `response.data.data`로 접근하면 안 된다.
- 업무 오류/HTTP 오류: 인터셉터가 서버 메시지를 모달로 표시하고 reject한다. 화면 JS의 catch에서 알림을 중복으로 띄우지 않는다.
- `ApiResponse`가 아닌 응답(예: 세션 만료로 받은 로그인 HTML)은 가공 없이 통과하므로 `typeof response.data === 'string'`으로 구분할 수 있다.

## 금지

- CDN 참조 금지. 로컬 `static/lib`, `static/vendor`만 사용한다.
- 화면별 자체 레이아웃, 자체 그리드 구현 금지.
- `defaultLayout.html`을 거치지 않는 업무 화면 금지 (login.html은 예외).
- 화면에서 권한을 임의 계산하지 않는다. 메뉴/버튼 권한은 서버가 내려준 값만 사용한다.
- 개인정보는 마스킹된 값만 화면에 표시한다.

## 공통 자산 이식 상태

v2 공통 UI 자산 이식은 완료되었다. 상세 내역은 `docs/base/ui-assets-implementation.md`를 따른다.

- `SESSION_INFO` 전역변수는 v3 인증 모델 기준 `empId`, `depId`, `empNm`, `depNm`을 노출한다.
- 사이드바/헤더의 `user`, `menus` 모델은 `GlobalModelAdvice`가 공통 주입한다. 개별 Controller에서 중복 주입하지 않는다.
- `index.html`은 `defaultLayout` 기반으로 정렬되었다. `login.html`은 레이아웃 미적용 예외 화면이다.
