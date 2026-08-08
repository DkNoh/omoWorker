# CoreUI 매뉴얼 (프로젝트 한정)

> 대상 라이브러리: **CoreUI** — 버전 확인 필요 (vendored, CDN 미사용)
>
> 대상 파일
> - `src/main/resources/static/vendor/coreui/css/coreui.min.css` — CSS 본체
> - `src/main/resources/static/vendor/coreui/js/coreui.bundle.min.js` — JS 번들 (Popper 포함)
> - `src/main/resources/static/css/admin-common.css` — 디자인 토큰 + `--cui-*` 별칭 매핑
> - `src/main/resources/static/css/admin-ui-bridge.css` — CoreUI 컴포넌트 브리지 스타일
>
> 이 문서는 CoreUI 전체 API를 다루지 않는다. **이 프로젝트에서 실제로 쓰는 부분만** 다룬다.
> 관련 문서: [`css-design_manual.md`](./css-design_manual.md) (디자인 토큰 체계), [`common-js_manual.md`](./common-js_manual.md) (modal-manager.js)

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [CSS 브리지 패턴 — CoreUI 위에 프로젝트 토큰 입히기](#3-css-브리지-패턴--coreui-위에-프로젝트-토큰-입히기)
4. [프로젝트에서 사용하는 CoreUI 컴포넌트](#4-프로젝트에서-사용하는-coreui-컴포넌트)
   4.1 사이드바 (Sidebar)
   4.2 모달 (Modal)
   4.3 중첩 모달 제한과 역할 분리
   4.4 토스트 (Toast)
   4.5 헤더 (Header)
   4.6 아바타 (Avatar)
   4.7 기본 유틸리티 클래스
6. [주의사항 및 프로젝트 특이사항](#6-주의사항-및-프로젝트-특이사항)
7. [참조](#7-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 라이브러리 | CoreUI |
| 버전 | **확인 필요** — 파일 헤더에서 버전 미검출. 교체 시 확정 필요 |
| 라이선스 | MIT |
| 출처 | https://coreui.io/ |
| 용도 | 관리자 화면의 베이스 컴포넌트 라이브러리 (버튼, 카드, 폼, 모달, 사이드바, 토스트 등) |

`MANIFEST.md` 등재 행:

```text
| coreui/css/coreui.min.css      | CoreUI | 확인 필요 | https://coreui.io/ | MIT | (기존) | 헤더에서 버전 확정 후 기입 |
| coreui/js/coreui.bundle.min.js | CoreUI | 확인 필요 | https://coreui.io/ | MIT | (기존) | |
```

### `static/vendor`에 위치

CoreUI는 `static/lib`이 아닌 **`static/vendor`** 에 있다. `static/lib`은 JS 유틸 라이브러리, `static/vendor`는 CSS 프레임워크·번들 자산을 구분해 관리한다.

---

## 2. 프로젝트 로드 방식

### 2.1 CSS 로드 — `<head>` 내부

`defaultLayout.html`의 `<head>`에서 TUI CSS 이후, 프로젝트 CSS 이전에 로드:

```html
<!-- defaultLayout.html 29~38행 -->
<!-- CoreUI CSS (base component library — CoreUI owns .btn/.card/.form-control/.modal) -->
<link rel="stylesheet" th:href="@{/vendor/coreui/css/coreui.min.css}">

<!-- Design token layer — must load before admin-layout/bridge -->
<link rel="stylesheet" th:href="@{/css/admin-common.css}" />

<!-- Project shell/bridge CSS -->
<link rel="stylesheet" th:href="@{/css/admin-layout.css}" />
<link rel="stylesheet" th:href="@{/css/admin-ui-bridge.css}" />
```

`error.html`에서도 동일하게 CoreUI CSS + admin-common.css를 직접 로드한다 (defaultLayout을 사용하지 않는 예외 페이지).

### 2.2 JS 로드 — `<body>` 하단

```html
<!-- defaultLayout.html 98~99행 -->
<!-- CoreUI Bundle JS -->
<script th:src="@{/vendor/coreui/js/coreui.bundle.min.js}"></script>
```

`coreui.bundle.min.js`는 Popper.js를 포함한 번들이다. 별도 Popper 스크립트 추가 없이 동작한다.

### 2.3 CSS 로드 순서 (핵심)

```text
1. coreui.min.css        ← CoreUI 베이스 컴포넌트 (.btn, .card, .form-control, .modal 소유)
2. admin-common.css      ← --sms-* 토큰 정의 + --cui-* 별칭 매핑
3. admin-layout.css      ← 관리자 셸 레이아웃 (--sms-* 소비)
4. admin-ui-bridge.css   ← TUI Grid/아이콘/폼 브리지 (--sms-* 소비)
```

이 순서가 깨지면 CoreUI 컴포넌트에 프로젝트 팔레트가 적용되지 않거나, 브리지 스타일이 CoreUI 기본값에 덮어씌워진다.

### 2.4 전역 변수

| 전역 | 설명 |
|---|---|
| `window.coreui` | CoreUI JS 네임스페이스. `coreui.Modal`, `coreui.Toast` 등 컴포넌트 클래스 포함 |

---

## 3. CSS 브리지 패턴 — CoreUI 위에 프로젝트 토큰 입히기

이 프로젝트는 CoreUI 컴포넌트 클래스를 **교체하지 않고**, 디자인 토큰을 주입해 시각을 통일한다.

### 3.1 `admin-common.css` — 토큰 정의 + `--cui-*` 별칭

`:root`에 `--sms-*` 프로젝트 토큰을 정의하고, CoreUI가 읽는 `--cui-*` 변수에 별칭으로 매핑한다:

```css
/* admin-common.css 143~172행 (발췌) */
:root {
    /* 프로젝트 토큰 */
    --sms-primary: #2557d6;
    --sms-bg-app: #f5f7fa;
    --sms-text-body: #2a3a52;
    --sms-border: #dfe3eb;
    /* ... */

    /* CoreUI 별칭 — CoreUI 컴포넌트가 이 값을 자동으로 상속 */
    --cui-primary: var(--sms-primary);
    --cui-body-bg: var(--sms-bg-app);
    --cui-body-color: var(--sms-text-body);
    --cui-border-color: var(--sms-border);
    --cui-danger: var(--sms-danger);
    --cui-success: var(--sms-success);
    --cui-warning: var(--sms-warning);
    --cui-info: var(--sms-info);
    /* ... */
}
```

**규칙**: 프로젝트 CSS/템플릿은 `--sms-*`만 참조한다. `--cui-*` 직접 참조는 `admin-common.css`의 별칭 매핑 부분으로 한정한다.

### 3.2 `admin-ui-bridge.css` — 컴포넌트 보강

CoreUI 컴포넌트의 구조는 유지하면서, 토큰 기반 시각 보강을 추가한다:

```css
/* admin-ui-bridge.css 헤더 주석 */
/* CoreUI component classes (.btn / .card / .form-control / .modal) are
   NOT replaced — only augmented with token-driven refinements. */
```

주요 보강 대상:

| 대상 | 내용 |
|---|---|
| `.form-control`, `.form-select` | 포커스 글로우(`--sms-primary`), 보더 색상 |
| `.card.shadow-sm.border-0` | 토큰 기반 배경/보더/반경/그림자 통일 |
| `.card-header.bg-white` | 토큰 기반 패딩/폰트/색상 |
| `.alert-*` | 토큰 기반 상태 색상 |
| TUI Grid 셀/헤더/페이지네이션 | CoreUI와 시각 통일 |

---

## 4. 프로젝트에서 사용하는 CoreUI 컴포넌트

### 4.1 사이드바 (Sidebar)

`fragments/sidebar.html`:

```html
<div class="sidebar" data-coreui-theme="light" id="sidebar" th:fragment="sidebar">
    <div class="sidebar-brand ...">...</div>
    <ul class="sidebar-nav ..." data-coreui="navigation" data-simplebar="">
        <li class="nav-group">
            <a class="nav-link nav-group-toggle" href="#">...</a>
            <ul class="nav-group-items">
                <li class="nav-item"><a class="nav-link" href="...">...</a></li>
            </ul>
        </li>
    </ul>
</div>
```

| 속성/클래스 | 용도 |
|---|---|
| `data-coreui-theme="light"` | 사이드바 라이트 테마 |
| `data-coreui="navigation"` | CoreUI 네비게이션 초기화 |
| `.sidebar`, `.sidebar-nav`, `.nav-group`, `.nav-group-items` | CoreUI 사이드바 구조 클래스 |
| `.sidebar-brand-full`, `.sidebar-brand-narrow` | 접힘/펼침 상태별 표시 요소 |

사이드바 접힘/펼침은 `defaultLayout.html`의 인라인 스크립트가 `.collapsed` 클래스 토글로 처리한다 (CoreUI JS API 미사용):

```javascript
// defaultLayout.html 107~109행
sidebar.classList.toggle('collapsed');
const isCollapsed = sidebar.classList.contains('collapsed');
toggleBtn.setAttribute('aria-expanded', String(!isCollapsed));
```

### 4.2 모달 (Modal)

`fragments/modal-base.html` — 프로젝트 표준 모달 뼈대:

```html
<div class="modal fade" th:id="${modalId}" tabindex="-1" aria-hidden="true"
     data-coreui-backdrop="static">
    <div class="modal-dialog" th:classappend="${size ?: 'modal-md'}">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" th:id="${modalId + '-title'}">제목</h5>
                <button type="button" class="btn-close" data-coreui-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">...</div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-coreui-dismiss="modal">닫기</button>
                <button type="button" class="btn btn-primary" th:id="${modalId + '-btn-save'}">저장</button>
            </div>
        </div>
    </div>
</div>
```

| 속성 | 용도 |
|---|---|
| `data-coreui-backdrop="static"` | 배경 클릭 시 모달 닫힘 방지 |
| `data-coreui-dismiss="modal"` | 버튼 클릭 시 모달 닫기 (declarative) |
| `.modal`, `.modal-dialog`, `.modal-content` | CoreUI 모달 구조 클래스 |

JS에서는 `modal-manager.js`가 `coreui.Modal.getOrCreateInstance()`로 인스턴스를 중앙 관리한다:

```javascript
// modal-manager.js getFrameworkModal() (24~33행)
const getFrameworkModal = (el) => {
    if (!el) return null;
    if (window.coreui && window.coreui.Modal) {
        return window.coreui.Modal.getOrCreateInstance(el);
    }
    if (window.bootstrap && window.bootstrap.Modal) {
        return window.bootstrap.Modal.getOrCreateInstance(el);  // 폴백
    }
    return null;
};
```

모달 lifecycle 이벤트는 CoreUI 전용 이벤트명을 사용한다:

```javascript
modalEl.addEventListener('show.coreui.modal', ...);    // 열기 전
modalEl.addEventListener('shown.coreui.modal', ...);   // 열린 후
modalEl.addEventListener('hidden.coreui.modal', ...);  // 닫힌 후
```

### 중첩 모달 제한과 역할 분리

CoreUI(Bootstrap 기반)는 **동시에 여러 모달을 여는 것을 공식 지원하지 않는다**
("multiple modals cannot be open simultaneously"). 업무 모달 위에서 확인·알림 팝업을
겹쳐 띄우면 backdrop·ESC·focus 관리가 충돌한다.

이 프로젝트의 역할 분리:

| 역할 | 담당 | 비고 |
|------|------|------|
| 업무 모달 (등록/수정/상세) | CoreUI `.modal` + `ModalManager` | `modal-base.html` 프래그먼트 |
| 차단형 알림/확인 팝업 | `Notify.alert` / `Notify.confirm` (SweetAlert2) | 업무 모달 위에서도 안전 |

업무 모달 내부에서 삭제·저장 확인이 필요하면 `Notify.confirm(msg, callback)`을 사용한다.
두 번째 CoreUI 모달을 직접 생성하지 않는다.

### 4.3 토스트 (Toast)

`notify.js`에서 알림 토스트 생성에 사용:

```javascript
// notify.js 210행
const instance = new coreui.Toast(toastEl, { delay: 3000 });
```

### 4.4 헤더 (Header)

`fragments/header.html`:

```html
<header class="header header-sticky p-0 app-header" th:fragment="header" role="banner">
    <div class="container-fluid px-3 header-bar">
        <ul class="header-nav ...">...</ul>
    </div>
</header>
```

`.header`, `.header-sticky`, `.header-nav`는 CoreUI 헤더 컴포넌트 클래스다.

### 4.5 아바타 (Avatar)

```html
<!-- header.html 28행 -->
<div class="avatar avatar-md me-3 header-avatar" aria-hidden="true">A</div>
```

### 4.6 기본 유틸리티 클래스

CoreUI는 Bootstrap 호환 유틸리티를 포함한다. 프로젝트 전반에서 사용:

- 레이아웃: `.d-flex`, `.d-none`, `.container-fluid`, `.row`, `.col-md-*`
- 간격: `.p-0`, `.px-3`, `.me-2`, `.mb-3`, `.gap-2`
- 버튼: `.btn`, `.btn-primary`, `.btn-secondary`, `.btn-outline-danger`, `.btn-sm`, `.btn-link`
- 폼: `.form-control`, `.form-select`, `.form-label`, `.is-invalid`
- 카드: `.card`, `.card-body`, `.card-header`, `.card-footer`, `.shadow-sm`, `.border-0`
- 텍스트: `.fw-bold`, `.text-truncate`, `.fs-5`
- 배지: `.badge`, `.bg-primary`, `.bg-success`, `.bg-danger`

---

## 5. 핵심 JS API 요약

이 프로젝트에서 사용하는 CoreUI JS API는 3개뿐이다.

| API | 용도 | 호출 위치 |
|---|---|---|
| `coreui.Modal.getOrCreateInstance(el)` | 모달 인스턴스 획득/생성 | `modal-manager.js`, `notify.js` |
| `instance.show()` / `instance.hide()` | 모달 열기/닫기 | `modal-manager.js` `open()`, `close()` |
| `new coreui.Toast(el, { delay })` | 토스트 생성·표시 | `notify.js` |

### Bootstrap 폴백

`modal-manager.js`와 `notify.js`는 `window.coreui`가 없으면 `window.bootstrap`으로 폴백한다. 이는 CoreUI가 Bootstrap API 호환을 유지하기 때문이다. 현재 프로젝트에는 CoreUI만 로드되어 있으므로 폴백 경로는 사실상 비활성이다.

---

## 6. 주의사항 및 프로젝트 특이사항

### 6.1 버전 미확인

MANIFEST.md에 **"확인 필요"** 로 표기되어 있다. 파일 헤더에서 버전 문자열이 검출되지 않았다. 교체 시 동일 메이저 버전을 우선하고, 교체 후 버전을 확정해 MANIFEST.md에 기입한다.

### 6.2 `static/vendor` 위치 — `static/lib` 아님

CoreUI는 `static/vendor/coreui/`에 있다. `static/lib`에 복사하거나 경로 혼동 금지. MANIFEST.md도 `static/lib`과 `static/vendor` 섹션이 분리되어 있다.

### 6.3 CSS 로드 순서 준수

`coreui.min.css` → `admin-common.css` → `admin-layout.css` → `admin-ui-bridge.css` 순서를 지켜야 한다. `admin-common.css`가 `--cui-*` 별칭을 정의하므로, CoreUI CSS보다 뒤에 로드되어야 별칭이 적용된다.

### 6.4 CoreUI 컴포넌트 클래스 교체 금지

`admin-ui-bridge.css`는 CoreUI 클래스를 **보강**만 한다. `.btn`, `.card`, `.form-control`, `.modal` 등 CoreUI 구조 클래스를 프로젝트 CSS에서 재정의하거나 제거하지 않는다.

### 6.5 `--cui-*` 직접 참조 제한

템플릿·프로젝트 CSS에서 `--cui-*` 변수를 직접 참조하지 않는다. `--sms-*` 토큰만 사용한다. `--cui-*` 매핑은 `admin-common.css` 한 파일에서만 소유한다.

### 6.6 모달 이벤트명

CoreUI 모달 이벤트는 `show.coreui.modal`, `shown.coreui.modal`, `hidden.coreui.modal`이다. Bootstrap의 `show.bs.modal`과 다르므로, 이벤트 리스너 등록 시 `.coreui.` 네임스페이스를 사용해야 한다.

### 6.7 사이드바 접힘 — CSS 클래스 토글

사이드바 접힘은 CoreUI JS API가 아닌 `.collapsed` 클래스 토글로 처리한다. 접힘 후 TUI Grid 레이아웃 갱신을 위해 `setTimeout(() => grid.refreshLayout(), 210)`을 호출한다 (CSS transition 200ms + 여유).

### 6.8 Dropdown 미사용

현재 템플릿에서 CoreUI Dropdown 컴포넌트(`data-coreui-toggle="dropdown"`)는 사용하지 않는다. `coreui.bundle.min.js`에 포함되어 있으나 활성 사용처 없음.

---

## 7. 참조

- `src/main/resources/static/lib/MANIFEST.md` — 버전·라이선스 (37~38행, `static/vendor` 섹션)
- `src/main/resources/templates/defaultLayout.html` — CSS 로드 (30행), JS 로드 (99행)
- `src/main/resources/static/css/admin-common.css` — `--cui-*` 별칭 매핑 (143~172행)
- `src/main/resources/static/css/admin-ui-bridge.css` — 컴포넌트 브리지 스타일
- `src/main/resources/templates/fragments/sidebar.html` — 사이드바 컴포넌트 사용
- `src/main/resources/templates/fragments/modal-base.html` — 모달 표준 뼈대
- `src/main/resources/templates/fragments/header.html` — 헤더 컴포넌트 사용
- `src/main/resources/static/js/common/modal-manager.js` — `coreui.Modal` 인스턴스 관리
- `src/main/resources/static/js/common/notify.js` — `coreui.Toast` 사용 (210행)
- [`css-design_manual.md`](./css-design_manual.md) — 디자인 토큰 체계 상세
