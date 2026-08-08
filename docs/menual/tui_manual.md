# TUI 공통 모듈 매뉴얼 (`tui-common.js` · `tui-page-builder.js`)

> 대상 파일
> - `src/main/resources/static/js/common/tui-common.js`
> - `src/main/resources/static/js/common/tui-page-builder.js`
>
> 관련 규약: `docs/base/screen-convention.md` "TuiPageBuilder / TuiCommon 계약" 절, `docs/base/grid-formatter-guide.md`
>
> 이 문서는 **프로젝트의 래퍼(wrapper) 모듈**을 설명한다. TUI Grid / TUI DatePicker / TUI Pagination 라이브러리 자체의 API는 다루지 않는다.

---

## 목차

1. [개요](#1-개요)
2. [로드 순서와 전역 환경](#2-로드-순서와-전역-환경)
3. [`tui-common.js` — `TuiCommon`](#3-tui-commonjs--tuicommon)
4. [`tui-page-builder.js` — `TuiPageBuilder`](#4-tui-page-builderjs--tuipagebuilder)
5. [화면 JS 사용 패턴](#5-화면-js-사용-패턴)
6. [확장 가이드](#6-확장-가이드)
7. [의존 라이브러리](#7-의존-라이브러리)
8. [Troubleshooting](#8-troubleshooting)
9. [계약 요약 (Quick Reference)](#9-계약-요약-quick-reference)

---

## 1. 개요

### 1.1 이 모듈들이 하는 일

이 프로젝트의 모든 목록형 업무 화면은 TOAST UI(TUI) 라이브러리 패밀리 위에 올라간다.
`tui-common.js`와 `tui-page-builder.js`는 라이브러리와 화면 JS 사이에 놓인 **프로젝트 전용 래퍼 계층**이다.

```text
┌─────────────────────────────────────────────────────────────┐
│  생성된 화면 JS / 정적 샘플 JS                              │
│  - 컬럼 정의, 업무 이벤트(행 클릭/모달/저장) 만 작성          │
└──────────────────────────┬──────────────────────────────────┘
                           │ 사용
┌──────────────────────────▼──────────────────────────────────┐
│  tui-page-builder.js  (class TuiPageBuilder)                │
│  - 그리드 초기화 / 검색 / 페이징 / 비동기 조회를 한 곳에서 자동화│
│  - 단순 CRUD 백오피스 화면의 반복 코드를 제거하는 Facade       │
└──────────────────────────┬──────────────────────────────────┘
                           │ 사용
┌──────────────────────────▼──────────────────────────────────┐
│  tui-common.js  (const TuiCommon)                           │
│  - formatter(날짜/배지/마스킹), 그리드 기본 옵션,              │
│    총 건수 갱신, 페이징 버튼 렌더링, 엑셀 내보내기             │
└──────────────────────────┬──────────────────────────────────┘
                           │ 래핑
┌──────────────────────────▼──────────────────────────────────┐
│  TOAST UI 라이브러리 (static/lib, 로컬 참조)                  │
│  tui-grid 4.21.22 · tui-date-picker 4.3.3 · tui-pagination  │
│  + xlsx(SheetJS) · dayjs · axios                            │
└─────────────────────────────────────────────────────────────┘
```

| 모듈 | 형태 | 역할 |
|---|---|---|
| `tui-common.js` | IIFE가 만드는 전역 `const TuiCommon` 객체 | 상태 없는(stateless) 유틸 모음. formatter, 그리드 기본 옵션, 페이징/총 건수 DOM 렌더링, 엑셀 내보내기 |
| `tui-page-builder.js` | 전역 `class TuiPageBuilder` | 화면 단위 인스턴스. 그리드 생성 → 검색 조건 수집 → `axios` 조회 → 데이터 적재 → 페이징/총 건수 갱신까지의 흐름을 자동화 |

### 1.2 설계 원칙

- **목록 그리드는 `TuiPageBuilder`로만 초기화한다.** 화면에서 `new tui.Grid(...)`를 직접 호출하지 않는다 (`screen-convention.md`).
- **그리드 공통 옵션의 단일 통제점은 `TuiCommon.gridDefaults`다.** 화면별 예외만 `config.gridOptions`로 덮어쓴다.
- **도메인 코드값(SMS/LMS, SUCCESS/FAIL 등)은 공통 JS가 알지 못한다.** 화면이 `badgeByValue({ labels, tones })`로 선언한다. 그래서 공통 모듈을 건드리지 않고 300개 화면으로 확장할 수 있다.
- **HTTP는 axios로 통일한다.** 전역 인터셉터(`http-client.js` / `common-utils.js`)가 스피너, `ApiResponse` 언래핑, 오류 모달, 세션 만료 전환을 담당하므로 화면 JS는 `fetch`를 쓰지 않는다.

---

## 2. 로드 순서와 전역 환경

### 2.1 `defaultLayout.html` 로드 순서

모든 업무 화면은 `layout:decorate="~{defaultLayout}"`로 이 레이아웃을 상속한다. 스크립트 로드 순서는 고정되어 있다.

```html
<!-- 1) TUI + 보조 라이브러리 (static/lib 로컬 참조, CDN 금지) -->
<link rel="stylesheet" href="/lib/tui-pagination.css" />
<link rel="stylesheet" href="/lib/tui-date-picker.css" />
<link rel="stylesheet" href="/lib/tui-grid.css" />

<script src="/lib/tui-pagination.min.js"></script>
<script src="/lib/xlsx.full.min.js"></script>      <!-- grid.export('xlsx')에 필요 -->
<script src="/lib/tui-date-picker.min.js"></script>
<script src="/lib/tui-grid.min.js"></script>
<script src="/lib/axios.min.js"></script>
<script src="/lib/dayjs.min.js"></script>
<script src="/lib/ko.js"></script>                 <!-- dayjs 한국어 로케일 -->
<script src="/lib/lucide.js"></script>
<script src="/lib/imask.min.js"></script>
<script src="/lib/just-validate.min.js"></script>
<script>dayjs.locale('ko');</script>

<!-- 2) CoreUI + 프로젝트 CSS -->

<!-- 3) 공통 JS — 의존 순서대로 로드 -->
<script src="/js/common/notify.js"></script>
<script src="/js/common/http-client.js"></script>
<script src="/js/common/modal-manager.js"></script>
<script src="/js/common/common-utils.js"></script>
<script src="/js/common/form-binder.js"></script>
<script src="/js/common/field-format.js"></script>
<script src="/js/common/tui-common.js"></script>        <!-- ① -->
<script src="/js/common/tui-page-builder.js"></script>  <!-- ② -->

<!-- 4) Thymeleaf가 주입하는 전역 변수 -->
<script th:inline="javascript">
    const SESSION_INFO = { empId, depId, empNm, depNm };
    window.PAGE_AUTH = {
        read, create, update, delete,
        approve, cancel, download, maskView
    };
</script>

<!-- 5) 화면 JS (layout:fragment="script") -->
```

규칙:

- `tui-common.js`(①)는 반드시 `tui-page-builder.js`(②)보다 먼저 로드된다. 빌더가 `TuiCommon.gridDefaults` 등을 참조하기 때문이다.
- 화면 JS는 `layout:fragment="script"` 블록에서 로드되므로 항상 공통 JS 이후에 실행된다.
- 화면 초기화는 `document.addEventListener('DOMContentLoaded', ...)` 안에서 수행한다.

### 2.2 `window.PAGE_AUTH`

서버가 메뉴 권한(`TB_MENU_AUTH`)을 해석해 페이지별로 주입하는 권한 객체다. 모든 값은 boolean이며, `TuiPageBuilder`와 화면 JS는 이 값을 **strict(`=== true`)** 로 검사한다.

| 필드 | 용도 |
|---|---|
| `read` | 목록 조회 |
| `create` / `update` / `delete` | 등록 / 수정 / 삭제 |
| `approve` / `cancel` | 승인 / 취소 |
| `download` | 엑셀 다운로드. **그리드 우클릭 메뉴(컨텍스트 메뉴) 구성에 직접 사용된다** — [4.5절](#45-page_auth-연동--그리드-컨텍스트-메뉴) 참고 |
| `maskView` | 개인정보 마스킹 해제 여부 |

> 권한의 최종 검증은 서버(`MenuAuthInterceptor`)가 담당한다. `PAGE_AUTH` 검사는 "사용자에게 가능한 동작만 보여 주는" UI 제어다.

---

## 3. `tui-common.js` — `TuiCommon`

IIFE(즉시 실행 함수)로 캡슐화된 전역 객체다. 내부 헬퍼(`rawValue`, 배지 색상 캐시 등)는 노출되지 않고, 아래 공개 멤버만 사용할 수 있다.

```javascript
const TuiCommon = (() => {
    // ... 내부 구현
    return {
        fmt,               // formatter 모음
        badgeByValue,      // 배지 formatter 팩토리
        formatDate,        // 날짜 포맷 유틸
        maskValue,         // 개인정보 마스킹 유틸
        gridDefaults,      // 그리드 공통 옵션
        updateTotalCount,  // 총 건수 텍스트 갱신
        renderPagination,  // 페이징 버튼 렌더링
        exportExcel,       // 그리드 엑셀 내보내기
    };
})();
```

### 3.1 공개 API 한눈에 보기

| 멤버 | 종류 | 시그니처 | 용도 |
|---|---|---|---|
| `fmt.date` | formatter | `(v) => string` | 날짜/일시 자동 포맷 (그리드 컬럼용) |
| `badgeByValue` | 팩토리 | `({ labels?, tones? }) => ({ value }) => string` | 코드값 → 색상 배지 formatter 생성 |
| `formatDate` | 유틸 | `(value, pattern = 'YYYY-MM-DD') => string` | dayjs 기반 날짜 포맷 |
| `maskValue` | 유틸 | `(value, type) => string` | 개인정보 마스킹 (PHONE/NAME/EMAIL/RRN) |
| `gridDefaults` | 상수 객체 | — | 그리드 공통 옵션 단일 통제점 |
| `updateTotalCount` | DOM 유틸 | `(count, selector = '#total-count') => void` | 총 건수 표시 갱신 |
| `renderPagination` | DOM 유틸 | `(page, totalPages, onMove, paginationId = 'pagination') => void` | Bootstrap 버튼 기반 페이징 렌더링 |
| `exportExcel` | 그리드 유틸 | `(gridObj, fileName = 'download') => void` | TUI Grid 내장 엑셀 내보내기 |

### 3.2 `fmt.date` — 날짜 컬럼 formatter

```javascript
fmt.date: v => string
```

- **파라미터** `v`: TUI Grid formatter 호출 규칙인 `{ value, row }` 객체 **또는** 직접 호출 시 원시 문자열. 내부의 `rawValue()`가 두 형태를 모두 처리한다.
- **반환**: `string`. 빈 값은 `'-'`, 파싱 실패 시 원본 문자열.
- **포맷 규칙**: 값의 문자열 길이가 10보다 길면(시간 포함) `YYYY-MM-DD HH:mm`, 아니면 `YYYY-MM-DD`. 즉 `LocalDate`와 `LocalDateTime`을 하나의 formatter로 처리한다.

```javascript
columns: [
    { header: 'SENT_AT', name: 'sentAt', align: 'center', width: 150,
      formatter: TuiCommon.fmt.date }
]
// '2026-07-28'            -> '2026-07-28'
// '2026-07-28T10:30:00'   -> '2026-07-28 10:30'
// null / ''               -> '-'
```

> Scaffold가 날짜 계열 컬럼(`*_DT`, `*_DTTM`, `*_AT`)에 이 formatter를 자동 부착한다.

### 3.3 `badgeByValue` — 자동 색상 배지 팩토리

```javascript
badgeByValue({ labels = {}, tones = {} } = {}) => ({ value }) => string
```

코드값 컬럼에 **가장 추천하는** formatter다. 값마다 다른 색의 Bootstrap/CoreUI 배지(`<span class="badge ...">`)를 자동으로 만들어 준다.

- **파라미터**
  - `labels`: `{ 코드값: 표시라벨 }` 매핑. 생략하면 코드값 자체가 라벨이 된다.
  - `tones`: `{ 코드값: CSS 클래스 }` 매핑. 특정 값의 색을 고정한다. 생략한 값은 **라벨 해시 기반 자동 색**을 받는다.
- **반환**: TUI Grid formatter 함수 `({ value }) => string`. 빈 값은 `'-'`.
- **자동 색 규칙**: 내부 6색 팔레트(`bg-primary`, `bg-success`, `bg-danger`, `bg-warning text-dark`, `bg-info text-dark`, `bg-secondary`)를 라벨 문자열 해시로 선택한다. **같은 라벨은 항상 같은 색**이 보장되고, 결과는 내부 캐시(`badgeColorCache`)에 저장된다.

```javascript
// 방식 A — 값 그대로 표시 (코드값 자체가 표시용)
{ header: '부서코드', name: 'deptCd', formatter: TuiCommon.badgeByValue() }

// 방식 B — 코드값 → 한글 라벨 (가장 흔한 패턴)
{
    header: '승인상태', name: 'apprStat',
    formatter: TuiCommon.badgeByValue({
        labels: { APPR: '승인', REJ: '반려', REAPPR: '재승인' }
    })
}

// 방식 C — 특정 값의 색 고정 (의미론적 색상)
{
    header: '발송상태', name: 'sendStatus',
    formatter: TuiCommon.badgeByValue({
        labels: { SUCCESS: '성공', FAIL: '실패', WAIT: '대기' },
        tones:  { SUCCESS: 'bg-success', FAIL: 'bg-danger',
                  WAIT: 'bg-warning text-dark' }
    })
}
```

상세한 선택 기준은 `docs/base/grid-formatter-guide.md` 2절을 참고한다.

### 3.4 `formatDate` — 날짜 포맷 유틸

```javascript
formatDate(value, pattern = 'YYYY-MM-DD') => string
```

- **파라미터**
  - `value`: 원시 값 또는 `{ value }` 객체. 내부 `rawValue()` 처리.
  - `pattern`: dayjs 포맷 문자열.
- **반환**: 빈 값은 `'-'`, 유효하지 않은 날짜는 `String(value)`, 그 외 `dayjs(value).format(pattern)`.

formatter가 아닌 일반 화면 코드(예: 모달에 날짜 표시)에서 직접 호출할 때 사용한다. 그리드 컬럼에는 `fmt.date`를 쓰는 것이 정석이다.

### 3.5 `maskValue` — 개인정보 마스킹 유틸

```javascript
maskValue(value, type) => string
```

- **파라미터**
  - `value`: 원시 값 또는 `{ value }` 객체.
  - `type`: `'PHONE'` | `'NAME'` | `'EMAIL'` | `'RRN'`. 그 외 값은 원본 반환.
- **반환**: 빈 값은 `'-'`, 그 외 마스킹된 문자열.

| type | 입력 예 | 출력 예 | 규칙 |
|---|---|---|---|
| `PHONE` | `01012345678` | `010****5678` | 앞 3자리 + 중간 전체 `*` + 뒤 4자리 |
| `NAME` | `김철수` | `김**` | 첫 글자만 노출 (1글자는 `*`) |
| `EMAIL` | `kim@test.com` | `k**@test.com` | 첫 글자 + 로컬부 나머지 `*` + `@도메인` |
| `RRN` | `900101-1234567` | `900101-1******` | 앞 6자리 + 뒷자리 첫 숫자 + `******` |

> **주의**: 이 프로젝트의 1차 마스킹은 서버(`@PrivacyLog` / `MaskingUtil`)에서 수행된다. `maskValue`는 클라이언트에서 추가로 표시를 제어해야 하는 예외 상황용 보조 수단이며, Scaffold 생성 코드는 이 함수를 사용하지 않도록 계약되어 있다(`ScaffoldTemplateTest`가 검증).

### 3.6 `gridDefaults` — 그리드 공통 옵션

```javascript
const gridDefaults = {
    rowHeight: 38,
    bodyHeight: 380,
    minBodyHeight: 200,
    scrollX: true,
    scrollY: false,
};
```

`TuiPageBuilder._initGrid()`이 이 객체를 기본값으로 사용하고, 화면의 `config.gridOptions`로 덮어쓴다. **이 값이 모든 그리드의 단일 통제점**이다.

> ⚠️ `docs/base/screen-convention.md`의 계약 표에는 이 값이 `rowHeight 42, bodyHeight 420, scrollY true, minBodyHeight 300`으로 기재되어 있다(2026-07 기준 코드와 불일치). **실제 동작 기준은 이 코드의 값이며**, 화면 설계 시 코드 값을 기준으로 한다. 문서 정합성 정리는 별도 과제로 관리한다.

### 3.7 `updateTotalCount` — 총 건수 갱신

```javascript
updateTotalCount(count, selector = '#total-count') => void
```

- **파라미터**
  - `count`: 숫자. `Number(count).toLocaleString()`으로 천 단위 콤마 표시된다.
  - `selector`: 대상 요소 CSS selector. 기본값 `'#total-count'` (v3 화면 골격 기준).
- 요소가 없으면 아무 일도 하지 않는다(no-op).
- `TuiPageBuilder`가 조회 성공 후 자동 호출하므로 화면에서 직접 부를 일은 거의 없다.

```html
<!-- fragments/toast-grid.html 의 표시 구조 -->
<span>총 <strong class="text-primary" id="total-count">0</strong>건</span>
```

### 3.8 `renderPagination` — 페이징 버튼 렌더링

```javascript
renderPagination(page, totalPages, onMove, paginationId = 'pagination') => void
```

**중요**: 이 프로젝트의 페이징 UI는 TUI Pagination 위젯(`new tui.Pagination(...)`)이 아니라, 이 함수가 생성하는 **Bootstrap 버튼 기반 커스텀 페이징**이다. (`tui-pagination.min.js`/`.css`는 라이브러리 의존성으로 로드되지만, 화면 페이징 렌더링에는 이 함수가 사용된다.)

- **파라미터**
  - `page`: 현재 페이지 (1-based).
  - `totalPages`: 전체 페이지 수. `0` 이하면 컨테이너를 비우고 종료한다.
  - `onMove`: `(p: number) => void` — 페이지 버튼 클릭 콜백.
  - `paginationId`: 컨테이너 요소의 id. 기본값 `'pagination'`.
- **동작**
  - 10페이지 블록(`BLOCK = 10`) 단위로 `« ‹ 1..10 › »` 버튼을 렌더링한다.
  - 현재 페이지는 `btn-primary`, 나머지는 `btn-outline-secondary`.
  - 컨테이너에 `d-flex justify-content-center gap-1` 클래스를 추가한다.
  - 콜백은 `window['__movePage_' + paginationId.replace(/-/g, '_')]`에 등록되고 버튼의 `onclick` 인라인 핸들러가 호출한다. 예: id `pagination` → `window.__movePage_pagination`.

```javascript
TuiCommon.renderPagination(3, 25, (p) => loadData(p), 'pagination');
// « ‹ [1]..[10] › » 블록에서 3페이지가 강조됨
```

`TuiPageBuilder`가 조회 성공 후 자동 호출하며, 콜백은 `(p) => this.searchData(p)`로 연결된다.

### 3.9 `exportExcel` — 그리드 엑셀 내보내기

```javascript
exportExcel(gridObj, fileName = 'download') => void
```

- **파라미터**
  - `gridObj`: `tui.Grid` 인스턴스 (`pageBuilder.getGrid()`으로 취득). `null`/`undefined`면 no-op.
  - `fileName`: 저장 파일 이름(확장자 제외). 기본값 `'download'`.
- 내부적으로 `gridObj.export('xlsx', { fileName })`를 호출한다. **`xlsx.full.min.js`(SheetJS)가 로드되어 있어야 한다** — `defaultLayout.html`이 기본 로드한다.
- 클라이언트 사이드 내보내기(그리드에 적재된 데이터 대상)다. 검색 조건 전체의 서버 사이드 엑셀 다운로드는 별도 endpoint 방식(`window.location.href = '/도메인/excel?' + params`)을 사용한다 — [5.1절](#51-패턴-a-scaffold-excel-기본-생성물) 참고.

### 3.10 내부 헬퍼 (비공개)

참고용. 직접 호출할 수 없다.

| 헬퍼 | 역할 |
|---|---|
| `rawValue(v)` | `{ value }` 객체에서 `value` 추출, 원시 값은 그대로 반환. formatter/직접 호출 양쪽 지원의 핵심 |
| `badgeColorFor(value)` | 문자열 해시 → 6색 팔레트 인덱스. `badgeColorCache`로 결과 캐시 |

---

## 4. `tui-page-builder.js` — `TuiPageBuilder`

단순 CRUD(목록 조회 위주) 백오피스 화면에서 반복되는 **그리드 초기화 · 페이징 · 비동기 통신(axios) · 공통 버튼 이벤트(조회/초기화/페이지 사이즈)** 를 단일 진입점(Facade)에서 자동화하는 클래스다. 화면 JS는 컬럼 정의와 업무 상호작용만 작성하면 된다.

### 4.1 `config` 전체 레퍼런스

```javascript
new TuiPageBuilder(config)
```

생성자는 아래 기본값과 `config`를 `Object.assign`으로 병합한다.

| 키 | 타입 | 기본값 | 필수 | 설명 |
|---|---|---|---|---|
| `el` | `string` | `'grid'` | — | 그리드가 렌더링될 DOM 요소의 **id** |
| `apiUrl` | `string` | `''` | **필수** | 목록 데이터를 조회할 백엔드 GET API URL |
| `searchInputs` | `string[]` | `[]` | — | 검색 조건 input/select 요소의 id 배열. **`SearchRequestDTO` 필드명과 1:1 일치**해야 한다 |
| `requiredInputs` | `string[]` | `[]` | — | 필수 검색 조건 id 배열. 비어 있으면 조회를 중단하고 경고 toast |
| `rowHeaders` | `Array` | `['rowNum']` | — | 그리드 좌측 헤더. `'rowNum'`(또는 `{type:'rowNum'}`)은 자동 번호 컬럼으로 변환된다 — [4.4절](#44-행-번호-자동-컬럼-rownum) 참고. 체크박스는 `['checkbox', 'rowNum']` |
| `columns` | `Object[]` | `[]` | **필수** | TUI Grid 컬럼 메타데이터 배열. **`name`은 응답 VO 필드명과 1:1 일치** |
| `pageSizeEl` | `string` | `'pageSizeSelect'` | — | 페이지 사이즈 select의 id |
| `btnSearch` | `string` | `'btn-search'` | — | 조회 버튼 id |
| `btnReset` | `string` | `'btn-reset'` | — | 초기화 버튼 id |
| `paginationId` | `string` | `'pagination'` | — | 페이징 컨테이너 id |
| `totalCountSelector` | `string` | `'#total-count'` | — | 총 건수 요소 selector |
| `gridOptions` | `Object` | `{}` | — | 화면별 그리드 옵션 예외. `gridDefaults` 위에 덮어쓴다 |
| `searchDefaults` | `Object` | `{}` | — | 검색 조건 기본값. 예: `{ startDt: 'THIS_MONTH', endDt: 'TODAY' }` — [4.7절](#47-검색조건-기본값-searchdefaults) 참고 |
| `datePickerInputs` | `string[] \| null` | `null` | — | DatePicker를 적용할 input id. `null`이면 `data-search-type="date"` 입력을 자동 감지 — [4.8절](#48-datepicker-자동-초기화) 참고 |
| `onGridUpdated` | `Function \| null` | `null` | — | 데이터 갱신 완료 후 콜백. `(page: PageResponseDTO) => void` |

> **레거시 키 경고**: 일부 화면 JS가 전달하는 `btnCreate` 키는 현재 빌더가 소비하지 않는다(no-op). 등록 버튼 이벤트는 화면 JS가 직접 연결한다.

### 4.2 생성자 초기화 순서

`new TuiPageBuilder(config)`가 실행되는 순간 아래 순서로 초기화되고, **마지막에 1페이지 자동 조회가 실행된다**.

```text
1. config 병합 (기본값 + 사용자 config)
2. 내부 상태 초기화
   - grid = null, currentPage = 1, currentSize = 10
   - usesOffsetRowNo = false, searchDatePickers = {}
3. pageSizeSelect 동기화 — select의 현재 value로 currentSize 결정
4. 날짜 기본 세팅
   - CommonUtils.setDefaultDateTime() (로드된 경우)
   - _applySearchDefaults(false)  — 빈 입력에만 기본값 적용
   - _initSearchDatePickers()     — 날짜 입력에 tui.DatePicker 부착
5. _initGrid()    — tui.Grid 생성 + empty-state 초기 표시
   _bindEvents()  — 조회/초기화/사이즈/Enter 이벤트 바인딩
6. searchData(1)  — 1페이지 자동 조회
```

따라서 화면 JS는 `DOMContentLoaded` 안에서 인스턴스를 생성하기만 하면 초기 목록이 표시된다.

### 4.3 공개 메서드 / 속성

#### `searchData(page = 1)` — 핵심 조회 메서드

```javascript
searchData(page = 1) => void
```

설정된 `apiUrl`로 검색 조건 + 페이징 정보를 담아 `axios.get`을 수행하고, 응답을 그리드/페이징/총 건수에 반영한다. 전체 흐름은 [4.9절](#49-데이터-로딩-흐름) 참고.

- 조회 버튼, 초기화 버튼, 페이지 사이즈 변경, Enter 키, 페이징 버튼이 모두 이 메서드를 호출한다.
- 저장/삭제 후 현재 페이지를 다시 그릴 때 화면에서 직접 호출한다:
  ```javascript
  await pageBuilder.searchData(pageBuilder.currentPage || 1);
  ```

#### 접근자 메서드

| 메서드 | 반환 | 용도 |
|---|---|---|
| `getGrid()` | `tui.Grid` 인스턴스 | 그리드 원본 객체 취득. `grid.on('click', ...)` 등 화면 전용 이벤트/제어에 사용 |
| `getCheckedRows()` | `Object[]` | 좌측 체크박스가 선택된 행 데이터 배열. 일괄 처리(일괄 승인 등)에 사용. `rowHeaders: ['checkbox']` 필요 |
| `getFocusedCell()` | `Object \| null` | 현재 포커스된 단일 셀 정보 |
| `getCurrentPage()` | `number` | 현재 페이지 번호. 수정/삭제 후 재조회에 사용 |
| `getSearchParams(options = {})` | `URLSearchParams` | 현재 검색 조건을 파라미터로 수집. `options.includePaging = true`면 `page`/`size` 포함 |

#### 공개 속성 (직접 접근)

| 속성 | 타입 | 설명 |
|---|---|---|
| `currentPage` | `number` | 현재 페이지. 화면 JS에서 `pageBuilder.currentPage || 1` 형태로 직접 읽기도 한다 |
| `currentSize` | `number` | 현재 페이지 사이즈 |
| `config` | `Object` | 병합된 최종 설정 |
| `grid` | `tui.Grid` | 그리드 인스턴스 (가급적 `getGrid()` 사용) |

#### `getSearchParams` 상세

```javascript
getSearchParams({ includePaging: false }) => URLSearchParams
```

- `searchInputs`에 등록된 id 순서대로 값을 수집해 `URLSearchParams`에 append한다.
- 값 읽기 규칙(`_readSearchValue`):
  - `type="date"` 또는 `data-search-type="date"` 입력 → `dayjs(value).format('YYYYMMDD')` (하이픈 제거)
  - `type="datetime-local"` 입력 → `dayjs(value).format('YYYYMMDDHHmm')`
  - 그 외 input/select → `el.value`
  - id 요소가 없으면 `input[name="<id>"]:checked` 라디오 값
- 서버 SQL은 이 형식을 전제로 한다: 날짜 파라미터 비교 시 `TO_DATE(#{변수}, 'YYYYMMDD')`로 감싼다.

엑셀 다운로드처럼 **페이징 없는 검색 조건만** 필요할 때:

```javascript
const params = new URLSearchParams(pageBuilder.getSearchParams());
window.location.href = '/sms/example-history/excel?' + params.toString();
```

### 4.4 행 번호 자동 컬럼 (`rowNum`)

`rowHeaders`에 `'rowNum'`(또는 `{ type: 'rowNum' }`)이 있으면, 빌더는 TUI Grid의 rowHeader 대신 **오프셋 기반 `rowNo` 데이터 컬럼**을 생성해 `columns` 맨 앞에 자동 삽입한다.

```javascript
// 자동 생성되는 컬럼 정의 (_rowNoColumn)
{
    header: 'No.',
    name: 'rowNo',
    align: 'center',
    width: 60,
    sortable: false,
    formatter: ({ value }) => value || '-'
}
```

- 번호는 페이지 오프셋을 반영한다: `rowNo = (page - 1) * size + index + 1`.
  즉 2페이지(10건/페이지)의 첫 행은 11번이다. (`_withRowNo()`가 조회 응답 적재 시 계산)
- 이미 `columns`에 `name: 'rowNo'` 컬럼이 정의되어 있으면 중복 삽입하지 않는다.
- `'rowNum'`을 제외한 나머지 rowHeaders(예: `'checkbox'`)는 그대로 TUI Grid에 전달된다.

```javascript
rowHeaders: ['rowNum']            // No. 컬럼만
rowHeaders: ['checkbox', 'rowNum'] // 체크박스 + No.
rowHeaders: []                     // 행 번호 없음
```

### 4.5 `PAGE_AUTH` 연동 — 그리드 컨텍스트 메뉴

`_initGrid()`은 그리드 생성 직전에 다운로드 권한을 검사한다.

```javascript
_hasDownloadPermission() {
    return !!(window.PAGE_AUTH && window.PAGE_AUTH.download === true);  // strict
}
```

- `PAGE_AUTH.download === true` → TUI Grid 기본 컨텍스트 메뉴(엑셀 내보내기 포함) 유지.
- 그 외(권한 없음/미정의) → **복사 전용 메뉴로 강제 교체**:

```javascript
// _copyOnlyContextMenu() — TUI Grid 4.x MenuItem[][] 형식
function () {
    return [[
        { name: 'copy',        label: '복사',   action: 'copy' },
        { name: 'copyColumns', label: '열 복사', action: 'copyColumns' },
        { name: 'copyRows',    label: '행 복사', action: 'copyRows' }
    ]];
}
```

> 엑셀 버튼(`#btn-excel`)도 Thymeleaf `th:if="${pageAuth.download}"`로 서버에서 렌더링을 통제하고, 화면 JS에서 `PAGE_AUTH.download !== true` 재검사로 이중 방어한다.

### 4.6 이벤트 자동 바인딩 (`_bindEvents`)

생성자는 아래 DOM 요소에 이벤트를 자동 연결한다. **해당 id의 요소가 없으면 silently skip**하므로, 요소가 없는 화면은 그저 이벤트가 붙지 않을 뿐 오류가 나지 않는다.

| 대상 | 이벤트 | 동작 |
|---|---|---|
| `#btn-search` (`config.btnSearch`) | `click` | `searchData(1)` |
| `#btn-reset` (`config.btnReset`) | `click` | 검색 조건 초기화 → 기본값 재적용 → `searchData(1)` |
| `#pageSizeSelect` (`config.pageSizeEl`) | `change` | `currentSize` 갱신 → `searchData(1)` |
| `searchInputs`의 `INPUT` 요소들 | `keypress` (Enter) | 기본 폼 제출 방지 → `searchData(1)` |

**초기화 버튼의 상세 동작**:

1. `searchInputs`의 각 id에 대해: input은 `value = ''`, select는 `selectedIndex = 0`, 라디오 그룹(`input[name="<id>"]`)은 첫 항목 checked.
2. `_applySearchDefaults(true)` — `searchDefaults` 기본값을 **강제 재적용**.
3. `CommonUtils.resetFields()` — 로드되어 있으면 호출 (화면 공용 추가 초기화).
4. `_syncSearchDatePickers()` — DatePicker 위젯 상태를 input 값과 동기화.
5. `searchData(1)`.

### 4.7 검색조건 기본값 (`searchDefaults`)

```javascript
searchDefaults: { startDt: 'THIS_MONTH', endDt: 'TODAY', sendType: 'SMS' }
```

- 키 = input id, 값 = 기본값 코드 또는 리터럴 값.
- 초기 로드 시 **빈 입력에만** 적용되고(`forceReset=false`), 초기화 버튼 클릭 시 **강제 재적용**된다(`forceReset=true`).
- `'NONE'` 또는 빈 값은 스킵.
- select와 라디오 그룹 모두 지원한다.

**내장 날짜 코드** (`_resolveDefaultValue`, dayjs 기반):

| 코드 | 시작일(일반 id) | 종료일(range-end id) |
|---|---|---|
| `TODAY` | 오늘 | 오늘 |
| `YESTERDAY` | 어제 | 어제 |
| `RECENT_7_DAYS` | 오늘 - 6일 | 오늘 |
| `THIS_MONTH` / `CURRENT_MONTH_TO_TODAY` | 이번 달 1일 | 오늘 |
| 그 외 문자열 | 리터럴 그대로 적용 (예: `'SMS'`) | 동일 |

**range-end 판정** (`_isRangeEnd`): id가 `'To'`로 끝나거나 `'end'`/`'to'`로 시작하면 종료일로 간주한다.
예: `endDt`(시작 `end`), `startDtTo` 아님 — `endDt`는 종료일, `startDt`는 시작일.

### 4.8 DatePicker 자동 초기화

`window.tui.DatePicker`(tui-date-picker 4.3.3)가 로드되어 있으면, 빌더는 날짜 검색 입력에 DatePicker를 자동 부착한다.

**대상 감지** (`_datePickerInputIds`):

1. `config.datePickerInputs` 배열이 지정되어 있으면 그것을 사용.
2. 없으면 `config.searchInputs`에서 자동 감지.
3. 감지 조건(`_isDateSearchInput`): `el.type === 'date'` **또는** `el.dataset.searchType === 'date'`.

**DOM 계약** — 각 날짜 input에는 **레이어 요소가 형제(sibling)로 존재**해야 한다:

```html
<div class="scaffold-date-field scaffold-date-field-md">
    <div class="tui-datepicker-input tui-datetime-input scaffold-datepicker-input">
        <input type="text" id="startDt" data-search-type="date" autocomplete="off">
        <span class="tui-ico-date" aria-hidden="true"></span>
    </div>
    <!-- id 규칙: "<inputId>PickerLayer" -->
    <div id="startDtPickerLayer" class="scaffold-date-picker-layer"></div>
</div>
```

- input 또는 레이어 중 하나라도 없으면 해당 input은 조용히 스킵된다.
- 생성 옵션: `language: 'ko'`, `input.format: 'yyyy-MM-dd'`, `calendar.showToday: true`, 초기 날짜는 input 현재 값(dayjs 파싱).
- 인스턴스는 `this.searchDatePickers[id]`에 보관되고, 초기화 버튼 클릭 시 `_syncSearchDatePickers()`로 input 값과 재동기화(`setDate(date, true)` 또는 `setNull()`)된다.

### 4.9 데이터 로딩 흐름

`searchData(page)`의 전체 파이프라인:

```text
searchData(page)
  │
  ├─ 0. 검증
  │    ├─ requiredInputs 중 빈 값 → toast('필수 검색 조건을 입력해 주세요.', 'warning')
  │    │   + focus + return (API 호출 중단)
  │    └─ #startDate > #endDate → toast('시작일은 종료일보다 클 수 없습니다.', 'warning')
  │        + focus + return  ※ id가 정확히 startDate/endDate일 때만 동작
  │
  ├─ 1. 파라미터 조립
  │    getSearchParams({ includePaging: true })
  │    → page, size, ...searchInputs
  │
  ├─ 2. axios.get(apiUrl, { params })
  │    ※ 전역 인터셉터(http-client/common-utils)가
  │      ApiResponse 껍데기를 언래핑 → response.data = PageResponseDTO
  │
  ├─ 3. 응답 렌더링
  │    ├─ page가 객체가 아니면 return (세션 만료 HTML은 인터셉터가 /login 전환)
  │    ├─ contents = _withRowNo(page.contents, page.page, page.size)
  │    ├─ grid.resetData(contents, {
  │    │      pageState: { page, totalCount, perPage }
  │    │   })
  │    ├─ _toggleEmptyState(totalCount)
  │    │   → 그리드 부모의 [data-empty-state] 오버레이에 is-visible 토글
  │    ├─ 0건이면 toast('조회된 데이터가 없습니다.', 'info')
  │    ├─ TuiCommon.updateTotalCount(totalCount, totalCountSelector)
  │    ├─ TuiCommon.renderPagination(page, totalPages, p => searchData(p), paginationId)
  │    └─ config.onGridUpdated(page)   ※ 주입한 경우
  │
  └─ catch: console.error('[TuiPageBuilder] 목록 조회 실패', err)
       ※ 오류 모달은 전역 인터셉터가 표시. 화면 catch에서 중복 알림 금지
```

**기대 응답 계약** — `apiUrl`은 `PageResponseDTO`를 반환해야 한다(인터셉터 언래핑 후 기준):

```json
{
    "contents":   [ { "smsHistoryId": "...", "sentAt": "..." } ],
    "page":       1,
    "size":       10,
    "totalCount": 1234,
    "totalPages": 124
}
```

`contents` 행 객체의 필드명은 `columns[].name`과 1:1로 대응한다.

### 4.10 필수 DOM 골격

표준 그리드 카드는 `fragments/toast-grid.html`의 `gridCard` 프래그먼트로 제공한다. 화면은 이것을 `th:replace`로 삽입한다.

```html
<div th:replace="~{fragments/toast-grid :: gridCard}"></div>
```

프래그먼트가 제공하는 id/구조와 빌더의 대응:

| DOM | 빌더 사용처 | 주의 |
|---|---|---|
| `#total-count` | `updateTotalCount` | — |
| `#pageSizeSelect` (10/20/50) | `currentSize` 동기화 + change 이벤트 | — |
| `#grid` (`.toast-grid`) | `new tui.Grid({ el })` 마운트 포인트 | **반드시 비어 있어야 한다.** 자식 요소가 있으면 TUI 내부 DOM 구성이 깨진다. 무데이터 안내는 형제 오버레이로 처리 |
| `[data-empty-state]` | `_toggleEmptyState`가 `is-visible` 토글 | `#grid`의 부모(`.toast-grid-shell`) 안에서 조회 |
| `#pagination` | `renderPagination` 컨테이너 | — |

검색 카드의 표준 id는 [9절](#9-계약-요약-quick-reference) 참고.

---

## 5. 화면 JS 사용 패턴

### 5.1 패턴 A: Scaffold EXCEL 기본 생성물

현재 메뉴의 테스트 화면이 아니라 `scaffold-templates/excel/page.js.tpl`이 생성하는 표준 구조를 기준으로 한다. 행 클릭 상호작용이 없는 조회+서버 Excel 화면이다.

```javascript
document.addEventListener('DOMContentLoaded', function () {
    const SCREEN_URL = '/sms/example-history';
    const API = { excel: SCREEN_URL + '/excel' };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: SCREEN_URL + '/data',
        searchInputs: ['status', 'startDt', 'endDt'],
        searchDefaults: {},
        rowHeaders: [],          // 행 번호 없는 단순 조회
        columns: [
            { header: '처리 ID',   name: 'processId',   align: 'center', width: 150 },
            { header: '처리일시', name: 'processedAt', align: 'center', width: 150,
              formatter: TuiCommon.fmt.date },          // 날짜 컬럼
            // ...
        ]
    });

    // 서버 사이드 엑셀 다운로드 — 검색 조건만 재사용
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
```

포인트:

- `searchInputs`의 id(`startDt`, `endDt`)는 HTML의 `data-search-type="date"` input과 연결되어 `YYYYMMDD`로 자동 변환 전송된다.
- `#btn-excel`은 서버에서 `th:if="${pageAuth.download}"`로 렌더링 통제 + 클라이언트 재검증.
- 엑셀은 `getSearchParams()`(페이징 제외)로 **현재 검색 조건 전체**를 서버 endpoint에 전달한다.

### 5.2 패턴 B: Scaffold CRUD 기본 생성물

Scaffold **CRUD** 모드 생성물의 전형. 그리드 행 클릭 → 모달 → 저장/삭제 → 현재 페이지 재조회 흐름이다.

```javascript
// scaffold-templates/crud/page.js.tpl 생성 결과의 핵심 흐름
document.addEventListener('DOMContentLoaded', function () {
    const SCREEN_URL = '/sms/example-item';
    const MODAL_ID = 'example-item-modal';
    const API = {
        create: SCREEN_URL + '/create',
        update: SCREEN_URL + '/update',
        delete: SCREEN_URL + '/delete'
    };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: SCREEN_URL + '/data',
        searchInputs: ['searchKeyword', 'useYn'],
        btnCreate: 'crud-modal-auto-create-disabled', // 레거시 no-op 키 — 직접 바인딩한다는 관례 표식
        rowHeaders: ['rowNum'],                        // 자동 No. 컬럼
        columns: [
            // ... name은 VO 필드명과 1:1
            { header: '등록일시', name: 'regDttm', align: 'center', width: 150,
              formatter: TuiCommon.fmt.date }
        ]
    });

    // ① 빌더가 만든 그리드 원본에 화면 전용 이벤트 추가
    const grid = pageBuilder.getGrid();
    grid.on('click', (ev) => {
        if (ev.rowKey === null || ev.rowKey === undefined) return; // 헤더 클릭 등
        openEdit(grid.getRow(ev.rowKey));   // 행 데이터로 폼 바인딩 + 모달 오픈
    });

    // ② PAGE_AUTH 기반 저장 권한 검사 (strict)
    const canSave = () => {
        const auth = window.PAGE_AUTH || {};
        return (state.mode === 'create' && auth.create === true)
            || (state.mode === 'update' && auth.update === true);
    };

    // ③ 저장 성공 후 — 모달을 닫고 "보던 페이지" 재조회
    const save = async () => {
        if (!canSave()) return;
        const payload = FormBinder.toObject('#detail-form');
        if (state.mode === 'create') await ApiClient.post(API.create, payload);
        else                         await ApiClient.post(API.update, payload);
        ModalManager.close(MODAL_ID);
        await pageBuilder.searchData(pageBuilder.currentPage || 1);
    };

    ModalManager.init(MODAL_ID, { onSubmit: save, onDelete: remove });
});
```

포인트:

- **그리드 원본 접근은 `getGrid()`으로.** 이벤트 등록(`grid.on`)과 행 데이터 취득(`grid.getRow(rowKey)`)은 TUI Grid API를 직접 사용한다.
- 행 클릭 시 별도 상세 API를 부르지 않고 그리드에 이미 적재된 행 데이터를 `FormBinder.bind('#detail-form', row)`로 폼에 채운다.
- 저장/삭제 후 `searchData(pageBuilder.currentPage || 1)`로 **현재 페이지**를 재조회한다 (1페이지로 튀지 않음).
- 등록 버튼(`#btn-create`)은 `PAGE_AUTH.create`가 있을 때만 서버가 렌더링하므로 `if (btnCreate)` 가드 후 바인딩한다.

### 5.3 패턴 C: 빌더를 쓰지 않는 화면 (`scaffold.js`)

`js/system/scaffold.js`(Query Scaffold 도구 화면)는 **`TuiPageBuilder`를 사용하지 않는다**. 그리드 목록 화면이 아니라 폼 기반 도구 화면이기 때문이다.

- 그리드/페이징이 없으므로 빌더의 전제(DOM 골격, `apiUrl` GET 계약)가 성립하지 않는다.
- HTTP는 `axios.post`를 직접 호출하고, 응답 검증(문자열 응답 = 세션 만료 의심 등)을 화면에서 처리한다.
- 이런 화면은 예외다. **목록형 업무 화면은 반드시 `TuiPageBuilder`를 사용한다.**

---

## 6. 확장 가이드

### 6.1 커스텀 formatter 추가

**원칙: `badgeByValue({ labels, tones })`로 충분한 경우가 대부분이다.** 진짜 범용(도메인 무관) 포맷만 `tui-common.js`의 `fmt`에 추가한다.

공통에 추가하는 경우의 작성 규칙:

```javascript
// tui-common.js 의 fmt 객체에 추가
const fmt = {
    date: v => { /* 기존 */ },

    // 신규 예시: 금액 formatter
    money: v => {
        const val = rawValue(v);          // 1) ({value})와 원시 값 모두 지원
        if (!val && val !== 0) return '-'; // 2) 빈 값은 '-'
        return Number(val).toLocaleString() + '원';
    }
};
```

체크리스트:

1. `rawValue()`로 인자를 정규화한다 (TUI Grid는 `{ value, row }`를 넘긴다).
2. 빈 값은 `'-'`를 반환한다.
3. 색상은 CoreUI/Bootstrap 클래스(`bg-success`, `var(--cui-success)`)를 사용한다.
4. **반환 HTML은 이스케이프되지 않는다.** 사용자 입력을 직접 삽입하면 XSS다. 정형 코드값/시스템 값에만 HTML을 사용한다.
5. 도메인 코드값 매핑(SMS/LMS 등)은 절대 공통에 넣지 않는다 — 화면에서 `badgeByValue`로 선언.

**화면 전용 formatter**는 컬럼 정의에 직접 작성한다:

```javascript
// 조건부 강조
{
    header: '대기건수', name: 'waitCount',
    formatter: ({ value }) => {
        const n = Number(value) || 0;
        const cls = n >= 100 ? 'text-danger fw-bold' : (n > 0 ? 'text-warning' : 'text-muted');
        return `<span class="${cls}">${n.toLocaleString()}</span>`;
    }
}

// 두 필드 합쳐 표시
{
    header: '담당자', name: 'empNm',
    formatter: ({ row }) => `${row.empNm} (${row.deptNm || '-'})`
}
```

더 많은 패턴(아이콘/인라인 SVG, 행 단위 스타일)은 `docs/base/grid-formatter-guide.md` 참고.
**그리드 셀에 `data-lucide` 사용 금지** — TUI Grid 가상 렌더링이 스크롤 시 셀을 재생성해 `lucide.createIcons()`가 따라가지 못한다. 유니코드 문자나 인라인 SVG를 사용한다.

### 6.2 커스텀 이벤트 추가

빌더가 바인딩하지 않는 그리드 이벤트는 `getGrid()` 이후 TUI Grid 이벤트 API로 추가한다.

```javascript
const pageBuilder = new TuiPageBuilder({ /* ... */ });
const grid = pageBuilder.getGrid();

// 행 클릭 → 상세 모달
grid.on('click', (ev) => {
    if (ev.rowKey == null) return;        // 헤더/여백 클릭 제외
    const row = grid.getRow(ev.rowKey);
    // ... 업무 처리
});

// 더블클릭, 체크박스 변경 등
grid.on('dblclick', (ev) => { /* ... */ });
grid.on('check', (ev) => { /* ... */ });
```

> 생성자 안에서 `this.grid`가 만들어진 **후**에 호출해야 하므로, 이벤트 등록은 `new TuiPageBuilder(...)` 반환 이후에 작성한다. 생성자 마지막이 `searchData(1)`이므로, 초기 조회 데이터에도 이벤트가 정상 적용된다.

### 6.3 그리드 옵션 오버라이드

`gridDefaults`를 건드리지 말고 `config.gridOptions`로 화면 예외를 지정한다.

```javascript
new TuiPageBuilder({
    apiUrl: '/도메인/data',
    columns: [ /* ... */ ],
    gridOptions: {
        bodyHeight: 600,     // 기본 380 → 이 화면만 크게
        scrollY: true,       // 세로 스크롤 활성화
        rowHeight: 44
    }
});
```

병합 순서: `TuiCommon.gridDefaults` < `config.gridOptions` < 빌더 고정값(`el`, `rowHeaders`, `columns`).
즉 `gridOptions`으로 `el`/`columns`를 넘겨도 빌더 값이 우선한다.

### 6.4 `onGridUpdated` 활용

데이터 적재 완료 후 실행될 콜백을 주입한다. 행 단위 스타일, 추가 집계 표시 등에 사용한다.

```javascript
new TuiPageBuilder({
    apiUrl: '/도메인/data',
    columns: [ /* ... */ ],
    onGridUpdated: (page) => {
        // page: PageResponseDTO { contents, page, size, totalCount, totalPages }
        const failCount = page.contents.filter(r => r.sendStatus === 'FAIL').length;
        // ... 화면 전용 후처리
    }
});
```

### 6.5 일괄 처리 (체크박스 선택 행)

```javascript
const pageBuilder = new TuiPageBuilder({
    rowHeaders: ['checkbox', 'rowNum'],
    // ...
});

document.querySelector('#btn-approve-all').addEventListener('click', async () => {
    const rows = pageBuilder.getCheckedRows();
    if (rows.length === 0) {
        CommonUtils.toast('선택된 행이 없습니다.', 'warning');
        return;
    }
    const ids = rows.map(r => r.apprId);
    await ApiClient.post('/도메인/approve-all', { ids });
    pageBuilder.searchData(pageBuilder.getCurrentPage());
});
```

### 6.6 여러 그리드를 한 화면에

빌더는 id 기반으로 DOM을 찾으므로, **모든 관련 id를 그리드마다 고유하게** 지정하면 여러 인스턴스를 만들 수 있다.

```javascript
const builderA = new TuiPageBuilder({
    el: 'grid-a', apiUrl: '/a/data',
    paginationId: 'pagination-a', totalCountSelector: '#total-count-a',
    pageSizeEl: 'pageSizeSelectA', btnSearch: 'btn-search-a', btnReset: 'btn-reset-a',
    columns: [ /* ... */ ]
});
const builderB = new TuiPageBuilder({
    el: 'grid-b', apiUrl: '/b/data',
    paginationId: 'pagination-b', totalCountSelector: '#total-count-b',
    pageSizeEl: 'pageSizeSelectB', btnSearch: 'btn-search-b', btnReset: 'btn-reset-b',
    columns: [ /* ... */ ]
});
```

`renderPagination`의 콜백은 `window.__movePage_<id>`에 등록되므로, `paginationId`가 다르면 충돌하지 않는다.

---

## 7. 의존 라이브러리

모두 `static/lib` 로컬 파일로 참조한다. **CDN 참조 금지** (폐쇄망 원칙).

| 라이브러리 | 버전 | 파일 | 이 모듈들과의 관계 |
|---|---|---|---|
| TUI Grid | **4.21.22** (2024-01-10) | `tui-grid.min.js` / `tui-grid.css` | `TuiPageBuilder`가 `new tui.Grid(...)`으로 래핑. `resetData`, `getCheckedRows`, `getFocusedCell`, `getRow`, `on`, `export` API 사용 |
| TUI Date Picker | **4.3.3** | `tui-date-picker.min.js` / `tui-date-picker.css` | `TuiPageBuilder._initSearchDatePickers()`가 `new tui.DatePicker(layer, ...)`로 검색 날짜 입력에 부착 |
| TUI Pagination | **3.4.1** | `tui-pagination.min.js` / `tui-pagination.css` | 라이브러리는 로드되나, 화면 페이징 UI는 `TuiCommon.renderPagination`(Bootstrap 버튼)이 담당. `new tui.Pagination` 직접 사용처 없음 |
| SheetJS (xlsx) | 번들 | `xlsx.full.min.js` | `TuiCommon.exportExcel()` → `grid.export('xlsx')`의 전제 조건 |
| dayjs + ko 로케일 | 번들 | `dayjs.min.js` / `ko.js` | `fmt.date`, `formatDate`, 날짜 전송 변환, DatePicker 초기 날짜 파싱. `dayjs.locale('ko')` 선행 |
| axios | 번들 | `axios.min.js` | `searchData()`의 HTTP 통신. 전역 인터셉터(`http-client.js`/`common-utils.js`)가 스피너/언래핑/오류 모달/세션 만료 전환 담당 |

선택 연동(로드되어 있으면 자동 사용):

| 모듈 | 사용 지점 |
|---|---|
| `CommonUtils` (`common-utils.js`) | `setDefaultDateTime()`, `resetFields()`, `toast()` — 빌더가 `typeof` 가드 후 호출 |
| `Notify` (`notify.js`) | `CommonUtils`가 없을 때의 폴백 알림 |

라이브러리 자체 API(그리드 옵션 전체 목록, DatePicker 메서드 등)는 각 라이브러리 공식 문서를 참고한다. 이 매뉴얼은 프로젝트 래퍼의 계약만 다룬다.

---

## 8. Troubleshooting

### 8.1 그리드가 렌더링되지 않아요

| 원인 | 확인 / 조치 |
|---|---|
| `#grid`에 자식 요소가 있음 | **`#grid`는 빈 마운트 포인트여야 한다.** `fragments/toast-grid.html` 주석에도 명시되어 있다. 자식 요소가 있으면 TUI 내부 DOM 구성이 깨진다. 무데이터 안내는 `[data-empty-state]` 형제 오버레이를 사용할 것 |
| `config.el`과 실제 id 불일치 | `document.getElementById(config.el)`이 `null`이면 `new tui.Grid`가 실패한다. 표준 id `grid`를 사용 |
| 스크립트 로드 순서 위반 | `tui-grid.min.js` → `tui-common.js` → `tui-page-builder.js` → 화면 JS 순서가 지켜져야 한다. `defaultLayout.html`을 통하면 자동 보장 |
| `DOMContentLoaded` 이전에 실행 | 화면 JS는 반드시 `document.addEventListener('DOMContentLoaded', ...)` 안에서 빌더를 생성 |
| 사이드바 토글 후 레이아웃 깨짐 | `defaultLayout.html`이 토글 애니메이션 종료 후 `grid.refreshLayout()`을 호출한다(210ms). 커스텀 레이아웃 변경 시 동일 패턴 사용 |

### 8.2 페이징이 동작하지 않아요

| 원인 | 확인 / 조치 |
|---|---|
| `#pagination` 컨테이너 없음 | `renderPagination`은 컨테이너가 없으면 silently skip. `toast-grid` 프래그먼트 사용 여부 확인 |
| `totalPages <= 0` | 응답의 `totalPages`가 0이면 컨테이너를 **비우고** 종료한다. 데이터가 없으면 정상 동작. 백엔드 `PageResponseDTO` 계산 확인 |
| 여러 그리드에서 id 충돌 | 두 빌더가 같은 `paginationId`를 쓰면 콜백(`window.__movePage_pagination`)이 마지막 인스턴스로 덮어써진다. [6.6절](#66-여러-그리드를-한-화면에)처럼 고유 id 지정 |
| 응답이 언래핑되지 않음 | `response.data`가 `PageResponseDTO` 객체여야 한다. 전역 인터셉터가 `ApiResponse`를 언래핑하므로 화면에서 `response.data.data`로 접근하지 않는다 |

### 8.3 DatePicker 캘린더가 안 뜨거나 위치가 어긋나요

| 원인 | 확인 / 조치 |
|---|---|
| 레이어 요소 없음 / id 규칙 위반 | input id가 `startDt`면 레이어는 **`startDtPickerLayer`** 여야 한다. 둘 중 하나라도 없으면 조용히 스킵 |
| 감지 조건 미충족 | input에 `data-search-type="date"` 또는 `type="date"`가 있어야 자동 감지된다. 임의 id는 `config.datePickerInputs: ['myDate']`로 명시 |
| CSS 미로드 | `tui-date-picker.css`와 위치 보정용 프로젝트 CSS(`scaffold-date-picker-layer` 등) 로드 확인. 레이어는 input의 형제 요소로 배치 |
| 한국어 로케일 미적용 | `dayjs.locale('ko')`와 DatePicker 옵션 `language: 'ko'`는 기본 적용되어 있다. `ko.js` 로드 여부 확인 |
| 초기화 후 위젯 불일치 | 초기화 버튼은 `_syncSearchDatePickers()`로 위젯을 재동기화한다. 직접 input 값을 바꾼 뒤 위젯이 따라오지 않으면 이 흐름 참고 |

### 8.4 검색 조건이 서버로 전달되지 않아요

- `searchInputs`의 id는 **HTML 요소 id이자 `SearchRequestDTO` 필드명**이다. 둘 중 하나라도 어긋나면 값이 비거나 바인딩되지 않는다.
- 날짜 input은 `YYYYMMDD`(하이픈 제거)로 자동 변환된다. 서버 SQL은 `TO_DATE(#{변수}, 'YYYYMMDD')`로 받아야 한다.
- 라디오 그룹은 요소 id 대신 `input[name="<id>"]:checked` 값이 수집된다.
- 시작일 > 종료일 차단 검증은 **id가 정확히 `startDate`/`endDate`일 때만** 동작한다. 그 외 id(예: `startDt`/`endDt`)는 서버 또는 화면에서 별도로 검증한다.

### 8.5 행 번호(No.)가 이상해요

- `rowHeaders: ['rowNum']`이 있어야 자동 `rowNo` 컬럼이 생긴다. `[]`이면 번호 컬럼 자체가 없다.
- 번호는 `(page - 1) * size + index + 1`로 계산된다. 매 페이지 1부터 다시 시작하면 백엔드 응답의 `page`/`size` 값이 올바른지 확인한다.
- 화면이 `columns`에 직접 `name: 'rowNo'`를 정의했다면 빌더는 자동 삽입을 생략한다(정의한 컬럼이 우선).

### 8.6 우클릭 메뉴에 엑셀 내보내기가 없어요

- 의도된 동작이다. `PAGE_AUTH.download === true`가 아니면 빌더가 컨텍스트 메뉴를 **복사 전용(복사/열 복사/행 복사)** 으로 강제한다.
- 메뉴 권한(`TB_MENU_AUTH`의 DOWNLOAD) 부여 여부를 서버에서 확인한다.
- `TuiCommon.exportExcel(grid, '파일명')`로 버튼 기반 클라이언트 내보내기를 직접 구현할 수도 있다(`xlsx.full.min.js` 필요).

### 8.7 조회/저장 후 로그인 페이지로 튕겨요

- 세션 만료 시 서버가 HTML(로그인 페이지)을 응답하면, **전역 응답 인터셉터**가 감지해 `/login`으로 전환한다. 정상 동작이다.
- 화면 JS에서 `typeof response.data === 'string'`으로 직접 분기하지 않는다. `searchData`는 `page`가 객체가 아니면 조용히 return한다.

### 8.8 오류 알림이 두 번 떠요

- 업무 오류/HTTP 오류의 모달 표시는 전역 인터셉터 담당이다. 화면의 `.catch`에서 `toast`/`alert`를 중복 호출하지 않는다. 빌더의 catch는 `console.error`만 남긴다.

### 8.9 `fmt.date`가 날짜를 포맷하지 않고 원본을 보여줘요

- dayjs 파싱 실패 시 원본 문자열을 그대로 반환한다. 백엔드 직렬화 형식(ISO-8601 계열)을 확인한다.
- dayjs 또는 `ko.js`가 로드되지 않으면 빌더/포매터 전체의 날짜 처리가 깨진다. `defaultLayout.html` 로드 순서 확인.

### 8.10 empty-state 오버레이가 사라지지 않아요

- `_toggleEmptyState`는 **그리드 요소(`#grid`)의 부모**에서 `[data-empty-state]`를 찾는다. 구조가 `부모 > (#grid + [data-empty-state])` 형제 배치여야 한다.
- `toast-grid` 프래그먼트를 변형했다면 이 구조를 유지했는지 확인한다.

---

## 9. 계약 요약 (Quick Reference)

### 표준 DOM id (screen-convention.md)

| 용도 | id |
|---|---|
| 조회 버튼 | `btn-search` |
| 초기화 버튼 | `btn-reset` |
| 엑셀 버튼 | `btn-excel` |
| 그리드 컨테이너 | `grid` |
| 페이지네이션 | `pagination` |
| 총 건수 | `total-count` |
| 페이지 크기 | `pageSizeSelect` |
| 화면 JS 경로 | `static/js/<도메인>/<화면명>.js` (HTML 파일명과 동일) |
| 검색 input id | `SearchRequestDTO` 필드명과 동일 camelCase |
| 컬럼 `name` | 응답 VO 필드명과 1:1 |

### 형식 계약

| 용도 | 형식 | 처리 주체 |
|---|---|---|
| 그리드 날짜 표시 | 날짜 `YYYY-MM-DD` / 일시 `YYYY-MM-DD HH:mm` | `TuiCommon.fmt.date` |
| 날짜 검색 전송 | `YYYYMMDD` | `TuiPageBuilder._readSearchValue` |
| datetime-local 전송 | `YYYYMMDDHHmm` | 동일 |
| 총 건수 표시 | 천 단위 콤마 | `TuiCommon.updateTotalCount` |

### 금지

- 화면에서 `new tui.Grid(...)` 직접 생성 (빌더 사용)
- `fetch` 사용 (axios 통일)
- CDN 참조 (`static/lib`, `static/vendor`만)
- 그리드 셀에 `data-lucide` (가상 렌더링 비호환)
- 공통 JS에 도메인 코드값 매핑 추가 (`badgeByValue`로 화면 선언)
- 화면 catch에서 오류 알림 중복 표시 (인터셉터 담당)

### 최소 화면 템플릿

```javascript
document.addEventListener('DOMContentLoaded', function () {
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/도메인/data',
        searchInputs: ['검색조건ID'],
        rowHeaders: ['rowNum'],
        columns: [
            { header: '컬럼명', name: 'voField', align: 'center', width: 150 }
        ]
    });
    // 업무 이벤트는 pageBuilder.getGrid() 이후에 추가
});
```
