# SMS V3 프론트엔드 개선 방안 (3.2~3.5)

> 작성일: 2026-06-24
> 대상: `design-pattern-review.md` 3.2~3.5 항목의 구체적 수정 방안 도출
> 성격: **수정 방안 문서** (코드 수정 전 설계 합의용)
> 맥락: 폐쇄망 내부 SSR 도구, CoreUI + Thymeleaf + 바닐라 JS + axios

---

## 0. 분석 대상 코드 (실제 확인 결과)

| 항목 | 주요 파일 |
|------|-----------|
| 3.2 에러 응답 | `ApiResponse.java`, `GlobalExceptionHandler.java`, `ErrorCode.java`, `common-utils.js`(axios interceptor) |
| 3.3 A11Y | `defaultLayout.html`, `fragments/header.html`, `sms/history.html`, `system/scaffold.html`, `error/error.html` |
| 3.4 뷰포트 | `defaultLayout.html`(meta), `admin-layout.css`, `admin-ui-bridge.css` |
| 3.5 Design Token | `admin-layout.css`, `admin-ui-bridge.css`, `fragments/header.html`, `error/error.html` |

---

## 1. 3.2 에러 응답 구조화 (AJAX 대상 한정) — P1

### 1.1 현재 상태

**`ApiResponse.java`** — 성공/실패 공통 래퍼. 필드는 `timestamp`, `code`, `message`, `data`뿐.

```java
public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);  // data = null 고정
}
```

**`GlobalExceptionHandler#handleMethodArgumentNotValidException`** — `@Valid` 실패 시 **첫 번째 에러 메시지 1개**만 추출해 반환.

```java
String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
return respond(ErrorCode.INVALID_INPUT_VALUE.getStatus(), errorMessage, request);
```

**`common-utils.js` axios 인터셉터** — 응답의 `message`만 사용. 필드별 에러를 UI에 매핑할 수 있는 구조가 아님.

```js
// 실패 시
if (error.response && error.response.data && error.response.data.message) {
    _showCustomModal('alert', error.response.data.message, '오류');
}
```

### 1.2 문제점

- `@Valid`로 여러 필드가 동시에 실패해도 **1개 메시지만** 반환 → 사용자가 한 번에 수정 불가
- 프론트엔드가 "어떤 입력 필드가 잘못되었는지" 알 수 없어, **해당 입력란 하이라이트/에러 문구 표시** 불가
- 현재는 모달 1개로 에러를 띄우는 단순 UX라 "동작은 하지만", 폼이 많아지면 UX 저하

### 1.3 수정 방안

#### Step 1: `ApiResponse`에 `errors` 필드 추가 (오버로드, 기존 시그니처 유지)

```java
// ApiResponse.java — 신규 필드 + 팩토리 추가. 기존 error(code, message)는 그대로 유지

private final List<FieldError> errors;

private ApiResponse(int code, String message, T data, List<FieldError> errors) {
    this.timestamp = LocalDateTime.now().toString();
    this.code = code;
    this.message = message;
    this.data = data;
    this.errors = errors;
}

/** 기존 시그니처 유지 (errors = null) */
public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null, null);
}

/** 검증 실패 전용 — 필드별 에러 목록 포함 */
public static <T> ApiResponse<T> error(int code, String message, List<FieldError> errors) {
    return new ApiResponse<>(code, message, null, errors);
}

public List<FieldError> getErrors() { return errors; }

/** 필드 검증 에러 1건을 표현하는 중첩 DTO */
@lombok.Getter
@lombok.AllArgsConstructor
public static class FieldError {
    private String field;
    private String message;
}
```

> JSON 직렬화 시 `errors`가 `null`이면 필드 자체가 생략되거나 `null`로 나가므로, 기존 성공 응답 형태는 변하지 않음.

#### Step 2: `GlobalExceptionHandler`에서 `errors[]` 반환 (JSON 응답에 한정)

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
protected Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                       HttpServletRequest request) {
    BindingResult bindingResult = e.getBindingResult();
    // 모든 필드 에러를 추출 (첫 번째 1개가 아닌 전체)
    List<ApiResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
        .map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
        .toList();

    String firstMessage = fieldErrors.isEmpty()
        ? ErrorCode.INVALID_INPUT_VALUE.getMessage()
        : fieldErrors.get(0).getMessage();
    log.warn("입력값 검증 실패: {}건", fieldErrors.size());

    // JSON 요청일 때만 errors[]를 포함한 응답 반환
    if (!isHtmlRequest(request)) {
        return new ResponseEntity<>(
            ApiResponse.error(HttpStatus.BAD_REQUEST.value(), firstMessage, fieldErrors),
            HttpStatus.BAD_REQUEST);
    }
    // HTML 요청은 기존대로 첫 메시지로 에러 페이지
    return respond(ErrorCode.INVALID_INPUT_VALUE.getStatus(), firstMessage, request);
}
```

#### Step 3: `respond()` 헬퍼는 기존 시그니처 유지 (영향 최소화)

`respond(HttpStatus, String, HttpServletRequest)`는 그대로 두고, 검증 실패 케이스만 별도 분기로 처리 → 기존 핸들러(CustomException, 404, 500)는 수정 없음.

#### Step 4: `common-utils.js` 인터셉터 확장 (errors[] 인식)

```js
// 기존: error.response.data.message만 사용
// 개선: errors[]가 있으면 각 필드 에러를 입력 요소에 매핑하거나 목록으로 표시
error => {
    hideSpinner();
    const body = error.response?.data;
    if (body?.errors?.length) {
        // 필드별 에러 UI 처리 (선택): 에러 문구를 입력란 하단에 표시
        applyFieldErrors(body.errors);
        _showCustomModal('alert',
            `${body.message}\n\n(${body.errors.length}건의 입력 오류)`, '입력 오류');
    } else if (body?.message) {
        _showCustomModal('alert', body.message, '오류');
    } else {
        _showCustomModal('alert', '서버와 통신 중 알 수 없는 오류가 발생했습니다.', '시스템 오류');
    }
    return Promise.reject(error);
}
```

`applyFieldErrors(errors)` 헬퍼는 `errors[].field` 값을 `name` 속성으로 검색해 `.is-invalid` 클래스 부여 + 에러 문구 표시. (CoreUI/Bootstrap 의 `is-invalid` 패턴 재사용)

### 1.4 영향 범위 및 주의사항

| 파일 | 변경 유형 | 위험도 |
|------|-----------|--------|
| `ApiResponse.java` | 필드 추가(기존 시그니처 유지) | 낮음 — 기존 호출부 영향 없음 |
| `GlobalExceptionHandler.java` | 검증 실패 핸들러만 수정 | 낮음 — HTML 분기 유지 |
| `common-utils.js` | 인터셉터 확장 | 중간 — 모든 AJAX에 적용되므로 회귀 테스트 필요 |
| `SmsHistoryController.java` | 변경 없음 | — (이미 `@Valid` 사용 중) |

- **SSR 화면(HTML) 요청은 기존과 동일**하게 동작 (에러 페이지로 이동)
- `errors[]`는 **JSON 응답에만** 추가되므로 화면 이동 시 영향 없음
- 완료 기준: `mvn test` + `mvn -DskipTests package` 성공, AJAX 등록/수정 폼에서 다중 필드 에러 정상 표시 확인

---

## 2. 3.3 웹 접근성 (A11Y) — P1, 점진적 개선

### 2.1 현재 상태 점검 결과

| 항목 | 상태 | 비고 |
|------|------|------|
| `<html lang="ko">` | ✅ 있음 | `defaultLayout.html`, `error.html` |
| `<label for>` 연결 | ✅ 양호 | `history.html`, `scaffold.html` 폼 라벨 모두 `for` 연결 |
| `aria-label` | 🟡 일부 | datepicker input에 `aria-label="sentAt"` (의미 부족) |
| `title` 속성 | 🟡 일부 | sidebar toggle, 로그아웃 버튼에 `title` 사용 |
| `:focus-visible` | ❌ 없음 | 키보드 포커스 시각 표시 부재 |
| 아이콘 전용 버튼 | 🟡 | `<i data-lucide>`만 있고 텍스트 없는 버튼 일부 (로그아웃 등) |
| 스킵 네비게이션 | ❌ 없음 | "본문으로 건너뛰기" 링크 부재 |
| 색상 대비 (4.5:1) | ⚠️ 미점검 | CoreUI 기본 테마 사용 중이나 `.text-secondary` 등 일부 의심 |

### 2.2 문제점 (폐쇄망 내부 도구 맥락)

- 폐쇄망 내부 도구라 **법적 준수(Web Accessibility Act 등) 압력은 낮음**
- 그러나 키보드 사용자, 고령 운영자, 고대비 모드 사용자를 고려하면 **기본 수준의 a11y는 가치 있음**
- 특히 **키보드 포커스 표시(`:focus-visible`)**와 **아이콘 전용 버튼의 대체 텍스트**는 비용이 낮고 효과가 큼

### 2.3 수정 방안 (우선순위 순)

#### P1-A: `:focus-visible` 글로벌 스타일 추가 (`admin-ui-bridge.css` 또는 신규 `a11y.css`)

```css
/* 모든 포커스 가능 요소에 일관된 포커스 링 제공 (CoreUI 기본을 보강) */
:focus-visible {
    outline: 2px solid var(--cui-primary);
    outline-offset: 2px;
}

/* 버튼/링크는 CoreUI의 box-shadow를 존중하되, 키보드 조작 시엔 outline으로 보강 */
.btn:focus-visible,
.nav-link:focus-visible,
a:focus-visible {
    outline: 2px solid var(--cui-primary);
    outline-offset: 2px;
}
```

#### P1-B: 아이콘 전용 버튼에 `aria-label` 추가 (`header.html`)

```html
<!-- 수정 전 -->
<button id="sidebarToggleBtn" type="button" title="사이드바 접기/펼치기" ...>
    <i id="toggleIcon" data-lucide="panel-left-close" class="layout-icon"></i>
</button>

<!-- 수정 후 -->
<button id="sidebarToggleBtn" type="button"
        aria-label="사이드바 접기/펼치기" title="사이드바 접기/펼치기" ...>
    <i id="toggleIcon" data-lucide="panel-left-close" class="layout-icon"
       aria-hidden="true"></i>
</button>
```

```html
<!-- 로그아웃 버튼 -->
<button type="submit" class="btn btn-link nav-link px-0 text-secondary"
        aria-label="로그아웃" title="로그아웃" ...>
    <i data-lucide="log-out" class="layout-icon" aria-hidden="true"></i>
</button>
```

> `title`만 있으면 스크린 리더가 읽지 않을 수 있음. `aria-label` 필수. 장식용 `<i>`에는 `aria-hidden="true"`.

#### P1-C: 라벨 텍스트 의미화 (`history.html` 등 scaffold 생성 화면)

현재 `history.html`의 검색 라벨이 `sendType`, `sendStatus` 등 **컬럼명(영문)** 그대로 노출됨.

```html
<!-- 수정 전 -->
<label for="sendType" class="col-form-label fw-bold">sendType</label>

<!-- 수정 후 (한글 라벨 + 컬럼명은 title/placeholder로 보조) -->
<label for="sendType" class="col-form-label fw-bold">발송유형</label>
```

> Scaffold 생성기(`ScaffoldController`)가 컬럼명을 라벨로 내보내는 패턴이라면, **헤더명(header name) 컬럼 옵션**을 라벨에 매핑하도록 scaffold 로직 수정이 선행되어야 함. 이는 별도 작업.

#### P2: 스킵 네비게이션 (`defaultLayout.html`)

```html
<body class="admin-shell">
    <a href="#main-content" class="visually-hidden-focusable skip-link">본문으로 건너뛰기</a>
    <!-- 사이드바 ... -->
    <main id="main-content" layout:fragment="content"></main>
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
    transition: top .2s;
}
.skip-link:focus { top: 0; }
```

#### P3: 색상 대비 점검 (도구 활용)

- CoreUI 기본 테마의 `.text-secondary`(`#6c757d` on white = 약 4.5:1, 경계치) 사용처 점검
- 폐쇄망에서는 자동 검사 도구(axe-core 등 npm 의존성) 도입이 어려우므로 **수동 점검 체크리스트**로 운영
- 주요 점검 대상: `header.html`의 사용자명(`text-secondary`), `scaffold.html`의 보조 텍스트(`text-secondary small`)

### 2.4 영향 범위 및 주의사항

| 파일 | 변경 유형 | 위험도 |
|------|-----------|--------|
| `admin-ui-bridge.css` | `:focus-visible`, `.skip-link` 추가 | 낮음 — 시각만 보강 |
| `fragments/header.html` | `aria-label`, `aria-hidden` 추가 | 낮음 |
| `sms/history.html` 등 | 라벨 텍스트 의미화 | 중간 — scaffold 생성 패턴 수정 선행 필요 |
| `defaultLayout.html` | 스킵 링크 추가 | 낮음 |

- **CoreUI 컴포넌트 내부 ARIA는 CoreUI가 관리**하므로, 우리가 직접 추가하지 않음 (중복 방지)
- 점진적 적용: `:focus-visible` + `aria-label` 먼저, 라벨 의미화는 scaffold 개선과 병행

---

## 3. 3.4 뷰포트 / 반응형 — P2, 최소 보완

### 3.1 현재 상태

**`defaultLayout.html` line 6:**
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```
✅ **이미 올바르게 설정되어 있음.**

**반응형 미디어 쿼리 현황:**
- `admin-layout.css`: 미디어 쿼리 없음 (사이드바는 JS 토글 기반)
- `admin-ui-bridge.css`: `@media (max-width: 576px)` 1개 (모달 상세 row만)
- `scaffold.html`: Bootstrap/CoreUI 그리드(`col-12 col-md-3`) 사용 → 프레임워크 반응형에 의존

### 3.2 평가 (데스크톱 위주 맥락)

> 폐쇄망 내부 운영 도구이고 **운영 디바이스는 데스크톱이 압도적**. 모바일/태블릿 최적화는 과잉.

- `viewport` meta는 이미 충족 → **추가 작업 불필요**
- 사이드바 collapse는 JS 기반(`sidebarToggleBtn`)으로 이미 대응
- Bootstrap/CoreUI 그리드가 기본 반응형 제공 → 추가 미디어 쿼리는 최소화

### 3.3 최소 보완 사항 (선택)

| 항목 | 필요성 | 권장 |
|------|--------|------|
| `viewport` meta | ✅ 이미 있음 | 유지 |
| 사이드바 모바일 오버레이 | 낮음 | 데스크톱 전용이므로 생략 |
| 검색 카드 가로 스크롤 | 낮음 | `history.html` 검색 조건이 많을 경우 `flex-wrap` 추가 정도 |
| 그리드 가로 스크롤 | 이미 있음 | `toast-grid` fragment에 `overflow-x: auto` 확인 필요 |

**유일한 권장 보완:** `history.html` 검색 조건 row에 `flex-wrap` 추가 (창 너비 좁아질 시 줄바꿈)

```html
<!-- 수정 전 -->
<div class="row align-items-center g-3">

<!-- 수정 후 -->
<div class="row align-items-center g-3 flex-wrap">
```

### 3.4 결론

> **3.4는 사실상 "이미 충족"에 가깝다.** `viewport` meta 존재 + CoreUI 그리드 + 데스크톱 위주 환경이므로, 별도 작업은 검색 카드 `flex-wrap` 정도가 전부. P2 유지하되 **작업량은 거의 0**.

---

## 4. 3.5 Design Token CSS — P3, 점진적 정리

### 4.1 현재 상태

**양호한 부분:**

`admin-layout.css` — 프로젝트 로컬 변수 정의:
```css
.admin-shell {
    --sms-sidebar-width: 256px;
    --sms-sidebar-collapsed-width: 64px;
}
```

`admin-ui-bridge.css` — CoreUI 변수(`--cui-*`) 적극 활용:
```css
.tui-grid-container { border-color: var(--cui-border-color) !important; }
.tui-grid-cell { color: var(--cui-body-color) !important; }
.autocomplete-balloon {
    background: var(--cui-body-bg);
    border: 1px solid var(--cui-border-color);
    box-shadow: var(--cui-box-shadow);
}
```

→ **이미 CoreUI 디자인 토큰(`--cui-*`)을 일관되게 사용하고 있음.**

### 4.2 문제점 (인라인 스타일·하드코딩 산재)

#### A. `fragments/header.html` — 인라인 스타일 다수

```html
<header ... style="z-index: 10;">
<div class="container-fluid px-3" style="min-height: 56px;">
<button ... style="text-decoration:none; box-shadow:none;">
<span ... style="font-size: 0.9rem;">
<div class="avatar ..." style="width: 32px; height: 32px; font-size: 0.85rem; ...">
<button ... style="text-decoration: none;">
```

→ 매직 넘버(`56px`, `32px`, `0.9rem`, `0.85rem`)와 인라인 스타일이 혼재. 유지보수성 저하.

#### B. `error/error.html` — 하드코딩 색상값

```css
.error-status { color: #dc3545; }       /* CoreUI danger */
.error-message { color: #495057; }      /* CoreUI body color */
.error-actions a.primary { background: #0d6efd; border-color: #0d6efd; }  /* primary */
```

→ CoreUI 변수를 쓰면 일관성 확보 가능 (`var(--cui-danger)`, `var(--cui-body-color)`, `var(--cui-primary)`). 단, `error.html`은 레이아웃을 공유하지 않는 **독립 화면**이므로 CoreUI CSS가 로드되지 않을 수 있음 → 변수 사용 시 CoreUI CSS 링크 추가 필요.

#### C. `admin-ui-bridge.css` — magic number 산재

```css
.toast-grid-page-size { width: 80px; }
.scaffold-search-control { width: 160px; }
.history-datepicker-input { height: 38px; }
#tui-auto-modal .detail-row { grid-template-columns: minmax(148px, 190px); }
```

→ 컴포넌트 크기값이 하드코딩. 토큰화하면 일관 조정 가능.

### 4.3 수정 방안 (단계별)

#### Step 1: 프로젝트 디자인 토큰 정의 (`admin-tokens.css` 신규 또는 `admin-layout.css` 상단)

```css
:root {
    /* Layout */
    --sms-header-height: 56px;
    --sms-sidebar-width: 256px;
    --sms-sidebar-collapsed-width: 64px;

    /* Component sizes */
    --sms-control-height: 38px;        /* datepicker, input 높이 통일 */
    --sms-search-control-width: 160px; /* 검색 input 기본 너비 */
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
/* admin-layout.css 또는 admin-ui-bridge.css */
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

#### Step 3: `error.html` 색상 토큰화 (CoreUI CSS 링크 추가 후)

```html
<!-- error.html head에 CoreUI CSS 추가 (독립 화면이므로) -->
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

#### Step 4: `admin-ui-bridge.css` magic number 토큰 참조로 교체

```css
.scaffold-search-control { width: var(--sms-search-control-width); }
.toast-grid-page-size { width: var(--sms-page-size-width); }
.history-datepicker-input { height: var(--sms-control-height); }
```

### 4.4 영향 범위 및 주의사항

| 파일 | 변경 유형 | 위험도 |
|------|-----------|--------|
| `admin-tokens.css` (신규) 또는 `admin-layout.css` | 토큰 정의 | 낮음 |
| `header.html` | 인라인 → 클래스 | 낮음 — 시각 동일 확인 필요 |
| `error.html` | CoreUI CSS 링크 + 색상 변수화 | 낮음 — 독립 화면이므로 영향 격리 |
| `admin-ui-bridge.css` | magic number → 토큰 참조 | 낮음 — 값 동일 |

- **다크모드는 도입하지 않음** (폐쇄망 내부 도구, 불필요)
- 다크모드 없이 **토큰화만** 수행해도 일관성·유지보수성 향상 효과 확실
- 점진적 적용: 토큰 정의 → `header.html` 이관 → `error.html` → `admin-ui-bridge.css` 순

---

## 5. 우선순위 요약

| 우선순위 | 항목 | 작업량 | 영향도 | 비고 |
|----------|------|--------|--------|------|
| **P1** | 3.2 에러 응답 `errors[]` | 중간 | 중간 | 백엔드 2파일 + JS 1파일. AJAX UX 직접 개선 |
| **P1** | 3.3 A11Y: `:focus-visible`, `aria-label` | 낮음 | 중간 | CSS + header.html. 비용 대비 효과 큼 |
| **P2** | 3.3 A11Y: 스킵 네비게이션, 라벨 의미화 | 중간 | 낮음~중간 | 라벨은 scaffold 개선 선행 |
| **P2** | 3.4 뷰포트 | 거의 0 | 낮음 | 이미 충족. `flex-wrap` 정도 |
| **P3** | 3.5 Design Token | 중간 | 낮음 | 점진적 정리. 다크모드 제외 |

---

## 6. 제약사항 (AGENTS.md 준수)

- 폐쇄망 환경 → npm 외부 registry 의존성(axe-core 등) 도입 금지
- 매직넘버/매직스트링은 상수(또는 CSS 변수)로 외부화
- 실패를 fallback으로 우회하지 않음 (기존 `ApiResponse.error(code, message)` 시그니처 유지)
- `mvn test` + `mvn -DskipTests package` 성공이 완료 조건
- local/dev/prod profile별 설정 차이 없음 (프론트엔드 정적 리소스는 공통)

---

## 7. 후속 단계 제안

1. 본 문서 검토 후 **우선순위 합의**
2. P1(3.2 에러 응답 + 3.3 `:focus-visible`/`aria-label`)부터 개별 작업 티켓으로 분리
3. 각 작업 완료 시 `design-pattern-review.md` 6절 조치 이력 갱신