# CSS 아키텍처 & 디자인 토큰 매뉴얼

> 대상 파일
> - `src/main/resources/static/css/admin-common.css` — 디자인 토큰(`--sms-*`) + CoreUI alias + 전역 base 스타일
> - `src/main/resources/static/css/admin-layout.css` — 관리자 셸 레이아웃(사이드바/헤더/콘텐츠)
> - `src/main/resources/static/css/admin-ui-bridge.css` — CoreUI·TUI Grid·아이콘 브리지
> - `src/main/resources/static/css/admin-form-detail.css` — 등록/수정 상세폼 행 패턴
> - `src/main/resources/static/css/auth.css` — 로그인 페이지 전용
> - `src/main/resources/static/css/scaffold-tool.css` — Query Scaffold 화면 전용
>
> 설계 근거: `DESIGN.md` (루트) — 토큰 계층 설계의 원문
>
> 관련 매뉴얼: [`common-js_manual.md`](./common-js_manual.md), [`tui_manual.md`](./tui_manual.md)

---

## 목차

1. [개요](#1-개요)
2. [로드 순서와 계층 구조](#2-로드-순서와-계층-구조)
3. [디자인 토큰 (`--sms-*`) 전체 목록](#3-디자인-토큰---sms--전체-목록)
4. [CoreUI 브리지 (`--cui-*` alias)](#4-coreui-브리지---cui--alias)
5. [레이아웃 시스템 — `admin-layout.css`](#5-레이아웃-시스템--admin-layoutcss)
6. [UI 브리지 — `admin-ui-bridge.css`](#6-ui-브리지--admin-ui-bridgecss)
7. [상세폼 패턴 — `admin-form-detail.css`](#7-상세폼-패턴--admin-form-detailcss)
8. [인증 페이지 — `auth.css`](#8-인증-페이지--authcss)
9. [Scaffold 도구 — `scaffold-tool.css`](#9-scaffold-도구--scaffold-toolcss)
10. [새 CSS 추가 가이드](#10-새-css-추가-가이드)
11. [DESIGN.md 참조 — 토큰 계층 설계 근거](#11-designmd-참조--토큰-계층-설계-근거)

---

## 1. 개요

### 1.1 CSS 스택

UI 프레임워크는 **CoreUI 5**(Bootstrap 기반)이며, 프로젝트는 그 위에 **디자인 토큰 계층**(`--sms-*` custom properties)을 얹는다. CSS preprocessor 없음, 빌드 파이프라인 없음 — 모든 CSS는 plain 파일로 `static/css/`에 배치되고 `<link>` 태그로 로드된다.

| # | 파일 | 소유 범위 | 토큰 소비 |
|---|------|-----------|-----------|
| 1 | `admin-common.css` | `--sms-*` 토큰 정의 + `--cui-*` alias + 전역 base 스타일(body, heading, link, focus, reduced-motion) | 토큰 **정의** 파일 |
| 2 | `admin-layout.css` | 관리자 셸 geometry(사이드바/헤더/콘텐츠 영역), 페이지 타이틀, 버튼 피드백, skip-link | `--sms-*`만 참조 |
| 3 | `admin-ui-bridge.css` | Lucide 아이콘, surface primitive, 폼 컨트롤 focus, TUI Grid/페이지네이션/DatePicker, autocomplete, spinner, empty state, alert, 메뉴관리 트리 | `--sms-*`만 참조 |
| 4 | `admin-form-detail.css` | 등록/수정 화면의 상세폼 행 레이아웃(`form-detail-*` 클래스) | `--sms-*`만 참조 |
| 5 | `auth.css` | 로그인(`body.auth-page`) 및 홈(`body.home-page`) 페이지 전용 | `--sms-*`만 참조 |
| 6 | `scaffold-tool.css` | Query Scaffold 화면(local 전용) 전용 미세 보정 | `--cui-*` 2건만 참조 (예외 허용) |

### 1.2 핵심 원칙

- **CoreUI가 소유한 컴포넌트 시각**(`.btn`, `.card`, `.form-control`, `.modal` …)은 클래스를 교체하지 않고 **변수 주입**으로만 재스타일링한다. 프로젝트 CSS는 그 위에 토큰 기반 정제(refinement)를 덧입히는 역할이다.
- **템플릿과 프로젝트 CSS는 `--sms-*` 토큰만 참조한다.** `--cui-*` 직접 참조는 ① `admin-common.css`의 alias 매핑, ② CoreUI 변수 매핑이 반드시 필요한 bridge 섹션(sidebar nav link, btn hover 등)에서만 허용된다. `scaffold-tool.css`의 `--cui-secondary-color`·`--cui-tertiary-bg` 참조가 후자의 예다.
- **raw hex 금지.** 새 색이 필요하면 `admin-common.css` `:root`에 토큰을 먼저 추가한다(`*-rgb` 토큰은 `rgba()` 오버레이 전용).
- **아이콘은 Lucide**(`data-lucide`)만 사용. emoji 아이콘 금지.
- **폐쇄망 제약.** 웹폰트/CDN/외부 이미지 로딩 금지 — OS 내장 폰트와 `static/lib`, `static/vendor` 로컬 번들만 사용.

---

## 2. 로드 순서와 계층 구조

### 2.1 관리자 화면 — `defaultLayout.html`

모든 업무 화면은 `layout:decorate="~{defaultLayout}"`로 상속한다. `<head>` 내 CSS 로드 순서는 **고정**이며, 뒤늦게 로드된 파일이 cascade에서 이긴다.

```html
<!-- ① TUI 라이브러리 CSS (static/lib 로컬 참조) -->
<link rel="stylesheet" href="/lib/tui-pagination.css" />
<link rel="stylesheet" href="/lib/tui-date-picker.css" />
<link rel="stylesheet" href="/lib/tui-grid.css" />

<!-- ② CoreUI — base 컴포넌트 라이브러리 (.btn/.card/.form-control/.modal 소유자) -->
<link rel="stylesheet" th:href="@{/vendor/coreui/css/coreui.min.css}">

<!-- ③ 디자인 토큰 계층 — admin-layout/bridge 보다 반드시 먼저 로드 -->
<link rel="stylesheet" th:href="@{/css/admin-common.css}" />

<!-- ④ 프로젝트 셸/브리지 CSS -->
<link rel="stylesheet" th:href="@{/css/admin-layout.css}" />
<link rel="stylesheet" th:href="@{/css/admin-ui-bridge.css}" />
<link rel="stylesheet" th:href="@{/css/admin-form-detail.css}" />

<!-- ⑤ 화면별 CSS — layout:fragment="css" (head 마지막에 주입) -->
<th:block layout:fragment="css"></th:block>
```

계층을 한 줄로 표현하면:

```text
coreui.min.css → admin-common.css → admin-layout.css → admin-ui-bridge.css → admin-form-detail.css → [화면별 CSS]
   (base)          (토큰+alias)        (셸 geometry)        (컴포넌트 브리지)       (상세폼 행)        (scope 한정)
```

### 2.2 로그인 화면 — `login.html`

`login.html`은 `defaultLayout`을 사용하지 않는 유일한 예외다. CoreUI 없이 토큰 계층만 로드한다.

```html
<!-- Design token layer first, then auth page styles. -->
<link rel="stylesheet" th:href="@{/css/admin-common.css}">
<link rel="stylesheet" th:href="@{/css/auth.css}">
```

> `auth.css`가 `--sms-*` 토큰을 소비하므로 `admin-common.css`가 **반드시 먼저** 로드되어야 한다.

### 2.3 Scaffold 화면 — `system/scaffold.html`

`defaultLayout`을 상속하면서 `layout:fragment="css"`로 전용 CSS를 추가한다.

```html
<th:block layout:fragment="css">
    <link rel="stylesheet" th:href="@{/css/scaffold-tool.css}">
</th:block>
```

### 2.4 계층별 소유 규칙

| 계층 | 누가 소유 | 프로젝트가 하는 일 |
|------|-----------|--------------------|
| 컴포넌트 시각(structure) | CoreUI | 건드리지 않음 |
| 팔레트/타이포/간격 값 | `admin-common.css`의 `--sms-*` | `--cui-*` alias로 CoreUI에 주입 |
| 셸 geometry(사이드바/헤더/콘텐츠) | `admin-layout.css` | 토큰 참조로 직접 스타일 |
| TUI Grid/DatePicker/autocomplete | `admin-ui-bridge.css` | 토큰 참조로 오버라이드(`!important` 사용 — TUI 자체 스타일보다 뒤에 로드되지만 TUI가 인라인 근접 선택자를 쓰므로 필요) |

> **참고 — 셸 토큰 이중 선언**: `--sms-header-height`, `--sms-sidebar-width`, `--sms-sidebar-collapsed-width`, `--sms-content-padding`, `--sms-avatar-md`는 `admin-common.css`와 `admin-layout.css` 양쪽 `:root`에 선언되어 있다(값 동일). `admin-layout.css` 쪽은 "shell-specific geometry 토큰은 이 파일이 소유한다"는 의도의 선언으로, 로드 순서상 `admin-layout.css`가 최종 값을 가진다. 값을 바꿀 때는 **두 파일을 함께** 수정해야 한다.

---

## 3. 디자인 토큰 (`--sms-*`) 전체 목록

모든 토큰은 `admin-common.css`의 `:root`에 정의된다. 카테고리별 전체 목록은 아래와 같다.

### 3.1 Color — Surfaces

표면 계층은 미세한 명도 차(`#ffffff` → `#f9fafc` → `#f5f7fa`)로 깊이를 표현하며, 그림자는 덜 쓴다(tonal shift).

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-bg-app` | `#f5f7fa` | 콘텐츠 영역 전체 앱 배경(약간 쿨한 회색) |
| `--sms-bg-surface` | `#ffffff` | 카드, 패널, 모달 |
| `--sms-bg-surface-alt` | `#f9fafc` | 표 odd 행, 그리드 헤더, 라벨 셀 바탕 |
| `--sms-bg-muted` | `#eef1f6` | 입력 disabled, 탭 비활성 바탕 |

### 3.2 Color — Primary (deep corporate blue)

`--sms-primary`는 **오직 인터랙티브 요소**(버튼/링크/포커스 링/활성 메뉴/선택 행)에만 사용한다. 장식용 사용 금지.

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-primary` | `#2557d6` | CTA, 링크, 포커스 링, 활성 메뉴 |
| `--sms-primary-hover` | `#1e47b0` | 버튼 hover |
| `--sms-primary-active` | `#1a3e9c` | 버튼 press |
| `--sms-primary-soft` | `#e8efff` | 선택/hover 행, 배지 바탕, 아바타 바탕 |
| `--sms-primary-rgb` | `37, 87, 214` | `rgba()` 오버레이 전용 (focus glow, hover 행, btn shadow) |

### 3.3 Color — Status

상태 색은 표시 전용이며, 배경에는 대응하는 `*-soft` 토큰을 짝으로 쓴다.

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-danger` | `#d93b3b` | 삭제, 오류, 파괴적 액션, 필수 표시(`*`) |
| `--sms-danger-soft` | `#fdeaea` | 인라인 오류 메시지 바탕 |
| `--sms-danger-rgb` | `217, 59, 59` | `rgba()` 오버레이 전용 |
| `--sms-success` | `#1f8a4c` | 완료, 성공 배지 |
| `--sms-success-soft` | `#e8f6ed` | 성공 토스트/알림 바탕 |
| `--sms-success-rgb` | `31, 138, 76` | `rgba()` 오버레이 전용 |
| `--sms-warning` | `#c8780a` | 경고, 보류 |
| `--sms-warning-soft` | `#fdf3e0` | 경고 배지 바탕 |
| `--sms-warning-rgb` | `200, 120, 10` | `rgba()` 오버레이 전용 |
| `--sms-info` | `#2a72c4` | 정보성 알림 |
| `--sms-info-soft` | `#e6effa` | 정보 알림 바탕 |

### 3.4 Color — Text / Borders

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-text-strong` | `#0f1c2e` | 페이지 타이틀, 주요 수치, 라벨 |
| `--sms-text-body` | `#2a3a52` | 본문 기본 |
| `--sms-text-muted` | `#6b7a90` | 라벨, 캡션, 보조 텍스트 |
| `--sms-text-disabled` | `#a3acbd` | disabled 라벨, placeholder |
| `--sms-border` | `#dfe3eb` | 카드/입력/표 셀 구분선 |
| `--sms-border-strong` | `#c4cbda` | hover 페이지 버튼 테두리 등 보조 강조 |
| `--sms-border-subtle` | `#eef1f6` | 미세 구분(섹션 내부, 행 구분, 사이드바 border) |

### 3.5 Typography — Font stacks

폐쇄망 대상이라 웹폰트 로딩 없이 OS 내장 한국어 폰트를 우선한다. 폰트 패밀리는 최대 2개(sans/mono)만 허용.

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-font-sans` | `"Apple SD Gothic Neo", "Malgun Gothic", "Noto Sans KR", -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif` | 전체 UI 본문 (macOS: Apple SD Gothic Neo / Windows: Malgun Gothic) |
| `--sms-font-mono` | `"SF Mono", Menlo, Consolas, "Liberation Mono", monospace` | 코드, ID, byte 카운터 등 숫자/코드성 텍스트 |

### 3.6 Typography — Scale / Weights / Line heights

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-fs-page-title` | `1.25rem` (20px) | `content-header h2` — 각 화면 타이틀 |
| `--sms-fs-section` | `1rem` (16px) | 카드 헤더, 섹션 제목, empty-state 타이틀 |
| `--sms-fs-body` | `0.9375rem` (15px) | 기본 본문, 폼 라벨 |
| `--sms-fs-body-sm` | `0.8125rem` (13px) | 표 셀, 보조 텍스트 |
| `--sms-fs-caption` | `0.75rem` (12px) | 메타데이터, 사이드바 사용자 정보, 그리드 헤더 |
| `--sms-fs-overline` | `0.6875rem` (11px) | eyebrow (로그인 브랜드 블록) |
| `--sms-fs-display` | `3rem` (48px) | 에러 화면 상태 코드 |
| `--sms-fw-regular` | `400` | 본문 |
| `--sms-fw-medium` | `500` | 라벨, 버튼 |
| `--sms-fw-semibold` | `600` | 섹션 제목, 그리드 헤더 |
| `--sms-fw-bold` | `700` | 페이지 타이틀, 강조 |
| `--sms-lh-tight` | `1.3` | 제목 (CJK safe) |
| `--sms-lh-heading` | `1.4` | heading 기본 |
| `--sms-lh-body` | `1.55` | 본문 — CJK 가독성 하한 |

> 타이포 규칙: 본문은 13px 아래로 내리지 않는다 / 한국어 행간 최소 1.45 (클리핑 방지) / 제목에 음수 자간 금지(단, `content-header h2`와 로그인 `h1`은 `-0.01em` 예외) / 숫자 밀집 영역은 `font-variant-numeric: tabular-nums`(`.nums`, `.tui-grid-cell`에 적용됨).

### 3.7 Spacing — 4px grid

임의 px 값 금지 — 모든 간격은 아래 토큰을 참조한다.

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-space-1` | `4px` | 아이콘↔라벨 터이트 간격 |
| `--sms-space-2` | `8px` | 인라인 그룹, 리스트 아이템 내부 |
| `--sms-space-3` | `12px` | 폼 필드 기본 padding, card-header 세로 |
| `--sms-space-4` | `16px` | 표준 — 카드 padding, 버튼 그룹 간격 |
| `--sms-space-5` | `20px` | 쾌적 — 카드 내부 섹션 |
| `--sms-space-6` | `24px` | 콘텐츠 영역 padding 기본 |
| `--sms-space-8` | `32px` | 카드 그룹 간, 로그인 패널 padding |
| `--sms-space-10` | `40px` | 페이지 내 섹션 간 |
| `--sms-space-12` | `48px` | 메이저 섹션 구분, empty-state 세로 padding |

### 3.8 Layout — Shell / Icon

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-header-height` | `56px` | 앱 헤더 높이 (콘텐츠 영역 높이 계산 기준) |
| `--sms-sidebar-width` | `256px` | 사이드바 너비 |
| `--sms-sidebar-collapsed-width` | `64px` | 접힌 사이드바 너비 |
| `--sms-content-padding` | `var(--sms-space-6)` (24px) | 콘텐츠 영역 padding |
| `--sms-control-height` | `38px` | 입력/셀렉트/datepicker input 기본 높이 |
| `--sms-search-control-width` | `160px` | 검색 입력 폭 |
| `--sms-page-size-width` | `80px` | 페이지 사이즈 셀렉트 폭 |
| `--sms-avatar-md` | `32px` | 헤더 아바타 지름 |
| `--sms-icon-sm` | `0.95rem` | 버튼 내부 아이콘 |
| `--sms-icon-md` | `1rem` | 일반 아이콘, 토스트 아이콘 |
| `--sms-icon-lg` | `1.1rem` | 사이드바 nav 아이콘 |

### 3.9 Radius / Shadow

그림자는 순흑이 아닌 본문 텍스트 색을 입힌 tinted shadow(`rgba(15, 28, 46, …)`)만 사용한다.

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-radius-sm` | `4px` | 입력, 페이지 버튼, 배지 |
| `--sms-radius` | `6px` | 버튼 기본, alert, 로그인 input |
| `--sms-radius-md` | `8px` | 카드, 패널, autocomplete balloon |
| `--sms-radius-lg` | `12px` | 모달, 로그인 패널 |
| `--sms-radius-pill` | `999px` | 필터 칩, 상태 점 |
| `--sms-shadow-xs` | `0 1px 2px rgba(15, 28, 46, 0.04)` | 사이드바/헤더, 표 헤더 |
| `--sms-shadow-sm` | `0 2px 6px rgba(15, 28, 46, 0.06)` | 카드 기본 |
| `--sms-shadow-md` | `0 6px 18px rgba(15, 28, 46, 0.08)` | 드롭다운, autocomplete, date picker layer |
| `--sms-shadow-lg` | `0 16px 40px rgba(15, 28, 46, 0.10)` | 모달, 로그인 패널 |

### 3.10 Motion

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-transition-fast` | `120ms ease-out` | 버튼 press, hover 색 전환, 포커스 glow |
| `--sms-transition-base` | `200ms ease-in-out` | 사이드바 접힘/펼침, spinner overlay |
| `--sms-transition-slow` | `300ms cubic-bezier(0.16, 1, 0.3, 1)` | 모달/팝오버 진입 |

> 모션 규칙: 애니메이션 대상은 `transform` / `opacity` / `box-shadow` / `color` / `background-color`만 허용(`width`/`height`/`padding` 등 금지 — 단 사이드바 collapse는 예외). `prefers-reduced-motion: reduce` 시 `admin-common.css`가 모든 트랜지션을 무효화한다.

### 3.11 z-index scale

`z-index: 9999` 같은 magic number 금지 — 아래 스케일만 사용한다.

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--sms-z-sidebar` | `1020` | 사이드바 |
| `--sms-z-header` | `1030` | 앱 헤더 |
| `--sms-z-dropdown` | `1050` | 드롭다운, autocomplete balloon |
| `--sms-z-popover` | `1060` | 팝오버 |
| `--sms-z-modal` | `1070` | 모달 |
| `--sms-z-toast` | `1080` | 토스트 알림 |
| `--sms-z-overlay` | `1100` | 전역 spinner overlay, skip-link |
| `--sms-z-datepicker` | `3000` | TUI DatePicker layer (모달 위 — in-flight 콘텐츠보다 위에 있어야 함) |

---

## 4. CoreUI 브리지 (`--cui-*` alias)

### 4.1 전역 alias — `admin-common.css`

CoreUI 컴포넌트는 `--cui-*` 변수를 읽어 스타일을 결정한다. `admin-common.css`는 `:root`에서 프로젝트 토큰을 CoreUI 변수에 alias 하며, **이 매핑은 이 파일이 단독 소유**한다(한 파일이 map을 소유하므로 테마 변경 시 이 파일만 수정).

| CoreUI 변수 | alias 대상 | 효과 |
|-------------|-----------|------|
| `--cui-primary` | `var(--sms-primary)` | `.btn-primary`, `.text-primary` 등 primary 계열 전체 |
| `--cui-primary-rgb` | `var(--sms-primary-rgb)` | CoreUI의 primary `rgba()` 계산 |
| `--cui-body-bg` | `var(--sms-bg-app)` | 페이지 배경 |
| `--cui-body-color` | `var(--sms-text-body)` | 본문 텍스트 색 |
| `--cui-body-color-rgb` | `42, 58, 82` (static) | 본문 색 `rgba()` 계산 |
| `--cui-emphasis-color` | `var(--sms-text-strong)` | 강조 텍스트 |
| `--cui-secondary-color` | `var(--sms-text-muted)` | 보조 텍스트 |
| `--cui-tertiary-color` | `var(--sms-text-disabled)` | disabled 텍스트 |
| `--cui-border-color` | `var(--sms-border)` | 전체 구분선 기본 |
| `--cui-secondary-bg` | `var(--sms-bg-muted)` | secondary 배경 |
| `--cui-tertiary-bg` | `var(--sms-bg-surface-alt)` | tertiary 배경 |
| `--cui-light-bg-subtle` | `var(--sms-bg-surface-alt)` | `.bg-light` 계열 |
| `--cui-body-bg-rgb` | `245, 247, 250` (static) | 배경색 `rgba()` 계산 |
| `--cui-font-sans-serif` | `var(--sms-font-sans)` | 전체 폰트 스택 |
| `--cui-font-monospace` | `var(--sms-font-mono)` | 코드 폰트 |
| `--cui-border-radius` | `var(--sms-radius)` | 기본 라디우스 6px |
| `--cui-border-radius-sm` | `var(--sms-radius-sm)` | 소형 라디우스 4px |
| `--cui-border-radius-lg` | `var(--sms-radius-md)` | 대형 라디우스 8px |
| `--cui-box-shadow` | `var(--sms-shadow-sm)` | 기본 그림자 |
| `--cui-box-shadow-sm` | `var(--sms-shadow-xs)` | 소형 그림자 |
| `--cui-box-shadow-lg` | `var(--sms-shadow-lg)` | 대형 그림자 |
| `--cui-danger` / `--cui-danger-rgb` | `var(--sms-danger)` / `var(--sms-danger-rgb)` | `.text-danger`, `.btn-danger`, `.alert-danger` |
| `--cui-success` / `--cui-success-rgb` | `var(--sms-success)` / `var(--sms-success-rgb)` | success 계열 |
| `--cui-warning` / `--cui-warning-rgb` | `var(--sms-warning)` / `var(--sms-warning-rgb)` | warning 계열 |
| `--cui-info` | `var(--sms-info)` | info 계열 |

### 4.2 스코프 한정 alias — `admin-layout.css`

CoreUI 사이드바/버튼 컴포넌트는 전용 변수를 읽는다. 이 변수들은 해당 컴포넌트 스코프 안에서만 `admin-layout.css`가 매핑한다.

```css
/* 사이드바 nav 링크 — .admin-shell .sidebar 스코프 */
.admin-shell .sidebar {
    --cui-sidebar-nav-link-color: var(--sms-text-body);
    --cui-sidebar-nav-link-hover-color: var(--sms-primary);
    --cui-sidebar-nav-link-active-color: var(--sms-primary);
    --cui-sidebar-nav-link-icon-color: var(--sms-text-muted);
    --cui-sidebar-nav-link-hover-icon-color: var(--sms-primary);
    --cui-sidebar-nav-link-active-icon-color: var(--sms-primary);
    --cui-sidebar-nav-link-hover-bg: var(--sms-primary-soft);
    --cui-sidebar-nav-link-active-bg: var(--sms-primary-soft);
}

/* primary 버튼 hover/active — 전역 */
.btn-primary,
.btn-outline-primary {
    --cui-btn-hover-bg: var(--sms-primary-hover);
    --cui-btn-active-bg: var(--sms-primary-active);
}
```

> 사이드바 collapsed 상태에서는 `--cui-sidebar-width`도 `--sms-sidebar-collapsed-width`(64px)로 함께 전환된다.

---

## 5. 레이아웃 시스템 — `admin-layout.css`

관리자 셸(shell)의 **geometry**를 소유한다. 컴포넌트 시각은 CoreUI가 갖고, 이 파일은 2-column 셸 구조와 페이지 타이포 앵커를 담당한다. 모든 값은 `--sms-*` 토큰 참조.

### 5.1 셸 구조

`defaultLayout.html`의 `<body class="admin-shell">` 아래 구조와 대응한다.

```text
body.admin-shell            ← flex row, min-height 100vh
├── .sidebar                ← CoreUI 사이드바 (256px / collapsed 64px)
└── .main-wrapper           ← flex column, flex-grow 1
    ├── header.app-header   ← 56px 고정 높이
    └── .content-area       ← calc(100vh - 56px), 자체 스크롤
```

| 클래스 | 역할 | 핵심 스타일 |
|--------|------|-------------|
| `.admin-shell` | 사이드바 + 메인 2-column 래퍼 | `display: flex; min-height: 100vh; overflow: hidden` |
| `.admin-shell .sidebar` | 좌측 메뉴 | `width/min/max-width: var(--sms-sidebar-width)`, 흰 배경 + `--sms-border-subtle` 우측 border + `--sms-shadow-xs`, width 트랜지션(`--sms-transition-base`) |
| `.admin-shell .sidebar.collapsed` | 접힌 사이드바 | 64px로 축소, 브랜드 약어(`.sidebar-brand-narrow`)만 표시, nav 텍스트/그룹 숨김 |
| `.admin-shell .main-wrapper` | 헤더 + 콘텐츠 컬럼 | `flex-grow: 1; height: 100vh; min-width: 0` |
| `.admin-shell .content-area` | 스크롤되는 업무 콘텐츠 영역 | `height: calc(100vh - var(--sms-header-height))`, `overflow-y: scroll`, `scrollbar-gutter: stable`(스크롤바 등장/소멸에 따른 레이아웃 흔들림 방지), `padding: var(--sms-content-padding)` |

> 사이드바 collapse는 media query가 아니라 **JS 토글**(`defaultLayout.html`의 `sidebarToggleBtn` → `.collapsed` 클래스)로 동작한다. 토글 후 210ms 뒤에 `grid.refreshLayout()`을 호출해 그리드 폭을 재계산한다.

### 5.2 헤더 바

| 클래스 | 역할 |
|--------|------|
| `.header.app-header` | 흰 배경 + 하단 subtle border + `--sms-shadow-xs`, `z-index: var(--sms-z-header)` |
| `.header-bar` | `min-height: var(--sms-header-height)` (56px) |
| `.header-toggle-btn` | 사이드바 토글 — muted 색, hover 시 primary |
| `.header-title` | 현재 화면 제목 — `--sms-fs-body` + semibold + strong 색 |
| `.header-user-text` | 사용자 정보 — `--sms-fs-body-sm` + muted |
| `.header-avatar` | 32px 이니셜 아바타 — `--sms-primary-soft` 바탕 + primary 텍스트 |
| `.header-logout-btn` | muted → hover 시 `--sms-danger` |

### 5.3 사이드바 내부

| 클래스 | 역할 |
|--------|------|
| `.sidebar-userinfo` | 브랜드 아래 로그인 사용자 블록 — `--sms-bg-surface-alt` 바탕, caption 크기 |
| `.sidebar-empty-auth` | 권한 없음 안내 — 중앙 정렬, `--sms-danger` 텍스트 |

### 5.4 페이지 타이포 앵커

| 클래스 | 역할 |
|--------|------|
| `.content-header` | 하단 subtle border + margin — 모든 화면 타이틀 래퍼 |
| `.content-header h2` | 페이지 타이틀 — `--sms-fs-page-title`(20px) + bold + `--sms-lh-tight` + `-0.01em` 자간 |
| `.content-header .content-subtitle` | 부제 — `--sms-fs-body-sm` + muted |

### 5.5 버튼 피드백 & 접근성

CoreUI `.btn` 구조 위에 촉각 피드백을 덧입힌다.

- `.btn` — hover 시 `translateY(-1px)` 부상, active 시 원위치(물리적 클릭감), `--sms-fw-medium`
- `.btn-primary` — `rgba(var(--sms-primary-rgb), 0.18)` 미세 그림자
- `:focus-visible` — `.btn`, `.nav-link`, `.form-control`, `.form-select`, `a`에 `2px solid var(--sms-primary)` 아웃라인
- `.skip-link` — 포커스 시에만 상단에서 내려오는 본문 건너뛰기 링크 (`z-index: var(--sms-z-overlay)`)

### 5.6 반응형 기준

`admin-layout.css` 자체에는 media query가 없다(셸은 고정 geometry + JS collapse). 반응형이 필요한 경우 CoreUI/Bootstrap 표준 breakpoints를 사용한다.

| Breakpoint | 값 |
|------------|-----|
| `sm` | 576px |
| `md` | 768px |
| `lg` | 992px |
| `xl` | 1200px |
| `xxl` | 1400px |

> 프로젝트 CSS에서 실제 media query가 등장하는 곳은 `admin-form-detail.css`의 `@media (max-width: 575.98px)`(라벨/컨트롤 세로 스택 시 min-height 해제)뿐이다.

---

## 6. UI 브리지 — `admin-ui-bridge.css`

CoreUI 컴포넌트, TUI Grid/Picker, Lucide 아이콘 사이를 토큰으로 잇는 브리지다. CoreUI 클래스를 **교체하지 않고 정제만** 한다.

### 6.1 소유 범위 요약

| 섹션 | 대상 | 핵심 내용 |
|------|------|-----------|
| 아이콘 | `.lucide` 계열 | 기본 `--sms-icon-md`, 버튼 안 `--sms-icon-sm` + 우측 8px 여백, nav `--sms-icon-lg`, `.btn-icon`은 여백 제거 |
| Surface primitive | `.surface-card`, `.surface-flat` | 토큰화된 카드 대안. 기존 `.card.shadow-sm.border-0` 패턴도 동일하게 표준화 |
| 폼 컨트롤 | `.form-control`, `.form-select`, `.form-label` | focus 시 primary border + `rgba(primary-rgb, 0.16)` 3px glow, disabled는 muted 바탕 |
| TUI Grid | `.tui-grid-*` | 헤더 `--sms-bg-surface-alt` + caption/semibold, 셀 세로선 제거 + subtle 행 구분선, odd 행 alt 바탕, hover/선택 행 `--sms-primary-soft`, `tabular-nums` |
| 페이지네이션 | `.tui-page-btn` | 28×28, 선택 페이지 primary 배경 + 흰 텍스트, hover 시 alt 바탕 |
| DatePicker | `.scaffold-date-picker-layer` 등 | `z-index: var(--sms-z-datepicker)`(3000) — 콘텐츠 위, 모달 아래 계층 유지 |
| Autocomplete | `.autocomplete-balloon`, `.autocomplete-item` | 토큰화된 popover + 화살표, hover 행 alt 바탕, 코드 mono 폰트 |
| Spinner | `#global-spinner-overlay` | 전역 ajax 오버레이 — `.active`로 표시 전환(opacity 트랜지션) |
| Empty state | `.empty-state`, `.toast-grid-empty` | 데이터 0건/초기 상태. `.toast-grid-empty`는 `#grid` **형제**로 배치(TUI가 `#grid` 내부를 소유하므로), `TuiPageBuilder`가 `.is-visible` 토글 |
| Alert | `.alert-error/.alert-success/.alert-warning/.alert-info` | soft 바탕 + 상태 색 텍스트 + `rgba(*-rgb, 0.2)` border |
| 검색 컨트롤 | `.scaffold-search-control`, `.toast-grid-page-size` 등 | `--sms-search-control-width`(160px), `--sms-page-size-width`(80px) — TUI DatePicker 폭과 연동 |
| 히스토리 필터 | `.history-search-card` 스코프 | 발송이력 화면 전용 컴팩트 필터 툴바 + `.btn-ghost` |
| 메뉴관리 트리 | `.menu-tree`, `.menu-node-*`, `.menu-auth-*` | `/system/menu-manage` 전용 — 좌측 트리 + 우측 상세/권한 매트릭스 |

### 6.2 사용 예 — surface primitive와 empty state

```html
<!-- 카드: CoreUI .card + 프로젝트 .surface-card -->
<div class="card surface-card mb-4">
    <div class="card-header bg-white">발송 이력</div>
    <div class="card-body">…</div>
</div>

<!-- 그리드 empty state: #grid 내부에 정적 HTML로 배치 (TUI 초기화 전 placeholder) -->
<div class="empty-state" role="status">
    <i data-lucide="inbox" class="empty-state-icon" aria-hidden="true"></i>
    <p class="empty-state-title">조회된 데이터가 없습니다</p>
    <p class="empty-state-hint">검색 조건을 변경한 뒤 다시 조회해 주세요.</p>
</div>
```

---

## 7. 상세폼 패턴 — `admin-form-detail.css`

등록/수정(상세폼) 화면의 **공통 행 레이아웃**이다. 화면은 이 클래스를 직접 소비하며, `layout:fragment="css"`에 행 레이아웃을 재선언하지 않는다(화면 전용 스타일만 그곳에 둔다).

라이브 참조 샘플: `static/samples/message-edit.html`, `static/samples/campaign-register.html`

### 7.1 클래스 목록

| 클래스 | 부착 대상 | 역할 |
|--------|-----------|------|
| `.form-detail-row` | 행 래퍼(`.row.g-0`) | `min-height: 68px` + 하단 subtle 구분선. 마지막 행은 구분선 제거(`:last-child`) |
| `.form-detail-label` | 라벨 컬럼(`.col`) | 세로 중앙 정렬, `--sms-bg-surface-alt` 바탕 + strong 텍스트 + semibold, `word-break: keep-all` |
| `.form-detail-control` | 컨트롤 컬럼(`.col`) | 세로 중앙 정렬, 좌우 `--sms-space-4` padding |
| `.form-detail-required` | `<label>` / `<span>` | `::after`로 빨간 `*` 표시 (`--sms-danger`) |
| `.form-detail-counter` | byte 카운터 `<span>` | `.is-over` 상태 시 danger 색 + semibold (JS가 한도 초과 시 토글) |

반응형: `max-width: 575.98px`에서 라벨/컨트롤의 `min-height`가 해제되고 세로 스택에 맞게 padding이 조정된다.

### 7.2 사용 예 — 기본 행 (1 라벨 + 1 컨트롤)

```html
<div class="card surface-card">
    <div class="card-body p-0">
        <div class="row g-0 form-detail-row">
            <div class="col-12 col-sm-2 form-detail-label">
                <label class="form-detail-required" for="messageTitle">메시지 제목</label>
            </div>
            <div class="col-12 col-sm-10 form-detail-control">
                <input type="text" class="form-control" id="messageTitle" name="messageTitle"
                       placeholder="메시지 제목을 입력하세요" required>
            </div>
        </div>
    </div>
</div>
```

### 7.3 사용 예 — 2열 행 (라벨+컨트롤 × 2)

```html
<div class="row g-0 form-detail-row">
    <div class="col-12 col-sm-2 form-detail-label">
        <label class="form-detail-required" for="managerName">담당자명</label>
    </div>
    <div class="col-12 col-sm-4 form-detail-control">
        <input type="text" class="form-control" id="managerName" name="managerName">
    </div>
    <div class="col-12 col-sm-2 form-detail-label">
        <label for="managerEmail">담당자 이메일</label>
    </div>
    <div class="col-12 col-sm-4 form-detail-control">
        <input type="email" class="form-control" id="managerEmail" name="managerEmail">
    </div>
</div>
```

### 7.4 사용 예 — textarea + byte 카운터

긴 콘텐츠 행은 `align-items-start`로 상단 정렬을 풀고, 카운터에 `.form-detail-counter`를 사용한다. 한도 초과 시 JS가 `.is-over`를 추가한다.

```html
<div class="row g-0 form-detail-row">
    <div class="col-12 col-sm-2 form-detail-label align-items-start">
        <label class="form-detail-required" for="messageContent">메시지 내용</label>
    </div>
    <div class="col-12 col-sm-10 form-detail-control align-items-start">
        <div class="w-100">
            <textarea class="form-control" id="messageContent" name="messageContent" rows="11"></textarea>
            <div class="d-flex justify-content-between gap-3 mt-2 text-caption">
                <span>개인정보는 직접 입력하지 말고 승인된 치환 변수를 사용하세요.</span>
                <span class="form-detail-counter text-nowrap" id="message-counter">
                    <strong id="message-byte-count">0</strong> / 2,600byte
                </span>
            </div>
        </div>
    </div>
</div>
```

---

## 8. 인증 페이지 — `auth.css`

### 8.1 스코프

**로그인 페이지(`body.auth-page`)와 로그인 후 홈 정보 페이지(`body.home-page`) 전용.** `defaultLayout`을 사용하지 않고 `admin-common.css` → `auth.css` 순서로 단독 로드된다. 관리자 셸 화면에는 일절 적용되지 않는다.

과거 독립 팔레트(`--bg`, `--text`, `--primary` …)였으나 현재는 `--sms-*` 토큰을 소비해 로그인과 관리자 셸이 **하나의 디자인 시스템**을 공유한다.

### 8.2 구성

| 클래스 | 역할 |
|--------|------|
| `body.auth-page`, `body.home-page` | 전 화면 배경 `--sms-bg-app`, 본문 색/폰트 토큰 적용 |
| `.login-shell`, `.home-shell` | 수직/수평 중앙 정렬 래퍼 |
| `.login-panel`, `.home-panel` | `min(100%, 420px)` 흰 패널 — subtle border + `--sms-radius-lg` + `--sms-shadow-lg` |
| `.brand-block`, `.brand-lockup`, `.brand-name` | 로고 + 브랜드명 록업 |
| `.eyebrow` | primary 색 overline (11px, `0.12em` 자간, 대문자) |
| `h1` | 페이지 타이틀 — `--sms-fs-page-title` + bold |
| `.login-form` | 단일 컬럼 grid, 12px 간격 |
| `.login-form input` | 44px 높이, focus 시 primary border + glow |
| `.login-form button[type="submit"]` | 48px primary CTA — hover 시 부상 + 그림자 강화, active 시 착지 |
| `.login-panel .alert-*` | 토큰화된 인라인 알림 (auth 페이지가 단독 로드되므로 bridge와 별개로 재선언 — resilience 목적) |
| `.login-footer` | 폼 아래 안내 문구 — caption, 상단 subtle border |
| `.home-panel dl/dt/dd` | 로그인 후 사용자 정보 definition list (80px 라벨 컬럼 grid) |

---

## 9. Scaffold 도구 — `scaffold-tool.css`

### 9.1 스코프

**Query Scaffold 화면(`system/scaffold.html`, local 전용 도구) 전용.** `defaultLayout`의 `layout:fragment="css"`로 로드되며, 단 2개 클래스만 정의하는 미세 보정 파일이다.

| 클래스 | 값 | 용도 |
|--------|-----|------|
| `.scaffold-page-subtitle` | `color: var(--cui-secondary-color); font-size: .875rem; margin-left: .625rem` | 화면 타이틀 옆 "(local 전용 화면 생성 도구)" 부제 |
| `.scaffold-result-content` | `max-height: 520px; overflow: auto; font-size: 12px; background: var(--cui-tertiary-bg)` | 생성 결과 코드 출력 영역 (스크롤 + 톤 배경) |

> 이 파일은 bridge 예외 규정(CoreUI 변수 매핑이 필요한 섹션)에 따라 `--cui-*`를 직접 참조한다. 일반 업무 화면 CSS가 이를 흉내 내어 `--cui-*`를 직접 참조해서는 안 된다.

---

## 10. 새 CSS 추가 가이드

### 10.1 화면 전용 스타일 → `layout:fragment="css"`

특정 화면에서만 쓰는 스타일은 해당 템플릿의 `layout:fragment="css"` 블록에 `<link>`(별도 파일) 또는 `<style>`로 넣는다. `defaultLayout.html`의 `<head>` **마지막**에 주입되므로 공통 CSS보다 cascade 우선권이 있다.

```html
<head>
    <title>메시지 등록</title>
    <th:block layout:fragment="css">
        <link rel="stylesheet" th:href="@{/css/message-register.css}">
    </th:block>
</head>
```

이때 규칙:

- 행 레이아웃(`form-detail-*`), 카드, 버튼, 그리드 스타일은 **재선언하지 않는다** — 공통 자산 재발명 금지(`.claude/rules/thymeleaf.md`).
- 색/간격/라디우스/그림자는 반드시 `--sms-*` 토큰 참조. raw hex 금지.
- 새 파일은 `static/css/`에 배치하고 로컬 경로(`th:href="@{/css/…}"`)로만 참조. CDN 금지.

### 10.2 공통 CSS에 추가하는 경우

아래 기준에 해당하면 화면 CSS가 아니라 공통 파일에 추가한다.

| 상황 | 추가 위치 |
|------|-----------|
| 새 색/간격/폰트 크기/그림자가 필요 | `admin-common.css` `:root`에 **토큰부터** 추가 후 참조 |
| 2개 이상 화면이 같은 패턴을 반복 (예: `card shadow-sm border-0` 남발) | `admin-ui-bridge.css`에 토큰화 클래스로 표준화 |
| 셸(사이드바/헤더/콘텐츠 영역) geometry 변경 | `admin-layout.css` (셸 토큰 변경 시 `admin-common.css`와 동시 수정) |
| 등록/수정 행 레이아웃 변경 | `admin-form-detail.css` |
| 로그인 페이지 전용 | `auth.css` |

### 10.3 체크리스트

1. 기존 토큰으로 표현 가능한가? → 가능하면 토큰 사용, 새 값이 필요하면 `admin-common.css`에 토큰 추가
2. CoreUI가 이미 소유한 컴포넌트 시각인가? → 클래스 교체 대신 변수 주입/정제
3. 한 화면 전용인가? → `layout:fragment="css"`. 여러 화면 공통인가? → 공통 파일
4. `--cui-*`를 직접 참조하려 하는가? → bridge 예외가 아니면 `--sms-*`로 전환
5. z-index가 필요한가? → `--sms-z-*` 스케일 사용, magic number 금지

---

## 11. DESIGN.md 참조 — 토큰 계층 설계 근거

토큰 계층의 설계 원문은 루트 `DESIGN.md`에 있다. 문서와 코드가 충돌하면 **`DESIGN.md`의 설계 원문을 우선**하고, 이 매뉴얼과 CSS를 즉시 갱신한다.

### 11.1 계층 설계 요약 (DESIGN.md 기준)

- **정체성**: "차분한 업무 콘솔" — 정보 밀도가 높은 발송이력/통계/관리 화면이므로 시각적 소음을 최소화하고 데이터가 읽히게 한다. single accent(deep corporate blue `#2557d6`), 강조는 색이 아닌 tonal shift와 타이포 무게로. 화려한 그라데이션·네온·블러 글래스모피즘 금지.
- **토큰 단일 소스**: 모든 색은 `admin-common.css` `:root`의 `--sms-*`로 정의하고, 동일 파일에서 `--cui-*`로 alias 해 모든 CoreUI 컴포넌트가 일관된 팔레트/타이포/간격을 따르게 한다. 다크모드 없음(사내 관리자 라이트 테마).
- **참조 규칙**: 템플릿/컴포넌트/bridge CSS는 `--sms-*`만 참조. `--cui-*` 직접 참조는 alias 파일과 bridge 매핑 섹션에서만 허용.
- **CoreUI 철학**: CoreUI가 소유한 컴포넌트 시각은 클래스 교체 없이 변수 주입으로 재스타일링. 프로젝트 CSS 주석에 "CoreUI owns X" 철학을 명시.
- **한국어 보존**: 한글 라벨/버튼/메시지 원문 유지. CJK 클리핑 방지를 위해 `line-height ≥ 1.45`, 제목/메시지에 `word-break: keep-all`.
- **폐쇄망 제약**: 웹폰트/CDN/외부 이미지 로딩 금지. OS 내장 폰트와 기존 번들 자원만 사용.

### 11.2 로드 순서 원문

> 로드 순서: `coreui.min.css` → `admin-common.css` → `admin-layout.css` → `admin-ui-bridge.css`
> — `DESIGN.md` Implementation Notes

`admin-form-detail.css`는 bridge 이후, 화면별 CSS(`layout:fragment="css"`)는 가장 마지막에 로드된다(§2.1 참고).

### 11.3 함께 참고

| 문서 | 내용 |
|------|------|
| `DESIGN.md` | 디자인 시스템 원문 (Atmosphere, Color, Typography, Spacing, Components, Motion, Depth) |
| `docs/base/screen-convention.md` | 화면 구성 규약 (목록/상세폼/모달 패턴) |
| `.claude/rules/thymeleaf.md` | Thymeleaf/UI 실행 규칙 (공통 자산 재발명 금지, 로컬 참조만) |
| [`common-js_manual.md`](./common-js_manual.md) | 공통 JS 모듈 (이 CSS들이 결합되는 화면 JS 측) |
| [`tui_manual.md`](./tui_manual.md) | TUI Grid/DatePicker 사용법 (bridge CSS의 상대측) |
