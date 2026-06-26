# omoWorker (SMS전송시스템) Design System

> 관리자 화면 디자인 시스템. CoreUI 5 기반 위에 프로젝트 고유 토큰(`--sms-*`)을 얹고,
> CoreUI의 `--cui-*` 변수를 우리 토큰으로 alias 하여 모든 CoreUI 컴포넌트가
> 일관된 팔레트/타이포/간격을 따르도록 한다. 다크모드 없음(사내 관리자 라이트 테마).

---

## 1. Atmosphere & Identity

**"차분한 업무 콘솔"**. 정보 밀도가 높은 발송이력/통계/관리 화면이므로 시각적 소음을 최소화하고
데이터 자체가 읽히도록 한다. 신뢰를 주는 **deep corporate blue**(`#2557d6`)를 단일 액센트로 쓰고,
강조가 필요한 곳은 색이 아닌 **tonal shift**(미세한 명도 차)와 **타이포 그 자체의 무게**로 표현한다.

Signature: 흰 카드가 약간의 보랏빛을 띤 회색 앱 배경(`#f5f7fa`) 위에 떠 있는,
**표( table ) 중심의 업무 콘솔**. 그림자는 아주 옅고 선( border )은 얕다.
화려한 그라데이션·네온·블러 글래스모피즘은 금지.

---

## 2. Color

모든 색은 `admin-common.css` 의 `:root` 에 `--sms-*` 토큰으로 정의하고, 동일 파일에서
CoreUI 변수(`--cui-primary`, `--cui-body-bg`, `--cui-border-color` …)로 alias 한다.
컴포넌트/템플릿은 **`--sms-*` 토큰만** 참조한다(`--cui-*` 직접 참조는 bridge CSS 에서만 허용).

### Palette (Light only)

| Role | Token | Value | Usage |
|------|-------|-------|-------|
| App background | `--sms-bg-app` | `#f5f7fa` | 콘텐츠 영역 전체 배경(약간 쿨한 회색) |
| Surface | `--sms-bg-surface` | `#ffffff` | 카드, 패널, 모달 |
| Surface alt | `--sms-bg-surface-alt` | `#f9fafc` | 표 odd 행, hover 행 아래 바탕 |
| Surface muted | `--sms-bg-muted` | `#eef1f6` | 입력 disabled, 탭 비활성 |
| Primary | `--sms-primary` | `#2557d6` | CTA, 링크, 포커스 링, 활성 메뉴 |
| Primary hover | `--sms-primary-hover` | `#1e47b0` | 버튼 hover |
| Primary active | `--sms-primary-active` | `#1a3e9c` | 버튼 press |
| Primary soft | `--sms-primary-soft` | `#e8efff` | 선택된 행, 배지(badge) 바탕 |
| Danger | `--sms-danger` | `#d93b3b` | 삭제, 오류, 파괴적 액션 |
| Danger soft | `--sms-danger-soft` | `#fdeaea` | 인라인 오류 메시지 바탕 |
| Success | `--sms-success` | `#1f8a4c` | 완료, 성공 배지 |
| Success soft | `--sms-success-soft` | `#e8f6ed` | 성공 토스트/알림 바탕 |
| Warning | `--sms-warning` | `#c8780a` | 경고, 보류 |
| Warning soft | `--sms-warning-soft` | `#fdf3e0` | 경고 배지 바탕 |
| Info | `--sms-info` | `#2a72c4` | 정보성 알림 |
| Text strong | `--sms-text-strong` | `#0f1c2e` | 페이지 타이틀, 주요 수치 |
| Text body | `--sms-text-body` | `#2a3a52` | 본문 |
| Text muted | `--sms-text-muted` | `#6b7a90` | 라벨, 캡션, 보조 텍스트 |
| Text disabled | `--sms-text-disabled` | `#a3acbd` | disabled 라벨 |
| Border | `--sms-border` | `#dfe3eb` | 카드/입력/표 셀 구분선 |
| Border strong | `--sms-border-strong` | `#c4cbda` | 포커스 인디케이터 보조 |
| Border subtle | `--sms-border-subtle` | `#eef1f6` | 미세 구분(섹션 내부) |

### Rules

- Surface 계층은 미세한 명도 차(`#ffffff` → `#f9fafc` → `#f5f7fa`)로 깊이를 표현. 그림자는 덜 쓴다.
- `--sms-primary` 는 **오직 인터랙티브 요소**(버튼/링크/포커스/활성 메뉴/선택 행)에만 쓴다. 장식용 금지.
- `--sms-danger` / `--sms-success` / `--sms-warning` 는 상태 표시에만. 배경에는 대응하는 `*-soft` 토큰을 쓴다.
- 템플릿/컴포넌트 CSS 에서 raw hex 를 쓰지 않는다. 새 색이 필요하면 먼저 이 표에 토큰을 추가한다.

---

## 3. Typography

### Font Stack

폐쇄망 대상이라 웹폰트 로딩 없이 OS 내장 한국어 폰트를 우선한다.

```css
--sms-font-sans:
  "Apple SD Gothic Neo", "Malgun Gothic", "Noto Sans KR",
  -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif;
--sms-font-mono: "SF Mono", "Menlo", "Consolas", monospace;
```

- macOS: Apple SD Gothic Neo / Windows: Malgun Gothic / Linux fallback: Noto Sans KR(설치된 경우)
- 최대 폰트 패밀리 2개(sans / mono). 추가 도입 금지.

### Type Scale

| Level | Token | Size | Weight | Line Height | Usage |
|-------|-------|------|--------|-------------|-------|
| Page title | `--sms-fs-page-title` | 1.25rem (20px) | 700 | 1.3 | `content-header h2`(각 화면 타이틀) |
| Section | `--sms-fs-section` | 1rem (16px) | 600 | 1.4 | 카드 헤더, 섹션 제목 |
| Body | `--sms-fs-body` | 0.9375rem (15px) | 400 | 1.55 | 기본 본문, 폼 라벨 |
| Body sm | `--sms-fs-body-sm` | 0.8125rem (13px) | 400 | 1.5 | 표 셀, 보조 텍스트 |
| Caption | `--sms-fs-caption` | 0.75rem (12px) | 500 | 1.45 | 메타데이터, 사이드바 사용자 정보 |
| Overline | `--sms-fs-overline` | 0.6875rem (11px) | 700 | 1.3 | eyebrow(로그인 등) |

Weights: `--sms-fw-regular: 400`, `--sms-fw-medium: 500`, `--sms-fw-semibold: 600`, `--sms-fw-bold: 700`.

### Rules

- 본문은 13px(0.8125rem) 아래로 내리지 않는다(가독성).
- 한국어 행간은 최소 1.45(CJK 클리핑 방지).
- 제목에는 음수 자간을 주지 않는다(한국어는 영문처럼 tight 하면 안 됨).
- 숫자가 많은 표/통계에는 `font-variant-numeric: tabular-nums` 적용.

---

## 4. Spacing & Layout

4px 기반 스케일. 임의 px 값 금지 — 모든 간격은 아래 토큰을 참조한다.

| Token | Value | Usage |
|-------|-------|-------|
| `--sms-space-1` | 4px | 아이콘↔라벨 터이트 |
| `--sms-space-2` | 8px | 인라인 그룹, 리스트 아이템 내부 |
| `--sms-space-3` | 12px | 폼 필드 기본 padding |
| `--sms-space-4` | 16px | 표준 — 카드 padding, 버튼 그룹 간격 |
| `--sms-space-5` | 20px | 쾌적 — 카드 내부 섹션 |
| `--sms-space-6` | 24px | 카드 padding 기본 |
| `--sms-space-8` | 32px | 카드 그룹 간 |
| `--sms-space-10` | 40px | 페이지 내 섹션 간 |
| `--sms-space-12` | 48px | 메이저 섹션 구분 |

### Layout Shell

| Token | Value | Usage |
|-------|-------|-------|
| `--sms-header-height` | 56px | 앱 헤더 높이 |
| `--sms-sidebar-width` | 256px | 사이드바 너비 |
| `--sms-sidebar-collapsed-width` | 64px | 접힌 사이드바 |
| `--sms-content-padding` | 24px | 콘텐츠 영역 padding(`--sms-space-6`) |
| `--sms-control-height` | 38px | 입력/셀렉트 기본 높이 |
| `--sms-search-control-width` | 160px | 검색 입력 폭 |
| `--sms-page-size-width` | 80px | 페이지 사이즈 셀렉트 폭 |

### Grid

- 콘텐츠 영역은 `container-fluid` + 24px padding. 고정 max-width 없이 가용 폭 사용(관리자 표 중심).
- 검색 카드: `row + col-auto + g-3 flex-wrap`(기존 패턴 유지).
- Breakpoints: CoreUI/Bootstrap 표준 사용(`sm 576 / md 768 / lg 992 / xl 1200 / xxl 1400`).

---

## 5. Components

CoreUI 클래스(`.btn`, `.card`, `.form-control`, `.form-select` …)를 그대로 사용하되,
모든 색/간격/라디우스는 `--sms-*` 토큰을 통해 CoreUI 변수에 주입된다. 프로젝트는 **bridge 클래스**로
패턴을 문서화한다.

### Button

CoreUI `.btn` + variant(`.btn-primary`, `.btn-outline-primary`, `.btn-secondary`, `.btn-danger`,
`.btn-ghost`)를 그대로 쓴다. 색은 `--sms-primary` / `--sms-danger` 가 CoreUI alias 로 주입됨.

- **Structure**: `<button class="btn btn-primary px-4"><i data-lucide="search"></i><span>조회</span></button>`
- **Variants**:
  - `btn-primary` — 주 액션(조회, 저장). 한 화면에 원칙적으로 1개.
  - `btn-outline-primary` — 보조 생성 액션(등록).
  - `btn-secondary` — 초기화, 취소.
  - `btn-danger` — 삭제, 파괴적 액션.
  - `btn-ghost`(`.btn.btn-ghost`) — 아이콘 전용/희미한 액션(행 내부 수정·삭제 등).
- **Icon spacing**: `.btn .lucide { margin-right: var(--sms-space-2); }`(아이콘 단독 버튼은 제외).
- **States**: hover/active/focus/disabled — CoreUI 기본 + token 주입. 추가로 `:active { transform: translateY(1px) }` 로 물리적 클릕 느낌.
- **A11y**: `:focus-visible` → `outline: 2px solid var(--sms-primary); outline-offset: 2px`.

### Card / Panel

- **Structure**: `<div class="card surface-card">` 또는 기존 `class="card shadow-sm border-0"` 패턴.
- **Bridge class**: `.surface-card` — `background: var(--sms-bg-surface); border: 1px solid var(--sms-border-subtle); border-radius: var(--sms-radius-md); box-shadow: var(--sms-shadow-sm)`.
- **Variants**:
  - `.surface-card`(기본) — 카드/패널.
  - `.surface-flat` — 카드 안의 하위 섹션(구분선만, 그림자 없음).
- **Spacing**: card-body 기본 `padding: var(--sms-space-4)`(= CoreUI 기본 유지), card-header `padding: var(--sms-space-3) var(--sms-space-4)`.
- **Header**: `card-header.bg-transparent` + `--sms-fs-section`. 좌우 정렬은 flex.

### Data Table (TUI Grid)

TUI Grid 는 bridge CSS(`admin-ui-bridge.css`)로 토큰 주입.

- Header 배경: `--sms-bg-surface-alt`, weight 600, size `--sms-fs-body-sm`.
- Body 셀: size `--sms-fs-body-sm`, color `--sms-text-body`, 행 높이 36~40px.
- Odd 행: `--sms-bg-surface-alt`.
- Hover 행: `var(--sms-primary-soft)`(= `rgba(--sms-primary-rgb, 0.08)`).
- 선택 행: `var(--sms-primary-soft)` + 좌측 2px `--sms-primary` 바.
- 페이지네이션: `.tui-page-btn` 28×28, 선택 페이지는 `--sms-primary` 배경.

### Input / Select

- `.form-control`, `.form-select` CoreUI 유지. 높이 `--sms-control-height`.
- Border: `--sms-border`. Focus: border `--sms-primary` + box-shadow `0 0 0 3px var(--sms-primary-soft)`.
- Disabled: 배경 `--sms-bg-muted`, color `--sms-text-disabled`.
- Label: `--sms-fs-body`, weight `--sms-fw-medium`, color `--sms-text-body`.

### Search Card (scaffold-search-card / history-search-card)

- `.surface-card` + `padding: var(--sms-space-4) var(--sms-space-5)`.
- 라벨-입력 쌍은 `row g-3 flex-wrap align-items-center` 그리드(기존 패턴).
- 우측 액션 그룹: `ms-auto d-flex gap-2`.

### Empty State

데이터 0건/초기 상태용. TUI grid 초기화 전 `<div id="grid">` 안에 정적 HTML 로 들어간다.

- **Structure**:
  ```html
  <div class="empty-state" role="status">
    <i data-lucide="inbox" class="empty-state-icon" aria-hidden="true"></i>
    <p class="empty-state-title">조회된 데이터가 없습니다</p>
    <p class="empty-state-hint">검색 조건을 변경한 뒤 다시 조회해 주세요.</p>
  </div>
  ```
- **Tokens**: icon `--sms-text-disabled`(48px), title `--sms-fs-section` `--sms-fw-semibold` `--sms-text-body`, hint `--sms-fs-body-sm` `--sms-text-muted`.
- **Spacing**: padding `var(--sms-space-12) var(--sms-space-6)`, gap `--sms-space-2`.

### Error State (독립 화면)

`error/error.html`. 미인증 접근도 표시되므로 공통 레이아웃 없이 CoreUI CSS + `admin-common.css` 토큰만 사용.

- Layout: center-aligned single card on `--sms-bg-app`.
- Status code: `--sms-fs-display`(48px) weight 700, color `--sms-danger`(5xx)/`--sms-warning`(4xx) — 템플릿에서 선택.
- Lucide 아이콘(`alert-triangle` / `cloud-off`)을 status 위에 배치.
- 액션: primary(`홈으로`) + secondary(`이전 화면`).

---

## 6. Motion & Interaction

| Type | Duration | Easing | Usage |
|------|----------|--------|-------|
| Micro | 120ms | `ease-out` | 버튼 press, 토글 |
| Standard | 200ms | `ease-in-out` | 사이드바 접힘/펼침, hover 전환 |
| Emphasis | 300ms | `cubic-bezier(0.16, 1, 0.3, 1)` | 모달/팝오버 진입 |

### Tokens

```css
--sms-transition-fast: 120ms ease-out;
--sms-transition-base: 200ms ease-in-out;
--sms-transition-slow: 300ms cubic-bezier(0.16, 1, 0.3, 1);
```

### Rules

- `transform` / `opacity` / `box-shadow` / `color` / `background-color` 만 애니메이션.
  `width` / `height` / `padding` / `margin` / `top` / `left` 애니메이션 금지(GPU 비동기화).
- 사이드바 collapse/expand 만 예외적으로 `width` 트랜지션(기존 동작 보존).
- 모든 인터랙티브 요소는 hover/active/focus state 를 가진다.
- `:active` 시 `transform: translateY(1px)` 로 물리적 클릭 피드백.
- `prefers-reduced-motion: reduce` 시 트랜지션 무효화.

---

## 7. Depth & Surface

**Mixed(tonal-shift + very subtle shadow)**. 선과 미세한 명도 차를 주축으로 하되,
카드/모달/팝오버에는 옅은 색이 입힌 그림자(`rgba(15, 28, 46, …)`, 본문 텍스트 색의 투명도)를 더한다.
순흑 그림자 금지.

| Level | Token | Value | Usage |
|-------|-------|-------|-------|
| xs | `--sms-shadow-xs` | `0 1px 2px rgba(15, 28, 46, 0.04)` | 표 헤더, sticky 행 |
| sm | `--sms-shadow-sm` | `0 2px 6px rgba(15, 28, 46, 0.06)` | 카드(기본) |
| md | `--sms-shadow-md` | `0 6px 18px rgba(15, 28, 46, 0.08)` | 드롭다운, date picker layer |
| lg | `--sms-shadow-lg` | `0 16px 40px rgba(15, 28, 46, 0.10)` | 모달, 에러 카드 |

Border radius:

| Token | Value | Usage |
|-------|-------|-------|
| `--sms-radius-sm` | 4px | 입력, 페이지 버튼, 배지 |
| `--sms-radius` | 6px | 버튼 기본 |
| `--sms-radius-md` | 8px | 카드, 패널 |
| `--sms-radius-lg` | 12px | 모달, 큰 팝오버 |
| `--sms-radius-pill` | 999px | 필터 칩, 상태 점 |

---

## Implementation Notes

- **Token layer**: `src/main/resources/static/css/admin-common.css` 에 `:root { --sms-*: … }` 정의.
  같은 파일에서 CoreUI 변수(`--cui-primary`, `--cui-body-bg`, `--cui-border-color`, `--cui-font-sans-serif` …)를
  `--sms-*` 토큰으로 alias. 로드 순서: `coreui.min.css` → `admin-common.css` → `admin-layout.css` → `admin-ui-bridge.css`.
- **Token 참조 규칙**: 템플릿/컴포넌트/bridge CSS는 `--sms-*` 만 참조. `--cui-*` 직접 참조는 `admin-common.css` alias 와
  CoreUI 변수 매핑이 필요한 bridge 섹션에서만 허용.
- **CoreUI 철학 유지**: CoreUI 가 소유한 컴포넌트 시각(.btn, .card, .form-control, .modal)은 클래스를 교체하지 않고
  변수 주입으로 재스타일링한다. 프로젝트 CSS 주석에 "CoreUI owns X" 철학을 명시한다.
- **한국어 보존**: 모든 한글 라벨/버튼/메시지는 그대로. CJK 클리핑 방지를 위해 line-height ≥ 1.45, word-break: keep-all(제목/메시지).
- **icon**: Lucide 유지(`data-lucide`). emoji 아이콘 금지.
- **폐쇄망 제약**: 웹폰트/CDP/외부 이미지 로딩 금지. OS 내장 폰트와 기존 번들 자원만 사용.
