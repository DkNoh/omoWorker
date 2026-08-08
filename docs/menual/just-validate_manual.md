# JustValidate 매뉴얼 (프로젝트 한정)

> 대상 라이브러리: **JustValidate 4.3.0** (vendored, CDN 미사용)
>
> 대상 파일
> - `src/main/resources/static/lib/just-validate.min.js` — 라이브러리 본체 (production 빌드)
> - `src/main/resources/static/js/common/field-format.js` — `data-validate` 선언적 래퍼(`FieldFormat`)
> - `src/main/resources/static/js/common/modal-manager.js` — 모달 폼 검증 (`validateRules` 훅)
>
> 이 문서는 JustValidate 전체 API를 다루지 않는다. **이 프로젝트에서 실제로 쓰는 부분만** 다룬다.
> 관련 문서: [`common-js_manual.md` 8장](./common-js_manual.md) (`FieldFormat`), `docs/base/common-response-contract.md` (서버 `errors[]`)

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 방식 — 두 가지 진입점](#3-프로젝트-사용-방식--두-가지-진입점)
4. [핵심 API 요약](#4-핵심-api-요약)
5. [서버 `@Valid`와의 관계 — 최종 권위는 서버](#5-서버-valid와의-관계--최종-권위는-서버)
6. [주의사항 및 프로젝트 특이사항](#6-주의사항-및-프로젝트-특이사항)
7. [참조](#7-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 라이브러리 | JustValidate |
| 버전 | **4.3.0** |
| 라이선스 | MIT |
| 출처 | https://unpkg.com/just-validate@4.3.0/dist/just-validate.production.min.js |
| 용도 | 클라이언트 폼 검증 — 제출 전 UX 보조. **보안 경계가 아니다** |

`MANIFEST.md` 등재 행:

```text
| just-validate.min.js | JustValidate | 4.3.0 | https://unpkg.com/just-validate@4.3.0/dist/just-validate.production.min.js | MIT | 2026-06-19 | 클라 폼 검증(서버 @Valid가 최종 권위) |
```

> ⚠️ 핵심 원칙: **서버 `@Valid`가 최종 권위다.** JustValidate 검증은 사용자가 서버 왕복 없이 즉시 피드백을 받기 위한 UX 보조일 뿐이다. 클라이언트 검증을 통과한 값도 서버에서 다시 거부될 수 있고, 그 반대도 가능하다.

---

## 2. 프로젝트 로드 방식

### 2.1 스크립트 태그 위치

`src/main/resources/templates/defaultLayout.html`의 `<head>` 내부:

```html
<script src="/lib/imask.min.js"></script>
<script src="/lib/just-validate.min.js"></script>   <!-- 26행 -->
...
<script th:src="@{/js/common/modal-manager.js}"></script>  <!-- 44행 -->
<script th:src="@{/js/common/field-format.js}"></script>   <!-- 47행: data-validate 래퍼 -->
```

### 2.2 전역 변수

| 파일 | 전역 |
|---|---|
| `just-validate.min.js` | `window.JustValidate` — `new JustValidate(formEl)` 생성자 |
| `field-format.js` | `FieldFormat` (전역 `const`) — 선언적 진입점 |
| `modal-manager.js` | `window.ModalManager` — 모달 폼 진입점 |

> **화면 JS는 `new JustValidate()`를 직접 생성하지 않는다.** `FieldFormat`의 `data-validate` 속성 또는 `ModalManager.init()`의 `validateRules` 훅을 통해서만 부착한다.

---

## 3. 프로젝트 사용 방식 — 두 가지 진입점

### 3.1 진입점 A — `data-validate` 선언 패턴 (`FieldFormat`)

lucide의 `data-lucide`, IMask의 `data-mask`와 같은 선언적 패턴이다. 화면은 속성만 선언한다:

```html
<input type="text" name="receiverNo" data-mask="phone" data-validate="required|phone">
<input type="text" name="email"      data-validate="required|email">
<input type="text" name="memo"       data-validate="maxlength:200">
```

문법:

- 규칙은 `|`로 구분한다 (`required|phone`).
- 인자는 `:`로 전달한다 (`minlength:4`, `maxlength:20`).
- 미등록 토큰은 조용히 무시된다 (오타 주의).

내장 토큰 (`validatorRegistry`):

| 토큰 | JustValidate rule | 예시 |
|---|---|---|
| `required` | `required` | `data-validate="required"` |
| `email` | `email` | `data-validate="required\|email"` |
| `number` | `number` | `data-validate="required\|number"` |
| `minlength:N` | `minLength` (value: N) | `data-validate="minlength:4"` |
| `maxlength:N` | `maxLength` (value: N) | `data-validate="maxlength:20"` |
| `phone` | `customRegexp` `/^0\d{1,2}-?\d{3,4}-?\d{4}$/` | `data-validate="required\|phone"` |
| `ssn` | `customRegexp` `/^\d{6}-?\d{7}$/` | `data-validate="ssn"` |
| `bizno` | `customRegexp` `/^\d{3}-?\d{2}-?\d{5}$/` | `data-validate="bizno"` |

검증 실행:

```javascript
const isValid = await FieldFormat.validateForm(formEl);  // Promise<boolean>
if (!isValid) return;
const payload = FormBinder.toObject(formEl);
await HttpClient.post('/api/save', payload);
```

`validateForm`은 검증기가 없는 폼이면 `true`를 반환한다 (검증 속성 없음 = 검증 통과).

JustValidate 4.3.0의 `revalidate()`는 전체 폼 오류 라벨을 렌더링하지 않는 알려진 결함이 있다(upstream #155). `FieldFormat.validateForm`은 이 결함을 우회하기 위해 per-field `revalidateField`를 사용해 각 필드의 오류를 개별 렌더링한다.

실제 사용 예 (`sms/campaign-register.js`):

```javascript
const valid = await FieldFormat.validateForm(form);
if (!valid) return;
```

규칙 추가는 레지스트리 확장으로:

```javascript
FieldFormat.registerValidator('zipcode', () => ({
    rule: 'customRegexp',
    value: /^\d{5}$/,
    errorMessage: '우편번호 형식 오류'
}));
// 이후 <input data-validate="required|zipcode"> 사용
```

### 3.2 진입점 B — 모달 폼 (`ModalManager.validateRules`)

`ModalManager.init()`의 `validateRules` 훅은 JustValidate 인스턴스를 인자로 받는다. 모달이 **열릴 때마다** 인스턴스가 생성되고, **닫힐 때마다** `destroy()`된다.

```javascript
ModalManager.init('sms-detail-modal', {
    validateRules(validator) {
        validator
            .addField('#receiverNo', [{ rule: 'required' }])
            .addField('#senderNo',   [{ rule: 'required' }]);
    },
    async onSubmit() {
        // 저장 버튼 클릭 시 revalidate() 통과 후 자동으로 여기가 호출됨
        await HttpClient.post('/sms/update', FormBinder.toObject('#sms-detail-modal form'));
    }
});
```

저장 버튼은 `<form>` 바깥(`modal-footer`)에 있으므로 JustValidate가 제출을 자동 가로채지 못한다. `ModalManager`가 내부적으로 `validator.revalidate()`를 수동 호출하고, 통과해야 `onSubmit`을 실행한다.

---

## 4. 핵심 API 요약

프로젝트가 쓰는 JustValidate API는 이것뿐이다.

| API | 반환 | 용도 |
|---|---|---|
| `new JustValidate(formEl)` | 인스턴스 | 폼 1개에 검증기 1개 생성 (`FieldFormat`/`ModalManager` 내부) |
| `.addField(selector, rules)` | 인스턴스 (체인) | 필드별 규칙 등록. selector는 `#id` 형태 |
| `.revalidate()` | `Promise<boolean>` | 전체 폼 재검증 — `ModalManager.validateRules` 경로에서 직접 호출 |
| `.destroy()` | `void` | 인스턴스 정리 — 모달 닫기/재생성 시 누수 방지 |

규칙 객체 형태 (프로젝트 사용 예):

```javascript
{ rule: 'required' }
{ rule: 'email' }
{ rule: 'minLength', value: 4 }
{ rule: 'customRegexp', value: /^0\d{1,2}-?\d{3,4}-?\d{4}$/, errorMessage: '전화번호 형식이 올바르지 않습니다.' }
```

> `addField`의 selector는 **id 기반**(`#receiverNo`)이어야 한다. `FieldFormat`은 id가 없는 `[data-validate]` 요소에 `ff-{random}` 자동 id를 부여한 뒤 등록한다.
>
> `FieldFormat.validateForm`은 per-field `revalidateField`를 사용해 각 필드 오류를 개별 렌더링한다 (JustValidate 4.3.0 전체 폼 재검증 결함 우회).

---

## 5. 서버 `@Valid`와의 관계 — 최종 권위는 서버

클라이언트 검증과 서버 검증은 **역할이 다르며 서로 대체하지 않는다.**

```text
[클라이언트] JustValidate (FieldFormat / ModalManager)
   목적: 서버 왕복 없는 즉시 UX 피드백
   결과: 통과 못 하면 제출 차단
   한계: 우회 가능 — 보안 무효

        ↓ 제출

[서버] Spring @Valid (Controller DTO)
   목적: 최종 권위 검증
   결과: 실패 시 ApiResponse { code != 200, message, errors[] } 반환
```

서버 `@Valid` 실패 응답은 `http-client.js` 응답 인터셉터가 자동으로 처리한다:

1. `errors[]` 배열을 `"• 메시지"` 목록으로 포맷팅해 `Notify.alert` 표시.
2. 각 에러 필드를 DOM에서 찾아 `.is-invalid` 클래스 자동 부여.
   - 탐색 순서: `[data-field="${field}"]` → `#${field}` → `[name="${field}"]`
   - 사용자가 해당 필드를 수정(`input`/`change`)하면 `.is-invalid` 자동 제거.

따라서 화면 JS는 서버 검증 에러를 직접 처리할 필요가 없다 — `catch` 블록을 비워두는 것이 규약이다 (인터셉터가 이미 알림).

---

## 6. 주의사항 및 프로젝트 특이사항

### 6.1 클라이언트 검증은 절대 신뢰하지 않는다

JustValidate를 통과한 값도 서버에서 거부될 수 있다. 반대로 JS를 비활성화하거나 직접 요청을 보내면 클라이언트 검증은 완전히 우회된다. **모든 입력 검증은 서버 `@Valid`에 존재해야 한다.** 클라이언트 규칙은 서버 규칙의 부분집합(UX 미리보기)으로만 유지한다.

### 6.2 폼 밖 제출 버튼은 수동 `revalidate()`가 필요하다

저장 버튼이 `<form>` 바깥에 있으면(`modal-footer` 패턴) JustValidate가 자동 가로채지 못한다. 이 경우 `ModalManager`가 하듯 `validator.revalidate()`를 직접 호출해야 한다. `FieldFormat.validateForm(formEl)`은 per-field `revalidateField` 호출을 래핑한다.

> JustValidate 4.3.0의 `revalidate()`는 전체 폼 오류 라벨을 렌더링하지 않는 알려진 결함이 있다(upstream #155). `FieldFormat.validateForm`은 이 결함을 우회하기 위해 per-field `revalidateField`를 사용한다.

### 6.3 인스턴스는 반드시 `destroy()`한다

JustValidate 인스턴스는 이벤트 리스너를 등록한다. 모달을 닫거나 폼을 재생성할 때 `destroy()`하지 않으면 메모리 누수와 중복 검증이 발생한다.

- `ModalManager`: 모달이 닫힐 때(`hidden.coreui.modal`) 자동 `destroy()` + `form.reset()`.
- `FieldFormat`: 같은 폼에 `buildValidator`를 다시 호출하면 이전 인스턴스를 `destroy()`한 뒤 재생성 (WeakMap 관리).

### 6.4 selector는 id 기반 — 자동 id 부여에 의존한다

JustValidate는 필드를 CSS selector로 추적한다. `FieldFormat`은 id 없는 요소에 `ff-{random}` id를 자동 부여한다. 동적 폼에서 요소를 재생성하면 id도 바뀌므로, 재생성 후 `applyFieldFormats`를 다시 호출한다.

### 6.5 새 규칙은 화면 JS가 아니라 레지스트리에 추가한다

`data-validate` 토큰을 늘리려면 `FieldFormat.registerValidator()`로 등록한다. 화면마다 `new JustValidate()` + `addField()`를 흩어놓으면 인스턴스 lifecycle(destroy) 관리가 깨진다. 예외는 `ModalManager.validateRules` 훅뿐이다 (lifecycle을 ModalManager가 관리하므로 안전).

### 6.6 클라이언트 에러 표시와 서버 에러 표시는 다르다

- JustValidate 실패: 필드 아래에 JustValidate 자체 에러 메시지 렌더링.
- 서버 `@Valid` 실패: `Notify.alert` 모달 + 필드 `.is-invalid` 부트스트랩 스타일.

두 체계의 스타일이 다르지만 의도된 것이다. 서버 에러 표시를 직접 구현하지 않는다 (인터셉터가 처리).

### 6.7 JustValidate가 없어도 페이지는 동작한다

`FieldFormat`은 `typeof JustValidate === 'undefined'`면 검증기 없이 진행한다 (`validateForm`은 `true` 반환). 라이브러리 로드 실패 시 클라이언트 검증만 비활성화될 뿐 제출/서버 검증은 살아있다. 이 가드를 제거하지 말 것.

---

## 7. 참조

- **MANIFEST.md**: `src/main/resources/static/lib/MANIFEST.md` — `static/lib` 표의 `just-validate.min.js` 행 (4.3.0, MIT, "서버 @Valid가 최종 권위")
- **공식 문서**: https://just-validate.dev/ (이 프로젝트는 4.3.0 production 빌드 vendored — CDN 참조 금지, `thymeleaf.md` 규칙)
- **프로젝트 래퍼 상세**: [`common-js_manual.md` 8장](./common-js_manual.md) — `FieldFormat` (`validateForm`, `registerValidator`, `validatorRegistry`)
- **모달 검증**: [`common-js_manual.md` 5장](./common-js_manual.md) — `ModalManager.init()`의 `validateRules` 훅과 `revalidate()` 흐름
- **서버 에러 처리**: `docs/base/common-response-contract.md` — `ApiResponse.errors[]`와 인터셉터의 `.is-invalid` 자동 부여
