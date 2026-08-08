# TOAST UI Pagination 매뉴얼 (v3.4.1)

> 대상 라이브러리: **TOAST UI Pagination 3.4.1** (vendored, 로컬 참조)
>
> 관련 문서
> - `src/main/resources/static/lib/MANIFEST.md` — 버전·라이선스·출처 대장
> - `docs/menual/tui_manual.md` — 프로젝트 래퍼(`TuiCommon` · `TuiPageBuilder`) 상세
> - `docs/menual/tui-grid_manual.md` — 그리드 (이 라이브러리의 실사용 주체)
>
> 이 문서는 **라이브러리 자체**를 다루되, 이 프로젝트에서 실제로 쓰는 범위만 추렸다.

---

## 목차

1. [라이브러리 개요](#1-라이브러리-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 구조 — 핵심: 직접 쓰지 않는다](#3-프로젝트-사용-구조--핵심-직접-쓰지-않는다)
4. [실제 페이징 구현 — `TuiCommon.renderPagination`](#4-실제-페이징-구현--tuicommonrenderpagination)
5. [라이브러리 API 참고 (직접 사용 시)](#5-라이브러리-api-참고-직접-사용-시)
6. [옵션 설정 패턴](#6-옵션-설정-패턴)
7. [주의사항 (Pitfalls)](#7-주의사항-pitfalls)
8. [참조](#8-참조)

---

## 1. 라이브러리 개요

| 항목 | 값 |
|---|---|
| 이름 | TOAST UI Pagination |
| 버전 | **3.4.1** |
| 라이선스 | MIT |
| 출처 | `https://cdn.jsdelivr.net/npm/tui-pagination@3.4.1/dist/tui-pagination.min.js` |
| 파일 | `static/lib/tui-pagination.min.js`, `static/lib/tui-pagination.css` |
| 전역 변수 | `tui.Pagination` (UMD 네임스페이스 `window.tui` — Grid/DatePicker와 공유) |
| 용도 | 페이지 번호 버튼 UI 컴포넌트. 총 건수·페이지 크기 기반으로 버튼 묶음 렌더링 |

CSS는 TUI가 별도 `.min` 버전을 배포하지 않으므로 `tui-pagination.css`가 최종 dist 파일이다 (`MANIFEST.md` 규칙 참고).

### 1.1 이 프로젝트에서의 위치 — 요약

> **`tui.Pagination`은 화면 코드에서 직접 인스턴스화하지 않는다.**
>
> 로드하는 이유는 **TOAST UI Grid의 런타임 전역 의존성**이기 때문이다 (`MANIFEST.md`: "tui-grid … tui-pagination/xlsx 전역 의존").
> 목록 화면에 보이는 페이지 버튼은 라이브러리 UI가 아니라 프로젝트 자체 구현인 `TuiCommon.renderPagination`이 만든다 (4절).

---

## 2. 프로젝트 로드 방식

### 2.1 `defaultLayout.html` 로드 순서

`tui-pagination.min.js`는 **가장 먼저** 로드되는 TUI 스크립트다.

```html
<!-- CSS -->
<link rel="stylesheet" href="/lib/tui-pagination.css" />   <!-- 첫 번째 CSS -->
<link rel="stylesheet" href="/lib/tui-date-picker.css" />
<link rel="stylesheet" href="/lib/tui-grid.css" />

<!-- JS: pagination이 grid보다 반드시 먼저 -->
<script src="/lib/tui-pagination.min.js"></script>   <!-- ① grid가 window.tui.Pagination을 요구 -->
<script src="/lib/xlsx.full.min.js"></script>        <!-- ② -->
<script src="/lib/tui-date-picker.min.js"></script>  <!-- ③ -->
<script src="/lib/tui-grid.min.js"></script>         <!-- ④ -->
```

순서를 바꿔 `tui-grid.min.js`보다 뒤에 로드하면 그리드 초기화가 실패한다.

### 2.2 전역 변수

| 전역 | 설명 |
|---|---|
| `tui.Pagination` | 페이지네이션 생성자. **프로젝트 화면 코드에서 직접 호출하지 않음** |

---

## 3. 프로젝트 사용 구조 — 핵심: 직접 쓰지 않는다

### 3.1 왜 로드만 하고 쓰지 않는가

| 이유 | 설명 |
|---|---|
| Grid 전역 의존 | `tui-grid.min.js`는 UMD 번들이 `window.tui.Pagination`을 참조한다. 없으면 그리드가 깨진다 |
| 서버 페이징 정책 | 목록 데이터는 서버에서 페이지 단위로 조회한다(`PageResponseDTO`). 클라이언트 전체 데이터를 잘라 보여주는 라이브러리 기본 모델과 맞지 않는다 |
| UI 통일 | 프로젝트는 CoreUI/Bootstrap `.btn` 스타일의 버튼으로 페이징을 통일한다. 라이브러리 기본 마크업(`.tui-pagination`)을 쓰지 않는다 |

### 3.2 데이터 흐름

```text
[화면 JS]  TuiPageBuilder 생성 (columns, apiUrl 만 선언)
    │
    ▼
[TuiPageBuilder.searchData(page)]
    │  axios.get(apiUrl, { params: { page, size, ...검색조건 } })
    ▼
[서버]  PageResponseDTO { contents, page, size, totalCount, totalPages }
    │  (전역 인터셉터가 ApiResponse 언래핑)
    ▼
[TuiPageBuilder]
    ├─ grid.resetData(contents, { pageState: { page, totalCount, perPage } })
    │      → 그리드 내부 페이지 상태 동기화 (tui-grid 내부가 tui-pagination 개념을 사용)
    ├─ TuiCommon.updateTotalCount(totalCount)      → #total-count 텍스트 갱신
    └─ TuiCommon.renderPagination(page, totalPages, onMove)
           → #pagination 에 CoreUI 버튼 렌더링 (라이브러리 미사용)
```

- `grid.resetData`에 넘기는 `pageState`가 그리드 내부의 페이지 인식(총 건수·현재 페이지)을 서버와 맞춘다. 이 내부 동작이 tui-pagination에 의존한다.
- 사용자가 보는 페이지 이동 버튼은 전부 `TuiCommon.renderPagination` 산출물이다.

### 3.3 화면 HTML 규약

목록 화면(`screen-convention.md`)의 고정 DOM id:

```html
<!-- 총 건수 -->
<span id="total-count">0</span>

<!-- 페이지 버튼 컨테이너 — TuiCommon.renderPagination이 innerHTML을 채운다 -->
<nav id="pagination" aria-label="페이지 탐색"></nav>

<!-- 페이지 크기 선택 -->
<select id="pageSizeSelect">
    <option value="10">10건</option>
    <option value="20">20건</option>
    ...
</select>
```

`TuiPageBuilder` 기본 config가 이 id들을 기본값으로 가진다 (`paginationId: 'pagination'`, `totalCountSelector: '#total-count'`, `pageSizeEl: 'pageSizeSelect'`).

---

## 4. 실제 페이징 구현 — `TuiCommon.renderPagination`

목록 화면의 페이지 버튼은 `tui-common.js`의 자체 구현이다. **이것이 프로젝트의 "pagination"이다.**

### 4.1 전체 구현

```javascript
// static/js/common/tui-common.js
const renderPagination = (page, totalPages, onMove, paginationId = 'pagination') => {
    const wrap = document.getElementById(paginationId);
    if (!wrap || totalPages <= 0) {
        if (wrap) wrap.innerHTML = '';
        return;
    }

    const BLOCK = 10;
    const startPage = Math.floor((page - 1) / BLOCK) * BLOCK + 1;
    const endPage = Math.min(startPage + BLOCK - 1, totalPages);

    const fnName = `__movePage_${paginationId.replace(/-/g, '_')}`;

    wrap.classList.add('d-flex', 'justify-content-center', 'gap-1');

    const btn = (label, p, disabled) =>
        `<button type="button" class="btn btn-sm btn-outline-secondary"
                 ${disabled ? 'disabled aria-disabled="true"' : `onclick="${fnName}(${p})"`}>${label}</button>`;

    let html = btn('«', 1, startPage === 1);
    html += btn('‹', startPage - 1, startPage === 1);
    for (let p = startPage; p <= endPage; p++) {
        html += `<button type="button" class="btn btn-sm ${p === page ? 'btn-primary' : 'btn-outline-secondary'}"
                         onclick="${fnName}(${p})">${p}</button>`;
    }
    html += btn('›', endPage + 1, endPage === totalPages);
    html += btn('»', totalPages, endPage === totalPages);

    wrap.innerHTML = html;
    window[fnName] = onMove;
};
```

### 4.2 동작 요약

| 요소 | 동작 |
|---|---|
| 블록 크기 | 10개 페이지 단위 (`« 1~10 ›`, `« 11~20 ›` …) |
| `«` / `»` | 첫 / 마지막 페이지 |
| `‹` / `›` | 이전 / 다음 블록 |
| 현재 페이지 | `btn-primary`, 나머지 `btn-outline-secondary` (CoreUI 클래스) |
| 이동 처리 | `onclick` 인라인 핸들러 → `window.__movePage_<id>` 전역 함수. 이 함수는 `TuiPageBuilder.searchData(p)`로 연결 |

### 4.3 호출 지점 — `TuiPageBuilder.searchData()`

```javascript
// static/js/common/tui-page-builder.js (발췌)
if (typeof TuiCommon !== 'undefined') {
    TuiCommon.updateTotalCount(page.totalCount || 0, this.config.totalCountSelector);
    TuiCommon.renderPagination(
        page.page || 1,
        page.totalPages || 1,
        (p) => this.searchData(p),        // 페이지 이동 = 서버 재조회
        this.config.paginationId
    );
}
```

화면 JS는 페이징 코드를 작성하지 않는다. `TuiPageBuilder`가 조회 응답마다 버튼과 총 건수를 다시 그린다.

### 4.4 총 건수 갱신 — `TuiCommon.updateTotalCount`

```javascript
// tui-common.js
const updateTotalCount = (count, selector = '#total-count') => {
    const el = document.querySelector(selector);
    if (el) el.textContent = Number(count).toLocaleString();   // 1,234 형식
};
```

---

## 5. 라이브러리 API 참고 (직접 사용 시)

> 프로젝트는 아래 API를 **호출하지 않는다**. 그리드 내부가 사용할 뿐이다.
> 향후 라이브러리 UI를 직접 써야 하는 경우가 생길 때만 참고한다. 신규 화면은 기존 `TuiCommon.renderPagination` 패턴을 유지한다.

```javascript
// 공식 사용법 (프로젝트 미사용 — 참고용)
const pagination = new tui.Pagination(document.getElementById('pagination'), {
    totalItems: 500,      // 총 건수
    itemsPerPage: 10,     // 페이지당 건수
    visiblePages: 10,     // 노출 페이지 버튼 수
    page: 1,              // 현재 페이지
    centerAlign: true     // 중앙 정렬
});

pagination.on('beforeMove', (event) => {
    // event.page — 이동할 페이지. 여기서 서버 조회를 호출
});

pagination.movePageTo(3);      // 프로그래매틱 이동
pagination.reset(1000);        // 총 건수 변경 후 재생성
pagination.getCurrentPage();   // 현재 페이지
```

| API | 설명 |
|---|---|
| `new tui.Pagination(container, options)` | 컨테이너 DOM에 버튼 UI 렌더링 |
| `on('beforeMove' / 'afterMove', handler)` | 페이지 이동 전/후 이벤트 |
| `movePageTo(page)` | 코드에서 페이지 이동 |
| `reset(totalItems)` | 총 건수 재설정 |
| `getCurrentPage()` | 현재 페이지 반환 |

`tui-pagination.css`는 이 UI의 스타일(`.tui-pagination`, `.tui-page-btn` 등)을 정의한다. 프로젝트가 자체 버튼을 쓰므로 이 클래스들은 화면에 노출되지 않는다.

---

## 6. 옵션 설정 패턴

### 6.1 프로젝트가 실제로 설정하는 것 (TuiPageBuilder config)

라이브러리 옵션이 아니라 `TuiPageBuilder` 생성자 옵션이다.

```javascript
new TuiPageBuilder({
    el: 'grid',
    apiUrl: '/sms/example-history/data',
    paginationId: 'pagination',          // 기본값 — 페이지 버튼 컨테이너 id
    totalCountSelector: '#total-count',  // 기본값 — 총 건수 요소
    pageSizeEl: 'pageSizeSelect',        // 기본값 — 페이지 크기 select id
    ...
});
```

| config | 기본값 | 용도 |
|---|---|---|
| `paginationId` | `'pagination'` | `renderPagination`이 채울 컨테이너. 한 화면에 그리드가 둘이면 서로 다른 id 지정 |
| `totalCountSelector` | `'#total-count'` | `updateTotalCount` 대상 |
| `pageSizeEl` | `'pageSizeSelect'` | 변경 시 1페이지로 돌아가 재조회 (`_bindEvents`) |

### 6.2 그리드 내부 동기화 — `pageState`

```javascript
this.grid.resetData(contents, {
    pageState: {
        page: page.page,             // 서버 응답 현재 페이지
        totalCount: page.totalCount, // 서버 응답 총 건수
        perPage: page.size           // 페이지 크기
    }
});
```

이 값이 그리드 내부 페이지 상태와 서버 페이징을 일치시킨다. 화면 버튼 렌더링과는 별개다.

---

## 7. 주의사항 (Pitfalls)

### 7.1 `#pagination`에 `new tui.Pagination`을 직접 붙이지 않는다

- `#pagination` 컨테이너는 `TuiCommon.renderPagination`이 **조회할 때마다 `innerHTML`을 통째로 교체**한다.
- 라이브러리 인스턴스를 같은 요소에 묶으면 매 조회마다 DOM이 파괴되어 버튼이 중복되거나 이벤트가 사라진다.
- 페이징 UI 변경이 필요하면 `renderPagination`을 고치거나 `paginationId`를 다른 요소로 분리한다.

### 7.2 로드 순서 — grid보다 반드시 먼저

- `tui-grid.min.js`가 `window.tui.Pagination`을 전역에서 찾는다. 순서가 뒤바뀌면 그리드 초기화 실패.
- CSS도 `tui-pagination.css`가 TUI CSS 중 첫 번째로 로드된다.

### 7.3 인라인 `onclick` 전역 함수

- `renderPagination`은 `window.__movePage_<paginationId>`에 콜백을 저장하고 버튼 `onclick`에서 호출한다.
- `paginationId`의 하이픈은 언더스코어로 치환된다 (`pagination` → `__movePage_pagination`, `sub-pagination` → `__movePage_sub_pagination`).
- 같은 id로 두 빌더를 만들면 전역 함수가 덮어써진다. 그리드가 둘이면 `paginationId`를 반드시 구분한다.

### 7.4 서버 페이징 응답 계약

- `renderPagination`은 서버가 `page`, `totalPages`, `totalCount`를 내려준다는 전제다 (`PageResponseDTO`, `docs/base/common-response-contract.md`).
- `totalPages <= 0`이면 버튼 영역을 비운다. 총 건수 0건 화면은 그리드 empty-state가 담당한다.

### 7.5 페이지 크기 변경은 1페이지 리셋

- `#pageSizeSelect` 변경 시 `TuiPageBuilder`는 `currentSize`를 갱신하고 **1페이지부터 재조회**한다. 현재 위치 유지가 필요하면 별도 로직이 필요하다 (현재 정책상 유지하지 않음).

### 7.6 라이브러리 버전과 그리드 호환

- `tui-pagination` 3.4.1은 `tui-grid` 4.21.22가 요구하는 전역 API를 제공한다.
- `MANIFEST.md` 규칙: 동일 메이저/마이너 우선. pagination만 단독으로 올리지 말고 그리드 호환을 함께 확인한다.

---

## 8. 참조

| 항목 | 위치 |
|---|---|
| 공식 문서 | https://github.com/nhn/tui.pagination (API: `docs/en` 디렉터리) |
| vendored 파일 | `src/main/resources/static/lib/tui-pagination.min.js`, `tui-pagination.css` |
| MANIFEST 항목 | `src/main/resources/static/lib/MANIFEST.md` — "tui-pagination.min.js / TOAST UI Pagination / 3.4.1 / MIT" |
| 실제 페이징 구현 | `static/js/common/tui-common.js` — `renderPagination`, `updateTotalCount` |
| 호출·동기화 | `static/js/common/tui-page-builder.js` — `searchData`, `_bindEvents` |
| 응답 계약 | `docs/base/common-response-contract.md` (`PageResponseDTO`) |
| 화면 규약 (id) | `docs/base/screen-convention.md` — `pagination`, `total-count`, `pageSizeSelect` |
| 생성·샘플 예 | `scaffold-templates/list/page.js.tpl`, `scaffold-templates/excel/page.js.tpl`, `static/js/system/list-basic.js` |
