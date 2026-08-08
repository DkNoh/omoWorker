# TOAST UI Grid 매뉴얼 (v4.21.22)

> 대상 라이브러리: **TOAST UI Grid 4.21.22** (vendored, 로컬 참조)
>
> 관련 문서
> - `src/main/resources/static/lib/MANIFEST.md` — 버전·라이선스·출처 대장
> - `docs/menual/tui_manual.md` — 프로젝트 래퍼(`TuiCommon` · `TuiPageBuilder`) 상세
> - `docs/base/grid-formatter-guide.md` — 셀 포매터 패턴
> - `docs/base/screen-convention.md` — 화면 규약 (그리드 ID, TuiPageBuilder 의무 사용)
>
> 이 문서는 **라이브러리 자체**를 다루되, 이 프로젝트에서 실제로 쓰는 API만 추렸다.
> 래퍼 모듈의 내부 설계는 `tui_manual.md`를 본다.

---

## 목차

1. [라이브러리 개요](#1-라이브러리-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 구조](#3-프로젝트-사용-구조)
4. [핵심 API (프로젝트 사용 기준)](#4-핵심-api-프로젝트-사용-기준)
5. [컬럼 설정 패턴](#5-컬럼-설정-패턴)
6. [그리드 옵션 설정 패턴](#6-그리드-옵션-설정-패턴)
7. [주의사항 (Pitfalls)](#7-주의사항-pitfalls)
8. [참조](#8-참조)

---

## 1. 라이브러리 개요

| 항목 | 값 |
|---|---|
| 이름 | TOAST UI Grid |
| 버전 | **4.21.22** |
| 라이선스 | MIT |
| 출처 | `https://cdn.jsdelivr.net/npm/tui-grid@4.21.22/dist/tui-grid.min.js` |
| 파일 | `static/lib/tui-grid.min.js`, `static/lib/tui-grid.css` |
| 전역 변수 | `tui.Grid` (UMD 네임스페이스 `window.tui`) |
| 용도 | 고성능 데이터 그리드. 가상 렌더링, 정렬, 행 헤더(체크박스/번호), 컨텍스트 메뉴, 엑셀 내보내기 지원 |

> `MANIFEST.md` 비고: **"tui-date-picker/tui-pagination/xlsx 전역 의존"**.
> 이 세 라이브러리가 `window.tui` / `window.XLSX` 전역에 먼저 로드되어 있어야 `tui-grid.min.js`가 동작한다.

CSS는 TUI가 별도 `.min` 버전을 배포하지 않으므로 `tui-grid.css`가 최종 dist 파일이다 (`MANIFEST.md` 규칙 참고).

---

## 2. 프로젝트 로드 방식

### 2.1 `defaultLayout.html` 로드 순서

모든 업무 화면은 `layout:decorate="~{defaultLayout}"`를 상속하며, 라이브러리 로드 순서는 고정되어 있다.

```html
<!-- CSS: pagination → date-picker → grid 순 -->
<link rel="stylesheet" href="/lib/tui-pagination.css" />
<link rel="stylesheet" href="/lib/tui-date-picker.css" />
<link rel="stylesheet" href="/lib/tui-grid.css" />

<!-- JS: 의존 라이브러리 → grid 순. 순서를 바꾸면 grid 초기화가 실패한다 -->
<script src="/lib/tui-pagination.min.js"></script>   <!-- ① grid 내부 의존 -->
<script src="/lib/xlsx.full.min.js"></script>        <!-- ② grid 엑셀 내보내기 의존 -->
<script src="/lib/tui-date-picker.min.js"></script>  <!-- ③ grid 내부 의존 -->
<script src="/lib/tui-grid.min.js"></script>         <!-- ④ 본체 -->
<script src="/lib/axios.min.js"></script>
<script src="/lib/dayjs.min.js"></script>
<script src="/lib/ko.js"></script>
...
<!-- 공통 JS (의존 순서대로) -->
<script th:src="@{/js/common/tui-common.js}"></script>       <!-- TuiCommon -->
<script th:src="@{/js/common/tui-page-builder.js}"></script> <!-- TuiPageBuilder -->
```

### 2.2 전역 변수

| 전역 | 설명 |
|---|---|
| `tui.Grid` | 그리드 생성자. `new tui.Grid(options)` |
| `tui.Grid.applyTheme('clean')` 등 정적 메서드 | 테마 적용 (프로젝트 미사용 — 기본 테마 + CSS 브리지로 처리) |

화면 JS에서 `new tui.Grid(...)`를 **직접 호출하지 않는다**. 규약상 목록 그리드는 `TuiPageBuilder`로만 초기화한다 (`screen-convention.md`, `.claude/rules/thymeleaf.md`).

---

## 3. 프로젝트 사용 구조

```text
생성된 화면 JS / 정적 샘플 JS
  │  컬럼 정의 + 업무 이벤트만 작성
  ▼
TuiPageBuilder (tui-page-builder.js)
  │  new tui.Grid(...) 호출, resetData, 이벤트 바인딩 자동화
  ▼
TuiCommon (tui-common.js)
  │  gridDefaults, formatter, exportExcel, renderPagination
  ▼
tui.Grid (static/lib/tui-grid.min.js)
```

### 3.1 그리드 생성 — `TuiPageBuilder._initGrid()`

`tui.Grid` 생성은 `tui-page-builder.js` 한 곳에만 존재한다.

```javascript
// tui-page-builder.js (발췌)
const defaults = (typeof TuiCommon !== 'undefined' && TuiCommon.gridDefaults)
    ? TuiCommon.gridDefaults
    : { scrollX: false, scrollY: false, minBodyHeight: 300 };

const mergedOptions = Object.assign({}, defaults, this.config.gridOptions, {
    el: document.getElementById(this.config.el),   // 기본 id="grid"
    rowHeaders: rowHeaders,
    columns: columns
});

// DOWNLOAD 권한이 strict true가 아니면 복사 전용 contextMenu로 강제
if (!this._hasDownloadPermission()) {
    mergedOptions.contextMenu = this._copyOnlyContextMenu();
}

this.grid = new tui.Grid(mergedOptions);
```

### 3.2 데이터 적재 — 서버 페이징 + `resetData`

목록 데이터는 서버에서 페이지 단위로 받아 `resetData`로 갈아끼운다.

```javascript
// tui-page-builder.js searchData() (발췌)
axios.get(this.config.apiUrl, { params })
.then(response => {
    const page = response.data; // 전역 인터셉터가 언래핑한 PageResponseDTO
    const contents = this._withRowNo(page.contents || [], page.page, page.size);
    this.grid.resetData(contents, {
        pageState: {
            page: page.page || this.currentPage,
            totalCount: page.totalCount || contents.length,
            perPage: page.size || this.currentSize
        }
    });
    ...
});
```

- `pageState`는 그리드 내부 페이지 상태(총 건수·현재 페이지)를 서버 페이징과 동기화한다.
- 화면에 보이는 페이지 버튼은 그리드 내장이 아니라 `TuiCommon.renderPagination`이 `#pagination`에 렌더링한다 (별도 문서: `tui-pagination_manual.md`).

### 3.3 Scaffold EXCEL 생성 예

```javascript
// scaffold-templates/excel/page.js.tpl 생성 결과의 핵심 구조
document.addEventListener('DOMContentLoaded', function () {
    const SCREEN_URL = '/sms/example-history';
    const API = { excel: SCREEN_URL + '/excel' };

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: SCREEN_URL + '/data',
        searchInputs: ['status', 'startDt', 'endDt'],
        searchDefaults: {},
        rowHeaders: [],
        columns: [
            { header: '처리 ID', name: 'processId', align: 'center', width: 150 },
            { header: '처리일시', name: 'processedAt', align: 'center', width: 150,
              formatter: TuiCommon.fmt.date },
            ...
        ]
    });

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

### 3.4 행 클릭 + 원본 객체 접근 — `list-crud.js`

```javascript
// static/js/system/list-crud.js (발췌)
const grid = pageBuilder.getGrid();   // TuiPageBuilder가 생성한 tui.Grid 인스턴스
grid.on('click', (ev) => {
    if (ev.rowKey === null || ev.rowKey === undefined) return; // 헤더 클릭 등
    openEdit(grid.getRow(ev.rowKey)); // 행 데이터를 수정 폼에 바인딩
});
```

---

## 4. 핵심 API (프로젝트 사용 기준)

프로젝트 코드에서 실제로 호출되는 `tui.Grid` API만 정리한다. 전체 API는 공식 문서를 본다.

### 4.1 생성자 / 옵션

| API | 프로젝트 사용처 | 비고 |
|---|---|---|
| `new tui.Grid(options)` | `TuiPageBuilder._initGrid()` | 유일한 생성 지점 |
| `options.el` | `document.getElementById('grid')` | 필수. 화면 규약 ID는 `grid` |
| `options.columns` | 화면 JS → config | 5절 참고 |
| `options.rowHeaders` | `['rowNum']` / `['checkbox']` / `[]` | `rowNum`은 TuiPageBuilder가 오프셋 번호 컬럼으로 변환 |
| `options.contextMenu` | `TuiPageBuilder._copyOnlyContextMenu()` | 다운로드 권한 없을 때 복사 전용으로 강제 |
| `options.rowHeight / bodyHeight / minBodyHeight / scrollX / scrollY` | `TuiCommon.gridDefaults` | 6절 참고 |

### 4.2 데이터

| API | 프로젝트 사용처 | 설명 |
|---|---|---|
| `grid.resetData(data, { pageState })` | `TuiPageBuilder.searchData()` | 전체 데이터를 교체. 서버 페이징 응답을 그대로 적재 |
| `grid.getRow(rowKey)` | CRUD 템플릿·`list-crud.js` | 행 원본 객체 반환. 모달 폼 바인딩에 사용 |
| `grid.getCheckedRows()` | `TuiPageBuilder.getCheckedRows()` | 체크된 행 배열. 일괄 처리용 |
| `grid.getFocusedCell()` | `TuiPageBuilder.getFocusedCell()` | 포커스 셀 정보 |

### 4.3 이벤트

| API | 프로젝트 사용처 | 설명 |
|---|---|---|
| `grid.on('click', handler)` | CRUD 템플릿·`list-crud.js` | `handler(ev)`의 `ev.rowKey`로 클릭 행 식별. 데이터 행 밖 클릭은 `rowKey`가 `null`/`undefined` |

### 4.4 레이아웃 / 내보내기

| API | 프로젝트 사용처 | 설명 |
|---|---|---|
| `grid.refreshLayout()` | `defaultLayout.html` 사이드바 토글 | 컨테이너 크기 변경 후 그리드 재계산. 사이드바 전환 애니메이션(210ms) 이후 `setTimeout`으로 호출 |
| `grid.export('xlsx', { fileName })` | `TuiCommon.exportExcel(gridObj, fileName)` | 클라이언트 엑셀 내보내기. `xlsx.full.min.js` 전역 의존. 검색 전체 다운로드는 Scaffold EXCEL 모드의 서버 endpoint를 사용 |

사이드바 토글 시 `refreshLayout` 호출 (`defaultLayout.html`):

```javascript
setTimeout(() => {
    if (typeof grid !== 'undefined' && grid) grid.refreshLayout();
}, 210);
```

---

## 5. 컬럼 설정 패턴

### 5.1 기본 컬럼 객체

```javascript
{ header: 'SENT_AT', name: 'sentAt', align: 'center', width: 150, formatter: TuiCommon.fmt.date }
```

| 속성 | 프로젝트 사용 | 설명 |
|---|---|---|
| `header` | 필수 | 컬럼 헤더 텍스트 |
| `name` | 필수 | 데이터 필드 키 (서버 JSON key와 일치) |
| `align` | `'center'` 위주 | 셀 정렬 |
| `width` | 숫자(px) | 프로젝트는 대부분 `150`, 번호 컬럼은 `60` |
| `sortable` | `false` (번호 컬럼) | 서버 정렬 정책상 번호 컬럼만 비활성 |
| `formatter` | 함수 | 셀 표시 변환. 아래 5.2 |

### 5.2 formatter

`formatter`는 `({ row, value })`를 받아 **HTML 문자열을 반환**하는 함수다.

```javascript
// 날짜 — 공통 포매터 (tui-common.js)
{ header: 'REG_DTTM', name: 'regDttm', formatter: TuiCommon.fmt.date }
// LocalDate(길이≤10) → YYYY-MM-DD, LocalDateTime → YYYY-MM-DD HH:mm, 빈 값 → '-'

// 코드값 배지 — 화면에서 선언 (grid-formatter-guide.md 방식 B/C)
{
    header: '발송상태', name: 'sendStatus',
    formatter: TuiCommon.badgeByValue({
        labels: { SUCCESS: '성공', FAIL: '실패', WAIT: '대기' },
        tones:  { SUCCESS: 'bg-success', FAIL: 'bg-danger', WAIT: 'bg-warning text-dark' }
    })
}

// 두 필드 합치기
{ header: '담당자', name: 'empNm', formatter: ({ row }) => `${row.empNm} (${row.deptNm || '-'})` }
```

> 상세 패턴(조건부 색상, 아이콘, 행 스타일)은 `docs/base/grid-formatter-guide.md`를 본다.

### 5.3 자동 번호 컬럼 (`rowNo`)

`rowHeaders: ['rowNum']`을 넘기면 TuiPageBuilder가 페이지 오프셋 기반 번호 컬럼을 자동 삽입한다.

```javascript
// tui-page-builder.js _rowNoColumn()
{
    header: 'No.', name: 'rowNo', align: 'center', width: 60,
    sortable: false,
    formatter: ({ value }) => value || '-'
}
// 값 계산: (page - 1) * size + index + 1  (_withRowNo)
```

---

## 6. 그리드 옵션 설정 패턴

### 6.1 공통 기본값 — `TuiCommon.gridDefaults`

그리드 옵션의 **단일 통제점**. 모든 화면이 이 값을 상속한다.

```javascript
// tui-common.js
const gridDefaults = {
    rowHeight: 38,
    bodyHeight: 380,
    minBodyHeight: 200,
    scrollX: true,
    scrollY: false,   // 세로 스크롤 없이 페이지 단위 전체 행 렌더링
};
```

### 6.2 화면별 예외 — `config.gridOptions`

`gridDefaults` 위에 덮어쓴다. 진짜 예외만 넘긴다.

```javascript
new TuiPageBuilder({
    ...,
    gridOptions: { bodyHeight: 500 }  // 이 화면만 본문 높이 변경
});
```

### 6.3 contextMenu (권한 연동)

`PAGE_AUTH.download !== true`이면 복사 전용 메뉴로 고정된다. TUI Grid 4.x 메뉴 형식은 **2차원 배열**(`MenuItem[][]`)이다.

```javascript
// tui-page-builder.js _copyOnlyContextMenu()
function () {
    return [
        [
            { name: 'copy', label: '복사', action: 'copy' },
            { name: 'copyColumns', label: '열 복사', action: 'copyColumns' },
            { name: 'copyRows', label: '행 복사', action: 'copyRows' }
        ]
    ];
}
```

---

## 7. 주의사항 (Pitfalls)

### 7.1 전역 의존·로드 순서

- `tui-grid.min.js`는 로드 시점에 `window.tui.Pagination`, `window.tui.DatePicker`, `window.XLSX`가 존재해야 한다.
- `defaultLayout.html` 순서(`pagination → xlsx → date-picker → grid`)를 바꾸면 그리드 초기화가 실패한다.
- CDN 참조 금지. `static/lib` 로컬 파일만 사용한다 (폐쇄망, `thymeleaf.md`).

### 7.2 formatter는 HTML을 이스케이프하지 않는다 (XSS)

- formatter 반환 문자열은 그대로 DOM에 삽입된다. **사용자 입력값을 직접 삽입하면 XSS**다.
- 정형 코드값(배지 등)에만 HTML을 사용하고, 자유 텍스트는 문자열 그대로 반환한다.

### 7.3 그리드 셀에 `data-lucide` 사용 금지

- TUI Grid는 가상 렌더링으로 스크롤 시 셀 DOM을 동적 생성/제거한다. `lucide.createIcons()`가 새 셀을 치환하지 못해 아이콘이 빈 `<i>`로 남는다.
- 대체: 유니코드 문자(`★`) 또는 인라인 SVG 문자열을 formatter에서 반환 (`grid-formatter-guide.md` 4절).

### 7.4 직접 생성 금지

- 화면 JS에서 `new tui.Grid(...)`를 직접 호출하지 않는다. 반드시 `TuiPageBuilder` 경유.
- 그리드 인스턴스가 필요하면 `pageBuilder.getGrid()`으로 받는다.

### 7.5 컨테이너 크기 변경 시 `refreshLayout()`

- 사이드바 접기/펼치기 등으로 그리드 컨테이너 폭이 바뀌면 `grid.refreshLayout()`을 호출해야 컬럼/스크롤이 재계산된다.
- `defaultLayout.html`은 사이드바 토글 후 210ms `setTimeout`으로 처리한다. CSS transition 시간과 맞춰야 한다.

### 7.6 빈 상태(empty-state) DOM 위치

- TUI Grid는 초기화 시 `#grid` 요소 **내부**를 대체한다.
- 따라서 "데이터 없음" 안내(`[data-empty-state]`)는 `#grid`의 **형제 요소**로 두고, `TuiPageBuilder._toggleEmptyState`가 `parentElement.querySelector`로 제어한다. `#grid` 안에 넣으면 그리드가 지운다.

### 7.7 엑셀 내보내기 두 가지 방식

| 방식 | 사용처 | 특징 |
|---|---|---|
| 서버 엑셀 (`window.location.href = API.excel + '?' + params`) | Scaffold EXCEL 생성물 | 검색조건 전체 기준, 대용량 적합. `PAGE_AUTH.download` 검사 필수 |
| 클라이언트 (`TuiCommon.exportExcel(grid)`) | `grid.export('xlsx')` 래퍼 | 현재 적재된 페이지 데이터만. `xlsx.full.min.js` 전역 필요 |

권한 없이 다운로드하면 안 된다 — 서버 인터셉터가 최종 차단하지만 화면에서도 `PAGE_AUTH.download === true`를 검사한다.

### 7.8 버전 업그레이드 시

- `MANIFEST.md` 규칙: **동일 메이저/마이너 버전 우선**. 4.21.x 내 패치만 안전하다.
- 올리면 contextMenu 형식, `resetData` 시그니처, `pageState` 동작 호환성을 확인하고 비고에 기록한다.

---

## 8. 참조

| 항목 | 위치 |
|---|---|
| 공식 문서 | https://github.com/nhn/tui.grid (API: `docs/en` 디렉터리) |
| vendored 파일 | `src/main/resources/static/lib/tui-grid.min.js`, `tui-grid.css` |
| MANIFEST 항목 | `src/main/resources/static/lib/MANIFEST.md` — "tui-grid.min.js / TOAST UI Grid / 4.21.22 / MIT" |
| 프로젝트 래퍼 | `static/js/common/tui-common.js`, `static/js/common/tui-page-builder.js` |
| 래퍼 상세 매뉴얼 | `docs/menual/tui_manual.md` |
| 포매터 가이드 | `docs/base/grid-formatter-guide.md` |
| 화면 규약 | `docs/base/screen-convention.md` |
| 생성·샘플 예 | `scaffold-templates/excel/page.js.tpl`, `scaffold-templates/crud/page.js.tpl`, `static/js/system/list-crud.js` |
