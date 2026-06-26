# 프론트엔드 개선 수정 명세서 (3.2~3.5)

> 작성일: 2026-06-24
> 기반 문서: `docs/analysis/design-pattern-review.md` 3.2~3.5
> 목적: 각 개선 항목의 **구체적 수정 방안**(before/after 코드 포함)을 정의하고, **Scaffold 템플릿 반영 계획**까지 포함한다.
> 원칙: 모든 신규 화면은 Scaffold 기반으로 생성되므로, 개선사항은 Scaffold 템플릿에 반영되어야 자동 적용된다.

---

## 목차

- [1. 에러 응답 구조화 (3.2) + 모달 큐잉](#1-에러-응답-구조화-32--모달-큐잉)
- [2. 웹 접근성 A11Y (3.3)](#2-웹-접근성-a11y-33)
- [3. 뷰포트 / 반응형 (3.4)](#3-뷰포트--반응형-34)
- [4. Design Token CSS + Status 아이콘화 (3.5)](#4-design-token-css--status-아이콘화-35)
- [5. Scaffold 템플릿 반영 계획](#5-scaffold-템플릿-반영-계획)
- [6. 구현 순서 및 검증](#6-구현-순서-및-검증)

---

## 1. 에러 응답 구조화 (3.2) + 모달 큐잉

### 1.1 현재 문제

| 문제 | 위치 | 설명 |
|------|------|------|
| 필드별 에러 미반환 | `GlobalExceptionHandler.java` L36-45 | `@Valid` 실패 시 `getAllErrors().get(0)`로 **첫 번째 에러 1개만** 반환. 프론트에서 필드별 에러 매핑 불가 |
| `errors[]` 필드 부재 | `ApiResponse.java` | 공통 응답 래퍼에 `errors` 필드가 없어 구조화된 에러 전달 불가 |
| 모달 중첩 불가 | `common-utils.js` L96-139 | `_showCustomModal`이 단일 DOM 요소를 덮어씀. 동시 호출(예: 저장 중 세션 만료 + 검증 에러 동시) 시 **마지막 모달만 표시**, 이전 메시지 손실 |
| axios 인터셉터 `errors[]` 미인식 | `common-utils.js` L233-262 | 응답 본문의 `errors[]`를 무시하고 `message`만 모달로 표시 |

### 1.2 수정 방안

#### ① `ApiResponse.java` — `errors` 필드 + 오버로드 팩토리 추가

**기존 시그니처를 유지**하면서 `errors`를 추가한 오버로드를 제공한다.

```java
// 신규 추가: 필드 에러 목록을 담는 내부 record
public record FieldError(String field, String message) {}

// 기존 error()는 그대로 유지 (호환성)
public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null, null);
}

// 신규 추가: errors[] 포함
public static <T> ApiResponse<T> error(int code, String message, List<FieldError> errors) {
    return new ApiResponse<>(code, message, null, errors);
}
```

**전체 응답 구조 (JSON):**
```json
{
  "timestamp": "2026-06-24T15:00:00",
  "code": 400,
  "message": "입력값 검증 실패",
  "data": null,
  "errors": [
    { "field": "empId", "message": "사원번호는 필수입니다" },
    { "field": "receiverNo", "message": "올바른 전화번호 형식이 아닙니다" }
  ]
}
```

#### ② `GlobalExceptionHandler.java` — 전체 필드 에러 반환

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
protected Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                       HttpServletRequest request) {
    BindingResult bindingResult = e.getBindingResult();

    // 모든 필드 에러 추출 (첫 번째만이 아닌)
    List<ApiResponse.FieldError> errors = bindingResult.getFieldErrors().stream()
        .map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
        .toList();

    String firstMessage = errors.isEmpty()
        ? ErrorCode.INVALID_INPUT_VALUE.getMessage()
        : errors.get(0).message();
    log.warn("입력값 검증 실패: {}건", errors.size());

    // JSON 요청인 경우 errors[] 포함 반환
    if (!isHtmlRequest(request)) {
        return new ResponseEntity<>(
            ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE.getStatus().value(),
                              ErrorCode.INVALID_INPUT_VALUE.getMessage(), errors),
            ErrorCode.INVALID_INPUT_VALUE.getStatus());
    }
    // HTML 요청은 기존 로직 유지 (에러 페이지)
    return respond(ErrorCode.INVALID_INPUT_VALUE.getStatus(), firstMessage, request);
}
```

#### ③ `common-utils.js` — 모달 큐(Queue) 구조 도입

현재 `_showCustomModal`이 호출되면 즉시 같은 DOM을 덮어쓴다. 이를 **FIFO 큐**로 변경한다.

```javascript
// ── 신규: 모달 메시지 큐 ──
const _modalQueue = [];
let _modalActive = false;

const _processModalQueue = () => {
    if (_modalActive || _modalQueue.length === 0) return;
    const { type, msg, title, onConfirm, onCancel } = _modalQueue.shift();
    _modalActive = true;
    _renderModal(type, msg, title, onConfirm, onCancel);
};

// 기존 _showCustomModal의 시그니처는 그대로 유지 (외부 호출부 수정 불필요)
const _showCustomModal = (type, msg, title, onConfirm, onCancel) => {
    _modalQueue.push({ type, msg, title, onConfirm, onCancel });
    _processModalQueue();
};

// 모달이 닫힘을 감지했을 때 다음 큐 처리
const _onModalClosed = () => {
    _modalActive = false;
    _processModalQueue();
};

// 기존 _showCustomModal 내부 렌더링 로직을 _renderModal로 이동.
// 확인/취소 버튼 클릭 → hideModalElement() → _onModalClosed() 순서로 호출.
```

#### ④ `common-utils.js` — axios 인터셉터 `errors[]` 인식

```javascript
// HTTP 에러(4xx, 5xx) 처리
error => {
    hideSpinner();
    const body = error.response?.data;

    if (body?.errors && Array.isArray(body.errors) && body.errors.length > 0) {
        // 필드 에러를 개행 목록으로 조합하여 한 번에 표시
        const errorList = body.errors
            .map(e => `• ${e.field}: ${e.message}`)
            .join('\n');
        _showCustomModal('alert', errorList, body.message || '입력값 오류');
    } else if (body?.message) {
        _showCustomModal('alert', body.message, '오류');
    } else {
        _showCustomModal('alert', '서버와 통신 중 알 수 없는 오류가 발생했습니다.', '시스템 오류');
    }
    return Promise.reject(error);
}
```

### 1.3 영향 범위

| 파일 | 변경 유형 | 비고 |
|------|-----------|------|
| `ApiResponse.java` | 필드 + 팩토리 추가 | 기존 `error(code, msg)` 유지, 새 오버로드 추가. **하위 호환** |
| `GlobalExceptionHandler.java` | `handleMethodArgumentNotValidException` 수정 | JSON 요청만 `errors[]` 포함. HTML은 기존 유지 |
| `common-utils.js` | 모달 큐 도입 + 인터셉터 수정 | 기존 `_showCustomModal` 호출부는 API 동일 → 개별 JS 수정 불필요 |

---

## 2. 웹 접근성 A11Y (3.3)

### 2.1 현재 문제

| 문제 | 위치 | 설명 |
|------|------|------|
| `:focus-visible` 부재 | 전역 CSS | 키보드 탭 이동 시 포커스 위치가 시각적으로 구분되지 않음 |
| 아이콘 버튼 `aria-label` 부재 | `common-utils.js` 모달 버튼, scaffold 생성 버튼 | `<i data-lucide="search">`만 있고 `<i>`에 `aria-hidden` 누락 |
| skip navigation link 부재 | `defaultLayout.html` | 키보드 사용자가 매번 헤더/사이드바를 탭으로 건너뛰어야 함 |
| 검색폼 라벨 의미 부족 | `HtmlTemplate.java` L59-60 | 라벨이 변수명(`empId`) 그대로 출력됨. 스크린리더가 의미 전달 못 함 |

### 2.2 수정 방안

#### ① 전역 `:focus-visible` 스타일 추가

`admin-ui-bridge.css` 최상단에 추가:

```css
/* 키보드 포커스 접근성 — 마우스 클릭과 키보드 탭 구분 */
*:focus-visible {
    outline: 3px solid var(--sms-focus-ring, #4db8ff);
    outline-offset: 2px;
    border-radius: 3px;
}

/* 마우스 클릭 시에는 아웃라인 제거 (키보드만 표시) */
*:focus:not(:focus-visible) {
    outline: none;
}
```

#### ② 아이콘 버튼 ARIA 보완

**패턴 규칙 (Scaffold + 공통):**
- 아이콘만 있는 버튼: `<button aria-label="조회">` + `<i data-lucide="search" aria-hidden="true"></i>`
- 아이콘 + 텍스트 버튼: `<i data-lucide="search" aria-hidden="true"></i><span>조회</span>`

**`common-utils.js` 모달 버튼 수정 (예시):**
```html
<!-- before -->
<button class="btn btn-primary" id="custom-modal-btn-confirm">
    <i data-lucide="check"></i><span>확인</span>
</button>

<!-- after -->
<button class="btn btn-primary" id="custom-modal-btn-confirm">
    <i data-lucide="check" aria-hidden="true"></i><span>확인</span>
</button>
```

#### ③ Skip Navigation Link 추가

`defaultLayout.html` `<body>` 직후에 추가:

```html
<body class="admin-shell">
    <!-- 키보드 사용자를 위한 본문 바로가기 -->
    <a href="#main-content" class="skip-link">본문으로 바로가기</a>
    ...
    <div class="content-area">
        <main id="main-content" layout:fragment="content"></main>
    </div>
```

```css
/* admin-layout.css 또는 admin-ui-bridge.css */
.skip-link {
    position: absolute;
    top: -100px;          /* 평소에는 화면 백 바깥에 숨김 */
    left: 0;
    z-index: 10000;
    padding: 8px 16px;
    background: var(--sms-primary, #0d6efd);
    color: #fff;
    font-weight: bold;
    text-decoration: none;
}
.skip-link:focus {
    top: 0;               /* 포커스 받으면 화면 상단으로 진입 */
}
```

#### ④ Scaffold 검색폼 라벨 의미화

현재 `HtmlTemplate.java`는 라벨을 변수명 그대로 출력:

```java
// before (HtmlTemplate.java L59-60)
sb.append("\" class=\"col-form-label fw-bold\">").append(param.name()).append("</label>")
// → <label>empId</label>
```

수정 방안 — **라벨 사전(dictionary) + 자동 한글화**:

```java
// HtmlTemplate에 라벨 매핑 추가
private static final Map<String, String> LABEL_MAP = Map.ofEntries(
    Map.entry("empId", "사원번호"),
    Map.entry("empNm", "사원명"),
    Map.entry("depId", "부서코드"),
    Map.entry("depNm", "부서명"),
    Map.entry("receiverNo", "수신번호"),
    Map.entry("sendType", "발송구분"),
    Map.entry("sendStatus", "발송상태"),
    Map.entry("startDate", "시작일"),
    Map.entry("endDate", "종료일"),
    Map.entry("startDateTime", "시작일시"),
    Map.entry("endDateTime", "종료일시")
);

private static String resolveLabel(String paramName) {
    String label = LABEL_MAP.get(paramName);
    if (label != null) return label;
    // 사전에 없으면 camelCase를 공백 구분으로 (empId → emp Id)
    return paramName.replaceAll("([A-Z])", " $1");
}
```

적용:
```java
// after
sb.append("\" class=\"col-form-label fw-bold\">").append(resolveLabel(param.name())).append("</label>")
// → <label>사원번호</label>
```

### 2.3 영향 범위

| 파일 | 변경 유형 | 비고 |
|------|-----------|------|
| `admin-ui-bridge.css` | `:focus-visible` + skip-link 스타일 추가 | 전역 적용 |
| `defaultLayout.html` | skip-link + `<main id="main-content">` 추가 | |
| `common-utils.js` | 모달 버튼 `<i aria-hidden="true">` 추가 | |
| `HtmlTemplate.java` | `resolveLabel()` 도입 | scaffold 신규 화면에 적용. 기존 화면은 수동 적용 |

---

## 3. 뷰포트 / 반응형 (3.4)

### 3.1 현재 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| `<meta name="viewport">` | ✅ 충족 | `defaultLayout.html` L6: `width=device-width, initial-scale=1.0` |
| 미디어 쿼리 | ⚠️ 최소 | CoreUI 기본 반응형에 의존. 검색 카드 영역이 좁은 화면에서 오버플로우 가능 |
| Scaffold 검색폼 | ⚠️ `col-auto`만 사용 | 화면 축소 시 검색 입력 필드가 한 줄에 갇혀 오버플로우 |

> **참고**: v3는 폐쇄망 내부 데스크톱 환경이 주 타겟이므로 모바일 최적화는 과잉이다. 하지만 창 크기 축소(태블릿 폭) 시 레이아웃이 깨지지 않는 정도의 보완은 가치가 있다.

### 3.2 수정 방안

#### ① Scaffold 검색폼 `flex-wrap` 명시

현재 `HtmlTemplate.java`:
```html
<div class="row align-items-center g-3">
```

수정:
```html
<div class="row align-items-center g-3 flex-wrap">
```

CoreUI/Bootstrap 5의 `row`는 기본적으로 `flex-wrap: wrap`이지만, 커스텀 CSS 오버라이드가 있을 수 있으므로 명시적으로 보장한다.

#### ② 검색 입력 필드 최소 너비 보장

```css
/* admin-ui-bridge.css에 추가 */
.scaffold-search-control {
    min-width: 140px;
}

/* 좁은 화면(태블릿 이하)에서 검색 카드 패딩 축소 */
@media (max-width: 768px) {
    .scaffold-search-card .card-body {
        padding: 0.75rem;
    }
    .scaffold-search-card .col-auto {
        margin-bottom: 0.5rem;
    }
}
```

#### ③ 그리드 컨테이너 반응형 보완

TUI Grid 영역(toast-grid fragment)이 창 크기 변화에 대응하도록, `autoResize`가 이미 설정되어 있는지 확인하고, 미설정 시 scaffold에서 옵션 추가:

```javascript
// JsTemplate.java에서 TuiPageBuilder 옵션에 autoResize 보장
// (이미 TuiPageBuilder 기본값에 포함되어 있으면 생략)
```

### 3.3 영향 범위

| 파일 | 변경 유형 | 비고 |
|------|-----------|------|
| `admin-ui-bridge.css` | 미디어 쿼리 + 최소 너비 추가 | 전역 적용 |
| `HtmlTemplate.java` | `flex-wrap` 클래스 추가 | scaffold 신규 화면 |

---

## 4. Design Token CSS + Status 아이콘화 (3.5)

### 4.1 현재 문제

| 문제 | 위치 | 설명 |
|------|------|------|
| 인라인 하드코딩 색상 | `header.html`, `error.html` 등 | `color: #0d6efd`, `background: #f8f9fa` 등이 직접 하드코딩 |
| Design Token 부재 | 전역 CSS | `--sms-primary` 같은 프로젝트 네임스페이스 토큰이 정의되지 않음 |
| Status 값 텍스트 표시 | `JsTemplate.java` grid 컬럼 | 발송상태(성공/실패/대기)가 텍스트로만 표시되어 시인성 낮음 |

### 4.2 수정 방안

#### ① Design Token 정의

`admin-ui-bridge.css` (또는 신규 `design-tokens.css`) 최상단에 `:root` 변수 정의:

```css
:root {
    /* ── Brand Colors ── */
    --sms-primary:        #0d6efd;
    --sms-primary-dark:   #0b5ed7;
    --sms-secondary:      #6c757d;
    --sms-success:        #198754;
    --sms-danger:         #dc3545;
    --sms-warning:        #ffc107;
    --sms-info:           #0dcaf0;

    /* ── Status Colors (발송 상태 배지용) ── */
    --sms-status-success: #198754;  /* 발송성공 */
    --sms-status-fail:    #dc3545;  /* 발송실패 */
    --sms-status-pending: #6c757d;  /* 대기 */
    --sms-status-cancel:  #ffc107;  /* 취소 */

    /* ── Neutral / Surface ── */
    --sms-bg-light:       #f8f9fa;
    --sms-bg-white:       #ffffff;
    --sms-text-primary:   #212529;
    --sms-text-muted:     #6c757d;
    --sms-border:         #dee2e6;

    /* ── Focus Ring (A11Y) ── */
    --sms-focus-ring:     #4db8ff;
}
```

#### ② 인라인 스타일 → 토큰 참조로 이관

`header.html`, `error.html` 등에서 하드코딩된 색상을 토큰으로 교체:

```css
/* before (error.html 인라인 또는 CSS) */
.error-page-title { color: #dc3545; }

/* after */
.error-page-title { color: var(--sms-danger); }
```

#### ③ Status 아이콘화 — 공통 렌더러

**`common-utils.js`에 공통 렌더 함수 추가:**

```javascript
/**
 * 상태값을 아이콘 + 색상 배지로 렌더링하는 공통 포매터.
 * TUI Grid의 formatter: ({ value }) => CommonUtils.fmt.status(value) 형태로 사용.
 *
 * @param {string} value - 원본 상태값 (예: "SUCCESS", "FAIL", "01", "Y")
 * @param {object} [mapping] - 선택적 커스텀 매핑 (생략 시 기본 매핑 사용)
 * @returns {string} HTML 문자열
 */
const STATUS_BADGE_DEFAULT = {
    // 발송 결과 (코드/한글/영문 모두 매핑)
    'SUCCESS': { icon: 'circle-check',     cls: 'badge-success', label: '성공' },
    'FAIL':    { icon: 'circle-x',         cls: 'badge-danger',  label: '실패' },
    'FAILED':  { icon: 'circle-x',         cls: 'badge-danger',  label: '실패' },
    'PENDING': { icon: 'clock',            cls: 'badge-secondary', label: '대기' },
    'CANCEL':  { icon: 'ban',              cls: 'badge-warning', label: '취소' },
    'CANCELED':{ icon: 'ban',              cls: 'badge-warning', label: '취소' },
    // Y/N
    'Y':       { icon: 'check',            cls: 'badge-success', label: 'Y' },
    'N':       { icon: 'x',                cls: 'badge-secondary', label: 'N' },
    // 공통코드 01/02/03 매핑 (발송구분 예시)
    '01':      { icon: 'message-circle',   cls: 'badge-info',    label: 'SMS' },
    '02':      { icon: 'mail',             cls: 'badge-info',    label: 'LMS' },
    '03':      { icon: 'image',            cls: 'badge-info',    label: 'MMS' }
};

const renderStatusBadge = (value, mapping) => {
    if (value == null || value === '') return '';
    const lookup = mapping || STATUS_BADGE_DEFAULT;
    const key = String(value).toUpperCase();
    const config = lookup[key] || lookup[String(value)];

    if (!config) {
        // 매핑이 없으면 원본 값을 그대로 텍스트로 반환
        return String(value);
    }

    return `<span class="status-badge ${config.cls}">
                <i data-lucide="${config.icon}" aria-hidden="true"></i>
                <span>${config.label}</span>
            </span>`;
};

// CommonUtils.fmt 에 추가
fmt.status = renderStatusBadge;
```

**CSS (admin-ui-bridge.css):**
```css
.status-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 0.8rem;
    font-weight: 600;
    white-space: nowrap;
}
.status-badge i { width: 14px; height: 14px; }
.status-badge.badge-success  { background: var(--sms-status-success); color: #fff; }
.status-badge.badge-danger   { background: var(--sms-status-fail);    color: #fff; }
.status-badge.badge-secondary{ background: var(--sms-status-pending); color: #fff; }
.status-badge.badge-warning  { background: var(--sms-status-cancel);  color: #212529; }
.status-badge.badge-info     { background: var(--sms-info);           color: #fff; }
```

#### ④ Scaffold `JsTemplate` — status 컬럼 자동 감지

`JsTemplate.java`의 `appendFormatter`에서 컬럼명이 status/sendStat/div 등이면 자동으로 status 렌더러를 적용:

```java
private static void appendFormatter(StringBuilder sb, ScaffoldModel.ColumnConfig column) {
    // 1. 기존 mask/date 포매터 (우선순위 유지)
    if (column.hasMask()) { ... }

    // 2. 신규: 상태 컬럼 자동 감지
    if (isStatusColumn(column.fieldName())) {
        sb.append(", formatter: ({ value }) => CommonUtils.fmt.status(value)");
        return;
    }

    // 3. 기존 date 포매터
    ...
}

private static boolean isStatusColumn(String fieldName) {
    String upper = fieldName.toUpperCase();
    return upper.contains("STATUS")
        || upper.equals("SENDSTAT")
        || upper.equals("SEND_TYPE")
        || upper.equals("CANCEL")
        || upper.equals("ACT_YN")
        || upper.equals("USE_YN");
}
```

> **주의**: 자동 감지는 "확실한" 컬럼명만 포함. 애매한 경우 scaffold 사용자가 수동으로 옵션에서 지정할 수 있도록 향후 확장.

### 4.3 영향 범위

| 파일 | 변경 유형 | 비고 |
|------|-----------|------|
| `admin-ui-bridge.css` | `:root` 토큰 정의 + `.status-badge` 스타일 | 전역 |
| `common-utils.js` | `renderStatusBadge()` + `fmt.status` 추가 | |
| `JsTemplate.java` | `isStatusColumn()` + formatter 적용 | scaffold 신규 화면 |
| `header.html`, `error.html` 등 | 하드코딩 색상 → `var(--sms-*)` 교체 | 기존 템플릿 |

---

## 5. Scaffold 템플릿 반영 계획

모든 신규 화면이 Scaffold로 생성되므로, 다음 템플릿 파일에 개선사항을 반영한다.

### 5.1 `HtmlTemplate.java` 반영 항목

| 항목 | 반영 내용 |
|------|-----------|
| 라벨 의미화 | `resolveLabel(param.name())` 적용 → 한글 라벨 자동 출력 |
| `flex-wrap` | 검색 카드 `row`에 `flex-wrap` 클래스 추가 |
| 아이콘 `aria-hidden` | 생성 버튼/조회/초기화 버튼의 `<i>`에 `aria-hidden="true"` 추가 |

### 5.2 `JsTemplate.java` 반영 항목

| 항목 | 반영 내용 |
|------|-----------|
| Status 아이콘화 | `isStatusColumn()` 감지 시 `CommonUtils.fmt.status` formatter 자동 적용 |

### 5.3 기존 생성 파일에의 적용

이미 Scaffold로 생성된 기존 파일(`history.html`, `history.js` 등)은 수동으로 동일하게 적용한다:

- `history.html`: 라벨 한글화, `aria-hidden`, skip-link (layout에서 전역 적용되므로 별도 수정 불필요)
- `history.js`: status 컬럼에 `CommonUtils.fmt.status` formatter 적용

---

## 6. 구현 순서 및 검증

### 6.1 구현 순서

```
1단계: 공통 기반 작업 (한 번에 전체 적용)
  ├─ ApiResponse.java (errors 필드 추가)
  ├─ GlobalExceptionHandler.java (필드 에러 전체 반환)
  ├─ common-utils.js (모달 큐 + errors[] 인식 + fmt.status)
  ├─ admin-ui-bridge.css (design token + :focus-visible + status-badge)
  └─ defaultLayout.html (skip-link)

2단계: Scaffold 템플릿 반영
  ├─ HtmlTemplate.java (resolveLabel, flex-wrap, aria-hidden)
  └─ JsTemplate.java (isStatusColumn, status formatter)

3단계: 기존 파일 적용
  ├─ history.html, history.js 등
  └─ header.html, error.html (토큰 이관)

4단계: 검증
  ├─ mvn test
  ├─ mvn -DskipTests package
  └─ local 기동 후 화면 확인
```

### 6.2 완료 기준 (AGENTS.md 준수)

- [ ] `mvn test` 성공
- [ ] `mvn -DskipTests package` 성공
- [ ] local/dev/prod profile별 설정 검토 (본 작업은 profile 무관)
- [ ] 관련 문서 갱신 (본 명세서 + `design-pattern-review.md` 조치 이력)

### 6.3 리스크

| 리스크 | 대응 |
|--------|------|
| `ApiResponse` 구조 변경 시 기존 응답 호환성 | 기존 `error(code, msg)` 시그니처 유지. `errors`는 null 허용 |
| 모달 큐 도입 시 기존 호출부 영향 | `_showCustomModal` 외부 인터페이스 불변. 내부만 큐로 변경 |
| Status 자동 감지 오작동 | 명확한 컬럼명만 매핑. 매핑 없으면 원본 텍스트 반환 |
| 라벨 사전에 없는 변수명 | camelCase 공백 변환 fallback 제공 |