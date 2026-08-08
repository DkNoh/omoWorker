# Lucide 아이콘 매뉴얼 (프로젝트 한정 — 커스텀 서브셋)

> 대상 파일: `src/main/resources/static/lib/lucide.js` — **공식 lucide가 아니다.**
>
> ⚠️ 이 파일은 프로젝트가 직접 작성한 **5.8KB 커스텀 서브셋**이다. 공식 lucide 배포본(`lucide.min.js`)과 API·아이콘 목록이 다르며, **공식 `.min`으로 교체가 금지**되어 있다.
>
> 관련 문서: [`common-js_manual.md` 3장](./common-js_manual.md) (`Notify.refreshIcons`), `src/main/resources/static/lib/MANIFEST.md`

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [`createIcons()` 동작 방식](#3-createicons-동작-방식)
4. [사용 가능한 아이콘 목록 (27개)](#4-사용-가능한-아이콘-목록-27개)
5. [프로젝트 사용 패턴](#5-프로젝트-사용-패턴)
6. [아이콘 추가 방법](#6-아이콘-추가-방법)
7. [주의사항 및 프로젝트 특이사항](#7-주의사항-및-프로젝트-특이사항)
8. [참조](#8-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 파일 | `static/lib/lucide.js` |
| 성격 | **프로젝트 자체 작성 커스텀 서브셋** (공식 lucide 아님) |
| 크기 | 약 5.8KB (5,910 bytes) |
| 라이선스 | ISC (원본 lucide 라이선스. SVG 경로 데이터가 원본에서 유래) / 구현체는 프로젝트 소유 |
| 버전 | 없음 (공식 버전 체계와 무관) |
| 용도 | `data-lucide` 속성 요소를 인라인 SVG 아이콘으로 변환 |

`MANIFEST.md` 등재 행:

```text
| lucide.js | (커스텀 서브셋) | - | 프로젝트 자체 작성 | ISC(원본 lucide) | (기존) | 공식 lucide 아님. 사용 아이콘만 손으로 추린 5.8KB 구현 + 자체 createIcons(). 공식 .min으로 교체 금지 |
```

왜 커스텀 서브셋인가:

- 공식 lucide 전체 번들은 수백 KB(아이콘 수백 개)지만, 이 프로젝트가 쓰는 아이콘은 30개 내외다.
- 폐쇄망 배포 환경에서 불필요한 자산 반입을 줄이기 위해 사용 아이콘만 손으로 추렸다.
- `MANIFEST.md` 규칙상 프로젝트 자체 작성 소스는 vendoring/minify 대상이 아니다 — 이 파일은 "라이브러리"가 아니라 프로젝트 코드에 가깝다.

---

## 2. 프로젝트 로드 방식

### 2.1 스크립트 태그 위치

`src/main/resources/templates/defaultLayout.html`의 `<head>` 내부:

```html
<script src="/lib/dayjs.min.js"></script>
<script src="/lib/ko.js"></script>
<script src="/lib/lucide.js"></script>        <!-- 24행 -->
<script src="/lib/imask.min.js"></script>
```

### 2.2 전역 변수

```javascript
window.lucide = {
    createIcons,   // (options?) => void — [data-lucide] 요소를 SVG로 교체
    icons          // { [name]: [[tag, attrs], ...] } — 아이콘 경로 데이터 맵
};
```

- UMD가 아닌 단순 IIFE로 `window.lucide`에 직접 할당한다.
- `icons` 맵이 공개되어 있으므로 `window.lucide.icons['search']`처럼 존재 여부를 확인할 수 있다.

---

## 3. `createIcons()` 동작 방식

### 3.1 기본 흐름

```javascript
function createIcons(options) {
    const attrs = options && options.attrs ? options.attrs : {};
    document.querySelectorAll('[data-lucide]').forEach(element => {
        const name = element.getAttribute('data-lucide');
        element.replaceWith(createSvg(name, element, attrs));
    });
}
```

1. 문서 전체에서 `data-lucide` 속성을 가진 **모든 요소**를 찾는다 (`<i>`에 한정되지 않음).
2. 각 요소를 새로 생성한 `<svg>`로 **치환**한다 (`replaceWith` — 원본 요소는 DOM에서 제거됨).
3. `options.attrs`를 전달하면 모든 SVG에 추가 속성을 병합한다 (프로젝트에서는 사용하지 않음).

### 3.2 생성되는 SVG

```html
<!-- 변환 전 -->
<i data-lucide="search" class="nav-icon" aria-hidden="true"></i>

<!-- 변환 후 -->
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
     fill="none" stroke="currentColor" stroke-width="2"
     stroke-linecap="round" stroke-linejoin="round"
     aria-hidden="true" data-lucide="search" class="lucide lucide-search nav-icon">
  <circle cx="11" cy="11" r="8"></circle>
  <path d="m21 21-4.3-4.3"></path>
</svg>
```

속성 처리 규칙:

| 속성 | 처리 |
|---|---|
| 기본 8종 (`xmlns`, `width`, `height`, `viewBox`, `fill`, `stroke`, `stroke-width`, `stroke-linecap/join`) | 항상 설정 |
| `class` | 재구성: `lucide` + `lucide-{name}` + **원본 class** (보존됨) |
| `data-lucide` | SVG에 다시 설정 (재변환·아이콘 교체 가능) |
| 그 외 (`id`, `aria-hidden`, `style` 등) | 원본에서 그대로 복사 |

### 3.3 알 수 없는 아이콘 이름 → `file` 폴백

```javascript
(icons[name] || icons.file).forEach(...)
```

`icons` 맵에 없는 이름을 요청하면 **오류 없이 `file` 아이콘으로 렌더링된다.** 콘솔 경고도 없다. 화면에 서류 아이콘이 잘못 나오면 아이콘 이름 오타 또는 미등록을 의심한다.

---

## 4. 사용 가능한 아이콘 목록 (27개)

`lucide.js`의 `icons` 맵에 등록된 전체 목록이다. **이 목록에 없는 이름은 모두 `file`로 폴백된다.**

| 분류 | 아이콘 이름 |
|---|---|
| 검색/갱신 | `search`, `rotate-ccw`, `refresh-cw` |
| 파일/편집 | `file`, `copy`, `save`, `pencil`, `trash-2` |
| 상태/알림 | `check`, `circle-check`, `circle-x`, `info`, `triangle-alert` |
| 뷰/액션 | `eye`, `download`, `x`, `sparkles`, `send` |
| 레이아웃/내비 | `panel-left-close`, `panel-left-open`, `home`, `list` |
| 메뉴/업무 | `message-square-search`, `sliders-horizontal`, `users`, `chart-no-axes-combined`, `log-out` |

### 4.1 템플릿에서 쓰지만 서브셋에 없는 아이콘 (현재 `file`로 렌더링됨)

아래 이름은 화면에 선언되어 있으나 서브셋에 없어 **`file` 아이콘으로 대체 표시**된다. 정상 표시가 필요하면 6장 방법으로 추가한다.

| 아이콘 이름 | 사용처 |
|---|---|
| `arrow-left` | `error/error.html` |
| `folder-tree` | `system/menu-tree.html`, `system/menu-manage.html` |
| `plus` | `system/menu-manage.html`, `basic/notice.html`, `sms/customer-search.html` |
| `rotate-cw` | `system/menu-manage.html` |
| `settings-2` | `system/menu-manage.html` |
| `shield-check` | `system/menu-manage.html` |
| `mouse-pointer-click` | `system/menu-manage.html` |
| `external-link` | `basic/notice.html` |
| `inbox` | `fragments/toast-grid.html` |
| `chevron-right`, `folder`, `circle` | `js/system/menu-manage.js` (동적 생성) |

---

## 5. 프로젝트 사용 패턴

### 5.1 정적 아이콘 (템플릿 선언)

버튼·사이드바·헤더에 선언한다. **페이지 로드 시 `notify.js`가 `DOMContentLoaded`에서 `refreshIcons()`를 호출**하므로, `defaultLayout`을 쓰는 모든 페이지의 정적 아이콘은 자동으로 렌더링된다. 화면에서 별도 초기화 불필요.

```html
<!-- sms/history.html -->
<button type="button" id="btn-search" class="btn btn-primary px-4">
    <i data-lucide="search" aria-hidden="true"></i><span>조회</span>
</button>

<!-- fragments/sidebar.html — 메뉴명 기반 조건부 아이콘 -->
<i th:case="'SMS발송조회'" data-lucide="message-square-search" class="nav-icon"></i>
```

### 5.2 동적 아이콘 (JS로 DOM 삽입 후 갱신)

`innerHTML`로 `data-lucide` 요소를 넣은 뒤에는 **반드시 아이콘을 다시 그려야 한다.** 세 가지 동등한 호출이 있다:

```javascript
Notify.refreshIcons();          // 권장 (lucide 존재 가드 내장)
CommonUtils.refreshIcons();     // Notify.refreshIcons의 하위 호환 alias
window.lucide.createIcons();    // 직접 호출 (menu-manage.js 패턴)
```

실제 사용 예 (`system/menu-manage.js` — 트리 렌더 후):

```javascript
const renderTree = () => {
    // ... DOM 조립 (data-lucide="chevron-right" 등 포함) ...
    if (window.lucide && window.lucide.createIcons) {
        window.lucide.createIcons();
    }
};
```

`notify.js`도 toast·alert 모달을 동적 생성한 뒤 내부적으로 `refreshIcons()`를 호출한다 — toast의 `circle-check`/`triangle-alert` 아이콘이 이 경로로 그려진다.

### 5.3 아이콘 교체 (속성 재설정 + 재변환)

이미 변환된 `<svg>`도 `data-lucide`와 `id`를 보존하므로, 속성만 바꾸고 다시 변환하면 아이콘이 교체된다 (`defaultLayout.html` 사이드바 토글):

```javascript
const toggleIcon = document.getElementById('toggleIcon');  // 변환 후에도 id 유지
toggleIcon.setAttribute('data-lucide', isCollapsed ? 'panel-left-open' : 'panel-left-close');
CommonUtils.refreshIcons();  // svg가 새 svg로 교체됨
```

### 5.4 스타일링

- `stroke="currentColor"` — **부모의 텍스트 색상을 상속**한다. 버튼 안의 아이콘이 버튼 글자색과 자동으로 일치한다.
- 기본 크기는 `width/height="24"` 속성. CSS로 덮어쓴다:

```css
.layout-icon { width: 20px; height: 20px; }
.nav-icon    { width: 18px; height: 18px; }
```

- 클래스 `lucide`, `lucide-{name}`이 항상 붙으므로 이름별 타겟팅도 가능하다.

---

## 6. 아이콘 추가 방법

서브셋에 없는 아이콘이 필요하면 `lucide.js`의 `icons` 객체에 **직접 추가**한다. 공식 번들로 교체하지 않는다.

```javascript
// lucide.js의 icons 객체에 추가 (경로 데이터는 https://lucide.dev 에서 확인 — ISC 라이선스)
const icons = {
    // ... 기존 아이콘 ...
    'plus': [['path', { d: 'M5 12h14' }], ['path', { d: 'M12 5v14' }]],
};
```

규칙:

1. 키는 공식 lucide 아이콘 이름(kebab-case)을 그대로 쓴다 — 템플릿의 `data-lucide` 값과 일치해야 한다.
2. 값은 `[태그명, 속성객체]` 배열이다. 태그는 `path`, `circle`, `rect`, `line` 등을 쓴다.
3. 24×24 뷰박스 기준 경로 데이터만 넣는다 (공식 아이콘의 `svg` 내부 요소 그대로).
4. 추가 후 `createIcons()` 재호출(또는 페이지 새로고침)로 반영된다.
5. 이 파일은 프로젝트 자체 작성 소스이므로 `MANIFEST.md`의 버전 갱신 대상이 아니다 (비고란의 "커스텀 서브셋" 설명만 유지).

---

## 7. 주의사항 및 프로젝트 특이사항

### 7.1 공식 `.min`으로 교체 금지

`MANIFEST.md`에 명시된 규칙이다. 공식 번들은 수백 KB이고 폐쇄망 반입·검수 비용이 크다. 아이콘이 더 필요하면 6장 방식으로 서브셋에 추가한다. 교체가 꼭 필요하면 `MANIFEST.md` 규칙에 따라 동일 정책 검토 + 비고 기록 + 승인 절차를 거친다.

### 7.2 공식 lucide 문서의 API가 적용되지 않는다

이 파일은 공식 lucide와 **API가 다르다**:

- 공식의 `createIcons({ icons, nameAttr, attrs, ... })` 옵션 중 이 구현은 `attrs`만 지원한다.
- 공식의 `createElement()`, 개별 아이콘 노드 API, `LucideIcon` 컴포넌트 등은 존재하지 않는다.
- 공식 문서(https://lucide.dev)는 **아이콘 이름·경로 데이터를 확인할 때만** 참고한다. API 문서는 참고하지 않는다.

### 7.3 `createIcons()`는 요소를 교체한다 — 원본 참조와 리스너가 사라진다

`replaceWith`로 원본 `<i>`를 새 `<svg>`로 바꾸므로:

- 변환 전에 `<i>`에 걸어둔 **이벤트 리스너가 사라진다.** 리스너는 아이콘 요소가 아니라 **부모 버튼/행**에 건다.
- 변환 전에 확보한 요소 참조(`const icon = document.querySelector(...)`)는 DOM에서 detached된 노드를 가리킨다.
- `id`는 복사되므로 `getElementById`는 변환 후에도 동작한다.

### 7.4 `createIcons()` 중복 호출은 안전하지만 svg가 다시 교체된다

`data-lucide` 속성이 변환 후 svg에도 남으므로, 재호출 시 svg가 또 새 svg로 교체된다. 결과물은 동일(멱등)하지만 svg에 직접 건 리스너가 있다면 사라진다. 여러 모듈이 각자 `createIcons()`를 불러도 중복 렌더링(아이콘 2개)은 발생하지 않는다.

### 7.5 미등록 아이콘은 조용히 `file`로 나온다

존재하지 않는 아이콘 이름을 써도 **콘솔 경고·에러가 없다.** `file`(서류) 아이콘이 나오면 4장 목록과 이름을 대조한다. 디버깅 팁:

```javascript
console.log(Object.keys(window.lucide.icons));  // 사용 가능 이름 전체
console.log('plus' in window.lucide.icons);     // 존재 확인
```

### 7.6 페이지 초기 렌더링은 `notify.js`가 담당한다

정적 아이콘의 최초 변환은 `notify.js`의 `DOMContentLoaded → refreshIcons()` 호출이다. `notify.js`를 제거하거나 로드 순서를 바꾸면 모든 페이지의 아이콘이 `<i>` 빈 요소로 남는다. `lucide.js` 자체는 로드 시점에 아무것도 실행하지 않는다.

### 7.7 `<i>`가 아닌 요소에도 동작한다

`querySelectorAll('[data-lucide]')`로 탐색하므로 `data-lucide` 속성만 있으면 어떤 태그든 svg로 교체된다. 프로젝트 규약은 `<i data-lucide="...">`다. 이 규약을 벗어나면 CSS(`.nav-icon` 등)가 어긋날 수 있다.

---

## 8. 참조

- **MANIFEST.md**: `src/main/resources/static/lib/MANIFEST.md` — `static/lib` 표의 `lucide.js` 행 ("공식 lucide 아님", "**공식 .min으로 교체 금지**")
- **원본 lucide** (아이콘 이름·경로 데이터 참고용): https://lucide.dev / https://github.com/lucide-icons/lucide (ISC)
- **구현체**: `src/main/resources/static/lib/lucide.js` (84행, 전체가 프로젝트 코드)
- **프로젝트 호출부**: `notify.js` (`refreshIcons`, toast/모달 아이콘), `defaultLayout.html` (사이드바 토글), `system/menu-manage.js` (트리 렌더), `fragments/sidebar.html` (메뉴 아이콘)
