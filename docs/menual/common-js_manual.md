# 공통 JavaScript 모듈 매뉴얼 (비-TUI 6종)

> 대상 파일
> - `src/main/resources/static/js/common/http-client.js`
> - `src/main/resources/static/js/common/notify.js`
> - `src/main/resources/static/js/common/modal-manager.js`
> - `src/main/resources/static/js/common/common-utils.js`
> - `src/main/resources/static/js/common/form-binder.js`
> - `src/main/resources/static/js/common/field-format.js`
>
> **이 문서에서 다루지 않는 것**: `tui-common.js`, `tui-page-builder.js` → [`tui_manual.md`](./tui_manual.md) 참고
>
> 관련 규약: `docs/base/screen-convention.md`, `docs/base/common-response-contract.md`

---

## 목차

1. [개요](#1-개요)
2. [로드 순서와 의존 체인](#2-로드-순서와-의존-체인)
3. [`notify.js` — `window.Notify`](#3-notifyjs--windownotify)
4. [`http-client.js` — `window.HttpClient` / `window.ApiClient`](#4-http-clientjs--windowhttpclient--windowapiclient)
5. [`modal-manager.js` — `window.ModalManager`](#5-modal-managerjs--windowmodalmanager)
6. [`common-utils.js` — `window.CommonUtils`](#6-common-utilsjs--windowcommonutils)
7. [`form-binder.js` — `FormBinder`](#7-form-binderjs--formbinder)
8. [`field-format.js` — `FieldFormat`](#8-field-formatjs--fieldformat)
9. [모듈 간 상호작용](#9-모듈-간-상호작용)
10. [`SESSION_INFO` / `PAGE_AUTH` 전역 변수](#10-session_info--page_auth-전역-변수)
11. [화면 JS 실전 사용 패턴](#11-화면-js-실전-사용-패턴)
12. [주의사항 및 흔한 실수](#12-주의사항-및-흔한-실수)

---

## 1. 개요

### 1.1 공통 JS 모듈 전체 지도

이 프로젝트의 `static/js/common/`에는 **8개의 공통 JS 모듈**이 있다. 모두 Vanilla JS이며, 모듈 번들러 없이 `<script>` 태그로 로드되어 전역 변수(window 프로퍼티 또는 전역 `const`)를 노출한다.

| # | 파일 | 전역 이름 | 형태 | 핵심 역할 |
|---|---|---|---|---|
| 1 | `notify.js` | `window.Notify` | IIFE | SweetAlert2 11.26.25 기반 차단형 alert/confirm + toast, 모달 열기/닫기 도우미 |
| 2 | `http-client.js` | `window.HttpClient`, `window.ApiClient` | IIFE | axios interceptor (spinner·CSRF·세션만료·ApiResponse 언래핑) + HTTP 호출 래퍼 |
| 3 | `modal-manager.js` | `window.ModalManager` | IIFE | 비즈니스 모달 lifecycle 관리 (훅, 검증, 폼 리셋) |
| 4 | `common-utils.js` | `window.CommonUtils` | IIFE | 콤보박스 자동 생성, 날짜 기본값, 포매터, autocomplete |
| 5 | `form-binder.js` | `FormBinder` (전역 const) | Revealing Module | 상세폼 자동 바인딩 (JSON → form, form → 전송 객체) |
| 6 | `field-format.js` | `FieldFormat` (전역 const) | Revealing Module | IMask 입력 마스킹 + JustValidate 클라이언트 검증 |
| 7 | `tui-common.js` | `TuiCommon` (전역 const) | IIFE | TUI Grid formatter·기본 옵션·페이징·엑셀 (별도 매뉴얼) |
| 8 | `tui-page-builder.js` | `TuiPageBuilder` (전역 class) | Class | 그리드+검색+페이징 Facade (별도 매뉴얼) |

> 이 문서는 **1~6번** 모듈을 다룬다. 7~8번은 [`tui_manual.md`](./tui_manual.md) 참고.

### 1.2 설계 원칙

- **Vanilla JS, 번들러 없음.** 모든 파일은 IIFE 또는 Revealing Module 패턴으로 작성되며, `import`/`export`를 사용하지 않는다.
- **전역 변수가 곧 공개 API.** 화면 JS는 `window.Notify`, `HttpClient`, `FormBinder` 등을 직접 참조한다.
- **서버 `@Valid`가 최종 권위.** 클라이언트 검증(`field-format.js`)은 UX 보조일 뿐이다.
- **CDN 금지.** 모든 라이브러리는 `static/lib/`, `static/vendor/` 로컬 파일만 사용한다.
- **화면 JS는 `fetch`를 쓰지 않는다.** HTTP는 axios + 전역 interceptor로 통일한다.

---

## 2. 로드 순서와 의존 체인

### 2.1 `defaultLayout.html` 로드 순서

모든 업무 화면은 `layout:decorate="~{defaultLayout}"`로 이 레이아웃을 상속한다. `<head>` 내 스크립트 로드 순서는 **고정**되어 있으며, 순서를 바꾸면 동작이 깨진다.

```html
<!-- ① 외부 라이브러리 (static/lib 로컬 참조) -->
<script src="/lib/tui-pagination.min.js"></script>
<script src="/lib/xlsx.full.min.js"></script>
<script src="/lib/tui-date-picker.min.js"></script>
<script src="/lib/tui-grid.min.js"></script>
<script src="/lib/axios.min.js"></script>          <!-- http-client.js의 전제 -->
<script src="/lib/dayjs.min.js"></script>           <!-- common-utils.js의 전제 -->
<script src="/lib/ko.js"></script>                  <!-- dayjs 한국어 로케일 -->
<script src="/lib/lucide.js"></script>              <!-- notify.js 아이콘 -->
<script src="/lib/imask.min.js"></script>           <!-- field-format.js 마스킹 -->
<script src="/lib/just-validate.min.js"></script>   <!-- field-format.js, modal-manager.js 검증 -->
<script src="/lib/sweetalert2/11.26.25/sweetalert2.all.min.js"></script> <!-- notify.js alert/confirm -->
<script>dayjs.locale('ko');</script>

<!-- ② CoreUI 번들 (CSS는 head, JS는 body 말미) -->
<link rel="stylesheet" th:href="@{/vendor/coreui/css/coreui.min.css}">
<link rel="stylesheet" th:href="@{/lib/sweetalert2/11.26.25/sweetalert2.min.css}">
<!-- ... 프로젝트 CSS ... -->

<!-- ③ 공통 JS — 의존 순서대로 로드 (순서 변경 금지) -->
<script th:src="@{/js/common/notify.js}"></script>          <!-- 1st (SweetAlert2 의존) -->
<script th:src="@{/js/common/http-client.js}"></script>     <!-- 2nd -->
<script th:src="@{/js/common/modal-manager.js}"></script>   <!-- 3rd -->
<script th:src="@{/js/common/common-utils.js}"></script>    <!-- 4th -->
<script th:src="@{/js/common/form-binder.js}"></script>     <!-- 5th -->
<script th:src="@{/js/common/field-format.js}"></script>    <!-- 6th -->
<script th:src="@{/js/common/tui-common.js}"></script>      <!-- 7th -->
<script th:src="@{/js/common/tui-page-builder.js}"></script><!-- 8th -->

<!-- ④ Thymeleaf 전역 변수 주입 (공통 JS 이후, 화면 JS 이전) -->
<script th:inline="javascript">
    const SESSION_INFO = { empId, depId, empNm, depNm };
    window.PAGE_AUTH = { read, create, update, delete, approve, cancel, download, maskView };
</script>

<!-- ⑤ CoreUI 번들 JS (body 말미) -->
<script th:src="@{/vendor/coreui/js/coreui.bundle.min.js}"></script>

<!-- ⑥ 화면 JS (layout:fragment="script") -->
```

### 2.2 의존 체인 다이어그램

```text
axios (lib)
  │
  ├──► http-client.js ──► window.Notify (에러 알림)
  │         │
  │         └──► window.HttpClient / window.ApiClient
  │
lucide (lib) ──► notify.js ──► window.Notify
SweetAlert2 (lib) ──┘
CoreUI/Bootstrap (lib) ──┘
  │
  ├──► modal-manager.js ──► window.ModalManager
  │         │                  (내부적으로 CoreUI Modal 인스턴스 사용)
  │         └──► JustValidate (옵션, validateRules 사용 시)
  │
dayjs (lib) ──► common-utils.js ──► window.CommonUtils
axios (lib) ──┘                        (Notify getter re-export)
  │
  ├──► form-binder.js ──► FormBinder (전역 const)
  │         └──► field-format.js의 el._imask 참조 (unmaskedValue)
  │
IMask (lib) ──► field-format.js ──► FieldFormat (전역 const)
JustValidate (lib) ──┘
```

### 2.3 순서가 중요한 이유

| 순서 위반 시나리오 | 결과 |
|---|---|
| `http-client.js`를 `notify.js`보다 먼저 로드 | HTTP 에러 발생 시 `window.Notify`가 `undefined` → 에러 알림 모달이 표시되지 않음 (코드는 `if (notify)` 가드로 침묵) |
| `common-utils.js`를 `notify.js`보다 먼저 로드 | `CommonUtils.toast/alert/confirm` getter가 `() => {}` 빈 함수를 반환 → 기존 호출부가 조용히 실패 |
| `axios.min.js`를 `http-client.js`보다 먼저 로드하지 않음 | `typeof axios === 'undefined'` → `console.error` 후 IIFE가 즉시 return → `HttpClient` 미정의 |
| `field-format.js`를 `form-binder.js`보다 먼저 로드하지 않음 | `FormBinder.toObject()`에서 `field._imask`가 항상 `undefined` → 마스크 필드의 표시값(하이픈 포함)이 그대로 전송됨 |
| `sweetalert2.all.min.js`를 `notify.js`보다 나중에 로드하거나 CSS를 CoreUI CSS 이전에 로드 | `window.Swal`이 `undefined` → notify.js alert/confirm이 fail-closed: confirm은 onCancel만 호출, alert는 callback 없이 큐 진행. CSS 순서 위반 시 SweetAlert2 스타일이 CoreUI에 덮어씌워져 UI 깨짐 |
| SweetAlert2를 notify.js 뒤에 로드 | `window.Swal`이 `undefined` → alert/confirm이 fail-closed 동작. notify.js가 SweetAlert2를 의존하므로 반드시 notify.js 이전에 로드해야 함 |

---

## 3. `notify.js` — `window.Notify`

### 3.1 목적과 책임

화면 공통 알림을 담당한다. 세 가지 알림 방식(toast, alert, confirm)과 프레임워크 모달(CoreUI/Bootstrap) 열기/닫기 도우미를 제공한다.

- **toast**: 우측 상단 비차단 팝업. 성공/실패/경고 피드백용.
- **alert**: 차단형 알림 모달. 확인 버튼만 있음.
- **confirm**: 차단형 확인 모달. 확인 + 취소 버튼. 콜백 기반.
- **큐 기반 순차 처리**: alert/confirm을 동시에 여러 번 호출해도 하나씩 순서대로 표시된다.

### 3.2 공개 API

```javascript
window.Notify = {
    toast,            // (msg: string, type?: string) => void
    alert,            // (msg: string, title?: string, callback?: function) => void
    confirm,          // (msg: string, callback?: function, title?: string, onCancel?: function) => void
    refreshIcons,     // () => void
    showModal,        // (el: HTMLElement) => void
    hideModal,        // (el: HTMLElement) => void
    getFrameworkModal // (el: HTMLElement) => object|null
};
```

#### `Notify.toast(msg, type)`

우측 상단에 toast 팝업을 표시한다. 3초 후 자동 사라진다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `msg` | `string` | (필수) | 표시할 메시지. **HTML 이스케이프되지 않으므로** 사용자 입력을 직접 넣지 말 것 |
| `type` | `string` | `'info'` | `'info'` \| `'success'` \| `'error'` \| `'warning'` |

```javascript
// 저장 성공 피드백
Notify.toast('등록되었습니다.', 'success');

// 권한 없음 경고
Notify.toast('엑셀 다운로드 권한이 없습니다.', 'warning');

// 기본 info
Notify.toast('검색 조건을 확인하세요.');
```

> **내부 동작**: `#toast-container`가 없으면 자동 생성한다. CoreUI Toast → Bootstrap Toast → 수동 CSS 토글 순으로 폴백한다. lucide 아이콘(`circle-check`, `circle-x`, `triangle-alert`, `info`)이 메시지 앞에 렌더링된다.

#### `Notify.alert(msg, title, callback)`

차단형 알림 모달을 SweetAlert2 `Swal.fire()`로 렌더링한다. 확인 버튼 클릭 시에만 `callback`이 실행된다. backdrop 클릭, ESC, close 버튼 dismiss 시 callback 없이 큐만 진행한다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `msg` | `string` | (필수) | 본문 메시지. `white-space: pre-line`이라 `\n` 줄바꿈이 반영됨 |
| `title` | `string` | `'알림'` | 모달 제목 |
| `callback` | `function` | `undefined` | 확인 버튼 클릭 후 실행 |

```javascript
// 단순 알림
Notify.alert('menuId를 입력하세요.');

// 제목 + 콜백
Notify.alert('세션이 만료되었습니다.', '오류', () => {
    location.href = '/login';
});
```

> **내부 동작**: SweetAlert2 `Swal.fire()`로 렌더링. 확인 버튼 클릭 시에만 callback 실행. dismiss(backdrop/ESC/close) 시 callback 없이 큐만 진행. SweetAlert2 미로드 시 fail-closed: callback 없이 큐 진행.

#### `Notify.confirm(msg, callback, title, onCancel)`

차단형 확인 모달을 SweetAlert2 `Swal.fire()`로 렌더링한다. `result.isConfirmed`일 때만 `callback(onConfirm)`이 실행된다. 모든 dismiss(cancel/backdrop/ESC/close)는 `onCancel`을 정확히 1회 호출한다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `msg` | `string` | (필수) | 본문 메시지 |
| `callback` | `function` | `undefined` | **확인** 버튼 클릭 시 실행 |
| `title` | `string` | `'알림'` | 모달 제목 |
| `onCancel` | `function` | `undefined` | **취소** 버튼 또는 ESC 키 입력 시 실행 |

```javascript
// 삭제 확인
Notify.confirm('메뉴 [M001] 를 삭제하시겠습니까?', async () => {
    await axios.post('/system/menu-manage/delete', null, { params: { menuId: 'M001' } });
    Notify.toast('삭제되었습니다.', 'success');
});

// 취소 콜백 포함
Notify.confirm('변경사항을 저장하지 않고 나가시겠습니까?', () => {
    location.href = '/sms/example-history';
}, '확인', () => {
    // 취소 시 아무것도 안 함
});
```

> **주의**: 파라미터 순서가 `alert`와 다르다. `alert(msg, title, callback)` vs `confirm(msg, callback, title, onCancel)`.
>
> **내부 동작**: SweetAlert2 `Swal.fire()`로 렌더링. `result.isConfirmed`일 때만 `callback` 실행. 모든 dismiss(cancel/backdrop/ESC/close)는 `onCancel`을 정확히 1회 호출. callback의 동기 throw와 비동기 reject는 `console.error`로 기록하고 큐는 계속 진행. SweetAlert2 미로드 시 fail-closed: `onConfirm` 미실행, `onCancel` 1회 호출 후 큐 진행.

#### `Notify.refreshIcons()`

lucide 아이콘을 재렌더링한다. 동적으로 DOM에 `<i data-lucide="...">` 요소를 추가한 후 호출한다.

```javascript
container.innerHTML = '<i data-lucide="check"></i> 완료';
Notify.refreshIcons();  // lucide.createIcons() 래핑
```

#### `Notify.showModal(el)` / `Notify.hideModal(el)`

프레임워크 모달(CoreUI/Bootstrap)을 열거나 닫는다. 프레임워크가 없으면 수동 CSS 토글로 폴백한다.

```javascript
const modalEl = document.getElementById('my-modal');
Notify.showModal(modalEl);   // 열기
Notify.hideModal(modalEl);   // 닫기
```

#### `Notify.getFrameworkModal(el)`

CoreUI 또는 Bootstrap Modal 인스턴스를 반환한다. 둘 다 없으면 `null`.

```javascript
const instance = Notify.getFrameworkModal(document.getElementById('my-modal'));
if (instance) instance.toggle();
```

### 3.3 내부 동작 — 큐 기반 모달

alert/confirm은 내부 큐(`_queue`)로 관리된다. `_showing` 플래그가 `true`면 새 모달을 즉시 표시하지 않고 큐에 쌓는다. 현재 팝업이 닫히면(`finally`에서 `_showing=false` + `_processQueue()`) 다음 큐 아이템을 렌더링한다.

```text
Notify.alert('A')  →  즉시 표시
Notify.alert('B')  →  큐에 대기
Notify.alert('C')  →  큐에 대기
   ... A 확인 클릭 → A 닫힘 → B 표시 → B 확인 → C 표시
```

이 덕분에 HTTP interceptor가 에러 알림을 띄우는 동시에 화면 JS도 알림을 띄우는 경우, 모달이 겹치지 않는다.

SweetAlert2가 DOM/focus/backdrop을 자체 관리하므로 `#custom-modal-overlay` DOM 생성이 더 이상 없다.

### 3.4 DOM 자동 생성

SweetAlert2가 자체 DOM을 관리하므로 별도 DOM 생성 없음.

---

## 4. `http-client.js` — `window.HttpClient` / `window.ApiClient`

### 4.1 목적과 책임

전역 HTTP 인프라를 담당한다. 네 가지 역할을 한다.

1. **글로벌 스피너**: 모든 axios 요청 시작/종료 시 `#global-spinner-overlay` 토글. 다중 동시 요청은 `ajaxCount`로 추적.
2. **CSRF 헤더 자동 삽입**: `<meta name="_csrf">`, `<meta name="_csrf_header">` 값을 읽어 요청 헤더에 추가.
3. **세션 만료 감지**: 응답이 HTML(로그인 페이지)이거나 401/403이면 `/login`으로 전환.
4. **ApiResponse 언래핑**: 서버 `ApiResponse` 규격(`{ code, message, data }`)에서 `code === 200`이면 `data`만 추출, 그 외에는 `Notify.alert` 후 reject.

### 4.2 공개 API

```javascript
window.HttpClient = { get, post, put, delete: deleteFn, remove };
window.ApiClient  = HttpClient;  // 기존 api-client.js 호환 alias
```

모든 메서드는 **`response.data`를 직접 반환**한다 (인터셉터가 이미 ApiResponse 언래핑을 완료했으므로).

#### `HttpClient.get(url, params, config)`

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `url` | `string` | (필수) | 요청 URL |
| `params` | `object` | `{}` | 쿼리 파라미터 (axios `config.params`로 병합) |
| `config` | `object` | `{}` | 추가 axios 설정 |
| **반환** | `Promise<any>` | | `response.data` (ApiResponse 언래핑 후) |

```javascript
// 기본 조회
const list = await HttpClient.get('/sms/example-history/data', { page: 1, size: 20 });

// 추가 config (예: responseType)
const blob = await HttpClient.get('/api/export', {}, { responseType: 'blob' });
```

#### `HttpClient.post(url, data, config)`

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `url` | `string` | (필수) | 요청 URL |
| `data` | `object` | `{}` | 요청 본문 (JSON) |
| `config` | `object` | `{}` | 추가 axios 설정 |
| **반환** | `Promise<any>` | | `response.data` |

```javascript
// 메뉴 등록
await HttpClient.post('/system/menu-manage/create', {
    menuId: 'M001',
    menuNm: '테스트 메뉴',
    menuType: 'M'
});
```

#### `HttpClient.put(url, data, config)`

`post`와 동일하나 HTTP PUT 메서드를 사용한다.

```javascript
await HttpClient.put('/api/settings', { theme: 'dark' });
```

#### `HttpClient.delete(url, config)`

HTTP DELETE 메서드를 사용한다. **`params` 파라미터가 없다.** 쿼리 파라미터가 필요하면 `config.params`를 사용한다.

```javascript
await HttpClient.delete('/api/items/42');

// 쿼리 파라미터 포함
await HttpClient.delete('/api/items', { params: { id: 42 } });
```

#### `HttpClient.remove(url, params, config)`

**스캐폴드 delete 엔드포인트 호환용.** HTTP POST로 요청하되, 본문 없이 `params`를 쿼리 스트링으로 보낸다.

```javascript
// POST /sms/example-history/delete?itemId=123
await HttpClient.remove('/sms/example-history/delete', { itemId: 123 });
```

> `delete` vs `remove` 구분: `delete`는 실제 HTTP DELETE 메서드, `remove`는 POST + 쿼리 파라미터. 스캐폴드가 생성한 삭제 엔드포인트는 POST 기반이므로 `remove`를 사용한다.

### 4.3 전역 interceptor 상세

#### 요청 interceptor

```text
모든 axios 요청 → showSpinner() → CSRF 헤더 삽입 → 요청 전송
```

- `<meta name="_csrf">`과 `<meta name="_csrf_header">`가 모두 존재하고 비어있지 않을 때만 헤더를 설정한다.
- `defaultLayout.html`이 이 메타 태그를 Thymeleaf로 주입한다.

#### 응답 interceptor (성공)

```text
응답 수신 → hideSpinner()
  → 세션 만료 판별 (content-type: text/html 또는 LOGIN_MARKERS 매칭)
     → 만료 시: redirectToLogin() + haltChain() (Promise 영동결)
  → ApiResponse 규격 (response.data.code 존재)
     → code === 200: response.data = response.data.data (언래핑)
     → code !== 200: Notify.alert(message) + reject
  → 일반 응답: 그대로 통과
```

#### 응답 interceptor (에러)

```text
에러 수신 → hideSpinner()
  → 401/403: redirectToLogin() + haltChain()
  → error.response.data 존재:
     → message 추출
     → errors[] 배열 존재 시 (@Valid 실패):
        → 필드별 에러를 "• 메시지" 목록으로 포맷팅
        → 해당 DOM 필드에 .is-invalid 클래스 추가 (input/change 시 자동 제거)
     → Notify.alert(displayMsg, '오류')
  → 그 외: Notify.alert('서버와 통신 중 알 수 없는 오류가 발생했습니다.', '시스템 오류')
  → reject(error)
```

### 4.4 세션 만료 판별 로직

Spring Security `formLogin(loginPage="/login")` 설정으로 인해, 세션 만료 시 서버는 `/login`으로 302 리다이렉트한다. axios는 이를 투명하게 따라가므로, API 호출이 **로그인 페이지 HTML(200, text/html)** 을 응답으로 받는다.

판별 조건:
1. `content-type` 헤더에 `text/html` 포함 → 세션 만료
2. 응답 본문이 문자열이면서 `/login-shell|login-form|SMS V3 로그인/` 정규식 매칭 → 세션 만료
3. HTTP 401 또는 403 → 세션 만료

`redirectToLogin()`은 중복 전환을 방지하기 위해 `SESSION_REDIRECTING.done` 플래그를 사용한다. 이미 `/login` 경로에 있으면 전환하지 않는다.

`haltChain()`은 **영원히 해결되지 않는 Promise**(`new Promise(() => {})`)를 반환하여, 세션 만료 후 후속 `.then()` 체인이 실행되지 않도록 한다.

### 4.5 `@Valid` 필드 에러 자동 하이라이트

서버가 `errors[]` 배열을 포함한 ApiResponse를 반환하면 (Spring `@Valid` 실패), interceptor가 자동으로 해당 DOM 필드를 찾아 `.is-invalid` 클래스를 추가한다.

필드 탐색 순서:
1. `[data-field="${e.field}"]`
2. `#${e.field}`
3. `[name="${e.field}"]`

사용자가 해당 필드에 `input` 또는 `change` 이벤트를 발생시키면 `.is-invalid`가 자동 제거된다 (`{ once: true }`).

---

## 5. `modal-manager.js` — `window.ModalManager`

### 5.1 목적과 책임

개발자가 수동으로 관리하는 비즈니스 모달의 lifecycle을 중앙화한다. 모달 열기/닫기, 저장/삭제 버튼 바인딩, JustValidate 검증, 폼 리셋을 자동으로 처리한다.

`Notify`의 alert/confirm이 **단순 알림용**이라면, `ModalManager`는 **폼이 포함된 업무 모달용**이다.

### 5.2 DOM id 계약

`fragments/modal-base.html` 프래그먼트와 일치하는 id 규칙을 따른다.

| 요소 | id 패턴 | 예시 |
|---|---|---|
| 모달 컨테이너 | `${modalId}` | `sms-detail-modal` |
| 제목 | `${modalId}-title` | `sms-detail-modal-title` |
| 저장 버튼 | `${modalId}-btn-save` | `sms-detail-modal-btn-save` |
| 삭제 버튼 | `${modalId}-btn-delete` | `sms-detail-modal-btn-delete` |

### 5.3 공개 API

```javascript
window.ModalManager = { init, open, close };
```

#### `ModalManager.init(modalId, options)`

모달을 초기화한다. lifecycle 훅을 연결하고, 저장/삭제 버튼에 이벤트 리스너를 바인딩한다. **`DOMContentLoaded` 이후에 호출**해야 한다.

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `modalId` | `string` | 모달 DOM id (`#` 제외) |
| `options` | `object` | lifecycle 훅 옵션 (아래 표) |
| **반환** | `object\|null` | CoreUI Modal 인스턴스. 프레임워크 없으면 `null` |

**options 훅:**

| 훅 | 시점 | 시그니처 | 비고 |
|---|---|---|---|
| `onMount` | `init()` 직후 1회 | `() => void` | 초기 데이터 로드 등 |
| `beforeOpen` | `show.coreui.modal` 이전 | `(event) => boolean` | `false` 반환 시 열기 취소 |
| `onOpen` | `shown.coreui.modal` 이후 | `() => void` | 포커스 이동, 데이터 갱신 |
| `onSubmit` | 저장 버튼 클릭 (검증 통과 후) | `() => void` | 저장 API 호출 |
| `onDelete` | 삭제 버튼 클릭 | `() => void` | 삭제 확인 + API 호출 |
| `onClose` | `hidden.coreui.modal` 이후 | `() => void` | 상태 초기화 |
| `validateRules` | 모달 열릴 때 JustValidate 룰 등록 | `(validator) => void` | JustValidate 인스턴스를 인자로 받음 |

```javascript
ModalManager.init('sms-detail-modal', {
    onMount() {
        console.log('모달 초기화 완료');
    },
    beforeOpen(e) {
        // 조건부 열기 차단
        if (!state.selectedId) {
            Notify.toast('행을 먼저 선택하세요.', 'warning');
            return false;
        }
        return true;
    },
    async onOpen() {
        const detail = await HttpClient.get('/sms/detail', { id: state.selectedId });
        FormBinder.bind('#sms-detail-modal form', detail);
    },
    async onSubmit() {
        const payload = FormBinder.toObject('#sms-detail-modal form');
        await HttpClient.post('/sms/update', payload);
        Notify.toast('수정되었습니다.', 'success');
        ModalManager.close('sms-detail-modal');
    },
    onDelete() {
        Notify.confirm('삭제하시겠습니까?', async () => {
            await HttpClient.remove('/sms/delete', { id: state.selectedId });
            Notify.toast('삭제되었습니다.', 'success');
            ModalManager.close('sms-detail-modal');
        });
    },
    onClose() {
        state.selectedId = null;
    },
    validateRules(validator) {
        validator
            .addField('#receiverNo', [{ rule: 'required' }])
            .addField('#senderNo', [{ rule: 'required' }]);
    }
});
```

#### `ModalManager.open(modalId)`

모달을 연다. `init()`을 먼저 호출해야 한다.

```javascript
ModalManager.open('sms-detail-modal');
```

#### `ModalManager.close(modalId)`

모달을 닫는다. 닫히면 `onClose` 훅이 실행되고, **폼이 자동 리셋**되며, JustValidate 인스턴스가 파괴된다.

```javascript
ModalManager.close('sms-detail-modal');
```

### 5.4 내부 동작

#### JustValidate lifecycle

- 모달이 **열릴 때마다** (`shown.coreui.modal`) `validateRules`로 새 JustValidate 인스턴스를 생성한다.
- 모달이 **닫힐 때마다** (`hidden.coreui.modal`) `validator.destroy()`를 호출하고 `form.reset()`을 실행한다.
- 이 패턴은 JustValidate의 메모리 누수를 방지한다.

#### 저장 버튼 검증 흐름

```text
저장 버튼 클릭
  → validators[modalId] 존재?
     → Yes: validator.revalidate() → isValid ? onSubmit() : (아무것도 안 함)
     → No:  onSubmit() 즉시 호출
```

저장 버튼은 `<form>` 바깥에 있으므로 (`modal-footer`), JustValidate가 자동으로 가로채지 못한다. 그래서 `revalidate()`를 수동 호출한다.

---

## 6. `common-utils.js` — `window.CommonUtils`

### 6.1 목적과 책임

Phase 1 리팩터링에서 `http-client.js`, `notify.js`, `modal-manager.js`로 분리된 후 남은 **순수 유틸** 모듈이다. 콤보박스 자동 생성, 날짜/시간 기본값, 데이터 포매터, autocomplete을 제공한다.

기존 `CommonUtils.toast/alert/confirm` 호출부와의 하위 호환을 위해, `Notify` 메서드를 getter로 re-export한다.

### 6.2 공개 API

```javascript
window.CommonUtils = {
    initCombos,          // () => Promise<void>
    initAutocomplete,    // (options) => { close, select }
    setDefaultDateTime,  // (forceReset?) => void
    resetFields,         // () => void
    fmt,                 // { money(val), phone(val) }
    // Notify re-export (getter)
    toast,               // Notify.toast
    alert,               // Notify.alert
    confirm,             // Notify.confirm
    refreshIcons         // Notify.refreshIcons
};
```

#### `CommonUtils.initCombos()`

`.common-combo[data-code-type]` 셀렉트 요소를 찾아, 공통 코드 API에서 옵션을 자동 로드한다. `DOMContentLoaded` 시 자동 실행된다.

```html
<!-- HTML -->
<select id="sendType" class="common-combo" data-code-type="SEND_TYPE">
    <option value="">전체</option>
</select>
```

```javascript
// 자동 실행되므로 별도 호출 불필요.
// 수동 재호출 (동적 추가 요소):
await CommonUtils.initCombos();
```

**내부 동작:**
- `GET /api/common-code/{type}`으로 코드 목록을 조회한다.
- 동일 `data-code-type`을 가진 여러 콤보는 캐시로 중복 요청을 방지한다.
- 기존 `<option>`(예: "전체")을 보존하면서 코드 옵션을 **추가**한다.
- 조회 실패 시 `console.error` 후 빈 배열로 처리한다.

#### `CommonUtils.initAutocomplete(options)`

텍스트 입력에 말풍선 자동완성을 부착한다.

| 옵션 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `inputEl` | `string\|HTMLElement` | (필수) | 텍스트 입력 요소 (셀렉터 또는 요소) |
| `balloonEl` | `string\|HTMLElement` | (필수) | 말풍선 컨테이너 요소 |
| `apiUrl` | `string` | (필수) | 검색 API URL |
| `syncCombo` | `string\|HTMLElement` | `null` | 선택 시 동기화할 셀렉트 |
| `paramName` | `string` | `'keyword'` | API 쿼리 파라미터 이름 |
| `minLength` | `number` | `1` | 검색 시작 최소 글자 수 |
| `debounceMs` | `number` | `200` | 디바운스 지연 (ms) |
| `renderItem` | `function` | 기본 템플릿 | `(item) => htmlString` 커스텀 렌더 |
| `onSelect` | `function` | `null` | `(item) => void` 선택 콜백 |
| **반환** | `object` | | `{ close(), select(code, name) }` |

```javascript
const ac = CommonUtils.initAutocomplete({
    inputEl:   '#bankCdText',
    balloonEl: '#bankBalloon',
    apiUrl:    '/api/common-code/bank',
    syncCombo: '#bankCdCombo',
    minLength: 1,
    debounceMs: 200,
    onSelect: (item) => console.log('선택:', item.code, item.name)
});

// 외부에서 프로그래매틱 선택
ac.select('001', '우리은행');

// 말풍선 닫기
ac.close();
```

**내부 동작:**
- 입력 시 `debounceMs` 후 `GET apiUrl?keyword=...` 호출.
- 전역 interceptor가 ApiResponse를 언래핑하므로 `res.data`가 곧 목록.
- 항목 클릭 시 `input.value = code` 설정 + `input` 이벤트 수동 dispatch.
- `syncCombo` 지정 시 콤보 ↔ 텍스트 양방향 동기화.
- 외부 클릭 시 말풍선 자동 닫힘.

#### `CommonUtils.setDefaultDateTime(forceReset)`

날짜/시간 검색 폼에 기본값을 채운다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `forceReset` | `boolean` | `false` | `true`면 기존 값이 있어도 덮어씀 |

**대상 요소 (id 기준):**

| 패턴 | 요소 | 기본값 |
|---|---|---|
| 분리형 | `#startDate`, `#endDate` | 오늘 (`YYYY-MM-DD`) |
| 분리형 | `#startTime`, `#endTime` | 현재 ± 1시간 (`HH:mm`) |
| 통합형 | `#startDateTime` | 오늘 `T00:00` |
| 통합형 | `#endDateTime` | 오늘 `T23:59` |
| 일반 | `input[type="date"]` 전체 | 오늘 |

```javascript
// 페이지 로드 시 (값이 비어있는 필드만)
CommonUtils.setDefaultDateTime();

// 초기화 버튼에서 (모든 필드 강제 리셋)
CommonUtils.setDefaultDateTime(true);
```

#### `CommonUtils.resetFields()`

SMS 검색 폼 전용 초기화. `#receiverNo`, `#sendType`을 비우고 `setDefaultDateTime(true)`를 호출한다.

```javascript
document.querySelector('#btn-reset').addEventListener('click', () => {
    CommonUtils.resetFields();
});
```

#### `CommonUtils.fmt`

데이터 포매터 객체. 그리드/카드에서 자주 사용한다.

##### `fmt.money(val)`

숫자에 천단위 콤마를 적용한다.

```javascript
CommonUtils.fmt.money(1234567)   // "1,234,567"
CommonUtils.fmt.money('99999')   // "99,999"
CommonUtils.fmt.money(null)      // "0"
CommonUtils.fmt.money('')        // "0"
CommonUtils.fmt.money('abc')     // "abc" (숫자 아니면 원본 반환)
```

##### `fmt.phone(val)`

전화번호에 하이픈을 적용한다.

```javascript
CommonUtils.fmt.phone('01012345678')  // "010-1234-5678"
CommonUtils.fmt.phone('0212345678')   // "02-1234-5678"
CommonUtils.fmt.phone('0311234567')   // "031-123-4567"
CommonUtils.fmt.phone('123456789')    // "12-345-6789" (9자리)
CommonUtils.fmt.phone('')             // ""
CommonUtils.fmt.phone(null)           // ""
```

#### Notify re-export (하위 호환)

`CommonUtils.toast`, `.alert`, `.confirm`, `.refreshIcons`는 getter로 `window.Notify`의 메서드를 반환한다. `Notify`가 로드되지 않았으면 빈 함수(`() => {}`)를 반환한다.

```javascript
// 아래 두 호출은 동일하다 (Notify 로드 후)
CommonUtils.toast('완료', 'success');
Notify.toast('완료', 'success');

// 기존 코드 수정 없이 동작 유지
CommonUtils.confirm('삭제하시겠습니까?', () => { /* ... */ });
```

---

## 7. `form-binder.js` — `FormBinder`

### 7.1 목적과 책임

상세폼 화면의 **자동 바인딩**을 담당한다 (`screen-convention.md` "상세폼 화면 규약").

- `bind()`: 서버 조회 응답(JSON)을 form 필드에 자동 채움.
- `toObject()`: form 필드를 읽어 전송용 객체로 변환.

**계약**: form 필드의 `name` 속성 = 응답 JSON 필드명 = `UpdateRequestDTO` 프로퍼티명. 이 세 가지가 일치해야 자동 바인딩이 동작한다.

> 자동 바인딩은 화면 편의 기능일 뿐이다. 서버는 반드시 `UpdateRequestDTO`(화이트리스트)로만 요청을 받는다.

### 7.2 공개 API

```javascript
FormBinder = { bind, toObject };  // 전역 const (window.FormBinder 아님)
```

#### `FormBinder.bind(formSelector, data)`

조회 응답을 form에 자동 바인딩한다.

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `formSelector` | `string` | form 요소 CSS 셀렉터 |
| `data` | `object` | 서버 응답 JSON 객체 |

**바인딩 규칙:**

| 필드 타입 | 동작 |
|---|---|
| `checkbox` | `value === 'Y'` 또는 `value === true` → `checked = true` |
| `radio` | 같은 `name` 중 `field.value === data[name]`인 항목 `checked` |
| `select`, `text`, `textarea` 등 | `field.value = data[name]`. `null`/`undefined`는 빈 문자열 |

- `data[name]`이 존재하는 필드만 채운다. **없는 필드는 건드리지 않는다.**
- form 또는 data가 falsy면 아무것도 하지 않는다.

```javascript
// 서버에서 상세 조회 후 바인딩
const detail = await HttpClient.get('/sms/detail', { id: 42 });
FormBinder.bind('#detail-form', detail);

// 모달 내부 폼 바인딩
FormBinder.bind('#sms-detail-modal form', detail);
```

#### `FormBinder.toObject(formSelector)`

form의 `name` 필드를 읽어 전송용 객체로 만든다.

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `formSelector` | `string` | form 요소 CSS 셀렉터 |
| **반환** | `object` | `{ [name]: value }` 형태의 전송용 객체 |

**변환 규칙:**

| 필드 타입 | 변환 |
|---|---|
| `disabled` 필드 | **제외** (전송하지 않음) |
| `checkbox` | `checked ? 'Y' : 'N'` |
| `radio` | `checked` 항목의 `value`. 선택 없음 → `null` |
| `text`, `select` 등 (IMask 부착) | `field._imask.unmaskedValue` (마스크 제거 값) |
| `text`, `select` 등 (일반) | `field.value`. 빈 문자열 → `null` |

```javascript
// 저장 버튼 클릭 시
const payload = FormBinder.toObject('#detail-form');
await HttpClient.post('/sms/update', payload);

// 모달 폼에서 수집
const payload = FormBinder.toObject('#sms-detail-modal form');
```

> **빈 문자열 → `null` 변환**: BASE 확정 정책(2026-06-12). 서버 DTO가 빈 문자열 대신 `null`을 기대하므로, 클라이언트에서 변환한다.

### 7.3 IMask 연동

`toObject()`는 `field._imask` 프로퍼티를 확인한다. 이 프로퍼티는 `field-format.js`의 `applyMasks()`가 IMask를 부착하면서 설정한다.

```text
field-format.js:  el._imask = IMask(el, factory(el));
                              ↓
form-binder.js:   const raw = field._imask ? field._imask.unmaskedValue : field.value;
```

이 덕분에 `010-1234-5678`로 표시되는 전화번호 필드에서 전송 시 `01012345678`(unmasked)이 자동으로 사용된다.

---

## 8. `field-format.js` — `FieldFormat`

### 8.1 목적과 책임

입력 포맷 마스킹(IMask)과 클라이언트 폼 검증(JustValidate)을 **data 속성 선언적**으로 부착한다.

설계 철학: 화면/스캐폴드는 입력 요소에 `data-mask`, `data-validate` 속성만 선언하고, 이 모듈이 그 속성을 읽어 라이브러리를 부착한다. 라이브러리를 교체해도 화면 코드는 불변이다.

```html
<!-- 화면은 이것만 선언 -->
<input name="receiverNo" data-mask="phone" data-validate="required|phone">
<input name="amount" data-mask="number" data-validate="required|number">
<input name="email" data-validate="required|email">
```

### 8.2 공개 API

```javascript
FieldFormat = {
    applyMasks,          // (root?) => void
    applyFieldFormats,   // (root) => void
    validateForm,        // (root) => Promise<boolean>
    unmask,              // (el) => string
    registerMask,        // (name, factory) => void
    registerValidator,   // (name, factory) => void
    maskRegistry,        // object (읽기 가능)
    validatorRegistry    // object (읽기 가능)
};  // 전역 const
```

#### `FieldFormat.applyMasks(root)`

`root` 내부의 `[data-mask]` 요소에 IMask를 부착한다. 이미 부착된 요소(`el._imask` 존재)는 건너뛴다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `root` | `HTMLElement` | `document` | 탐색 범위 |

```javascript
// 정적 폼 — DOMContentLoaded 시 자동 실행되므로 별도 호출 불필요
// (파일 말미에 document.addEventListener('DOMContentLoaded', () => FieldFormat.applyMasks(document)) 있음)

// 동적 폼 (모달 등) — 수동 호출
FieldFormat.applyMasks(modalFormEl);
```

**내장 마스크 종류:**

| `data-mask` 값 | 포맷 | 예시 |
|---|---|---|
| `phone` | 전화번호 (02/0XX/010 자동 판별) | `010-1234-5678`, `02-123-4567` |
| `ssn` | 주민등록번호 6-7 | `900101-1234567` |
| `bizno` | 사업자등록번호 3-2-5 | `123-45-67890` |
| `number` | 숫자 (천단위 콤마, 소수 없음) | `1,234,567` |
| `date` | 날짜 YYYY-MM-DD | `2026-07-28` |

#### `FieldFormat.applyFieldFormats(root)`

마스크 + 검증을 한 번에 부착한다. **동적 폼용 통합 진입점.**

```javascript
// TuiPageBuilder 모달 생성 직후
FieldFormat.applyFieldFormats(formEl);
```

내부적으로 `applyMasks(root)` + `buildValidator(root)`를 순서대로 호출한다.

#### `FieldFormat.validateForm(root)`

`root` 폼의 각 필드를 순회하며 per-field `revalidateField`로 오류를 렌더링한다.

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `root` | `HTMLElement` | form 요소 |
| **반환** | `Promise<boolean>` | 검증 통과 여부. 검증기가 없으면 `true` |

```javascript
const isValid = await FieldFormat.validateForm(formEl);
if (!isValid) return;

const payload = FormBinder.toObject(formEl);
await HttpClient.post('/api/save', payload);
```

#### `FieldFormat.unmask(el)`

마스크 필드의 원본 값(unmasked)을 반환한다.

```javascript
const input = document.querySelector('#receiverNo');
FieldFormat.unmask(input);  // "01012345678" (표시는 "010-1234-5678")

// 마스크가 없는 필드는 el.value 그대로 반환
FieldFormat.unmask(plainInput);  // "hello"

// null/undefined → 빈 문자열
FieldFormat.unmask(null);  // ""
```

#### `FieldFormat.registerMask(name, factory)`

새 마스크 종류를 등록한다.

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `name` | `string` | `data-mask` 속성 값으로 사용할 이름 |
| `factory` | `(el: HTMLElement) => object` | IMask 옵션을 반환하는 팩토리 함수 |

```javascript
// 우편번호 마스크 등록
FieldFormat.registerMask('zipcode', () => ({ mask: '00000' }));

// HTML에서 사용
// <input name="zipcode" data-mask="zipcode">
```

#### `FieldFormat.registerValidator(name, factory)`

새 검증 규칙을 등록한다.

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `name` | `string` | `data-validate` 토큰으로 사용할 이름 |
| `factory` | `(arg: string, el: HTMLElement) => object` | JustValidate rule 객체를 반환 |

```javascript
// 우편번호 검증 등록
FieldFormat.registerValidator('zipcode', () => ({
    rule: 'customRegexp',
    value: /^\d{5}$/,
    errorMessage: '우편번호 형식 오류'
}));

// HTML에서 사용
// <input name="zipcode" data-mask="zipcode" data-validate="required|zipcode">
```

### 8.3 내장 검증 규칙

`data-validate` 속성에 `|`로 구분하여 여러 규칙을 선언한다. `:`로 인자를 전달한다.

| 토큰 | JustValidate rule | 예시 |
|---|---|---|
| `required` | `required` | `data-validate="required"` |
| `email` | `email` | `data-validate="required\|email"` |
| `number` | `number` | `data-validate="required\|number"` |
| `minlength:N` | `minLength` (value: N) | `data-validate="minlength:4"` |
| `maxlength:N` | `maxLength` (value: N) | `data-validate="maxlength:20"` |
| `phone` | `customRegexp` (`/^0\d{1,2}-?\d{3,4}-?\d{4}$/`) | `data-validate="required\|phone"` |
| `ssn` | `customRegexp` (`/^\d{6}-?\d{7}$/`) | `data-validate="ssn"` |
| `bizno` | `customRegexp` (`/^\d{3}-?\d{2}-?\d{5}$/`) | `data-validate="bizno"` |

### 8.4 적용 시점

| 상황 | 호출 | 비고 |
|---|---|---|
| 정적 폼 (검색조건 등) | `DOMContentLoaded` 시 `applyMasks(document)` **자동** | 파일 말미에 등록됨 |
| 동적 폼 (TuiPageBuilder 모달) | 모달 생성 직후 `applyFieldFormats(formEl)` **수동** | 화면 JS 또는 TuiPageBuilder 내부에서 호출 |

### 8.5 내부 동작 — JustValidate 인스턴스 관리

- `validators`는 `WeakMap<HTMLElement, JustValidate>`이다.
- 같은 폼에 `buildValidator()`를 다시 호출하면 이전 인스턴스를 `destroy()`한 후 새로 생성한다.
- `id`가 없는 `[data-validate]` 요소에는 자동 id(`ff-{random}`)를 부여한다 (JustValidate가 셀렉터 기반이므로).
- `validateForm()`은 JustValidate 4.3.0 `revalidate()` 결함(upstream #155) workaround로, per-field `revalidateField`를 사용해 각 필드 오류 라벨을 직접 렌더링한다.

---

## 9. 모듈 간 상호작용

### 9.1 상호작용 지도

```text
┌──────────────────────────────────────────────────────────────────┐
│                        화면 JS (page script)                      │
│  HttpClient.get/post  ·  Notify.toast/confirm                    │
│  ModalManager.init/open/close  ·  FormBinder.bind/toObject       │
│  FieldFormat.applyFieldFormats  ·  CommonUtils.fmt/initAutocomplete│
└───────┬──────────┬──────────┬──────────┬──────────┬──────────────┘
        │          │          │          │          │
   ┌────▼───┐ ┌───▼────┐ ┌──▼─────┐ ┌──▼─────┐ ┌─▼──────────┐
   │HttpClient│ │ Notify │ │ Modal  │ │ Form   │ │ Field      │
   │(http-    │ │(notify │ │ Manager│ │ Binder │ │ Format     │
   │client.js)│ │.js)    │ │(modal- │ │(form-  │ │(field-     │
   │          │ │        │ │mgr.js) │ │binder) │ │format.js)  │
    └────┬─────┘ └────────┘ └───┬────┘ └───┬────┘ └─────┬──────┘
         │                      │           │            │
         │  에러 시 Notify.alert │           │            │
         ├─────────────────────►│           │            │
         │                      │  SweetAlert2 의존     │
         │                      │  (alert/confirm)      │
        │                      │           │            │
        │                      │  JustValidate 검증     │
        │                      │  (validateRules 옵션)  │
        │                      │◄──────────┘            │
        │                      │                       │
        │                      │  el._imask 참조       │
        │                      │  (unmaskedValue)      │
        │                      │           ┌───────────┘
        │                      │           │
        │                      │     ┌─────▼─────┐
        │                      │     │  IMask     │
        │                      │     │  (lib)     │
        │                      │     └───────────┘
        │                      │
        │  ApiResponse 언래핑   │
        │  (code/data 구조)     │
        ▼                      ▼
   ┌─────────┐          ┌──────────┐
   │  axios   │          │ CoreUI / │
   │  (lib)   │          │Bootstrap │
   └─────────┘          │  (lib)   │
                        └──────────┘
```

### 9.2 핵심 상호작용 설명

#### http-client → notify (에러 알림)

`http-client.js`의 응답 interceptor는 에러 발생 시 `window.Notify.alert()`을 호출한다. **`Notify`가 먼저 로드되어야 하므로** `defaultLayout.html`에서 `notify.js`가 `http-client.js`보다 앞에 있다.

```javascript
// http-client.js 내부 (응답 interceptor 에러 핸들러)
const notify = window.Notify;
if (notify) notify.alert(displayMsg, '오류');
```

`if (notify)` 가드가 있으므로 `Notify` 미로드 시 크래시하지 않지만, 에러 알림이 표시되지 않는다.

#### notify → SweetAlert2 (alert/confirm 렌더링)

`Notify.alert`과 `Notify.confirm`은 내부적으로 `window.Swal.fire()`를 호출한다. **`SweetAlert2`가 `notify.js`보다 먼저 로드되어야 하므로** `defaultLayout.html`에서 `sweetalert2.all.min.js`가 `notify.js` 앞에 있다.

SweetAlert2 미로드 시 fail-closed 동작:
- `Notify.alert`: callback 없이 큐만 진행
- `Notify.confirm`: `onConfirm` 미실행, `onCancel` 1회 호출 후 큐 진행

#### modal-manager → JustValidate (폼 검증)

`ModalManager.init()`의 `validateRules` 옵션을 사용하면, 모달이 열릴 때마다 JustValidate 인스턴스가 생성된다. 저장 버튼 클릭 시 `revalidate()`로 검증한 후 통과해야 `onSubmit`이 실행된다.

#### form-binder → field-format (IMask unmaskedValue)

`FormBinder.toObject()`는 `field._imask` 프로퍼티를 확인하여 IMask 부착 여부를 판별한다. 이 프로퍼티는 `FieldFormat.applyMasks()`가 설정한다. **`field-format.js`가 `form-binder.js`보다 먼저 로드되어야** `_imask`가 정상 설정된다.

```javascript
// form-binder.js 내부
const raw = field._imask ? field._imask.unmaskedValue : field.value;
```

#### common-utils → notify (하위 호환 re-export)

`CommonUtils.toast/alert/confirm/refreshIcons`는 getter로 `window.Notify`를 참조한다. 기존 `CommonUtils.toast(...)` 호출 코드를 수정하지 않아도 `Notify`로 위임된다.

```javascript
// common-utils.js 내부
get toast() { return window.Notify ? window.Notify.toast : () => {}; }
```

#### common-utils → axios (콤보/autocomplete)

`initCombos()`와 `initAutocomplete()`는 내부적으로 `axios.get()`을 직접 호출한다. 이 요청에도 전역 interceptor가 적용되므로 spinner, CSRF, ApiResponse 언래핑이 자동으로 동작한다.

---

## 10. `SESSION_INFO` / `PAGE_AUTH` 전역 변수

### 10.1 정의 위치

`defaultLayout.html`의 `<head>` 내부, **공통 JS 로드 이후 · 화면 JS 이전**에 Thymeleaf inline JavaScript로 주입된다.

```html
<!-- defaultLayout.html (line 52~71) -->
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

### 10.2 `SESSION_INFO`

로그인 사용자 정보. **`const`로 선언**되므로 재할당 불가.

| 프로퍼티 | 타입 | 설명 |
|---|---|---|
| `empId` | `string` | 사번 (EMP_ID) |
| `depId` | `string` | 부서 코드 (DEP_ID) |
| `empNm` | `string` | 성명 |
| `depNm` | `string` | 부서명 |

> 사용자 식별은 **`EMP_ID + DEP_ID` 복합 키**가 원칙이다 (`emp-dep-identity-policy.md`). 한 사번이 여러 부서에 속할 수 있으므로 `empId`만으로 사용자를 특정하지 않는다.

```javascript
// 화면 JS에서 사용
console.log(`접속자: ${SESSION_INFO.empNm} (${SESSION_INFO.empId} / ${SESSION_INFO.depId})`);

// API 요청에 사용자 정보 포함
await HttpClient.post('/sms/send', {
    senderNo: '01012345678',
    empId: SESSION_INFO.empId,
    depId: SESSION_INFO.depId
});
```

### 10.3 `PAGE_AUTH`

현재 페이지의 버튼/API 권한. **`window.PAGE_AUTH`로 선언**되므로 전역 접근 가능.

| 프로퍼티 | 타입 | 설명 |
|---|---|---|
| `read` | `boolean` | 조회 권한 |
| `create` | `boolean` | 등록 권한 |
| `update` | `boolean` | 수정 권한 |
| `delete` | `boolean` | 삭제 권한 |
| `approve` | `boolean` | 승인 권한 |
| `cancel` | `boolean` | 취소 권한 |
| `download` | `boolean` | 다운로드(엑셀 등) 권한 |
| `maskView` | `boolean` | 개인정보 마스킹 해제 열람 권한 |

> 권한 값은 서버(Controller)가 `TB_MENU_AUTH` 기반으로 계산하여 모델에 실어 보낸다. **화면에서 권한을 임의 계산하지 않는다.** URL/API 접근 검증은 `MenuAuthInterceptor`가 담당한다.

```javascript
// 동적 버튼은 항상 strict + fail-closed로 검사한다.
const auth = window.PAGE_AUTH || {};
btnDownload.addEventListener('click', () => {
    if (auth.download !== true) {
        CommonUtils.toast('엑셀 다운로드 권한이 없습니다.', 'warning');
        return;
    }
    downloadCurrentSearch();
});

// 삭제 버튼 노출 제어
if (auth.delete === true) {
    btnDelete.classList.remove('d-none');
}
```

### 10.4 로드 타이밍 주의

`SESSION_INFO`와 `PAGE_AUTH`는 공통 JS **이후**에 주입된다. 따라서:

- 공통 JS 모듈 내부에서 이 변수들을 참조하면 **안 된다** (아직 정의되지 않음).
- 화면 JS(`layout:fragment="script"`)는 이 변수들 **이후**에 로드되므로 안전하게 사용 가능.
- `DOMContentLoaded` 콜백 내부에서도 안전 (모든 `<script>` 실행 완료 후이므로).

---

## 11. 화면 JS 실전 사용 패턴

### 11.1 Scaffold EXCEL 기본 생성물 — 목록 + 다운로드 권한

현재 메뉴에 배치된 테스트 화면이 아니라 `scaffold-templates/excel/page.js.tpl`의 생성 계약을 기준으로 한다. 아래
`SCREEN_URL`은 생성 시 실제 화면 URL로 치환되는 값을 설명하기 위한 예시다.

```javascript
document.addEventListener('DOMContentLoaded', function () {
    const SCREEN_URL = '/sms/example-history';
    const API = { excel: SCREEN_URL + '/excel' };

    // TuiPageBuilder로 그리드 초기화 (별도 매뉴얼 참고)
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: SCREEN_URL + '/data',
        searchInputs: ['status', 'startDt', 'endDt'],
        columns: [
            { header: '처리일시', name: 'processedAt', formatter: TuiCommon.fmt.date },
            // ...
        ]
    });

    // PAGE_AUTH를 이용한 엑셀 다운로드 권한 체크
    const btnExcel = document.querySelector('#btn-excel');
    if (btnExcel) {
        btnExcel.addEventListener('click', () => {
            if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) {
                CommonUtils.toast('엑셀 다운로드 권한이 없습니다.', 'warning');
                return;
            }
            const params = new URLSearchParams(pageBuilder.getSearchParams());
            window.location.href = API.excel + '?' + params.toString();
        });
    }
});
```

**사용된 공통 모듈**: `CommonUtils.toast` (→ `Notify.toast`), `PAGE_AUTH`.

### 11.2 `system/menu-manage.js` — CRUD + 확인 모달

```javascript
(function () {
    'use strict';

    const API = {
        tree:   '/system/menu-manage/tree',
        detail: '/system/menu-manage/detail',
        create: '/system/menu-manage/create',
        update: '/system/menu-manage/update',
        delete: '/system/menu-manage/delete'
    };

    // ── 조회: axios 직접 호출 (전역 interceptor가 spinner/언래핑/에러 처리) ──
    const selectMenu = async (menuId) => {
        try {
            const res = await axios.get(API.detail, { params: { menuId } });
            const detail = res.data || {};       // interceptor가 ApiResponse 언래핑 완료
            bindForm(detail.menu || {});         // form 바인딩 (수동 구현)
            renderAuthMatrix(detail.activeRoles, detail.authRows, true);
        } catch (e) {
            // axios interceptor가 이미 Notify.alert을 표시함
            // 여기서 추가 알림 불필요
        }
    };

    // ── 저장: CommonUtils.alert/toast + axios ──
    const save = async () => {
        const payload = collectMenuPayload();
        if (state.mode === 'create') {
            if (!payload.menuId) {
                CommonUtils.alert('menuId를 입력하세요.');  // ← Notify.alert
                els.fMenuId.focus();
                return;
            }
            try {
                await axios.post(API.create, payload);
                CommonUtils.toast('등록되었습니다.', 'success');  // ← Notify.toast
                await reloadTree();
            } catch (e) { /* interceptor 처리 */ }
        }
    };

    // ── 삭제: CommonUtils.confirm (확인 모달) ──
    const removeMenu = () => {
        const menuId = state.selectedMenuId;
        if (!menuId) return;
        CommonUtils.confirm(                              // ← Notify.confirm
            `메뉴 [${menuId}] 를 삭제하시겠습니까? 권한 행도 함께 삭제됩니다.`,
            async () => {
                try {
                    await axios.post(API.delete, null, { params: { menuId } });
                    CommonUtils.toast('삭제되었습니다.', 'success');
                    state.selectedMenuId = null;
                    await reloadTree();
                    showDetailPanel(false);
                } catch (e) { /* interceptor 처리 */ }
            }
        );
    };

    document.addEventListener('DOMContentLoaded', init);
})();
```

**사용된 공통 모듈**: `CommonUtils.alert/toast/confirm` (→ `Notify`), `axios` + 전역 interceptor (`http-client.js`).

### 11.3 상세폼 + 모달 조합 패턴 (권장)

```javascript
document.addEventListener('DOMContentLoaded', () => {
    // 1. 모달 초기화
    ModalManager.init('detail-modal', {
        async onOpen() {
            const data = await HttpClient.get('/api/detail', { id: state.id });
            FormBinder.bind('#detail-modal form', data);
            FieldFormat.applyFieldFormats(document.querySelector('#detail-modal form'));
        },
        async onSubmit() {
            const formEl = document.querySelector('#detail-modal form');
            const isValid = await FieldFormat.validateForm(formEl);
            if (!isValid) return;

            const payload = FormBinder.toObject(formEl);
            await HttpClient.post('/api/update', payload);
            Notify.toast('수정되었습니다.', 'success');
            ModalManager.close('detail-modal');
        },
        validateRules(validator) {
            validator.addField('#receiverNo', [{ rule: 'required' }]);
        }
    });

    // 2. 그리드 행 클릭 → 모달 열기
    grid.on('click', (ev) => {
        if (ev.rowKey == null) return;
        state.id = grid.getRow(ev.rowKey).id;
        ModalManager.open('detail-modal');
    });
});
```

---

## 12. 주의사항 및 흔한 실수

### 12.1 로드 순서 관련

| 실수 | 증상 | 해결 |
|---|---|---|
| `notify.js` 전에 `http-client.js` 로드 | HTTP 에러 시 알림 모달 미표시 | `defaultLayout.html` 순서 유지: `notify.js` → `http-client.js` |
| 화면 동작 전에 `field-format.js`가 로드되지 않음 | 마스크가 부착되지 않아 표시값이 그대로 전송될 수 있음 | `defaultLayout.html`의 `form-binder.js` → `field-format.js` 순서를 유지하고 화면 JS는 두 파일 뒤에 로드 |
| 화면 JS에서 `SESSION_INFO`를 IIFE 최상위에서 참조 | `const`이므로 TDZ(Temporal Dead Zone) 에러는 아니지만, 공통 JS 내부에서는 미정의 | 화면 JS의 `DOMContentLoaded` 콜백 내부에서 사용 |

### 12.2 API 사용 관련

| 실수 | 증상 | 해결 |
|---|---|---|
| `Notify.confirm(msg, title, callback)` 순서로 호출 | `title`이 `callback` 자리에 들어가 확인 버튼 클릭 시 아무 일도 안 일어남 | `confirm(msg, callback, title, onCancel)` 순서 준수 |
| `HttpClient.delete(url, params)` 호출 | `params`가 `config`로 해석되어 쿼리 파라미터 미전송 | `HttpClient.delete(url, { params: {...} })` 또는 `HttpClient.remove(url, params)` 사용 |
| `HttpClient.get()` 응답에서 `res.data.data` 접근 | interceptor가 이미 언래핑 → `undefined` 접근 | `const data = await HttpClient.get(url)` → `data`가 곧 알맹이 |
| `axios.get()` 직접 호출 후 `res.data.data` 접근 | interceptor 언래핑 후 `res.data`가 알맹이 → `.data`는 `undefined` | `res.data`直接使用 또는 `HttpClient.get()` 사용 |
| `CommonUtils.toast()`에 사용자 입력을 그대로 전달 | XSS 취약 (innerHTML로 렌더링) | 사용자 입력은 `textContent` 기반 요소로 별도 처리 |

### 12.3 폼 바인딩 관련

| 실수 | 증상 | 해결 |
|---|---|---|
| form 필드 `name`과 JSON 키 불일치 | `bind()` 시 해당 필드만 비어있음 | `name` = JSON 필드명 = DTO 프로퍼티명 일치 확인 |
| `toObject()` 결과에 `disabled` 필드가 없음 | 의도된 동작 (disabled는 전송 제외) | 전송이 필요하면 `disabled` 해제 후 수집 |
| 빈 문자열이 서버에 전송됨 | `toObject()`가 `''` → `null` 변환하므로 발생 불가 | BASE 정책: 빈 문자열은 `null`로 전송 |
| 모달 닫은 후 폼 값이 남아있음 | `ModalManager`가 `hidden.coreui.modal`에서 `form.reset()` 호출 | `ModalManager.init()`을 사용하면 자동 리셋 |

### 12.4 검증 관련

| 실수 | 증상 | 해결 |
|---|---|---|
| 클라이언트 검증만 믿고 서버 `@Valid` 생략 | 보안 취약 + 데이터 무결성 훼손 | 서버 `@Valid`가 최종 권위. 클라이언트 검증은 UX 보조 |
| 동적 폼에 `FieldFormat.applyFieldFormats()` 미호출 | 마스크/검증 미부착 | 모달/동적 폼 생성 직후 반드시 호출 |
| `data-validate` 요소에 `id` 없음 | JustValidate가 셀렉터 기반이라 오류 | `FieldFormat`이 자동 id(`ff-{random}`)를 부여하므로 문제 없음 |
| `ModalManager`의 `validateRules`와 `FieldFormat.buildValidator` 동시 사용 | 이중 검증 인스턴스 생성 | 하나만 사용. `ModalManager` 모달이면 `validateRules` 옵션 권장 |

### 12.5 전역 변수 관련

| 실수 | 증상 | 해결 |
|---|---|---|
| `SESSION_INFO.empId`만으로 사용자 특정 | 동일 사번이 타 부서에서 다른 사용자일 수 있음 | `EMP_ID + DEP_ID` 복합 키 사용 (`emp-dep-identity-policy.md`) |
| `PAGE_AUTH` 값을 화면에서 임의 계산 | 서버 권한과 불일치 | Controller가 `TB_MENU_AUTH` 기반으로 계산한 값만 사용 |
| `PAGE_AUTH` 미체크로 권한 없는 API 호출 | `MenuAuthInterceptor`가 403 반환 → interceptor가 로그인 전환 시도 | 버튼 클릭 전 `PAGE_AUTH` 체크 + 서버 Interceptor 이중 방어 |

---

## 부록: 계약 요약 (Quick Reference)

```text
┌─────────────────────────────────────────────────────────────────┐
│  window.Notify                                                  │
│    .toast(msg, type?)                                           │
│    .alert(msg, title?, callback?)                               │
│    .confirm(msg, callback?, title?, onCancel?)                  │
│    .refreshIcons()                                              │
│    .showModal(el) / .hideModal(el)                              │
│    .getFrameworkModal(el)                                       │
├─────────────────────────────────────────────────────────────────┤
│  window.HttpClient  (= window.ApiClient)                        │
│    .get(url, params?, config?)     → Promise<data>              │
│    .post(url, data?, config?)      → Promise<data>              │
│    .put(url, data?, config?)       → Promise<data>              │
│    .delete(url, config?)           → Promise<data>              │
│    .remove(url, params?, config?)  → Promise<data>  (POST 기반) │
├─────────────────────────────────────────────────────────────────┤
│  window.ModalManager                                            │
│    .init(modalId, options?)  → Modal instance|null              │
│    .open(modalId)                                               │
│    .close(modalId)                                              │
├─────────────────────────────────────────────────────────────────┤
│  window.CommonUtils                                             │
│    .initCombos()                   → Promise<void>              │
│    .initAutocomplete(options)      → { close, select }          │
│    .setDefaultDateTime(force?)                                  │
│    .resetFields()                                               │
│    .fmt.money(val) / .fmt.phone(val)                            │
│    .toast / .alert / .confirm / .refreshIcons  (Notify 위임)    │
├─────────────────────────────────────────────────────────────────┤
│  FormBinder  (전역 const)                                        │
│    .bind(formSelector, data)                                    │
│    .toObject(formSelector)         → object                     │
├─────────────────────────────────────────────────────────────────┤
│  FieldFormat  (전역 const)                                       │
│    .applyMasks(root?)                                           │
│    .applyFieldFormats(root)                                     │
│    .validateForm(root)             → Promise<boolean>           │
│    .unmask(el)                     → string                     │
│    .registerMask(name, factory)                                 │
│    .registerValidator(name, factory)                            │
│    .maskRegistry / .validatorRegistry                           │
├─────────────────────────────────────────────────────────────────┤
│  SESSION_INFO  (const, Thymeleaf 주입)                           │
│    { empId, depId, empNm, depNm }                               │
├─────────────────────────────────────────────────────────────────┤
│  window.PAGE_AUTH  (Thymeleaf 주입)                              │
│    { read, create, update, delete,                              │
│      approve, cancel, download, maskView }                      │
└─────────────────────────────────────────────────────────────────┘
```
