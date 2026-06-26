# SMS V3 프론트엔드 수정 가이드 (3.2~3.5)

> 작성일: 2026-06-24
> 근거: `docs/analysis/design-pattern-review.md` 3.2~3.5절
> 성격: **실행 지향적 수정 가이드** — 각 항목의 현재 상태, 수정 코드, 영향 범위를 한 곳에 정리
> 맥락: 폐쇄망 내부 SSR 도구 (CoreUI + Thymeleaf + 바닐라 JS + axios)

---

## 목차

- [1. 3.2 에러 응답 구조화 — P1 (✅ 적용 완료)](#1-32-에러-응답-구조화--p1-✅-적용-완료)
- [2. 3.3 웹 접근성 (A11Y) — P1/P2](#2-33-웹-접근성-a11y--p1p2)
- [3. 3.4 뷰포트 / 반응형 — P2](#3-34-뷰포트--반응형--p2)
- [4. 3.5 Design Token CSS — P3](#4-35-design-token-css--p3)
- [5. 적용 순서 및 체크리스트](#5-적용-순서-및-체크리스트)

---

## 1. 3.2 에러 응답 구조화 — P1 (✅ 적용 완료)

### 1.1 개요

`@Valid` 검증 실패 시 **전체 필드 에러**를 `errors[]` 배열로 반환하여, AJAX 프론트엔드에서 필드별 에러 매핑이 가능하도록 구조화.

### 1.2 수정 전 (문제점)

- `ApiResponse`에 `errors` 필드 부재 → 검증 실패해도 `message` 1개만 반환
- `GlobalExceptionHandler`가 **첫 번째 에러 메시지 1개만** 추출
- `common-utils.js` 인터셉터가 `message`만 모달로 표시 → 어떤 필드가 잘못되었는지 알 수 없음

### 1.3 수정 후 (적용 내역)

#### A. `ApiResponse.java` — `errors` 필드 + `FieldError` 레코드 추가

```java
/** 필드 단위 검증 에러 정보 */
public record FieldError(String field, String message) {
}

private final List<FieldError> errors;  // 신규 필드

// 기존 error(code, message) 시그니처 유지 — errors = null
public static <T> ApiResponse<T> error(int code, String message) { ... }

// 신규 오버로드 — 필드별 에러 목록 포함
public static <T> ApiResponse<T> error(int code, String message, List<FieldError> errors) {
    return new ApiResponse<>(code, message, null, errors);
}
```

> **기존 시그니처 유지**: `error(code, message)` 호출부는 수정 없음. `errors`가 `null`이면 JSON에서 생략되므로 성공 응답 형태 변화 없음.

#### B. `GlobalExceptionHandler.java` — `@Valid` 실패 시 전체 필드 에러 반환

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
protected Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
        HttpServletRequest request) {
    BindingResult bindingResult = e.getBindingResult();

    // 모든 필드 에러 추출 (첫 번째 1개가 아닌 전체)
    List<ApiResponse.FieldError> errors = bindingResult.getFieldErrors().stream()
            .map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

    // JSON 요청: errors[] 포함 응답 / HTML 요청: 첫 메시지로 에러 페이지
    return respond(ErrorCode.INVALID_INPUT_VALUE.getStatus(),
            ErrorCode.INVALID_INPUT_VALUE.getMessage(),
            firstMessage, errors, request);
}
```

#### C. `common-utils.js` — 모달 큐 시스템 + `errors[]` 인식

**모달 큐 (다중 에러 동시 발생 시 순차 표시):**

```js
const _modalQueue = [];
let _isModalShowing = false;

const _showCustomModal = (type, msg, title, onConfirm, onCancel) => {
    _modalQueue.push({ type, msg, title, onConfirm, onCancel });
    _processModalQueue();  // 큐에서 1개씩 순차 렌더링
};
```

**axios interceptor — `errors[]` 인식 및 필드 하이라이트:**

```js
error => {
    hideSpinner();
    const apiErr = error.response?.data;

    // @Valid 실패: errors[]가 있으면 목록 형태로 포맷팅 + 필드 하이라이트
    if (apiErr?.errors?.length > 0) {
        const errorList = apiErr.errors.map(e => `• ${e.message}`).join('\n');
        displayMsg = `${apiErr.message}\n\n${errorList}`;

        // 각 필드에 is-invalid 클래스 부여 (data-field, id, name 속성으로 검색)
        apiErr.errors.forEach(e => {
            const fieldEl = document.querySelector(`[data-field="${e.field}"]`)
                || document.querySelector(`#${e.field}`)
                || document.querySelector(`[name="${e.field}"]`);
            if (fieldEl) {
                fieldEl.classList.add('is-invalid');
                // 값 변경 시 에러 스타일 자동 제거
                fieldEl.addEventListener('input', () => fieldEl.classList.remove('is-invalid'), { once: true });
            }
        });
    }

    _showCustomModal('alert', displayMsg, '오류');
}
```

### 1.4 응답 JSON 예시

```json
{
  "timestamp": "2026-06-24T16:10:33.123",
  "code": 400,
  "message": "입력값 검증 실패",
  "data": null,
  "errors": [
    { "field": "receiverNo", "message": "수신번호는 필수입니다" },
    { "field": "sendType", "message": "발송유형을 선택하세요" }
  ]
}
```

### 1.5 영향 범위

| 파일 | 변경 내용 | 위험도 |
|------|-----------|--------|
| `ApiResponse.java` | `errors` 필드 + `FieldError` 레코드 + `error` 오버로드 | 낮음 — 기존 시그니처 유지 |
| `GlobalExceptionHandler.java` | `handleMethodArgumentNotValidException` 전체 에러 반환 + `respond` 오버로드 | 낮음 |
| `common-utils.js` | 모달 큐 + `errors[]` 인식 + 필드 하이라이트 | 중간 — 모든 AJAX에 적용 |

> **완료 기준**: `mvn test` + `mvn -DskipTests package` 성공, AJAX 폼에서 다중 필드 에러 정상 표시 확인

---

## 2. 3.3 웹 접근성 (A11Y) — P1/P2

### 2.1 현재 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| `<html lang="ko">` | ✅ 있음 | `defaultLayout.html` |
| `<label for>` 연결 | ✅ 양호 | 폼 라벨 대부분 `for` 연결 |
| `aria-label` | 🟡 일부 | datepicker input에 영문 `aria-label` |
| `:focus-visible` | ❌ 없음 | 키보드 포커스 시각 표시 부재 |
| 아이콘 전용 버튼 | 🟡 | `<i data-lucide>`만 있고 텍스트 없는 버튼 |
| 스킵 네비게이션 | ❌ 없음 | "본문으로 건너뛰기" 링크 부재 |
| 색상 대비 (4.5:1) | ⚠️ 미점검 | `.text-secondary` 등 일부 의심 |

### 2.2 맥락 평가

폐쇄망 내부 도구이므로 **법적 준수 압력은 낮음**. 그러나:
- 키보드 사용자, 고령 운영자 고려 시 **기본 수준의 a11y는 가치 있음**
- `:focus-visible`과 아이콘 버튼 `aria-label`은 **비용이 낮고 효과가 큼**

### 2.3 수정 방안 (우선순위 순)

#### P1-A: `:focus-visible` 글로벌 스타일 (`admin-ui-bridge.css` 또는 신규 `a11y.css`)

```css
/* 모든 포커스 가능 요소에 일관된 포커스 링 제공 */
:focus-visible {
    outline: 2px solid var(--cui-primary);
    outline-offset: 2px;
}

/* 버튼/링크 키보드 조작 시 outline으로 보강 */
.btn:focus-visible,
.nav-link:focus-visible,
a:focus-visible {
    outline: 2px solid var(--cui-primary);
    outline-offset: 2px;
}
```

**대상 파일:** `src/main/resources/static/css/admin-ui-bridge.css` (하단에 추가)

#### P1-B: 아이콘 전용 버튼에 `aria-label` + `aria-hidden` (`fragments/header.html`)

```html
<!-- 수정 전 -->
<button id="sidebarToggleBtn" type="button" title="사이드바 접기/펼치기">
    <i id="toggleIcon" data-lucide="panel-left-close" class="layout-icon"></i>
</button>

<!-- 수정 후 -->
<button id="sidebarToggleBtn" type="button"
        aria-label="사이드바 접기/펼치기" title="사이드바 접기/펼치기">
    <i id="toggleIcon" data-lucide="panel-left-close" class="layout-icon"
       aria-hidden="true"></i>
</button>
```

```html
<!-- 로그아웃 버튼 -->
<button type="submit" class="btn btn-link nav-link px-0 text-secondary"
        aria-label="로그아웃" title="로그아웃">
    <i data-lucide="log-out" class="layout-icon" aria-hidden="true"></i>
</button>
```

> `title`만 있으면 스크린 리더가 읽지 않을 수 있음. `aria-label` 필수. 장식용 `<i>`에는 `aria-hidden="true"`.

**대상 파일:** `src/main/resources/templates/fragments/header.html`

#### P1-C: 검색 라벨 한글화 (`sms/history.html` 등)

현재 검색 라벨이 `sendType`, `sendStatus` 등 **컬럼명(영문)** 그대로 노출됨.

```html
<!-- 수정 전 -->
<label for="sendType" class="col-form-label fw-bold">sendType</label>

<!-- 수정 후 -->
<label for="sendType" class="col-form-label fw-bold">발송유형</label>
```

> Scaffold 생성기가 컬럼명을 라벨로 내보내는 패턴이라면, **헤더명(header name) 매핑 로직 수정이 선행**되어야 함. 이는 별도 작업.

**대상 파일:** `src/main/resources/templates/sms/history.html`, scaffold 템플릿

#### P2: 스킵 네비게이션 (`defaultLayout.html`)

```html
<body class="admin-shell">
    <!-- 본문 건너뛰기 링크 (시각 장애인·키보드 사용자용) -->
    <a href="#main-content" class="visually-hidden-focusable skip-link">본문으로 건너뛰기</a>

    <!-- 기존 사이드바 ... -->
    <div class="content-area">
        <main id="main-content" layout:fragment="content"></main>
    </div>
```

```css
/* admin-ui-bridge.css */
.skip-link {
    position: absolute;
    top: -40px;
    left: 0;
    background: var(--cui-primary);
    color: var(--cui-white);
    padding: 8px 16px;
    z-index: 1100;
    text-decoration: none;
    transition: top .2s;
}
.skip-link:focus {
    top: 0;
}
```

**대상 파일:** `defaultLayout.html`, `admin-ui-bridge.css`

#### P3: 색상 대비 수동 점검

- 폐쇄망에서 axe-core 등 자동 검사 도구 도입 불가 → **수동 체크리스트** 운영
- 주요 점검: `header.html` 사용자명(`text-secondary`), `scaffold.html` 보조 텍스트

### 2.4 영향 범위

| 파일 | 변경 내용 | 위험도 |
|------|-----------|--------|
| `admin-ui-bridge.css` | `:focus-visible`, `.skip-link` 추가 | 낮음 |
| `fragments/header.html` | `aria-label`, `aria-hidden` 추가 | 낮음 |
| `defaultLayout.html` | 스킵 링크 + `<main id>` 추가 | 낮음 |
| `sms/history.html` 등 | 라벨 한글화 | 중간 — scaffold 개선 선행 |

> CoreUI 컴포넌트 내부 ARIA는 CoreUI가 관리하므로 직접 추가하지 않음.

---

## 3. 3.4 뷰포트 / 반응형 — P2

### 3.1 현재 상태

```html
<!-- defaultLayout.html line 6 -->
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

✅ **이미 올바르게 설정됨.** 추가 작업 불필요.

반응형 현황:
- `admin-layout.css`: 미디어 쿼리 없음 (사이드바는 JS 토글 기반)
- `admin-ui-bridge.css`: `@media (max-width: 576px)` 1개 (모달 상세 row)
- `scaffold.html`: CoreUI 그리드(`col-12 col-md-3`) 사용

### 3.2 맥락 평가

> 폐쇄망 내부 운영 도구, **운영 디바이스는 데스크톱이 압도적**. 모바일/태블릿 최적화는 과잉.

- `viewport` meta: 이미 충족 → **추가 작업 불필요**
- 사이드바: JS 토글(`sidebarToggleBtn`)로 이미 대응
- CoreUI 그리드: 기본 반응형 제공

### 3.3 최소 보완 사항

**유일한 권장:** `history.html` 검색 조건 row에 `flex-wrap` 추가

```html
<!-- 수정 전 -->
<div class="row align-items-center g-3">

<!-- 수정 후 -->
<div class="row align-items-center g-3 flex-wrap">
```

**대상 파일:** `src/main/resources/templates/sms/history.html`

### 3.4 결론

> **3.4는 사실상 "이미 충족"**. `viewport` meta + CoreUI 그리드 + 데스크톱 위주 환경이므로, 별도 작업은 `flex-wrap` 1줄 추가가 전부.

---

## 4. 3.5 Design Token CSS — P3

### 4.1 현재 상태

**양호한 부분:**

`admin-layout.css`에 이미 로컬 변수 정의:
```css
.admin-shell {
    --sms-sidebar-width: 256px;
    --sms-sidebar-collapsed-width: 64px;
}
```

`admin-ui-bridge.css`는 CoreUI 변수(`--cui-*`)를 적극 활용 중:
```css
.tui-grid-cell { color: var(--cui-body-color) !important; }
.autocomplete-balloon {
    background: var(--cui-body-bg);
    border: 1px solid var(--cui-border-color);
}
```

### 4.2 문제점 (인라인 스타일·하드코딩 산재)

#### A. `fragments/header.html` — 인라인 스타일 다수

```html
<header ... style="z-index: 10;">
<div class="container-fluid px-3" style="min-height: 56px;">
<span ... style="font-size: 0.9rem;">
<div class="avatar ..." style="width: 32px; height: 32px; font-size: 0.85rem;">
```

→ 매직 넘버(`56px`, `32px`, `0.9rem`) 산재. 유지보수성 저하.

#### B. `error/error.html` — 하드코딩 색상값

```css
.error-status { color: #dc3545; }        /* → var(--cui-danger) */
.error-message { color: #495057; }       /* → var(--cui-body-color) */
.error-actions a.primary { background: #0d6efd; }  /* → var(--cui-primary) */
```

#### C. `admin-ui-bridge.css` — magic number

```css
.toast-grid-page-size { width: 80px; }       /* → var(--sms-page-size-width) */
.scaffold-search-control { width: 160px; }   /* → var(--sms-search-control-width) */
.history-datepicker-input { height: 38px; }  /* → var(--sms-control-height) */
```

### 4.3 수정 방안 (단계별)

#### Step 1: 프로젝트 디자인 토큰 정의 (`admin-layout.css` 상단 또는 신규 `admin-tokens.css`)

```css
:root {
    /* Layout */
    --sms-header-height: 56px;
    --sms-sidebar-width: 256px;
    --sms-sidebar-collapsed-width: 64px;

    /* Component sizes */
    --sms-control-height: 38px;
    --sms-search-control-width: 160px;
    --sms-page-size-width: 80px;

    /* Icon */
    --sms-icon-sm: .95rem;
    --sms-icon-md: 1rem;
    --sms-icon-lg: 1.1rem;

    /* Avatar */
    --sms-avatar-md: 32px;
}
```

> CoreUI 변수(`--cui-*`)는 그대로 두고, **프로젝트 고유 값만** `--sms-*` 네임스페이스로 토큰화.

**대상 파일:** `src/main/resources/static/css/admin-layout.css` (상단) 또는 신규 `admin-tokens.css`

#### Step 2: `header.html` 인라인 스타일 → 클래스 이관

```html
<!-- 수정 전 -->
<header class="header header-sticky p-0 border-bottom" style="z-index: 10;">
<div class="container-fluid px-3" style="min-height: 56px;">

<!-- 수정 후 -->
<header class="header header-sticky p-0 border-bottom admin-header">
<div class="container-fluid px-3 admin-header-inner">
```

```css
/* admin-layout.css */
.admin-header { z-index: 10; }
.admin-header-inner { min-height: var(--sms-header-height); }
.admin-avatar {
    width: var(--sms-avatar-md);
    height: var(--sms-avatar-md);
    font-size: .85rem;
    display: flex;
    align-items: center;
    justify-content: center;
}
```

#### Step 3: `error.html` 색상 토큰화

> `error.html`은 독립 화면(레이아웃 미사용)이므로 CoreUI CSS 링크 추가 필요.

```html
<!-- error.html <head>에 CoreUI CSS 추가 -->
<link rel="stylesheet" th:href="@{/vendor/coreui/css/coreui.min.css}">
```

```css
.error-status { color: var(--cui-danger); }
.error-message { color: var(--cui-body-color); }
.error-actions a.primary {
    background: var(--cui-primary);
    border-color: var(--cui-primary);
    color: var(--cui-white);
}
```

#### Step 4: `admin-ui-bridge.css` magic number → 토큰 참조

```css
.scaffold-search-control { width: var(--sms-search-control-width); }
.toast-grid-page-size { width: var(--sms-page-size-width); }
.history-datepicker-input { height: var(--sms-control-height); }
```

### 4.4 영향 범위

| 파일 | 변경 내용 | 위험도 |
|------|-----------|--------|
| `admin-layout.css` 또는 신규 `admin-tokens.css` | 토큰 정의 | 낮음 |
| `header.html` | 인라인 → 클래스 | 낮음 — 시각 동일 확인 필요 |
| `error.html` | CoreUI CSS 링크 + 색상 변수화 | 낮음 — 독립 화면 |
| `admin-ui-bridge.css` | magic number → 토큰 참조 | 낮음 — 값 동일 |

> **다크모드는 도입하지 않음** (폐쇄망 내부 도구, 불필요). 토큰화만으로 일관성·유지보수성 향상.

---

## 5. 적용 순서 및 체크리스트

### 5.1 권장 적용 순서

| 순서 | 항목 | 작업량 | 상태 |
|------|------|--------|------|
| 1 | **3.2** 에러 응답 `errors[]` (백엔드 + JS) | 중간 | ✅ 적용 완료 |
| 2 | **3.3** A11Y: `:focus-visible` + `aria-label` (CSS + HTML) | 낮음 | ⬜ 대기 |
| 3 | **3.5** Design Token 정의 (`:root --sms-*`) | 낮음 | ⬜ 대기 |
| 4 | **3.5** `header.html` 인라인 → 클래스 이관 | 낮음 | ⬜ 대기 |
| 5 | **3.4** `flex-wrap` 추가 | 거의 0 | ⬜ 대기 |
| 6 | **3.3** 스킵 네비게이션, 라벨 한글화 | 중간 | ⬜ 대기 (라벨은 scaffold 선행) |
| 7 | **3.5** `error.html` + `admin-ui-bridge.css` 토큰화 | 중간 | ⬜ 대기 |

### 5.2 완료 기준 (AGENTS.md 준수)

각 단계 완료 시:

- [ ] `mvn test` 성공
- [ ] `mvn -DskipTests package` 성공
- [ ] 관련 문서 갱신 (본 문서 + `design-pattern-review.md` 6절 조치 이력)
- [ ] local/dev/prod profile별 설정 차이 없음 확인

### 5.3 제약사항

- 폐쇄망 환경 → npm 외부 registry 의존성(axe-core 등) 도입 금지
- 매직넘버/매직스트링은 상수(CSS 변수)로 외부화
- 실패를 fallback으로 우회하지 않음
- 지시받지 않은 패턴/라이브러리/의존성 임의 도입 금지