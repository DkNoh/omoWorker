# 고객별 조회(CustomerSearch) JS 분석

> 생성일: 2026-07-21 | 분석 대상: Scaffold v1 CRUD 화면

---

## 목차

1. [화면 개요](#1-화면-개요)
2. [로드 순서 및 의존 관계](#2-로드-순서-및-의존-관계)
3. [customer-search.js — 메인 화면 JS](#3-customerserachjs--메인-화면-js)
4. [TuiPageBuilder — 그리드/검색/페이징 엔진](#4-tuipagebuilder--그리드검색페이징-엔진)
5. [FormBinder — 폼 자동 바인딩](#5-formbinder--폼-자동-바인딩)
6. [ModalManager — 모달 라이프사이클](#6-modalmanager--모달-라이프사이클)
7. [HttpClient / ApiClient — HTTP 인프라](#7-httpclient--apiclient--http-인프라)
8. [PAGE_AUTH — 권한 전역 변수](#8-page_auth--권한-전역-변수)
9. [modal-base.html — 모달 뼈대 프래그먼트](#9-modal-basehtml--모달-뼈대-프래그먼트)
10. [row-click → 모달 열기 흐름](#10-row-click--모달-열기-흐름)
11. [Create / Update / Delete 흐름](#11-create--update--delete-흐름)
12. [관찰된 이슈](#12-관찰된-이슈)

---

## 1. 화면 개요

| 항목 | 내용 |
|---|---|
| URL | `/sms/customer-search` |
| Controller | `CustomerSearchController` (74 줄) |
| Template | `src/main/resources/templates/sms/customer-search.html` (66 줄) |
| JS | `src/main/resources/static/js/sms/customer-search.js` (201 줄) |
| 그리드 | TUI Grid 4.x (로컬 `static/lib`) |
| 모달 프레임워크 | CoreUI 5.x Modal |
| HTTP 클라이언트 | axios (전역 인터셉터 기반) |
| 권한 | `window.PAGE_AUTH` (Thymeleaf 주입) |

화면은 **검색 조건 카드 → TUI Grid 목록 → 등록/수정 모달**의 3단 구조다.
모달은 `src/main/resources/templates/fragments/modal-base.html`을 Thymeleaf fragment API로 삽입하고,
`ModalManager`가 CoreUI Modal 인스턴스를 관리한다.

---

## 2. 로드 순서 및 의존 관계

```
defaultLayout.html
├── CoreUI CSS/JS (vendor)
├── axios (vendor)
├── dayjs (vendor)
├── IMask (vendor, 옵션)
├── JustValidate (vendor, 옵션)
├── lucide (vendor)
├── notify.js              → window.Notify
├── http-client.js         → window.HttpClient, window.ApiClient
├── modal-manager.js       → window.ModalManager
├── common-utils.js        → window.CommonUtils
├── form-binder.js         → window.FormBinder
├── field-format.js        → window.FieldFormat
├── tui-common.js          → window.TuiCommon
├── tui-page-builder.js    → class TuiPageBuilder
└── customer-search.js     → TuiPageBuilder 인스턴스 생성
```

**핵심 의존 순서:**

1. `notify.js` — 알림(toast/alert/confirm) 전역 보조
2. `http-client.js` — axios 인터셉터 + `window.HttpClient`, `window.ApiClient`
3. `modal-manager.js` — `window.ModalManager = { init, open, close }`
4. `common-utils.js` — `window.CommonUtils` (toast/confirm/setDefaultDateTime)
5. `form-binder.js` — `window.FormBinder = { bind, toObject }`
6. `field-format.js` — `window.FieldFormat` (검증/포맷)
7. `tui-common.js` — `window.TuiCommon` (gridDefaults, fmt.date)
8. `tui-page-builder.js` — `class TuiPageBuilder`
9. `customer-search.js` — 위 모든 전역 객체 의존

---

## 3. customer-search.js — 메인 화면 JS

**파일:** `src/main/resources/static/js/sms/customer-search.js` (201 줄)

### 3.1 구조 요약

```javascript
document.addEventListener('DOMContentLoaded', () => {
    // 상수 정의
    const MODAL_ID = 'customer-search-modal';
    const API = { create, update, delete };
    const DEFAULT_FORM = { ... };
    const PK_FIELDS = ['customerId'];
    const LOCK = null;
    const state = { mode: 'create', selectedRow: null };

    // TuiPageBuilder 초기화 → 자동 1페이지 조회
    const pageBuilder = new TuiPageBuilder({ ... });

    // 그리드 행 클릭 → openEdit()
    const grid = pageBuilder.getGrid();
    grid.on('click', (ev) => { if (ev.rowKey != null) openEdit(grid.getRow(ev.rowKey)); });

    // 권한 검사
    const canSave = () => { ... };
    const syncActionButtons = () => { ... };

    // 모드별 모달 열기
    const openEdit = (row) => { ... };
    const openCreate = () => { ... };

    // CRUD 액션
    const save = async () => { ... };
    const remove = () => { ... };

    // ModalManager 초기화
    ModalManager.init(MODAL_ID, { onSubmit: save, onDelete: remove });

    // 등록 버튼 이벤트
    const btnCreate = document.querySelector('#btn-create');
    if (btnCreate) btnCreate.addEventListener('click', openCreate);

    // 필드 포맷 적용
    if (typeof FieldFormat !== 'undefined') FieldFormat.applyFieldFormats(form);
});
```

### 3.2 상태 관리

| 상태 | 타입 | 설명 |
|---|---|---|
| `state.mode` | `'create' \| 'update'` | 현재 모달 모드. 등록/수정 겸용 모달에서 구분 |
| `state.selectedRow` | `Object \| null` | 수정 모드에서 클릭한 그리드 행 원본 데이터 |

### 3.3 API 엔드포인트

| 용도 | 메서드 | URL | Controller 메서드 |
|---|---|---|---|
| 목록 조회 | GET | `/sms/customer-search/data` | `getData()` |
| 등록 | POST | `/sms/customer-search/create` | `create()` |
| 수정 | POST | `/sms/customer-search/update` | `update()` |
| 삭제 | POST | `/sms/customer-search/delete` | `delete()` |
| 원문 상세 | GET | `/sms/customer-search/unmask` | `getUnmaskedDetail()` |

### 3.4 검색 조건

| ID | name | HTML 요소 | 설명 |
|---|---|---|---|
| `searchKeyword` | — | `<input type="text">` | 자유 검색어 |
| `agreeYn` | — | `<input type="text">` | 동의 여부 |
| `useYn` | — | `<input type="text">` | 사용 여부 |

`searchDefaults: {}` — 기본값 없음.

### 3.5 그리드 컬럼

| header | name | align | width | formatter |
|---|---|---|---|---|
| CUSTOMER_ID | customerId | center | 150 | — |
| CUSTOMER_NM | customerNm | center | 150 | — |
| MOBILE_NO | mobileNo | center | 150 | — |
| EMAIL | email | center | 150 | — |
| BIRTH_DT | birthDt | center | 150 | — |
| GENDER_CD | genderCd | center | 150 | — |
| AGREE_YN | agreeYn | center | 150 | — |
| USE_YN | useYn | center | 150 | — |
| REG_DTTM | regDttm | center | 150 | `TuiCommon.fmt.date` |

`rowHeaders: ['rowNum']` — 왼쪽에 행 번호 컬럼 표시.

---

## 4. TuiPageBuilder — 그리드/검색/페이징 엔진

**파일:** `src/main/resources/static/js/common/tui-page-builder.js` (434 줄)

### 4.1 생성자 흐름

```
constructor(config)
├── config 병합 (기본값 + 사용자 설정)
├── currentPage = 1, currentSize = 10
├── pageSizeEl 값에서 currentSize 동기화
├── CommonUtils.setDefaultDateTime() 호출
├── _applySearchDefaults() — 검색 조건 기본값 주입
├── _initSearchDatePickers() — 날짜 피커 초기화 (옵션)
├── _initGrid() — TUI Grid 인스턴스 생성
│   ├── TuiCommon.gridDefaults 병합
│   ├── rowNum 헤더 처리 (rowNo 컬럼 자동 삽입)
│   └── new tui.Grid(...)
├── _bindEvents() — 버튼/Enter 키 바인딩
└── searchData(1) — 1페이지 자동 조회
```

### 4.2 searchData(page) — 목록 조회

```
searchData(page)
├── requiredInputs 검증 (선택)
├── startDate > endDate 검증 (선택)
├── getSearchParams({ includePaging: true })
│   ├── page, size 파라미터
│   └── searchInputs 값 순회
├── axios.get(apiUrl, { params })
│   ├── 전역 인터셉터가 ApiResponse 언래핑
│   └── 응답: PageResponseDTO<CustomerSearchVO>
├── _withRowNo() — rowNum 활성화 시 rowNo 계산 주입
├── grid.resetData(contents, { pageState: {...} })
├── TuiCommon.updateTotalCount() — 총 건수 표시
├── TuiCommon.renderPagination() — 페이지네이션 버튼 렌더링
│   └── window.__movePage_... 전역 함수 등록
└── onGridUpdated 콜백 (선택)
```

### 4.3 _bindEvents() — 버튼 바인딩

| 버튼 ID | 이벤트 | 동작 |
|---|---|---|
| `btn-search` | click | `searchData(1)` |
| `btn-reset` | click | 검색 조건 초기화 → `searchData(1)` |
| `pageSizeSelect` | change | 페이지 사이즈 변경 → `searchData(1)` |
| 각 searchInput | keypress (Enter) | `searchData(1)` |

### 4.4 공개 메서드

| 메서드 | 반환 | 설명 |
|---|---|---|
| `getGrid()` | `tui.Grid` | 그리드 인스턴스 |
| `getCheckedRows()` | `Array` | 체크된 행 데이터 |
| `getFocusedCell()` | `Object \| null` | 포커스된 셀 |
| `getCurrentPage()` | `number` | 현재 페이지 번호 |
| `getSearchParams()` | `URLSearchParams` | 검색 파라미터 조합 |
| `searchData(page)` | `void` | 비동기 조회 실행 |

---

## 5. FormBinder — 폼 자동 바인딩

**파일:** `src/main/resources/static/js/common/form-binder.js` (72 줄)

### 5.1 bind(formSelector, data)

```javascript
// data의 key와 동일한 name을 가진 input/select/textarea에 값 주입
Object.keys(data).forEach(name => {
    const fields = form.querySelectorAll(`[name="${name}"]`);
    fields.forEach(field => {
        if (field.type === 'checkbox') field.checked = (value === 'Y' || value === true);
        else if (field.type === 'radio') field.checked = String(field.value) === String(value);
        else field.value = (value == null) ? '' : value;
    });
});
```

**핵심 계약:** `data[name]` → `[name="name"]` 요소의 value 할당.
**주의:** `[name]` 속성이 없는 요소는 바인딩 대상이 아니다.

### 5.2 toObject(formSelector)

```javascript
form.querySelectorAll('input[name], select[name], textarea[name]').forEach(field => {
    if (field.disabled) return;
    if (field.type === 'checkbox') result[name] = field.checked ? 'Y' : 'N';
    else if (field.type === 'radio') { if (field.checked) result[name] = field.value; }
    else {
        const raw = field._imask ? field._imask.unmaskedValue : field.value;
        result[name] = raw === '' ? null : raw;
    }
});
```

**핵심 계약:** 빈 문자열 → `null`로 전송. IMask 필드는 `unmaskedValue` 사용.

---

## 6. ModalManager — 모달 라이프사이클

**파일:** `src/main/resources/static/js/common/modal-manager.js` (139 줄)

### 6.1 의존

`ModalManager`는 `Notify`에 의존하지 않는다. CoreUI 5.x 또는 Bootstrap 5 Modal 인스턴스를 직접 조작하며,
JustValidate가 로드된 경우에만 폼 검증 훅을 사용한다.

### 6.2 DOM ID 계약

| 요소 | ID 패턴 |
|---|---|
| 모달 컨테이너 | `${modalId}` |
| 제목 | `${modalId}-title` |
| 저장 버튼 | `${modalId}-btn-save` |
| 삭제 버튼 | `${modalId}-btn-delete` |

### 6.3 init(modalId, options)

```
init(modalId, { onMount, beforeOpen, onOpen, onSubmit, onDelete, onClose })
├── hooks[modalId] = 옵션 객체 (기본값 빈 함수)
├── getFrameworkModal() — CoreUI 또는 Bootstrap Modal 인스턴스
│   └── instances[modalId] = instance
├── show.coreui.modal 리스너 — beforeOpen 훅 (false 반환 시 열기 취소)
├── shown.coreui.modal 리스너 — JustValidate 바인딩 + onOpen 훅
├── hidden.coreui.modal 리스너 — onClose 훅 + destroyForm()
├── 저장 버튼 클릭 — 검증 통과 후 onSubmit()
├── 삭제 버튼 클릭 — onDelete()
└── onMount() — 초기화 직후 1회 호출
```

### 6.4 open / close

```javascript
open(modalId)  → instances[modalId].show()
close(modalId) → instances[modalId].hide()
```

### 6.4 destroyForm — 폼 리셋 + 메모리 누수 방지

```javascript
destroyForm(modalId, modalEl)
├── formEl.reset() — 폼 초기화
└── validator.destroy() — JustValidate 인스턴스 제거
```

---

## 7. HttpClient / ApiClient — HTTP 인프라

**파일:** `src/main/resources/static/js/common/http-client.js` (196 줄)

### 7.1 전역 인터셉터

**요청 인터셉터:**
- `ajaxCount++` → 스피너 표시
- CSRF 토큰 헤더 자동 주입

**응답 인터셉터:**
- `ajaxCount--` → 스피너 제거
- 세션 만료 감지 (`text/html` 응답 또는 `/login` 마커) → `/login` 리다이렉트
- `ApiResponse` 언래핑: `response.data.code !== undefined`이면 `response.data.data`로 덮어씀
- 비즈니스 에러 (`code !== 200`) → `Notify.alert()` + Promise reject
- 필드별 검증 에러 → `is-invalid` 클래스 자동 부착

### 7.2 ApiClient API

| 메서드 | HTTP | 설명 |
|---|---|---|
| `get(url, params)` | GET | 쿼리 파라미터 |
| `post(url, data)` | POST | JSON body |
| `put(url, data)` | PUT | JSON body |
| `delete(url)` | DELETE | — |
| `remove(url, params)` | POST + params | 삭제용 (스캐폴드 계약) |

**동시 노출:** `window.HttpClient` + `window.ApiClient = HttpClient` (기존 호환)

---

## 8. PAGE_AUTH — 권한 전역 변수

**계산:** `src/main/java/com/scbk/sms/advice/GlobalModelAdvice.addLayoutAttributes()`가 `pageAuth` 객체를 생성한다.
**렌더:** `src/main/resources/templates/defaultLayout.html:58~67`가 `window.PAGE_AUTH`에 주입한다.

```javascript
window.PAGE_AUTH = {
    read:     /*[[${pageAuth.read}]]*/ false,
    create:   /*[[${pageAuth.create}]]*/ false,
    update:   /*[[${pageAuth.update}]]*/ false,
    delete:   /*[[${pageAuth.delete}]]*/ false,
    approve:  /*[[${pageAuth.approve}]]*/ false,
    cancel:   /*[[${pageAuth.cancel}]]*/ false,
    download: /*[[${pageAuth.download}]]*/ false,
    maskView: /*[[${pageAuth.maskView}]]*/ false
};
```

`GlobalModelAdvice`가 모든 레이아웃 모델에 `pageAuth`를 추가하므로,
`CustomerSearchController.page()`는 별도로 모델을 전달하지 않아도 된다.

**customer-search.js에서의 사용:**

```javascript
// 저장 버튼: create 모드 → auth.create, update 모드 → auth.update
const canSave = () => {
    const auth = window.PAGE_AUTH || {};
    return (state.mode === 'create' && auth.create === true)
        || (state.mode === 'update' && auth.update === true);
};

// 삭제 버튼: update 모드 + auth.delete
const syncActionButtons = () => {
    deleteBtn.hidden = state.mode !== 'update' || !(window.PAGE_AUTH || {}).delete;
};
```

---

## 9. modal-base.html — 모달 뼈대 프래그먼트

**파일:** `src/main/resources/templates/fragments/modal-base.html` (67 줄)

### 9.1 Fragment API

```html
<th:block th:replace="~{fragments/modal-base :: layout(
    modalId='customer-search-modal',
    title='고객별 조회',
    size='modal-lg',
    bodyContent=~{::#modal-body},
    footerContent=null
)}">
```

### 9.2 기본 Footer (footerContent == null)

```html
<button type="button" class="btn btn-danger me-auto d-none"
        th:id="${modalId + '-btn-delete'}">삭제</button>
<button type="button" class="btn btn-secondary" data-coreui-dismiss="modal">닫기</button>
<button type="button" class="btn btn-primary" th:id="${modalId + '-btn-save'}">저장</button>
```

**중요:** 삭제 버튼은 기본 상태가 `d-none` (CSS `display: none`) + `ModalManager`가 `hidden` 속성으로 제어.

---

## 10. row-click → 모달 열기 흐름

```
[사용자] 그리드 행 클릭
    │
    ▼
grid.on('click', ev)          ← `src/main/resources/static/js/sms/customer-search.js`:66
    │  ev.rowKey != null?
    ├─ NO → return (헤더 클릭 등 무시)
    └─ YES → grid.getRow(ev.rowKey)
               │
               ▼
           openEdit(row)        ← `src/main/resources/static/js/sms/customer-search.js`:102
               │
               ├─ state.mode = 'update'
               ├─ state.selectedRow = row
               ├─ FormBinder.bind('#detail-form', row)
               │     └─ row.key → [name="key"] 요소 value 할당
               ├─ applyLockSnapshot(row) → LOCK=null 이므로 즉시 return
               ├─ syncActionButtons()
               │     ├─ saveBtn.hidden = !canSave()
               │     └─ deleteBtn.hidden = (mode != 'update' || !auth.delete)
               └─ ModalManager.open(MODAL_ID)
                      └─ instances[MODAL_ID].show()
                             │
                             ▼
                      shown.coreui.modal 이벤트
                         ├─ JustValidate 바인딩 (옵션)
                         └─ hooks.onOpen()
```

**핵심:** 별도 상세 API 호출 없이 **그리드 행 데이터 그대로**를 폼에 바인딩한다.
이는 `TuiPageBuilder.searchData()`가 이미 전체 행 데이터를 그리드에 적재하고 있기 때문에 가능한 최적화다.

---

## 11. Create / Update / Delete 흐름

### 11.1 등록 (Create)

```
[사용자] 상단 "등록" 버튼 클릭 (#btn-create)
    │
    ▼
openCreate()                   ← `src/main/resources/static/js/sms/customer-search.js`:119
    │
    ├─ state.mode = 'create'
    ├─ state.selectedRow = null
    ├─ form.reset()             — 브라우저 기본 폼 초기화
    ├─ FormBinder.bind('#detail-form', DEFAULT_FORM)
    │     └─ customerId='', customerNm='', ...
    ├─ syncActionButtons()
    └─ ModalManager.open(MODAL_ID)
```

### 11.2 수정 (Update)

```
[사용자] 그리드 행 클릭
    │
    ▼
openEdit(row)                  ← `src/main/resources/static/js/sms/customer-search.js`:102
    │
    ├─ state.mode = 'update'
    ├─ FormBinder.bind('#detail-form', row)
    │     └─ row의 각 key → [name="key"] 요소 value
    ├─ syncActionButtons()
    └─ ModalManager.open(MODAL_ID)
```

### 11.3 저장 (Save)

```
[사용자] 모달 "저장" 버튼 클릭 (#customer-search-modal-btn-save)
    │
    ▼
ModalManager 저장 버튼 리스너  ← `src/main/resources/static/js/common/modal-manager.js`:93
    │
    ├─ JustValidate 있으면 validator.revalidate()
    │     └─ isValid ? hooks.onSubmit() : skip
    └─ hooks.onSubmit() → save()
           │
           ├─ canSave() 검사
           ├─ FieldFormat.validateForm(form) (옵션)
           ├─ FormBinder.toObject('#detail-form')
           │     └─ [name] 요소 → 객체 (빈 문자열 → null)
           ├─ state.mode === 'create'
           │     └─ ApiClient.post(API.create, payload)
           └─ state.mode === 'update'
                 └─ ApiClient.post(API.update, payload)
                        │
                        ▼
                   axios.post + 전역 인터셉터
                      ├─ CSRF 헤더 주입
                      ├─ ApiResponse 언래핑
                      └─ 성공 → toast('등록/수정되었습니다.')
                             │
                             ▼
                        ModalManager.close(MODAL_ID)
                        pageBuilder.searchData(pageBuilder.currentPage)
```

### 11.4 삭제 (Delete)

```
[사용자] 모달 "삭제" 버튼 클릭 (#customer-search-modal-btn-delete)
    │
    ▼
ModalManager 삭제 버튼 리스너  ← `src/main/resources/static/js/common/modal-manager.js`:107
    │
    └─ hooks.onDelete() → remove()
           │
           ├─ state.mode === 'update'? (등록 중 데이터는 삭제 불가)
           ├─ CommonUtils.confirm('선택한 데이터를 삭제하시겠습니까?')
           │     └─ 큐 기반 모달 (notify.js)
           │        └─ 확인 시 콜백 실행
           ├─ pkParams()
           │     └─ PK_FIELDS=['customerId'] → [name="customerId"] 값 수집
           ├─ ApiClient.remove(API.delete, { customerId: ... })
           │     └─ axios.post('/sms/customer-search/delete', null, { params })
           │        └─ Controller: delete(@RequestParam Integer customerId)
           └─ 성공 → toast('삭제되었습니다.')
                  │
                  ▼
             ModalManager.close(MODAL_ID)
             pageBuilder.searchData(pageBuilder.currentPage)
```

---

## 12. 관찰된 이슈

### 이슈 1: 삭제 버튼의 `d-none` 클래스와 `hidden` 속성 충돌

**위치:** `src/main/resources/templates/fragments/modal-base.html:53` + `src/main/resources/static/js/sms/customer-search.js`:90

**문제:**
- `src/main/resources/templates/fragments/modal-base.html`에서 삭제 버튼은 기본 클래스에 `d-none`이 명시되어 있다:
  ```html
  <button class="btn btn-danger me-auto d-none" id="customer-search-modal-btn-delete">
  ```
- `src/main/resources/static/js/sms/customer-search.js:90`의 `syncActionButtons()`는 `hidden` 속성으로 표시/숨김을 제어한다:
  ```javascript
  if (deleteBtn) deleteBtn.hidden = state.mode !== 'update' || !(window.PAGE_AUTH || {}).delete;
  ```

**동작 분석:**
- 페이지 로드 시: `d-none` (CSS `display: none`)으로 숨겨짐
- `openEdit()` 호출 시: `deleteBtn.hidden = false` → `hidden` 속성 제거 → **`d-none`이 여전히 적용되어 숨겨진 상태 유지**
- `hidden` 속성이 `display: none`을 생성하지만, `d-none` 클래스의 CSS가 먼저 적용되어 `hidden` 속성 변경이 시각적으로 무의미

**결과:** 삭제 버튼이 `hidden` 속성으로 `false`가 되어도 `d-none` CSS 클래스가 `display: none`을 유지하므로, 권한이 있어도 버튼이 표시되지 않는다.

**권장 수정:** `src/main/resources/templates/fragments/modal-base.html`에서 삭제 버튼의 `d-none` 클래스를 제거하고, `hidden` 속성만으로 표시/숨김을 제어하도록 변경.

---

### 이슈 2: `data-readonly-field` div가 FormBinder 바인딩 대상이 아님

**위치:** `src/main/resources/templates/sms/customer-search.html:47, 55` + `src/main/resources/static/js/common/form-binder.js`:22, 51

**문제:**
- `src/main/resources/templates/sms/customer-search.html`에서 `customerId`와 `regDttm`은 `data-readonly-field` 속성을 가진 `div.form-control-plaintext`로 표시된다:
  ```html
  <div class="form-control-plaintext" data-readonly-field="customerId"></div>
  <div class="form-control-plaintext" data-readonly-field="regDttm"></div>
  ```
- `FormBinder.bind()`는 `[name="..."]` 선택자로만 필드를 찾는다:
  ```javascript
  const fields = form.querySelectorAll(`[name="${name}"]`);
  ```
- `FormBinder.toObject()` 역시 `input[name], select[name], textarea[name]`만 선택한다:
  ```javascript
  form.querySelectorAll('input[name], select[name], textarea[name]').forEach(field => { ... });
  ```

**결과:**
- `FormBinder.bind('#detail-form', row)` 호출 시 `customerId`와 `regDttm` 값이 readonly div에 반영되지 않는다
- `customerId`는 hidden input (`<input type="hidden" name="customerId">`)이 `src/main/resources/templates/sms/customer-search.html:45`에 별도로 존재하므로 삭제 시 PK 전달에는 문제 없음
- `regDttm`은 조회 표시 전용이므로 바인딩되지 않아도 기능적 영향은 적으나, **일관성 문제**로 남아있다

**권장 수정:** `FormBinder`에 `data-readonly-field` 속성을 가진 요소도 바인딩 대상으로 포함하거나, readonly 필드는 별도 처리 로직을 추가.

---

## 부록: 파일 매핑

| 컴포넌트 | 파일 경로 | 줄 수 |
|---|---|---|
| 메인 JS | `src/main/resources/static/js/sms/customer-search.js` | 201 |
| 페이지 HTML | `src/main/resources/templates/sms/customer-search.html` | 66 |
| 그리드 엔진 | `src/main/resources/static/js/common/tui-page-builder.js` | 434 |
| 폼 바인더 | `src/main/resources/static/js/common/form-binder.js` | 72 |
| 모달 관리자 | `src/main/resources/static/js/common/modal-manager.js` | 139 |
| HTTP 클라이언트 | `src/main/resources/static/js/common/http-client.js` | 196 |
| 알림 모듈 | `src/main/resources/static/js/common/notify.js` | 240 |
| 필드 포맷 | `src/main/resources/static/js/common/field-format.js` | 151 |
| 공통 유틸 | `src/main/resources/static/js/common/common-utils.js` | 240 |
| 그리드 유틸 | `src/main/resources/static/js/common/tui-common.js` | 126 |
| 모달 뼈대 | `src/main/resources/templates/fragments/modal-base.html` | 67 |
| 레이아웃 | `src/main/resources/templates/defaultLayout.html` | 130 |
| 컨트롤러 | `src/main/java/com/scbk/sms/controller/sms/CustomerSearchController.java` | 74 |
| 서비스 | `src/main/java/com/scbk/sms/service/sms/CustomerSearchService.java` | — |
| Mapper XML | `src/main/resources/mapper/sms/CustomerSearchMapper.xml` | — |
| VO | `src/main/java/com/scbk/sms/vo/sms/CustomerSearchVO.java` | — |
| DTO (검색) | `src/main/java/com/scbk/sms/dto/sms/CustomerSearchSearchRequestDTO.java` | — |
| DTO (수정) | `src/main/java/com/scbk/sms/dto/sms/CustomerSearchUpdateRequestDTO.java` | — |
