# axios 매뉴얼 (프로젝트 한정)

> 대상 라이브러리: **axios 1.16.1** (vendored, CDN 미사용)
>
> 대상 파일
> - `src/main/resources/static/lib/axios.min.js` — 라이브러리 본체
> - `src/main/resources/static/js/common/http-client.js` — 전역 인터셉터 + 호출 래퍼(`HttpClient`)
>
> 이 문서는 axios 전체 API를 다루지 않는다. **이 프로젝트에서 실제로 쓰는 부분만** 다룬다.
> 관련 문서: [`common-js_manual.md`](./common-js_manual.md) (http-client.js 상세), `docs/base/common-response-contract.md` (ApiResponse 규격)

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 방식 — 두 가지 호출 경로](#3-프로젝트-사용-방식--두-가지-호출-경로)
4. [전역 인터셉터가 하는 일](#4-전역-인터셉터가-하는-일)
5. [핵심 API 요약](#5-핵심-api-요약)
6. [주의사항 및 프로젝트 특이사항](#6-주의사항-및-프로젝트-특이사항)
7. [참조](#7-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 라이브러리 | axios |
| 버전 | **1.16.1** (파일 헤더 `/*! Axios v1.16.1 ... */`로 확인) |
| 라이선스 | MIT |
| 출처 | https://www.npmjs.com/package/axios |
| 용도 | HTTP 클라이언트. 이 프로젝트의 **모든 AJAX 통신은 axios로 통일**한다 (`fetch` 사용 금지) |

`MANIFEST.md` 등재 행:

```text
| axios.min.js | axios | 1.16.1 | https://www.npmjs.com/package/axios | MIT | (기존) | HTTP 클라이언트 |
```

---

## 2. 프로젝트 로드 방식

### 2.1 스크립트 태그 위치

`src/main/resources/templates/defaultLayout.html`의 `<head>` 내부:

```html
<script src="/lib/axios.min.js"></script>     <!-- 21행 -->
<script src="/lib/dayjs.min.js"></script>
<script src="/lib/ko.js"></script>
<script src="/lib/lucide.js"></script>
<script src="/lib/imask.min.js"></script>
<script src="/lib/just-validate.min.js"></script>
...
<script th:src="@{/js/common/notify.js}"></script>        <!-- 42행 -->
<script th:src="@{/js/common/http-client.js}"></script>   <!-- 43행: axios 래퍼 -->
```

### 2.2 로드 순서와 전역 변수

| 순서 | 파일 | 전역 변수 |
|---|---|---|
| 1 | `/lib/axios.min.js` | `window.axios` |
| 2 | `/js/common/notify.js` | `window.Notify` (에러 알림에 필요) |
| 3 | `/js/common/http-client.js` | `window.HttpClient`, `window.ApiClient` (호환 alias) |

순서가 중요한 이유:

- `http-client.js`는 로드 즉시 `axios.interceptors.*`를 등록한다. **axios가 먼저 로드되지 않으면** `typeof axios === 'undefined'` 검사에서 `console.error` 후 즉시 종료되어 `HttpClient`가 정의되지 않는다.
- 응답 인터셉터의 에러 알림은 `window.Notify`를 사용한다. `Notify`가 없으면 크래시하지 않지만(`if (notify)` 가드) 에러 모달이 표시되지 않는다.

> 로그인 페이지(`login.html`)는 `defaultLayout`을 사용하지 않으므로 axios/인터셉터가 없다. 로그인 페이지에서 별도 AJAX가 필요하면 `axios.min.js`를 직접 로드해야 한다.

---

## 3. 프로젝트 사용 방식 — 두 가지 호출 경로

이 프로젝트에는 axios를 부르는 경로가 **두 가지** 있고, 둘 다 현업 코드에 존재한다. 어느 쪽으로 부르든 전역 인터셉터(4장)는 동일하게 적용된다.

### 3.1 경로 A — `HttpClient` / `ApiClient` 래퍼 (권장)

`http-client.js`가 제공하는 얇은 래퍼. **`response.data`를 직접 반환**한다 (인터셉터가 ApiResponse 언래핑까지 마친 상태).

```javascript
// GET — 두 번째 인자가 params
const list = await HttpClient.get('/sms/example-history/data', { page: 1, perPage: 20 });

// POST — 두 번째 인자가 body
await HttpClient.post('/system/menu-manage/create', payload);

// 스캐폴드 삭제 엔드포인트 (POST + 쿼리 파라미터)
await HttpClient.remove('/sms/example-history/delete', { itemId: id });
```

`window.ApiClient = HttpClient`로 동일 객체를 가리킨다. 실제 사용 예 (`sms/campaign-register.js`):

```javascript
await ApiClient.post(API.create, FormBinder.toObject('#campaign-form'));
CommonUtils.toast('등록되었습니다.', 'success');
```

사용 화면: `basic/notice.js`, `basic/notice-popup.js`, `sms/customer-search.js`, `sms/campaign-register.js`, `system/message-edit.js`.

### 3.2 경로 B — `axios.*` 직접 호출

래퍼 없이 `axios.get/post`를 직접 부른다. 이 경우 **반환값이 axios 응답 객체**이므로 `res.data`를 한 번 더 열어야 한다. 단, 인터셉터가 ApiResponse를 이미 언래핑했으므로 `res.data`는 곧 업무 페이로드다 (`{code, message, data}` 껍데기가 아님).

실제 사용 예 (`system/menu-manage.js`):

```javascript
const res = await axios.get(API.detail, { params: { menuId } });
const detail = res.data || {};      // 인터셉터 언래핑 완료 — 곧바로 업무 데이터
const menu = detail.menu || {};
```

```javascript
// 삭제: POST + params (HttpClient.remove와 동일 형태)
await axios.post(API.delete, null, { params: { menuId } });
```

사용처: `system/menu-manage.js`, `system/scaffold.js`, `common/tui-page-builder.js`, `common/common-utils.js`(콤보/자동완성 내부).

### 3.3 두 경로의 차이

| | `HttpClient.get(url, params)` | `axios.get(url, { params })` |
|---|---|---|
| 반환값 | **업무 페이로드** (`response.data`) | **axios 응답 객체** → `res.data`가 업무 페이로드 |
| params 전달 | 두 번째 인자 | `config.params` |
| 에러 처리 | 인터셉터가 `Notify.alert` + reject | 동일 (인터셉터 공용) |
| 스피너/CSRF/세션만료 | 자동 | 자동 |

> 신규 코드는 `HttpClient`를 권장한다. 기존 `axios.*` 직접 호출 코드도 인터셉터 혜택을 모두 받으므로 동작상 문제는 없다. **`fetch`만 쓰지 않으면 된다.**

---

## 4. 전역 인터셉터가 하는 일

`http-client.js`가 등록한 전역 인터셉터는 모든 axios 요청(경로 A·B 공통)에 적용된다.

### 4.1 요청 인터셉터

1. **글로벌 스피너 표시** — `#global-spinner-overlay`에 `active` 클래스 추가. 동시 요청은 `ajaxCount`로 추적해 마지막 응답이 와야 숨긴다.
2. **CSRF 헤더 삽입** — `<meta name="_csrf">`, `<meta name="_csrf_header">` 값을 읽어 요청 헤더에 설정. 메타 태그는 `defaultLayout.html`이 Thymeleaf로 주입한다.

### 4.2 응답 인터셉터 (성공)

```text
응답 수신 → 스피너 해제
  → 세션 만료 판별 (content-type이 text/html 또는 본문이 로그인 페이지 마커 매칭)
      → /login 전환 + 영동결 Promise 반환(후속 .then 미실행)
  → ApiResponse 규격 (response.data.code 존재)
      → code === 200: response.data = response.data.data  ← 언래핑
      → code !== 200: Notify.alert(message) 후 reject (HTTP 200이어도 비즈니스 에러)
```

### 4.3 응답 인터셉터 (에러)

```text
에러 수신 → 스피너 해제
  → 401/403: /login 전환
  → error.response.data 존재:
      → message 추출
      → errors[] 배열 존재 (@Valid 실패):
          → "• 메시지" 목록으로 포맷팅하여 Notify.alert
          → 해당 DOM 필드에 .is-invalid 자동 부여 (input/change 시 자동 해제)
  → 그 외: Notify.alert('서버와 통신 중 알 수 없는 오류가 발생했습니다.', '시스템 오류')
  → reject(error)
```

`@Valid` 필드 에러의 필드 탐색 순서: `[data-field="${field}"]` → `#${field}` → `[name="${field}"]`.

### 4.4 화면 코드에서의 결과

인터셉터가 알림을 표시하므로, 화면 JS의 `catch` 블록은 **비워두는 것이 규약**이다:

```javascript
try {
    await axios.post(API.update, payload);
    CommonUtils.toast('수정되었습니다.', 'success');
} catch (e) {
    // 인터셉터가 이미 Notify.alert을 표시함 — 중복 알림 금지
}
```

---

## 5. 핵심 API 요약

프로젝트에서 쓰는 axios API는 이것뿐이다.

| API | 용도 | 프로젝트 예 |
|---|---|---|
| `axios.get(url, config)` | 조회. 쿼리 파라미터는 `config.params` | `axios.get(API.tree)` |
| `axios.post(url, data, config)` | 등록/수정/삭제(POST 기반) | `axios.post(API.delete, null, { params: { menuId } })` |
| `axios.interceptors.request.use(fn)` | 전역 요청 인터셉터 | `http-client.js`만 사용 — 화면에서 추가 등록 금지 |
| `axios.interceptors.response.use(fn)` | 전역 응답 인터셉터 | 동일 |

래퍼(`HttpClient`) 전체 메서드:

| 메서드 | HTTP | 시그니처 | 비고 |
|---|---|---|---|
| `get` | GET | `get(url, params = {}, config = {})` | |
| `post` | POST | `post(url, data = {}, config = {})` | |
| `put` | PUT | `put(url, data = {}, config = {})` | |
| `delete` | DELETE | `delete(url, config = {})` | params는 `config.params`로 |
| `remove` | POST | `remove(url, params = {}, config = {})` | **스캐폴드 delete 엔드포인트용** (본문 없이 쿼리 파라미터) |

> `delete` vs `remove`: 스캐폴드가 생성한 삭제 엔드포인트는 POST 기반이므로 `remove`를 쓴다. 실제 HTTP DELETE 메서드가 필요한 경우에만 `delete`.

---

## 6. 주의사항 및 프로젝트 특이사항

### 6.1 ApiResponse 언래핑을 전제로 코드를 짠다

서버는 `{ code, message, data }` 규격으로 응답하지만, 인터셉터가 `code === 200`이면 `data`로 덮어쓴다. 따라서:

- `HttpClient.get()`의 반환값은 **곧바로 목록/객체**다. `.data`를 한 번 더 열면 `undefined`가 나온다.
- `axios.get()` 직접 호출 시 `res.data`가 업무 페이로드다. `res.data.data`로 열면 깨진다.
- 서버가 ApiResponse 규격이 아닌 응답(파일 다운로드 등)을 주면 언래핑 없이 그대로 통과한다.

### 6.2 세션 만료 시 Promise가 영원히 대기한다

세션 만료(응답이 로그인 페이지 HTML이거나 401/403)면 인터셉터가 `/login`으로 전환하고 **해결되지 않는 Promise**(`haltChain`)를 반환한다. 즉:

- `await` 이후 코드가 실행되지 않는다 (정상 — 페이지가 전환되므로 의도된 동작).
- `finally` 블록도 실행되지 않는다. 세션 만료에 의존하는 정리 로직을 `finally`에 넣지 말 것.
- 중복 전환은 내부 플래그(`SESSION_REDIRECTING.done`)로 방지된다.

### 6.3 스피너는 자동이다 — 수동 로딩 표시 금지

모든 axios 요청에 글로벌 스피너가 붙는다. 화면에서 별도 로딩 오버레이를 만들지 않는다. `ajaxCount` 기반이라 동시 요청 중에는 계속 표시되고, 마지막 응답이 끝나야 사라진다.

### 6.4 CSRF 메타 태그가 없으면 POST가 403 난다

인터셉터는 `defaultLayout.html`이 주입하는 `<meta name="_csrf">` / `<meta name="_csrf_header">`를 읽는다. 이 메타가 없는 페이지(레이아웃 미사용)에서 POST하면 CSRF 검증 실패로 403 → 인터셉터가 `/login`으로 보내버린다. 업무 화면은 반드시 `layout:decorate="~{defaultLayout}"`를 사용한다.

### 6.5 인터셉터는 한 벌만 — 화면에서 추가 등록 금지

`axios.interceptors.*`는 `http-client.js`가 전역으로 등록한다. 화면 JS에서 인터셉터를 추가하면 스피너/언래핑 순서가 꼬인다. 개별 요청 설정(예: `responseType: 'blob'`)은 세 번째 `config` 인자로 전달한다.

### 6.6 파일 다운로드는 axios가 아니라 `location.href`

엑셀 다운로드는 `PAGE_AUTH.download` 권한 확인 후 `window.location.href = url + '?' + params`로 처리한다(Scaffold EXCEL 생성 패턴). 브라우저 다운로드를 axios로 가로채지 않는다.

### 6.7 에러 알림 중복 금지

인터셉터가 `Notify.alert`으로 에러를 표시한다. 화면 `catch`에서 다시 `Notify.alert`/`toast('error')`를 부르면 모달이 두 번 뜬다(큐에 쌓여 순차 표시됨). 성공 토스트만 화면에서 띄운다.

---

## 7. 참조

- **MANIFEST.md**: `src/main/resources/static/lib/MANIFEST.md` — `static/lib` 표의 `axios.min.js` 행 (버전 1.16.1, MIT)
- **공식 문서**: https://axios-http.com/docs/intro (이 프로젝트는 1.16.1 vendored — CDN 참조 금지, `thymeleaf.md` 규칙)
- **프로젝트 래퍼 상세**: [`common-js_manual.md` 4장](./common-js_manual.md) — `http-client.js` 인터셉터 전체 흐름
- **응답 규격**: `docs/base/common-response-contract.md` — `ApiResponse { code, message, data }`와 `errors[]`
