# IMask 매뉴얼 (프로젝트 한정)

> 대상 라이브러리: **IMask.js 7.6.1** (vendored, CDN 미사용)
>
> 대상 파일
> - `src/main/resources/static/lib/imask.min.js` — 라이브러리 본체
> - `src/main/resources/static/js/common/field-format.js` — `data-mask` 선언적 래퍼(`FieldFormat`)
> - `src/main/resources/static/js/common/form-binder.js` — 전송 시 `unmaskedValue` 추출
>
> 이 문서는 IMask 전체 API를 다루지 않는다. **이 프로젝트에서 실제로 쓰는 부분만** 다룬다.
> 관련 문서: [`common-js_manual.md` 8장](./common-js_manual.md) (`FieldFormat` 전체 API)

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 방식 — `data-mask` 선언 패턴](#3-프로젝트-사용-방식--data-mask-선언-패턴)
4. [핵심 API 요약](#4-핵심-api-요약)
5. [주의사항 및 프로젝트 특이사항](#5-주의사항-및-프로젝트-특이사항)
6. [참조](#6-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 라이브러리 | IMask.js |
| 버전 | **7.6.1** |
| 라이선스 | MIT |
| 출처 | https://cdn.jsdelivr.net/npm/imask@7.6.1/dist/imask.min.js |
| 용도 | 입력 포맷 마스킹 — 전화번호·주민번호·사업자번호·숫자·날짜를 입력 중 자동 포맷 |

`MANIFEST.md` 등재 행:

```text
| imask.min.js | IMask.js | 7.6.1 | https://cdn.jsdelivr.net/npm/imask@7.6.1/dist/imask.min.js | MIT | 2026-06-19 | 포맷 마스킹. unmaskedValue로 전송값 분리 |
```

---

## 2. 프로젝트 로드 방식

### 2.1 스크립트 태그 위치

`src/main/resources/templates/defaultLayout.html`의 `<head>` 내부:

```html
<script src="/lib/lucide.js"></script>
<script src="/lib/imask.min.js"></script>          <!-- 25행 -->
<script src="/lib/just-validate.min.js"></script>
...
<script th:src="@{/js/common/form-binder.js}"></script>   <!-- 46행 -->
<script th:src="@{/js/common/field-format.js}"></script>  <!-- 47행: IMask 래퍼 -->
```

### 2.2 전역 변수

| 파일 | 전역 |
|---|---|
| `imask.min.js` | `window.IMask` — `IMask(el, options)` 호출로 인스턴스 생성 |
| `field-format.js` | `FieldFormat` (전역 `const`) — 이 프로젝트의 유일한 IMask 진입점 |

> **화면 JS는 `IMask()`를 직접 호출하지 않는다.** `FieldFormat`의 `data-mask` 패턴을 통해서만 부착한다. 직접 호출하면 중복 부착 방지·전송값 분리 로직을 우회한다.

---

## 3. 프로젝트 사용 방식 — `data-mask` 선언 패턴

### 3.1 설계 철학

화면/스캐폴드는 입력 요소에 **`data-mask` 속성만 선언**한다. `field-format.js`가 그 속성을 읽어 IMask를 부착한다. 라이브러리를 교체해도 화면 코드는 불변이다 (lucide의 `data-lucide`와 동일 패턴).

```html
<!-- 화면은 이것만 선언한다 -->
<input type="text" name="receiverNo" data-mask="phone" data-validate="required|phone">
<input type="text" name="amount"     data-mask="number">
<input type="text" name="bizNo"      data-mask="bizno">
```

### 3.2 내장 마스크 종류 (`maskRegistry`)

| `data-mask` 값 | 포맷 | 표시 예 | 전송값(`unmaskedValue`) |
|---|---|---|---|
| `phone` | 전화번호 (지역번호/휴대폰 자동 판별) | `010-1234-5678`, `02-1234-5678` | `01012345678` |
| `ssn` | 주민등록번호 6-7 | `900101-1234567` | `9001011234567` |
| `bizno` | 사업자등록번호 3-2-5 | `123-45-67890` | `1234567890` |
| `number` | 숫자 (천단위 콤마, 소수·음수 불가) | `1,234,567` | `1234567` |
| `date` | 날짜 YYYY-MM-DD | `2026-07-28` | `20260728` |

`phone`은 4개 마스크(`000-0000-0000`, `000-000-0000`, `00-0000-0000`, `00-000-0000`)의 **dynamic mask**로, IMask가 입력에 가장 잘 맞는 패턴을 자동 선택한다.

`number`는 문자 패턴이 아니라 IMask의 `Number` 마스크다 (`{ mask: Number, thousandsSeparator: ',', scale: 0, min: 0, signed: false }`).

### 3.3 적용 시점

| 상황 | 호출 | 비고 |
|---|---|---|
| 정적 폼 (검색조건 등) | `DOMContentLoaded`에 `FieldFormat.applyMasks(document)` **자동** | `field-format.js` 파일 말미에 등록됨 — 별도 호출 불필요 |
| 동적 폼 (TuiPageBuilder 모달 등) | 모달 생성 직후 `FieldFormat.applyFieldFormats(formEl)` **수동** | 마스크 + 검증을 한 번에 부착 |

### 3.4 부착과 전송의 흐름

```text
① DOMContentLoaded (또는 applyFieldFormats)
   field-format.js:  el._imask = IMask(el, factory(el));
                     ↑ IMask 인스턴스를 el._imask에 보관 (중복 부착 방지 키)

② 사용자 입력 → IMask가 표시값을 실시간 포맷 (el.value = "010-1234-5678")

③ 저장 버튼 → FormBinder.toObject(form)
   form-binder.js:   const raw = field._imask ? field._imask.unmaskedValue : field.value;
                     ↑ 마스크 필드는 하이픈/콤마가 제거된 값("01012345678")을 전송
```

단일 필드의 전송값이 필요하면:

```javascript
FieldFormat.unmask(document.querySelector('#receiverNo'));  // "01012345678"
// 마스크가 없는 필드는 el.value 그대로, null/undefined는 '' 반환
```

### 3.5 종류 추가 — `registerMask`

새 마스크는 화면 JS가 아니라 **레지스트리에 등록**한다:

```javascript
FieldFormat.registerMask('zipcode', () => ({ mask: '00000' }));
// 이후 HTML에서 <input name="zipcode" data-mask="zipcode"> 사용
```

팩토리는 요소(`el`)를 인자로 받아 IMask 옵션 객체를 반환한다 (요소별 동적 구성 가능).

---

## 4. 핵심 API 요약

프로젝트가 쓰는 IMask API는 최소화되어 있다.

| API | 용도 | 사용처 |
|---|---|---|
| `IMask(el, options)` | 마스크 인스턴스 생성·부착 | `field-format.js` 내부 (`applyMasks`) — 화면에서 직접 호출 금지 |
| `instance.unmaskedValue` | 포맷 제거된 원본 값 (getter) | `form-binder.js`, `FieldFormat.unmask` |

`options` 형태 (프로젝트 사용 예):

```javascript
// 문자 패턴 마스크
{ mask: '000-00-00000' }

// dynamic mask (배열) — best-fit 자동 선택
{ mask: [{ mask: '000-0000-0000' }, { mask: '00-000-0000' }] }

// Number 마스크
{ mask: Number, thousandsSeparator: ',', scale: 0, min: 0, signed: false }
```

> IMask의 다른 API(`updateValue()`, `updateOptions()`, `typedValue`, 이벤트 등)는 이 프로젝트에서 쓰지 않는다. 필요하면 `FieldFormat`을 확장하는 방향으로 검토한다.

---

## 5. 주의사항 및 프로젝트 특이사항

### 5.1 전송값은 반드시 `unmaskedValue`다

표시값(`010-1234-5678`)을 그대로 서버에 보내면 안 된다. 서버 DTO는 하이픈 없는 값을 기대한다.

- `FormBinder.toObject()`를 쓰면 **자동 처리**된다 (`field._imask.unmaskedValue`).
- payload를 수동 조립할 때는 `FieldFormat.unmask(el)` 또는 `el._imask.unmaskedValue`를 직접 쓴다.
- `number` 마스크의 `unmaskedValue`는 콤마 없는 숫자 문자열(`"1234567"`)이다.

### 5.2 중복 부착은 `el._imask`로 방지된다

`applyMasks`는 `el._imask`가 이미 있는 요소를 건너뛴다. 모달을 열고 닫으며 `applyFieldFormats`를 반복 호출해도 안전하다. 단, **요소를 복제(clone)해서 쓰면 `_imask` 참조가 함께 복사되어 오동작**할 수 있으므로 복제 후 부착한다.

### 5.3 미등록 `data-mask` 값은 경고만 남기고 무시된다

`maskRegistry`에 없는 값을 선언하면 `console.warn('[FieldFormat] 미등록 mask: ...')` 후 마스크 없이 동작한다. 오타를 내면 조용히 일반 입력이 되므로, 새 종류가 필요하면 `registerMask`로 먼저 등록한다.

### 5.4 프로그래매틱 값 설정은 마스크를 거치지 않는다

`FormBinder.bind()`처럼 `field.value = ...`로 값을 직접 넣으면 IMask가 재포맷하지 않는다 (input 이벤트가 없어서). 다음 사용자 입력부터 마스크가 적용된다. 조회 바인딩 후 표시가 어색하면 사용자 조작 시 자연스럽게 교정되므로, 보통 문제되지 않는다. 마스크 인스턴스를 통한 값 설정이 꼭 필요하면 `el._imask.value = '01012345678'`처럼 인스턴스 setter를 쓴다.

### 5.5 주민등록번호(`ssn`)는 민감 PII다

`ssn` 마스크는 입력 편의용일 뿐, **표시·저장·로그 마스킹 정책과는 별개**다. 화면 표시 시 마스킹은 `PAGE_AUTH.maskView` 권한과 `TuiCommon`의 PII 포매터(`RRN` 타입)를 따르고, 서버는 `PrivacyLog` 정책을 따른다 (`field-format.js` 주석, `audit-masking-policy.md`).

### 5.6 검색조건 날짜는 IMask `date`보다 TUI DatePicker 우선

`data-mask="date"`는 자유 입력용이다. 검색조건의 날짜 범위는 기존 TUI DatePicker를 그대로 사용한다 (`tui-date-picker_manual.md` 참고). 둘을 같은 필드에 중복 부착하지 않는다.

### 5.7 IMask가 없어도 페이지는 동작한다

`applyMasks`는 `typeof IMask === 'undefined'`면 조용히 종료한다. 라이브러리 로드 실패 시 마스킹만 비활성화될 뿐 폼/검증은 살아있다. 이 가드를 제거하지 말 것.

---

## 6. 참조

- **MANIFEST.md**: `src/main/resources/static/lib/MANIFEST.md` — `static/lib` 표의 `imask.min.js` 행 (7.6.1, MIT, "unmaskedValue로 전송값 분리")
- **공식 문서**: https://imask.js.org/ (이 프로젝트는 7.6.1 vendored — CDN 참조 금지, `thymeleaf.md` 규칙)
- **프로젝트 래퍼 상세**: [`common-js_manual.md` 8장](./common-js_manual.md) — `FieldFormat` 전체 API (`applyMasks`, `applyFieldFormats`, `validateForm`, `unmask`, `registerMask`)
- **전송값 추출**: [`common-js_manual.md` 7장](./common-js_manual.md) — `FormBinder.toObject()`의 `_imask.unmaskedValue` 처리
