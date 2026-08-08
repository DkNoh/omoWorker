# TOAST UI Date Picker 매뉴얼 (v4.3.3)

> 대상 라이브러리: **TOAST UI Date Picker 4.3.3** (vendored, 로컬 참조)
>
> 관련 문서
> - `src/main/resources/static/lib/MANIFEST.md` — 버전·라이선스·출처 대장
> - `docs/menual/tui_manual.md` — 프로젝트 래퍼(`TuiCommon` · `TuiPageBuilder`) 상세
> - `static/js/common/field-format.js` — IMask 날짜 마스크와의 역할 분담
>
> 이 문서는 **라이브러리 자체**를 다루되, 이 프로젝트에서 실제로 쓰는 API만 추렸다.

---

## 목차

1. [라이브러리 개요](#1-라이브러리-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 구조](#3-프로젝트-사용-구조)
4. [핵심 API (프로젝트 사용 기준)](#4-핵심-api-프로젝트-사용-기준)
5. [옵션 설정 패턴](#5-옵션-설정-패턴)
6. [주의사항 (Pitfalls)](#6-주의사항-pitfalls)
7. [참조](#7-참조)

---

## 1. 라이브러리 개요

| 항목 | 값 |
|---|---|
| 이름 | TOAST UI Date Picker |
| 버전 | **4.3.3** |
| 라이선스 | MIT |
| 출처 | `https://cdn.jsdelivr.net/npm/tui-date-picker@4.3.3/dist/tui-date-picker.min.js` |
| 파일 | `static/lib/tui-date-picker.min.js`, `static/lib/tui-date-picker.css` |
| 전역 변수 | `tui.DatePicker` (UMD 네임스페이스 `window.tui` — Grid/Pagination과 공유) |
| 용도 | 날짜 입력 위젯. 텍스트 input과 캘린더 레이어를 연결하고, 한국어 로케일 내장 |

CSS는 TUI가 별도 `.min` 버전을 배포하지 않으므로 `tui-date-picker.css`가 최종 dist 파일이다 (`MANIFEST.md` 규칙 참고).

### 1.1 프로젝트 내 두 가지 사용 경로

| 경로 | 사용처 | 특징 |
|---|---|---|
| **자동 부착** | 목록 화면 검색조건 | `TuiPageBuilder`가 `data-search-type="date"` input을 감지해 자동 생성 |
| **직접 생성** | 상세폼/등록폼 | 화면 JS가 `new tui.DatePicker(layer, options)` 호출 (`message-edit.js`, `campaign-register.js`) |

---

## 2. 프로젝트 로드 방식

### 2.1 `defaultLayout.html` 로드 순서

```html
<link rel="stylesheet" href="/lib/tui-pagination.css" />
<link rel="stylesheet" href="/lib/tui-date-picker.css" />   <!-- 캘린더 레이어 스타일 -->
<link rel="stylesheet" href="/lib/tui-grid.css" />

<script src="/lib/tui-pagination.min.js"></script>
<script src="/lib/xlsx.full.min.js"></script>
<script src="/lib/tui-date-picker.min.js"></script>   <!-- grid보다 먼저 (grid가 전역 의존) -->
<script src="/lib/tui-grid.min.js"></script>
...
<script src="/lib/dayjs.min.js"></script>
<script src="/lib/ko.js"></script>
<script>dayjs.locale('ko');</script>
```

- `tui-date-picker.min.js`는 `tui-grid.min.js` **이전**에 로드한다. Grid가 `window.tui.DatePicker`를 전역에서 찾기 때문이다 (`MANIFEST.md`: "tui-grid … tui-date-picker/tui-pagination/xlsx 전역 의존").
- 날짜 파싱/포맷은 프로젝트 공통으로 **dayjs**를 쓴다. DatePicker 자체 포맷 토큰(`yyyy-MM-dd`)은 dayjs 토큰(`YYYY-MM-DD`)과 다르므로 6.3절 참고.

### 2.2 전역 변수

| 전역 | 설명 |
|---|---|
| `tui.DatePicker` | 생성자. `new tui.DatePicker(layerElement, options)` |

존재 여부 가드 패턴 (프로젝트 공통):

```javascript
if (!window.tui || !window.tui.DatePicker) {
    return;   // 라이브러리 미로드 시 조용히 스킵 — 텍스트 input으로라도 입력 가능
}
```

---

## 3. 프로젝트 사용 구조

### 3.1 필수 DOM 구조 (공통 규약)

DatePicker는 **input 래퍼 + 캘린더 레이어** 두 요소가 한 쌍이다. 스캐폴드와 샘플이 모두 이 구조를 따른다.

```html
<!-- static/samples/list-basic.html · scaffold-templates/*/page.html.tpl -->
<div class="tui-datepicker-input tui-datetime-input scaffold-datepicker-input">
    <input type="text" id="startDate" name="startDate" value="2026-01-01"
           autocomplete="off" aria-label="시작일자" placeholder="YYYY-MM-DD">
    <span class="tui-ico-date" aria-hidden="true"></span>   <!-- 캘린더 아이콘 -->
</div>
<div id="startDatePickerLayer" class="scaffold-date-picker-layer"></div>
```

| 요소 | 규약 |
|---|---|
| input | `type="text"`, `autocomplete="off"` (브라우저 자동완성과 캘린더 충돌 방지) |
| 래퍼 클래스 | `tui-datepicker-input tui-datetime-input` — 라이브러리 CSS가 이 클래스를 기준으로 스타일 적용 |
| 아이콘 | `<span class="tui-ico-date">` — 클릭 시 캘린더 토글 |
| 레이어 | **`id = "{inputId}PickerLayer"`** — input id 뒤에 `PickerLayer`를 붙이는 것이 프로젝트 고정 규칙 |

검색조건용(`static/samples/list-basic.html`, Scaffold 화면 템플릿)은 input에 `data-search-type="date"`를 추가한다. 이 속성이 `TuiPageBuilder` 자동 부착의 감지 키다.

```html
<input type="text" id="startDt" data-search-type="date" autocomplete="off" aria-label="startDt">
```

### 3.2 경로 A — 검색조건 자동 부착 (`TuiPageBuilder`)

목록 화면 JS는 날짜 관련 코드를 **아무것도 작성하지 않는다**. `searchInputs`에 날짜 input id만 포함하면 된다.

```javascript
// Scaffold LIST/EXCEL 화면 — startDt/endDt가 searchInputs에 있으면 끝
const pageBuilder = new TuiPageBuilder({
    el: 'grid',
    apiUrl: '/sms/example-history/data',
    searchInputs: ['status', 'startDt', 'endDt'],
    ...
});
```

`tui-page-builder.js` 내부 (`_initSearchDatePickers`):

```javascript
// tui-page-builder.js (발췌)
_initSearchDatePickers() {
    if (!window.tui || !window.tui.DatePicker) {
        return;
    }
    this._datePickerInputIds().forEach(id => {
        const input = document.getElementById(id);
        const layer = document.getElementById(`${id}PickerLayer`);   // id 규약
        if (!input || !layer) {
            return;
        }
        this.searchDatePickers[id] = new tui.DatePicker(layer, {
            language: 'ko',
            date: this._toDatePickerDate(input.value),   // dayjs 파싱 → Date
            input: { element: input, format: 'yyyy-MM-dd' },
            calendar: { showToday: true }
        });
    });
}

// 감지 조건: input.type === 'date' 또는 data-search-type="date"
_isDateSearchInput(id) {
    const el = document.getElementById(id);
    return !!(el && (el.type === 'date' || el.dataset.searchType === 'date'));
}
```

- 명시 지정이 필요하면 `config.datePickerInputs: ['startDt', 'endDt']`로 넘긴다. 비우면 `searchInputs`에서 자동 감지한다.
- 초기화(`btn-reset`) 시 `_syncSearchDatePickers()`가 input 값과 picker 날짜를 재동기화한다 (`setDate` / `setNull`).
- 서버 전송 시 날짜 값은 `_readSearchValue`가 `YYYYMMDD`로 변환한다 (`2026-07-28` → `20260728`).

### 3.3 경로 B — 상세폼 직접 생성 (`message-edit.js`)

```javascript
// static/js/system/message-edit.js (발췌)
let startDatePicker = null;

function initStartDatePicker() {
    const input = document.getElementById('startDate');
    const layer = document.getElementById('startDatePickerLayer');
    const initial = toDate(input.value);          // dayjs(value).toDate()

    if (!window.tui || !window.tui.DatePicker) {
        return;
    }

    startDatePicker = new tui.DatePicker(layer, {
        language: 'ko',
        date: initial,
        input: {
            element: input,
            format: 'yyyy-MM-dd'                  // DatePicker 토큰 (dayjs와 다름!)
        },
        calendar: {
            showToday: true
        }
    });
}

// 상세 조회 후 값 반영
if (startDatePicker && data.startDate) {
    const date = toDate(data.startDate);
    if (date) {
        startDatePicker.setDate(date);
    }
}

// dayjs 문자열 → Date 변환 헬퍼
function toDate(value) {
    if (!value || typeof dayjs === 'undefined') {
        return null;
    }
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.toDate() : null;
}
```

`campaign-register.js`도 동일 패턴이며, 기본값을 dayjs로 계산해 input과 picker에 함께 넣는다.

```javascript
// static/js/sms/campaign-register.js (발췌)
const initial = defaultSendAt();                  // dayjs 객체
input.value = initial.format('YYYY-MM-DD');       // input은 dayjs 토큰
sendDatePicker = new tui.DatePicker(layer, {
    language: 'ko',
    date: initial.toDate(),                       // picker는 Date 객체
    input: { element: input, format: 'yyyy-MM-dd' },
    calendar: { showToday: true }
});
```

---

## 4. 핵심 API (프로젝트 사용 기준)

### 4.1 생성자

```javascript
new tui.DatePicker(layerElement, options)
```

- `layerElement`: 캘린더가 렌더링될 **빈 div** (문자열 id가 아니라 DOM 요소).
- 프로젝트가 사용하는 옵션은 5절에 정리.

### 4.2 인스턴스 메서드

| API | 프로젝트 사용처 | 설명 |
|---|---|---|
| `picker.setDate(date)` | `message-edit.js` (상세 조회 후) | 날짜 설정. input 텍스트도 포맷에 맞춰 갱신 |
| `picker.setDate(date, true)` | `tui-page-builder.js` `_syncSearchDatePickers()` | 초기화 후 재동기화. 프로젝트 사용 패턴 |
| `picker.setNull()` | `tui-page-builder.js` `_syncSearchDatePickers()` | 선택 해제(빈 값) 동기화. `typeof picker.setNull === 'function'` 가드 후 호출 |

```javascript
// tui-page-builder.js _syncSearchDatePickers() (발췌)
Object.keys(this.searchDatePickers || {}).forEach(id => {
    const picker = this.searchDatePickers[id];
    const input = document.getElementById(id);
    const date = this._toDatePickerDate(input ? input.value : '');
    if (date) {
        picker.setDate(date, true);
    } else if (picker && typeof picker.setNull === 'function') {
        picker.setNull();
    }
});
```

### 4.3 프로젝트에서 사용하지 않는 API

공식 문서에는 있지만 이 프로젝트가 쓰지 않는 것들 — 필요 시 공식 문서를 참고하되, 기존 패턴과 충돌하지 않게 한다.

- `getDate()` — 제출 값은 항상 **input의 `value`**에서 읽는다 (`FormBinder.toObject`). picker에서 읽지 않는다.
- `on('change', ...)` 이벤트 — 폼 제출 시점에 input 값을 검증하는 흐름이라 미사용.
- `setRanges()` / `remove()` — 미사용.

---

## 5. 옵션 설정 패턴

프로젝트 전체가 **동일한 옵션 조합**만 사용한다. 신규 화면도 이 조합을 그대로 따른다.

```javascript
new tui.DatePicker(layer, {
    language: 'ko',                                // 한국어 로케일 (라이브러리 내장)
    date: initialDate,                             // Date | null — 초기 선택일
    input: {
        element: input,                            // 연결할 텍스트 input DOM
        format: 'yyyy-MM-dd'                       // 표시·입력 포맷 (DatePicker 토큰)
    },
    calendar: {
        showToday: true                            // 캘린더 하단 "오늘" 버튼
    }
});
```

| 옵션 | 프로젝트 값 | 비고 |
|---|---|---|
| `language` | `'ko'` | 고정. `en`/`ko` 내장 |
| `date` | dayjs 파싱 결과 (`Date`) 또는 `null` | 서버 값(`YYYY-MM-DD`)은 `dayjs(v).toDate()`로 변환 후 전달 |
| `input.element` | input DOM | 3.1절 DOM 구조 필수 |
| `input.format` | `'yyyy-MM-dd'` | 고정. DatePicker 전용 토큰 (소문자 `yyyy`) |
| `calendar.showToday` | `true` | 고정 |

### 5.1 날짜 기본값 — `searchDefaults` (검색조건 전용)

검색조건 날짜의 초기값은 DatePicker가 아니라 `TuiPageBuilder`의 `searchDefaults`로 선언한다.

```javascript
new TuiPageBuilder({
    ...,
    searchInputs: ['startDt', 'endDt'],
    searchDefaults: { startDt: 'THIS_MONTH', endDt: 'TODAY' }
});
```

지원 코드: `TODAY`, `YESTERDAY`, `RECENT_7_DAYS`, `THIS_MONTH` (`CURRENT_MONTH_TO_TODAY` 동일).
범위 끝 판별은 id 규칙 — `*To`로 끝나거나 `end*`/`to*`로 시작하면 종료일로 계산한다 (`_isRangeEnd`).
값이 input에 세팅된 뒤 `_initSearchDatePickers()`가 그 값을 picker 초기일로 읽는다.

---

## 6. 주의사항 (Pitfalls)

### 6.1 레이어 id 규약을 지키지 않으면 조용히 미부착

- 레이어는 반드시 `id="{inputId}PickerLayer"`. 예: `startDt` → `startDtPickerLayer`.
- input 또는 레이어가 없으면 `TuiPageBuilder`는 **오류 없이 스킵**한다 — 캘린더만 안 뜨고 원인을 찾기 어려우므로 DOM id부터 확인한다.

### 6.2 `autocomplete="off"` 필수

- 브라우저 자동완성 드롭다운이 캘린더 레이어와 겹친다. 스캐폴드/샘플의 input은 모두 `autocomplete="off"`다.

### 6.3 포맷 토큰 혼동 (가장 흔한 실수)

| 대상 | 토큰 | 예 |
|---|---|---|
| DatePicker `input.format` | **소문자** `yyyy-MM-dd` | `'yyyy-MM-dd'` |
| dayjs (프로젝트 공통) | **대문자** `YYYY-MM-DD` | `dayjs().format('YYYY-MM-DD')` |
| 서버 전송 (검색조건) | `YYYYMMDD` | `TuiPageBuilder._readSearchValue`가 자동 변환 |

DatePicker 옵션에 dayjs 토큰(`YYYY-MM-DD`)을 넣으면 포맷이 깨진다. 반대로 dayjs에 `yyyy`를 넣어도 깨진다.

### 6.4 IMask 날짜 마스크와 중복 사용 금지

- `field-format.js`의 `data-mask="date"`(`0000-00-00`)는 **자유 입력용**이다. 파일 주석에 명시되어 있다:
  > "날짜 YYYY-MM-DD (자유 입력용. 검색조건 날짜는 기존 TUI DatePicker 사용)"
- DatePicker가 붙은 input에 `data-mask="date"`를 함께 붙이면 양쪽이 input 값을 서로 덮어쓴다. **둘 중 하나만** 사용한다.

### 6.5 레이어 위치·z-index는 브리지 CSS가 담당

- 캘린더 레이어의 절대 위치와 z-index는 `admin-ui-bridge.css`가 프로젝트 디자인 토큰(`--sms-*`)으로 재정의한다.

```css
/* static/css/admin-ui-bridge.css (발췌) */
.scaffold-date-field { position: relative; z-index: 40; }
.scaffold-date-picker-layer {
    position: absolute;
    top: var(--sms-control-height);
    left: 0;
    z-index: var(--sms-z-datepicker);
    margin-top: -1px;
}
.scaffold-date-picker-layer .tui-datepicker {
    z-index: var(--sms-z-datepicker);
    box-shadow: var(--sms-shadow-md);
    border: 1px solid var(--sms-border);
    border-radius: var(--sms-radius-md);
}
```

- 신규 화면은 레이어 div에 `scaffold-date-picker-layer` 클래스를 붙이는 것만으로 동일 스타일을 적용받는다. 인라인 스타일로 위치를 직접 잡지 않는다.
- CoreUI/Bootstrap의 `.input-group`, 카드 `overflow` 설정과 겹치면 캘린더가 잘린다. 위 클래스 구조를 유지한다.

### 6.6 제출 값은 input에서 읽는다

- 폼 전송 시 날짜 값은 **input의 `value`**(`yyyy-MM-dd` 텍스트)를 `FormBinder.toObject`로 수집한다. `picker.getDate()`를 사용하지 않는다.
- 따라서 input의 `name` 속성이 서버 DTO 필드와 일치해야 한다 (`startDate`, `startDt` 등).

### 6.7 라이브러리 미로드 가드

- 모든 생성 지점은 `if (!window.tui || !window.tui.DatePicker) return;` 가드를 둔다. 라이브러리 로드 실패 시에도 텍스트 직접 입력은 가능해야 한다.

### 6.8 버전 업그레이드 시

- `MANIFEST.md` 규칙: 동일 메이저/마이너(4.3.x) 우선.
- 4.x는 `tui-time-picker` 없이 단독 동작하지만, `window.tui` 네임스페이스를 Grid/Pagination과 공유하므로 세 라이브러리를 함께 검증한다.

---

## 7. 참조

| 항목 | 위치 |
|---|---|
| 공식 문서 | https://github.com/nhn/tui.date-picker (API: `docs/en` 디렉터리) |
| vendored 파일 | `src/main/resources/static/lib/tui-date-picker.min.js`, `tui-date-picker.css` |
| MANIFEST 항목 | `src/main/resources/static/lib/MANIFEST.md` — "tui-date-picker.min.js / TOAST UI Date Picker / 4.3.3 / MIT" |
| 자동 부착 로직 | `static/js/common/tui-page-builder.js` — `_initSearchDatePickers`, `_syncSearchDatePickers` |
| 직접 생성 예 | `static/js/system/message-edit.js`, `static/js/sms/campaign-register.js` |
| DOM 구조 샘플 | `static/samples/list-basic.html`, `static/samples/message-edit.html`, `scaffold-templates/*/page.html.tpl` |
| 레이어 스타일 | `static/css/admin-ui-bridge.css` (`.scaffold-date-picker-layer`) |
| 날짜 마스크와의 분담 | `static/js/common/field-format.js` (주석 참고) |
