# 정적 샘플 활용 매뉴얼 — 비표준 UI 참고용

> **대상 독자**: Query Scaffold로 기본 화면을 생성한 뒤, 생성기에 없는 화면 고유 UI를 `static/samples/`에서 참고하려는 개발자
> **문서 지위**: 표준 화면 생성 절차가 아니다. 신규 업무 화면은 먼저 Query Scaffold로 생성하며, 샘플 전체를 복사해 화면을 시작하지 않는다.
> **관련 문서**: 화면 규약 `docs/base/screen-convention.md`, 화면 생성 절차 `docs/base/screen-generation-guide.md`, 공통 JS 매뉴얼 `docs/menual/common-js_manual.md`, CSS/디자인 매뉴얼 `docs/menual/css-design_manual.md`

---

## 목차

1. [개요 — 샘플이 존재하는 이유와 활용 범위](#1-개요--샘플이-존재하는-이유와-활용-범위)
2. [현재 샘플 목록](#2-현재-샘플-목록)
3. [전환 6단계](#3-전환-6단계)
4. [복사하지 말 것 (defaultLayout이 담당)](#4-복사하지-말-것-defaultlayout이-담당)
5. [Controller 설정과 메뉴 등록](#5-controller-설정과-메뉴-등록)
6. [전환 후 체크리스트](#6-전환-후-체크리스트)
7. [prod 샘플 차단 (SamplesBlockInterceptor)](#7-prod-샘플-차단-samplesblockinterceptor)

---

## 1. 개요 — 샘플이 존재하는 이유와 활용 범위

### 1.1 샘플이란

`src/main/resources/static/samples/`에 있는 정적 HTML 파일은 **상세폼(등록/수정) 화면의 프로토타입**이다. 서버 없이 브라우저에서 파일을 직접 열어도(`file://`) 동작하고, 서버 기동 후 `/samples/<파일명>.html`로도 확인할 수 있다.

샘플의 목적은 다음 두 가지다.

- **UI 선행 검증**: 서버·Thymeleaf·DB 없이 폼 레이아웃, 입력 컨트롤, 미리보기 패널 같은 화면 구조를 브라우저에서 바로 확인한다.
- **UI 패턴 참고**: Scaffold가 생성하지 않는 미리보기 패널, 복합 입력 배치 등 필요한 부분만 생성된 화면에 선택적으로 적용한다.

표준 목록/엑셀/CRUD 화면의 출발점은 `scaffold-templates/**/*.tpl`이다. 이 문서는 Scaffold로 만들 수 없는 화면 고유 정보 구조를 보조할 때만 사용한다.

### 1.2 샘플과 실제 화면의 차이

| 항목 | 정적 샘플 (`static/samples/`) | 실제 화면 (`templates/<도메인>/`) |
|---|---|---|
| CSS/JS 경로 | 상대 경로 (`../lib/...`, `../css/...`) | Thymeleaf URL (`th:href="@{/css/...}"`) — defaultLayout이 로드 |
| 공통 CSS/JS | 샘플이 직접 `<link>`, `<script>`로 로드 | `defaultLayout.html`이 일괄 로드. 화면에서 중복 로드 금지 |
| `SESSION_INFO` / `PAGE_AUTH` | 하드코딩 스텁 (SAMPLE 값) | defaultLayout이 서버 세션 값으로 주입 |
| 레이아웃 | 사이드바/헤더 없음 (`.content-area` 여백 제거) | defaultLayout이 사이드바/헤더 포함 전체 셸 제공 |
| 데이터 바인딩 | 하드코딩 `value="..."` | `th:value`, `th:each` 서버 바인딩 |
| prod 접근 | `SamplesBlockInterceptor`가 404 차단 | 메뉴 권한 Interceptor 통제 |

### 1.3 권장 활용 흐름

```text
1. Query Scaffold로 LIST/EXCEL/CRUD 기본 화면 생성
   ↓
2. 생성기에 없는 화면 고유 UI 요구사항 식별
   ↓
3. 정적 샘플을 브라우저에서 확인
   ↓
4. 필요한 DOM·화면 전용 CSS·동작만 개발자 소유 화면에 선택 적용
   ↓
5. 공통 자산 중복 여부와 화면 동작 검증
```

아래의 기존 전환 예시는 Scaffold로 처리하기 어려운 특수 화면을 수동 구현할 때의 참고 절차다. 일반 업무 화면 생성에는 사용하지 않는다.

---

## 2. 현재 샘플 목록

`src/main/resources/static/samples/index.html`이 샘플 목록의 기준이다. 인덱스를 제외한 현재 샘플 12개는 다음과 같다.

| 분류 | 파일 | 참고할 패턴 |
|---|---|---|
| 목록 | `list-basic.html` | 검색조건 + 단일 TUI Grid + 페이징 + 클라이언트 Excel |
| 목록 CRUD | `list-crud.html` | 행 클릭 + `admin-form-detail` CRUD 모달 |
| 목록 모달 | `list-basic-modal.html` | 모달 내부 독립 검색조건·그리드·페이징 |
| 목록 폼 모달 | `list-form-modal.html` | `modal-lg` 등록·수정 폼과 목록 결합 |
| 다중 Grid | `multi-grid-2.html` | 마스터·디테일 `TuiPageBuilder` 2개 |
| 다중 Grid | `multi-grid-3.html` | 상태별 독립 `TuiPageBuilder` 3개 |
| 상세 폼 | `message-edit.html` | 단일/분할 상세행, DatePicker, radio, byte counter |
| 상세 폼 | `campaign-register.html` | 좌측 입력 + 우측 메시지 미리보기 합성 |
| 읽기 전용 | `detail-view.html` | 입력 컨트롤 없는 상세 조회 행 |
| 트리 | `tree-detail.html` | 좌측 트리 + 우측 상세 폼 |
| 모달 | `modal-patterns.html` | CoreUI 업무 모달과 SweetAlert2 확인 팝업 역할 분리 |
| 에디터 | `editor-popup.html` | 화면 단위 TOAST UI Editor 로드와 접근성 보정 |

일반 LIST·EXCEL·CRUD 화면은 Scaffold가 표준 생성 경로다. 위 샘플은 Scaffold에 없는 화면 조합이나 레이아웃을 생성 후 개발자가 추가할 때 참고한다. 특히 `list-crud.html`은 CRUD 모달의 시각 기준이고, 실제 생성 계약은 `scaffold-templates/crud/`와 Golden 테스트가 담당한다.

### 2.1 대표 수동 전환 예제

이 문서의 3장 이후 전체 전환 예제는 `message-edit.html`을 사용한다. `campaign-register.html`은 분할 레이아웃과 미리보기 패널을 추가하는 변형 예제로 함께 설명한다. 다른 샘플도 동일한 원칙으로 전환하되 화면별 CSS와 JS만 선택적으로 가져온다.

### 2.2 샘플의 공통 구조

개별 샘플은 공통 자산을 상대 경로로 로드하고 정적 실행용 `SESSION_INFO`/`PAGE_AUTH` 스텁을 둔다.

```text
<!DOCTYPE html>
<!-- 전환 절차 주석 (6단계) -->
<html lang="ko">
<head>
    <!-- meta, title -->
    <!-- lib CSS/JS (상대 경로 ../lib/) -->
    <!-- vendor/coreui CSS -->
    <!-- 공통 CSS (../css/admin-*.css) -->
    <!-- 공통 JS (../js/common/*.js) -->
    <!-- SESSION_INFO / PAGE_AUTH 스텁 -->
    <!-- <style> 화면 전용 CSS -->
</head>
<body class="admin-shell">
<div class="content-area">
    <div class="container-fluid mb-4 p-0">
        <main id="main-content">
            <!-- 화면 본문: content-header + 화면별 콘텐츠 -->
        </main>
    </div>
</div>
<!-- coreui.bundle.min.js -->
<!-- 페이지 JS (../js/<도메인>/<화면>.js) -->
</body>
</html>
```

전환 시 복사하는 범위는 `<main>...</main>` 내부와 `<style>`의 화면 전용 CSS, 하단 페이지 JS뿐이다. 라이브러리·공통 CSS/JS·정적 스텁·body shell은 `defaultLayout.html`이 담당하므로 복사하지 않는다.

---

## 3. 전환 6단계

샘플 파일 상단 주석에 적힌 6단계를 실제 코드 before/after로 풀어서 설명한다.

### 3.1 단계 1 — `<main>` 복사 + 레이아웃 선언 추가

샘플의 `<main>...</main>` 내부를 `templates/<도메인>/<화면>.html`로 복사하고, Thymeleaf Layout Dialect 네임스페이스와 `layout:decorate`를 선언한다.

**Before (샘플)**:

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>메시지 관리 — 상세폼 샘플</title>
    <!-- lib, css, js 직접 로드 ... -->
</head>
<body class="admin-shell">
<div class="content-area">
    <div class="container-fluid mb-4 p-0">
        <main id="main-content">
            <div class="content-header">
                <h2>메시지 관리 <small class="text-body-sm">(시스템관리 &gt; 메시지관리 &gt; 수정)</small></h2>
            </div>
            <form id="message-edit-form" autocomplete="off" novalidate>
                <!-- 폼 필드 ... -->
            </form>
        </main>
    </div>
</div>
</body>
</html>
```

**After (Thymeleaf 템플릿)**:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{defaultLayout}">
<head>
    <title>메시지 관리</title>
</head>
<body>
<main layout:fragment="content">

    <div class="content-header">
        <h2>메시지 관리 <small class="text-body-sm">(시스템관리 &gt; 메시지관리 &gt; 수정)</small></h2>
    </div>
    <form id="message-edit-form" autocomplete="off" novalidate>
        <!-- 폼 필드 ... -->
    </form>

</main>
</body>
</html>
```

**핵심 변경 사항**:

- `<html>` 태그에 `xmlns:th`, `xmlns:layout`, `layout:decorate="~{defaultLayout}"` 3개를 추가한다.
- `<head>`에는 `<title>`만 남긴다. meta, CSS, JS 로드는 전부 defaultLayout이 담당한다.
- `<body>`의 `class="admin-shell"`, `.content-area`, `.container-fluid` 래퍼는 제거한다. defaultLayout이 이미 이 구조를 제공한다.
- `<main>` 바로 아래 내용(content-header + form)만 복사한다.

### 3.2 단계 2 — `<main>`에 `layout:fragment="content"` 지정

샘플의 `<main id="main-content">`를 `<main layout:fragment="content">`로 바꾼다.

**Before**:

```html
<main id="main-content">
```

**After**:

```html
<main layout:fragment="content">
```

`id="main-content"`는 defaultLayout의 `<main layout:fragment="content" id="main-content" tabindex="-1">`에 이미 있으므로 화면 템플릿에서는 `id`를 쓰지 않는다. `layout:fragment="content"`를 선언하면 defaultLayout의 해당 영역에 화면 내용이 삽입된다.

### 3.3 단계 3 — 화면 전용 `<style>`을 `layout:fragment="css"`로 이동

샘플의 `<style>` 블록에서 **화면 전용 스타일만** `<th:block layout:fragment="css">`로 옮긴다. 공통 클래스(`form-detail-row`, `form-detail-label`, `form-detail-control`, `form-detail-required`, `form-detail-counter`)는 `admin-form-detail.css`에 이미 정의되어 있으므로 복사하지 않는다.

**Before (샘플 `<style>`)**:

```html
<style>
    /* 화면 전용 스타일 — 실제 화면 전환 시 layout:fragment="css" 로 이동 */
    .message-edit-date-field {
        width: min(100%, 220px);
    }
    .message-edit-unit-control {
        width: min(100%, 180px);
    }
    .message-edit-ad-warning {
        color: var(--sms-danger);
        font-size: var(--sms-fs-body-sm);
        font-weight: var(--sms-fw-semibold);
        word-break: keep-all;
    }
    .message-edit-readonly {
        background: var(--sms-bg-muted) !important;
        color: var(--sms-text-muted);
    }
    /* 샘플은 셸(사이드바/헤더)이 없어 content-area 좌측 여백을 제거 */
    .content-area {
        margin-left: 0 !important;
    }
</style>
```

**After (템플릿 `<head>`)**:

```html
<head>
    <title>메시지 관리</title>
    <th:block layout:fragment="css">
        <style>
            .message-edit-date-field {
                width: min(100%, 220px);
            }
            .message-edit-unit-control {
                width: min(100%, 180px);
            }
            .message-edit-ad-warning {
                color: var(--sms-danger);
                font-size: var(--sms-fs-body-sm);
                font-weight: var(--sms-fw-semibold);
                word-break: keep-all;
            }
            .message-edit-readonly {
                background: var(--sms-bg-muted) !important;
                color: var(--sms-text-muted);
            }
        </style>
    </th:block>
</head>
```

**제외할 스타일**:

| 제외 대상 | 이유 |
|---|---|
| `.content-area { margin-left: 0 !important; }` | 샘플 전용 보정. defaultLayout은 사이드바가 있으므로 이 규칙이 필요 없다 |
| `form-detail-*` 클래스 일체 | `admin-form-detail.css`에 이미 정의됨 |
| `.card`, `.btn`, `.form-control` 등 CoreUI 클래스 | CoreUI CSS가 담당 |

`campaign-register.html`의 경우 `.campaign-preview-*` 클래스군(약 130줄)이 화면 전용이므로 전부 옮긴다. `.content-area` 보정은 동일하게 제외한다.

### 3.4 단계 4 — 하드코딩 값을 Thymeleaf 바인딩으로 교체

샘플의 하드코딩 `value`, `selected`, `checked`를 서버 데이터 바인딩으로 교체한다.

**Before (샘플 — 하드코딩)**:

```html
<input type="text" class="form-control" id="messageTitle" name="messageTitle"
       value="온라인 즉시 발급 카드정보"
       maxlength="100" placeholder="메시지 제목을 입력하세요"
       data-validate="required|maxlength:100" required>

<select class="form-select" id="onlineBatchType" name="onlineBatchType"
        data-validate="required" required>
    <option value="">구분을 선택하세요</option>
    <option value="ONLINE" selected>온라인</option>
    <option value="BATCH">배치</option>
</select>

<input class="form-check-input" type="radio" id="useNo"
       name="useYn" value="N" checked required>
```

**After (Thymeleaf 바인딩)**:

```html
<input type="text" class="form-control" id="messageTitle" name="messageTitle"
       th:value="${message != null ? message.messageTitle : ''}"
       maxlength="100" placeholder="메시지 제목을 입력하세요"
       data-validate="required|maxlength:100" required>

<select class="form-select" id="onlineBatchType" name="onlineBatchType"
        data-validate="required" required>
    <option value="">구분을 선택하세요</option>
    <option value="ONLINE" th:selected="${message != null and message.onlineBatchType == 'ONLINE'}">온라인</option>
    <option value="BATCH" th:selected="${message != null and message.onlineBatchType == 'BATCH'}">배치</option>
</select>

<input class="form-check-input" type="radio" id="useNo"
       name="useYn" value="N"
       th:checked="${message != null and message.useYn == 'N'}" required>
```

**동적 목록(`th:each`) 예시** — 발송대상 select처럼 서버에서 목록을 받는 경우:

```html
<!-- Before: 하드코딩 option -->
<select class="form-select" id="targetGroupId" name="targetGroupId">
    <option value="">발송 대상을 선택하세요</option>
    <option value="TG-01">VIP 고객 (1,200명)</option>
    <option value="TG-02">신규 발급 고객 (3,500명)</option>
</select>

<!-- After: th:each 바인딩 -->
<select class="form-select" id="targetGroupId" name="targetGroupId">
    <option value="">발송 대상을 선택하세요</option>
    <option th:each="group : ${targetGroups}"
            th:value="${group.groupId}"
            th:text="${group.groupNm + ' (' + #numbers.formatInteger(group.memberCount, 0, 'COMMA') + '명)'}"
            th:selected="${campaign != null and campaign.targetGroupId == group.groupId}">
    </option>
</select>
```

**바인딩 원칙**:

- 수정 화면: Controller가 Model에 담은 상세 VO를 `th:value="${vo.field}"`로 표시한다.
- 등록 화면: 빈 값으로 시작하므로 `th:value` 없이 빈 `value=""` 또는 생략한다.
- 공통코드 콤보(`data-code-type="dept"` 등): `th:each` 대신 JS `CommonUtils.initCombos()`가 `/api/common-code/{type}`에서 동적으로 채운다. 이 경우 `<option value="">선택</option>` 하나만 남긴다.
- `name` 속성은 `UpdateRequestDTO` 필드명과 일치시킨다 (`screen-convention.md` "name 일치 계약").

### 3.5 단계 5 — 페이지 스크립트를 `layout:fragment="script"`로 이동

샘플 하단의 페이지 JS를 `<th:block layout:fragment="script">` 안에 `th:src`로 로드한다.

**Before (샘플 하단)**:

```html
<script src="../vendor/coreui/js/coreui.bundle.min.js"></script>
<script src="../js/system/message-edit.js"></script>
```

**After (템플릿 `</body>` 직전)**:

```html
<th:block layout:fragment="script">
    <script th:src="@{/js/system/message-edit.js}"></script>
</th:block>
```

**변경 사항**:

- `coreui.bundle.min.js`는 defaultLayout이 이미 로드하므로 복사하지 않는다.
- 상대 경로(`../js/...`)를 Thymeleaf URL 표현(`th:src="@{/js/...}"`)으로 바꾼다.
- 페이지 JS 파일 자체(`static/js/system/message-edit.js`)는 수정하지 않는다. 이미 공통 유틸(`CommonUtils`, `FormBinder`, `FieldFormat` 등)을 전역 변수로 참조하므로 경로 변경 없이 동작한다.

**campaign-register.html의 경우**:

```html
<th:block layout:fragment="script">
    <script th:src="@{/js/sms/campaign-register.js}"></script>
</th:block>
```

### 3.6 단계 6 — 샘플 전용 스텁 제거

샘플의 `SESSION_INFO` / `PAGE_AUTH` 하드코딩 스텁은 제거한다. defaultLayout이 서버 세션 값으로 실제 값을 주입한다.

**Before (샘플 — 제거 대상)**:

```html
<script>
    /* 정적 샘플용 스텁 — 실제 화면에서는 defaultLayout.html 이 서버 값으로 주입한다. */
    const SESSION_INFO = { empId: 'SAMPLE', depId: 'SAMPLE', empNm: '샘플사용자', depNm: '샘플부서' };
    window.PAGE_AUTH = {
        read: true, create: true, update: true, delete: true,
        approve: false, cancel: false, download: false, maskView: false
    };
</script>
```

**After**: 이 블록 전체를 삭제한다. 아무것도 넣지 않는다.

defaultLayout이 주입하는 실제 값 (`defaultLayout.html` 52~71행):

```html
<script th:inline="javascript">
    /*<![CDATA[*/
    const SESSION_INFO = {
        empId:  /*[[${user != null ? user.empId : ''}]]*/ '',
        depId:  /*[[${user != null ? user.depId : ''}]]*/ '',
        empNm:  /*[[${user != null ? user.empNm : ''}]]*/ '',
        depNm:  /*[[${user != null ? user.depNm : ''}]]*/ ''
    };
    window.PAGE_AUTH = {
        read:     /*[[${pageAuth != null ? pageAuth.read : false}]]*/ false,
        create:   /*[[${pageAuth != null ? pageAuth.create : false}]]*/ false,
        update:   /*[[${pageAuth != null ? pageAuth.update : false}]]*/ false,
        delete:   /*[[${pageAuth != null ? pageAuth.delete : false}]]*/ false,
        approve:  /*[[${pageAuth != null ? pageAuth.approve : false}]]*/ false,
        cancel:   /*[[${pageAuth != null ? pageAuth.cancel : false}]]*/ false,
        download: /*[[${pageAuth != null ? pageAuth.download : false}]]*/ false,
        maskView: /*[[${pageAuth != null ? pageAuth.maskView : false}]]*/ false
    };
    /*]]>*/
</script>
```

`user`, `pageAuth` 모델은 `GlobalModelAdvice`가 공통 주입한다. 개별 Controller에서 중복 주입하지 않는다.

---

## 4. 복사하지 말 것 (defaultLayout이 담당)

샘플에서 아래 항목은 **절대 템플릿에 복사하지 않는다**. `defaultLayout.html`이 일괄 제공한다.

### 4.1 공통 CSS/JS 로드

```html
<!-- ❌ 복사 금지 — defaultLayout이 로드 -->
<link rel="stylesheet" href="../lib/tui-pagination.css" />
<link rel="stylesheet" href="../lib/tui-date-picker.css" />
<link rel="stylesheet" href="../lib/tui-grid.css" />
<script src="../lib/tui-pagination.min.js"></script>
<script src="../lib/xlsx.full.min.js"></script>
<script src="../lib/tui-date-picker.min.js"></script>
<script src="../lib/tui-grid.min.js"></script>
<script src="../lib/axios.min.js"></script>
<script src="../lib/dayjs.min.js"></script>
<script src="../lib/ko.js"></script>
<script src="../lib/lucide.js"></script>
<script src="../lib/imask.min.js"></script>
<script src="../lib/just-validate.min.js"></script>
<script>dayjs.locale('ko');</script>

<link rel="stylesheet" href="../vendor/coreui/css/coreui.min.css">
<link rel="stylesheet" href="../css/admin-common.css" />
<link rel="stylesheet" href="../css/admin-layout.css" />
<link rel="stylesheet" href="../css/admin-ui-bridge.css" />
<link rel="stylesheet" href="../css/admin-form-detail.css" />

<script src="../js/common/notify.js"></script>
<script src="../js/common/http-client.js"></script>
<script src="../js/common/modal-manager.js"></script>
<script src="../js/common/common-utils.js"></script>
<script src="../js/common/form-binder.js"></script>
<script src="../js/common/field-format.js"></script>
<script src="../js/common/tui-common.js"></script>
<script src="../js/common/tui-page-builder.js"></script>

<script src="../vendor/coreui/js/coreui.bundle.min.js"></script>
```

defaultLayout은 위 전부를 Thymeleaf URL(`th:href="@{/...}"`, `th:src="@{/...}"`)로 로드한다. 화면 템플릿에서 이 중 하나라도 중복 로드하면 이중 로딩으로 동작 오류가 발생할 수 있다.

### 4.2 SESSION_INFO / PAGE_AUTH 스텁

```html
<!-- ❌ 복사 금지 — defaultLayout이 서버 값으로 주입 -->
<script>
    const SESSION_INFO = { empId: 'SAMPLE', ... };
    window.PAGE_AUTH = { read: true, ... };
</script>
```

### 4.3 샘플 전용 보정 스타일

```css
/* ❌ 복사 금지 — 사이드바 없는 샘플 전용 보정 */
.content-area {
    margin-left: 0 !important;
}
```

### 4.4 body 래퍼 구조

```html
<!-- ❌ 복사 금지 — defaultLayout이 제공 -->
<body class="admin-shell">
<div class="content-area">
    <div class="container-fluid mb-4 p-0">
        <!-- ... -->
    </div>
</div>
</body>
```

### 4.5 CSRF meta 태그

```html
<!-- ❌ 복사 금지 — defaultLayout이 서버 값으로 주입 -->
<meta name="_csrf" content="" />
<meta name="_csrf_header" content="X-CSRF-TOKEN" />
```

defaultLayout은 `th:content="${_csrf != null ? _csrf.token : ''}"`로 실제 CSRF 토큰을 주입한다.

---

## 5. Controller 설정과 메뉴 등록

### 5.1 화면 Controller 생성

`controller/<도메인>/` 아래에 `@Controller`를 생성한다. 화면 렌더링은 `@GetMapping`에서 Thymeleaf 뷰 이름을 반환하는 것으로 끝난다.

```java
package com.scbk.sms.controller.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/system/message")
public class MessageController {

    // private final MessageService service;  // API endpoint 추가 시 주입

    /** 메시지 관리 목록 화면 */
    @GetMapping
    public String page() {
        return "system/message";   // templates/system/message.html
    }

    /** 메시지 수정 상세 화면 (샘플 전환 대상) */
    @GetMapping("/edit")
    public String editPage() {
        return "system/message-edit";   // templates/system/message-edit.html
    }
}
```

**규칙**:

- `@RequestMapping` 경로는 `TB_MENU.MENU_URL`과 일치해야 한다.
- 뷰 이름은 `templates/` 이하 상대 경로에서 `.html`을 뺀 값이다 (`templates/system/message-edit.html` → `"system/message-edit"`).
- 권한 검사는 Controller에 작성하지 않는다. `MenuAuthInterceptor`가 URL 기반으로 처리한다.
- 수정 화면에 상세 데이터가 필요하면 `@GetMapping("/edit/{id}")`에서 Service를 호출해 Model에 담는다.

### 5.2 메뉴 seed 등록

`db/oracle/02_menu_auth_seed.sql`에 `upsert_menu` + `upsert_menu_auth` 호출을 추가한다.

```sql
-- 메뉴 등록 (예: 시스템관리 > 메시지 관리)
upsert_menu('SYSTEM_MESSAGE', 'G_SYSTEM', '메시지 관리', '/system/message', 2, 20, 'M');

-- 역할별 권한 부여 (ROLE_ADMIN에 전체 권한)
upsert_menu_auth('SYSTEM_MESSAGE', 'ROLE_ADMIN');
```

`upsert_menu` 파라미터 순서: `(MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_URL, MENU_LEVEL, SORT_ORD, MENU_TYPE)`.

- `MENU_TYPE`: `'G'`(그룹) 또는 `'M'`(메뉴 화면).
- `MENU_LEVEL`: 그룹 1, 화면 2.
- `upsert_menu_auth`는 해당 역할에 `CAN_READ`~`CAN_MASK_VIEW` 전부 `'Y'`로 부여한다. 역할별로 세분화하려면 seed 이후 별도 UPDATE로 조정한다.

**메뉴 등록 시 함께 갱신할 문서** (`.claude/rules/menu-authority.md`):

- `docs/base/v2-menu-baseline.md`
- `StaticMenuSource` (static 메뉴 소스 사용 시)
- `db/oracle/02_menu_auth_seed.sql`

### 5.3 Interceptor 제외 경로에 추가하지 않는다

새 화면 URL은 `WebMvcConfig.COMMON_EXCLUDE_PATHS`나 `sms.menu.auth.exclude-paths`에 추가하지 않는다. 메뉴 등록 + 권한 부여로 접근을 여는 것이 원칙이다. 제외 경로에 추가하면 권한 통제 없이 누구나 접근할 수 있다.

---

## 6. 전환 후 체크리스트

전환 완료 전 아래 항목을 순서대로 확인한다.

### 6.1 레이아웃 렌더링

- [ ] 화면 진입 시 HTTP 200 반환
- [ ] 사이드바 + 헤더가 정상 렌더링 (defaultLayout 적용 확인)
- [ ] 사이드바에서 해당 메뉴가 활성화 표시
- [ ] `<title>`이 브라우저 탭에 표시
- [ ] 화면 전용 CSS(`layout:fragment="css"`)가 적용 — 인스펙터에서 `<style>` 블록 확인
- [ ] 공통 CSS/JS 이중 로드 없음 — 네트워크 탭에서 중복 요청 확인

### 6.2 PAGE_AUTH 동작

- [ ] `window.PAGE_AUTH`가 서버 값으로 주입됨 (콘솔에서 `PAGE_AUTH` 입력 확인)
- [ ] `SESSION_INFO.empId`, `SESSION_INFO.depId`가 로그인 사용자 값
- [ ] 권한 없는 역할로 접근 시 403 (메뉴 미등록 또는 `CAN_READ = 'N'`)
- [ ] `th:if="${pageAuth.download}"` 등 조건부 버튼이 권한에 따라 표시/숨김

### 6.3 폼 검증

- [ ] 필수 필드 미입력 시 저장 버튼 클릭 → 검증 메시지 표시
- [ ] `data-validate="required|maxlength:100"` 규칙 동작
- [ ] `data-mask="phone"` 전화번호 자동 포맷 (IMask)
- [ ] 라디오/체크박스 선택 값이 `FormBinder.toObject()` 결과에 반영
- [ ] 읽기전용 필드(`readonly`)가 저장 payload에 포함되지 않음

### 6.4 날짜 선택기

- [ ] TUI DatePicker input 클릭 시 캘린더 팝업 표시
- [ ] 날짜 선택 후 input에 `YYYY-MM-DD` 형식 표시
- [ ] `scaffold-date-picker-layer` div가 캘린더 레이어로 동작
- [ ] 날짜 값이 서버 전송 시 올바른 형식 (`YYYYMMDD` 또는 `YYYY-MM-DD`)

### 6.5 서버 연동 (API endpoint 추가 후)

- [ ] 수정 화면: 상세 데이터가 `th:value`로 정상 표시
- [ ] 저장: `POST /<도메인>/update` → `ApiResponse` 성공 응답 → `CommonUtils.toast` 알림
- [ ] 낙관적 잠금: `beforeUpdateDttm` hidden 값 전송, 충돌 시 409 (C004)
- [ ] 취소 버튼: `history.back()` 또는 목록 URL로 이동

### 6.6 빌드/테스트

```text
mvn test                  PASS
mvn -DskipTests package   PASS
```

서버 기동 후 수동 확인 (서버 실행은 사용자 담당):

- 화면 진입 200, 사이드바 메뉴 활성화
- 권한 없는 역할 접근 시 403
- prod 프로파일에서 `/samples/**` 접근 시 404

---

## 7. prod 샘플 차단 (SamplesBlockInterceptor)

### 7.1 동작 원리

`SamplesBlockInterceptor`는 **prod 프로파일에서만** `/samples/**` 경로 접근을 404로 차단한다. local/dev에서는 이 빈이 등록되지 않아 샘플에 접근할 수 있다.

```java
@Component
@Profile("prod")
public class SamplesBlockInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
        HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }
}
```

### 7.2 Interceptor 등록 (WebMvcConfig)

`WebMvcConfig`에서 `ObjectProvider`로 주입받아 prod일 때만 등록한다.

```java
// WebMvcConfig.addInterceptors()
SamplesBlockInterceptor samplesBlocker = samplesBlockInterceptor.getIfAvailable();
if (samplesBlocker != null) {
    registry.addInterceptor(samplesBlocker).addPathPatterns("/samples/**");
}
```

`/samples/**`는 `COMMON_EXCLUDE_PATHS`에 포함되어 있어 `MenuAuthInterceptor`(메뉴 권한) 검증은 거치지 않는다. 즉 샘플은 메뉴 권한 대상이 아니며, prod에서는 404로 완전히 차단된다.

### 7.3 프로파일별 접근 가능 여부

| 프로파일 | `/samples/message-edit.html` | `/samples/campaign-register.html` |
|---|---|---|
| local | 200 (접근 가능) | 200 (접근 가능) |
| dev | 200 (접근 가능) | 200 (접근 가능) |
| prod | **404 (차단)** | **404 (차단)** |

### 7.4 file:// 접근

샘플은 서버 없이 브라우저에서 파일을 직접 열어도(`file://`) 동작한다. CSS/JS가 상대 경로(`../`)로 되어 있어 `static/` 디렉터리 구조가 유지되면 로컬 파일 시스템에서도 정상 렌더링된다. 이 방식은 서버 기동이 어려운 환경에서 UI 구조를 빠르게 확인할 때 유용하다.

---

## 부록 — 전환 결과물 전체 예시 (message-edit)

`message-edit.html` 샘플을 전환한 최종 템플릿의 전체 구조다.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{defaultLayout}">
<head>
    <title>메시지 관리</title>
    <th:block layout:fragment="css">
        <style>
            .message-edit-date-field { width: min(100%, 220px); }
            .message-edit-unit-control { width: min(100%, 180px); }
            .message-edit-ad-warning {
                color: var(--sms-danger);
                font-size: var(--sms-fs-body-sm);
                font-weight: var(--sms-fw-semibold);
                word-break: keep-all;
            }
            .message-edit-readonly {
                background: var(--sms-bg-muted) !important;
                color: var(--sms-text-muted);
            }
        </style>
    </th:block>
</head>
<body>
<main layout:fragment="content">

    <div class="content-header">
        <h2>메시지 관리 <small class="text-body-sm">(시스템관리 &gt; 메시지관리 &gt; 수정)</small></h2>
    </div>

    <form id="message-edit-form" autocomplete="off" novalidate>
        <input type="hidden" id="messageId" name="messageId"
               th:value="${message != null ? message.messageId : ''}">
        <input type="hidden" id="messageCode" name="messageCode"
               th:value="${message != null ? message.messageCode : ''}">
        <input type="hidden" id="beforeUpdateDttm" name="beforeUpdateDttm"
               th:value="${message != null ? message.updateDttm : ''}">

        <div class="card shadow-sm border-0 mb-0">
            <div class="card-header bg-white py-3">
                <div class="d-flex align-items-center justify-content-between gap-3">
                    <h3 class="fs-6 fw-semibold mb-0">메시지 상세 정보</h3>
                    <span class="text-caption"><span class="text-danger">*</span> 필수 입력</span>
                </div>
            </div>
            <div class="card-body p-0">
                <div class="row g-0 form-detail-row">
                    <div class="col-12 col-sm-2 form-detail-label">
                        <label class="form-detail-required" for="messageTitle">메시지 제목</label>
                    </div>
                    <div class="col-12 col-sm-10 form-detail-control">
                        <input type="text" class="form-control" id="messageTitle" name="messageTitle"
                               th:value="${message != null ? message.messageTitle : ''}"
                               maxlength="100" placeholder="메시지 제목을 입력하세요"
                               data-validate="required|maxlength:100" required>
                    </div>
                </div>
                <!-- 나머지 행도 동일 패턴으로 th:value 바인딩 -->
            </div>
        </div>

        <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" id="btn-cancel" class="btn btn-secondary px-3">
                <i data-lucide="x" aria-hidden="true"></i>
                <span>취소</span>
            </button>
            <button type="submit" id="btn-save" class="btn btn-primary px-4">
                <i data-lucide="save" aria-hidden="true"></i>
                <span>저장</span>
            </button>
        </div>
    </form>

</main>
<th:block layout:fragment="script">
    <script th:src="@{/js/system/message-edit.js}"></script>
</th:block>
</body>
</html>
```

이 템플릿에 대응하는 Controller:

```java
@Controller
@RequiredArgsConstructor
@RequestMapping("/system/message")
public class MessageController {

    private final MessageService service;

    @GetMapping("/edit")
    public String editPage(@RequestParam Integer messageId, Model model) {
        model.addAttribute("message", service.getDetail(messageId));
        return "system/message-edit";
    }
}
```
