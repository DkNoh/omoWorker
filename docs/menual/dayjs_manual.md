# Day.js 매뉴얼 (프로젝트 한정)

> 대상 라이브러리: **Day.js** (본체) + **ko 로케일** (한국어)
>
> 대상 파일
> - `src/main/resources/static/lib/dayjs.min.js` — Day.js 본체 (7.1KB)
> - `src/main/resources/static/lib/ko.js` — 한국어 로케일 (1.3KB)
>
> 이 문서는 Day.js 전체 API를 다루지 않는다. **이 프로젝트에서 실제로 쓰는 부분만** 다룬다.
> 관련 문서: [`common-js_manual.md`](./common-js_manual.md) (`CommonUtils.setDefaultDateTime`), [`tui_manual.md`](./tui_manual.md) (`TuiCommon.fmt.date`)

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 방식](#3-프로젝트-사용-방식)
4. [핵심 API 요약](#4-핵심-api-요약)
5. [주의사항 및 프로젝트 특이사항](#5-주의사항-및-프로젝트-특이사항)
6. [참조](#6-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 라이브러리 | Day.js |
| 버전 | **미확정** — 파일 헤더에 버전 표기 없음. `MANIFEST.md`에 "확인 필요"로 등재. 교체 시 버전 확정 필수 |
| 라이선스 | MIT |
| 출처 | https://www.npmjs.com/package/dayjs |
| 로케일 | `ko.js` (한국어, 본체와 동일 배포판) |
| 용도 | 날짜/시간 파싱·포맷·연산. Moment.js 호환 문법의 경량(2KB급 코어) 라이브러리 |

`MANIFEST.md` 등재 행:

```text
| dayjs.min.js | Day.js | 확인 필요 | https://www.npmjs.com/package/dayjs | MIT | (기존) | 헤더에서 버전 미검출 — 교체 시 확정 |
| ko.js | Day.js locale (ko) | dayjs와 동일 | https://www.npmjs.com/package/dayjs | MIT | (기존) | dayjs 한국어 로케일 |
```

> ⚠️ 버전을 올리거나 파일을 교체하려면 먼저 헤더/배포 출처에서 버전을 확정하고 `MANIFEST.md` 비고를 갱신한다 (`MANIFEST.md` 규칙).

---

## 2. 프로젝트 로드 방식

### 2.1 스크립트 태그 위치

`src/main/resources/templates/defaultLayout.html`의 `<head>` 내부:

```html
<script src="/lib/axios.min.js"></script>
<script src="/lib/dayjs.min.js"></script>      <!-- 22행: 본체 -->
<script src="/lib/ko.js"></script>             <!-- 23행: ko 로케일 등록 -->
<script src="/lib/lucide.js"></script>
<script src="/lib/imask.min.js"></script>
<script src="/lib/just-validate.min.js"></script>
<script>dayjs.locale('ko');</script>           <!-- 27행: 전역 활성화 -->
```

### 2.2 로드 순서와 전역 변수

| 순서 | 파일 | 효과 |
|---|---|---|
| 1 | `dayjs.min.js` | `window.dayjs` 전역 함수 노출 |
| 2 | `ko.js` | `dayjs.locale('ko')`가 인식할 수 있도록 ko 로케일을 **등록**만 함 (아직 활성화 아님) |
| 3 | `<script>dayjs.locale('ko');</script>` | ko 로케일을 **전역 기본값으로 활성화** |

- `ko.js`는 본체보다 **반드시 뒤에** 로드해야 한다 (본체의 `dayjs` 전역에 로케일을 등록하므로).
- `dayjs.locale('ko')` 호출은 공통 JS(`common-utils.js` 등)보다 앞에 있으므로, 이후 모든 dayjs 호출에 ko가 적용된다.
- 업무 화면은 `defaultLayout`을 상속하므로 **화면에서 `dayjs.locale('ko')`를 다시 부르지 않는다.**

---

## 3. 프로젝트 사용 방식

Day.js는 공통 모듈 3곳과 화면 JS 2곳에서 직접 사용된다. 별도 래퍼 없이 `dayjs()` 전역 함수를 그대로 부른다.

### 3.1 검색 폼 기본값 — `common-utils.js`

```javascript
const now = dayjs();
const todayStr = now.format('YYYY-MM-DD');              // "2026-07-28"
const fromTime = now.subtract(1, 'hour').format('HH:mm'); // 1시간 전 "HH:mm"
const toTime   = now.add(1, 'hour').format('HH:mm');      // 1시간 후
```

`#startDate`/`#endDate`에는 `YYYY-MM-DD`, `#startTime`/`#endTime`에는 `HH:mm`을 채운다.

### 3.2 그리드 날짜 포매터 — `tui-common.js`

```javascript
const formatDate = (value, pattern = 'YYYY-MM-DD') => {
    const val = rawValue(value);
    if (!val) return '-';
    const parsed = dayjs(val);
    return parsed.isValid() ? parsed.format(pattern) : String(val);  // isValid 가드 필수
};

// 날짜(10자 이하)는 YYYY-MM-DD, 일시(초과)는 YYYY-MM-DD HH:mm 자동 판별
fmt.date = v => {
    const val = rawValue(v);
    const pattern = String(val || '').length > 10 ? 'YYYY-MM-DD HH:mm' : 'YYYY-MM-DD';
    return formatDate(val, pattern);
};
```

그리드 컬럼에서 `formatter: TuiCommon.fmt.date`로 사용 (`sms/history.js`의 `sentAt` 컬럼 등).

### 3.3 검색 파라미터 직렬화 — `tui-page-builder.js`

```javascript
// 날짜 범위 유효성 검사
if (dayjs(startDateEl.value).isAfter(dayjs(endDateEl.value))) { /* 시작 > 종료 차단 */ }

// 서버 전송용 컴팩트 포맷
value = value ? dayjs(value).format('YYYYMMDD') : '';        // 날짜만
value = value ? dayjs(value).format('YYYYMMDDHHmm') : '';    // 일시
```

### 3.4 예약 발송 일시 — `sms/campaign-register.js`

```javascript
// 현재 +10분에서 5분 단위로 올림
const initial = dayjs().add(10, 'minute').second(0).millisecond(0);
const remainder = initial.minute() % 5;
const sendAt = remainder === 0 ? initial : initial.add(5 - remainder, 'minute');

// 입력값 조합 → 파싱 → 검증
const sendAt = dayjs(`${date} ${hour}:${minute}`, 'YYYY-MM-DD HH:mm');
if (!sendAt || !sendAt.isValid() || !sendAt.isAfter(dayjs())) {
    CommonUtils.toast('전송일시는 현재 시각 이후로 선택하세요.', 'warning');
    return;
}
document.getElementById('sendAt').value = sendAt.format('YYYYMMDDHHmm');
```

### 3.5 프로젝트에서 쓰는 포맷 패턴 전체

| 패턴 | 용도 | 사용처 |
|---|---|---|
| `YYYY-MM-DD` | 날짜 (표시·검색조건) | `common-utils`, `tui-common`, `campaign-register` |
| `HH:mm` | 시간 (검색조건) | `common-utils` |
| `YYYY-MM-DD HH:mm` | 일시 (그리드 표시) | `tui-common` |
| `YYYYMMDD` | 서버 전송용 날짜 | `tui-page-builder` |
| `YYYYMMDDHHmm` | 서버 전송용 일시 | `tui-page-builder`, `campaign-register` |
| `HH`, `mm` | 시/분 셀렉트 기본값 | `campaign-register` |

---

## 4. 핵심 API 요약

프로젝트에서 쓰는 Day.js API는 이것뿐이다.

| API | 반환 | 프로젝트 예 |
|---|---|---|
| `dayjs()` | Dayjs | 현재 시각: `dayjs()` |
| `dayjs(value)` | Dayjs | 파싱: `dayjs('2026-07-28')`, `dayjs(startDateEl.value)` |
| `.format(pattern)` | `string` | `dayjs().format('YYYY-MM-DD')` |
| `.isValid()` | `boolean` | 파싱 실패 가드: `parsed.isValid() ? ... : String(val)` |
| `.add(n, unit)` | Dayjs (새 인스턴스) | `now.add(1, 'hour')`, `initial.add(10, 'minute')` |
| `.subtract(n, unit)` | Dayjs (새 인스턴스) | `now.subtract(1, 'hour')` |
| `.isAfter(other)` | `boolean` | 범위/미래시각 검증: `start.isAfter(end)` |
| `.second(n)` / `.millisecond(n)` | Dayjs (새 인스턴스) | 초·밀리초 0 정규화 |
| `.minute()` | `number` | 5분 단위 올림 계산 |
| `.toDate()` | `Date` | TUI DatePicker 연동: `sendDatePicker.setDate(initial.toDate())` |

사용 단위(unit): `'hour'`, `'minute'` (프로젝트에서 쓰는 것).

---

## 5. 주의사항 및 프로젝트 특이사항

### 5.1 Day.js는 불변(immutable)이다

`.add()`, `.subtract()`, `.second(0)` 등은 **새 인스턴스를 반환**하고 원본은 변하지 않는다.

```javascript
const a = dayjs();
const b = a.add(1, 'hour');
// a는 그대로, b만 1시간 후. a.add(1,'hour')를 호출만 하고 버리면 아무 일도 일어나지 않는다.
```

### 5.2 커스텀 포맷 파싱 플러그인이 없다

`campaign-register.js`의 `dayjs(str, 'YYYY-MM-DD HH:mm')`처럼 두 번째 인자로 포맷을 넘기지만, **`customParseFormat` 플러그인이 로드되지 않아 두 번째 인자는 무시된다.** `'2026-07-28 10:00'` 형태가 네이티브 `Date` 파서로 처리되어 우연히 동작하는 것이다.

- 포맷 문자열에 의존하는 파싱을 새로 작성하지 말 것.
- 반드시 `.isValid()`로 검증한 뒤 사용한다 (해당 코드도 `isValid()` 가드 있음).
- 플러그인을 추가하려면 별도 vendoring + `MANIFEST.md` 등재가 필요하다 — 함부로 추가하지 않는다.

### 5.3 로드된 플러그인이 없다 (코어만)

이 프로젝트의 dayjs는 **플러그인 없는 코어 본체**다. `relativeTime`, `customParseFormat`, `duration` 등은 쓸 수 없다. 공식 문서의 플러그인 전용 API를 코드에 인용하지 말 것.

### 5.4 ko 로케일의 영향 범위

`dayjs.locale('ko')`는 **요일명(`dddd` → "화요일"), 오전/오후(`A`), 상대시간 문자열**에만 영향을 준다. 프로젝트가 쓰는 숫자 패턴(`YYYY-MM-DD`, `HH:mm` 등)은 로케일과 무관하게 항상 같은 결과다. 화면에 요일·오전/오후를 표시할 때만 ko 로케일의 혜택을 본다.

### 5.5 파싱 실패는 예외가 아니라 Invalid Date다

`dayjs('잘못된값')`은 throw하지 않고 `Invalid Date` 객체를 반환한다. `.format()` 결과는 `"Invalid Date"` 문자열이 된다. 그래서 `tui-common.js`는 반드시 `isValid()`로 검증하고, 실패 시 원본 문자열을 그대로 표시한다. 사용자 입력·서버 응답을 파싱할 때는 항상 같은 가드를 쓴다.

### 5.6 날짜 표시는 `TuiCommon.fmt.date`로 통일

그리드 컬럼의 날짜 포맷을 화면마다 hand-roll하지 않는다. `formatter: TuiCommon.fmt.date`를 쓰면 길이 기반 패턴 판별 + `isValid` 가드 + 빈 값 `-` 처리가 자동으로 따라온다.

### 5.7 버전 미확정 — 교체 시 주의

`dayjs.min.js` 헤더에 버전 표기가 없다. 파일을 교체할 때는:
1. 공식 배포본에서 버전을 확정하고,
2. 동일 메이저(1.x) 유지를 우선하고 (`MANIFEST.md` 규칙),
3. `MANIFEST.md`의 "확인 필요" 비고를 확정 버전으로 갱신한다.

`ko.js`는 본체와 같은 배포판의 로케일 파일을 쓴다. 본체 메이저가 바뀌면 로케일도 같은 배포판에서 함께 교체한다.

---

## 6. 참조

- **MANIFEST.md**: `src/main/resources/static/lib/MANIFEST.md` — `static/lib` 표의 `dayjs.min.js`, `ko.js` 행 (버전 확인 필요, MIT)
- **공식 문서**: https://day.js.org/ (이 프로젝트는 vendored 본체 + ko 로케일 — CDN 참조 금지, `thymeleaf.md` 규칙)
- **공식 로케일 문서**: https://day.js.org/docs/en/i18n/i18n
- **프로젝트 사용처**: `common-utils.js` (`setDefaultDateTime`), `tui-common.js` (`fmt.date`), `tui-page-builder.js` (검색 파라미터), `sms/campaign-register.js` (예약 발송)
