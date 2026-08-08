# 인증과 페이지 권한 매뉴얼

> **대상 독자**: omoWorker(SMS전송시스템 v3)에서 화면/API를 추가·수정하는 개발자, 인증·권한 흐름을 처음 파악하는 개발자
> **문서 성격**: profile별 인증 방식, 세션 모델, 페이지 권한(`PageAuth`) 계산과 주입, `PAGE_AUTH` JS 전역, `MenuAuthInterceptor`의 URL 검증, `GlobalModelAdvice`·`AuthSourceGuard`의 역할을 한 곳에서 파악하는 실무 안내서. 개별 주제의 설계 근거는 `docs/base/` 원문을 참조한다.
> **관련 문서**: 환경/인증 정책 `docs/base/environment-auth-policy.md`, 메뉴/권한 테이블 설계 `docs/base/menu-authority-table-design.md`, Interceptor 구현 기록 `docs/base/menu-auth-interceptor-implementation.md`, 메뉴 source 정책 `docs/base/menu-source-policy.md`, 구조/구현 `docs/menual/구조및구현_manual.md`

---

## 목차

1. [인증 개요 (profile별 인증)](#1-인증-개요-profile별-인증)
2. [전체 흐름도](#2-전체-흐름도)
3. [세션 모델 (SESSION_INFO)](#3-세션-모델-session_info)
4. [페이지 권한 모델 (PageAuth)](#4-페이지-권한-모델-pageauth)
5. [PAGE_AUTH JS 전역](#5-page_auth-js-전역)
6. [MenuAuthInterceptor (URL 권한 검증)](#6-menuauthinterceptor-url-권한-검증)
7. [GlobalModelAdvice (공통 모델 주입)](#7-globalmodeladvice-공통-모델-주입)
8. [AuthSourceGuard (기동 시 source 검증)](#8-authsourceguard-기동-시-source-검증)
9. [새 화면에 권한 검사 추가하기](#9-새-화면에-권한-검사-추가하기)
10. [자주 하는 실수](#10-자주-하는-실수)
11. [핵심 파일 지도](#11-핵심-파일-지도)

---

## 1. 인증 개요 (profile별 인증)

이 시스템은 **별도의 인가 프레임워크 없이 Spring Security 6의 인증(authentication)만 사용**한다. 복잡한 권한 DSL(`@PreAuthorize`, `hasRole(...)`)을 쓰지 않고, 인증은 Spring Security에 맡기며 **인가(authorization)는 `MenuAuthInterceptor`가 `TB_MENU_AUTH` 기준으로 직접 수행**한다.

핵심 원칙은 **인증 방식만 profile별로 다르고, 권한 판단은 모든 환경에서 동일**하다는 것이다.

| profile | 인증 방식 | 비밀번호 검증 | 활성화되는 컴포넌트 |
|---|---|---|---|
| `local` | ID-only | 하지 않음 | `LocalIdOnlyAuthenticationProvider` (`@Profile("local")`) |
| `dev` | LDAP | LDAP bind로 검증 | `LdapAuthenticationConfig` (`@Profile({"dev","prod"})`) |
| `prod` | LDAP | LDAP bind로 검증 | `LdapAuthenticationConfig` (`@Profile({"dev","prod"})`) |

세 profile 모두 인증 성공 후 **동일한 권한 경로**를 탄다.

```text
local  -> ID-only 인증 -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
dev    -> LDAP 인증    -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
prod   -> LDAP 인증    -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
```

### 1.1 인증이 하는 일 (공통)

어떤 profile이든 인증 성공 시 다음 순서로 사용자 정보를 확정한다.

1. 입력한 사번(`empId`)으로 `ActiveEmployeeResolver.resolveSingleActiveEmployee(empId)`를 호출한다.
   - `EMP.ACT_YN = 'Y'` 이고 소속 `DEP.ACT_YN = 'Y'` 인 활성 사용자를 조회한다.
   - 결과가 **0건**이면 로그인 실패("활성 사용자 정보를 찾을 수 없습니다.").
   - 결과가 **2건 이상**이면 로그인 실패("동일 사번에 활성 부서가 여러 건입니다. EMP 데이터를 정리해야 합니다.").
2. 확정된 `(EMP_ID, DEP_ID)`로 `EmployeeRoleService.getActiveRoleCodes(empId, depId)`를 호출해 활성 역할 코드 목록(`List<String>`)을 조회한다.
   - 활성 역할이 하나도 없으면 로그인 실패("No active role assigned to employee.").
3. `LoginEmployeeVO`(empId, depId, empNm, depNm)와 역할 코드로 `SmsUserPrincipal`을 생성한다.

즉 **사용자는 항상 `(EMP_ID, DEP_ID)` 복합키로 식별**하며, 권한은 직원에게 직접 주지 않고 역할(`TB_ROLE`)에 부여한다(`TB_EMP_ROLE` → `TB_MENU_AUTH`). legacy `EMP.PERM_*` 컬럼은 인증·권한 판단에 일절 사용하지 않는다.

### 1.2 local (ID-only)

`LocalIdOnlyAuthenticationProvider`는 `UsernamePasswordAuthenticationToken`을 받아 **비밀번호를 보지 않고** 사번만으로 위 1.1의 절차를 수행한다. 개발 편의용이지만 **권한 로직까지 우회하지는 않는다.** 역할·메뉴 권한은 dev/prod와 똑같이 DB(`TB_EMP_ROLE`, `TB_MENU_AUTH`)에서 조회한다.

> 단, 화면 렌더링용 `PageAuth`는 local에서 모든 권한을 켜는 특례가 있다(버튼 표시 편의). 자세한 것은 [4.3절](#43-local-특례-pageauthall)과 [10절](#10-자주-하는-실수) 참조. **서버의 실제 API 차단(`MenuAuthInterceptor`)은 local에서도 DB 권한 그대로 동작한다.**

### 1.3 dev / prod (LDAP)

`LdapAuthenticationConfig`가 `LdapAuthenticationProvider`를 구성한다. LDAP bind 인증이 성공하면 `LdapEmployeeContextMapper.mapUserFromContext(...)`가 호출되어, LDAP에서 받은 username(사번)으로 위 1.1의 절차를 수행하고 `SmsUserPrincipal`을 만든다.

LDAP 접속 정보(URL, base DN, manager DN, search filter)는 `SmsLdapProperties`(`sms.ldap.*`)로 주입되며, **manager password는 환경변수(`${SMS_LDAP_MANAGER_PASSWORD}`)로만** 둔다. LDAP 서버 운영 설정 자체는 이 코드베이스의 관리 대상이 아니다.

### 1.4 SecurityConfig (폼 로그인·CSRF)

`SecurityConfig`는 인증 진입점을 정의한다.

- `formLogin`: 로그인 페이지 `/login`, 처리 URL `/login`, **`usernameParameter("empId")`**, `passwordParameter("password")`, 성공 시 `/`, 실패 시 `/login?error`.
- `authorizeHttpRequests`: `/login`, `/error`, 정적 리소스(`/css/**`, `/js/**`, `/lib/**`, `/vendor/**`, `/img/**`, `/favicon.ico`)만 `permitAll`, **나머지는 모두 `authenticated()`**.
- **CSRF 활성화**(Spring Security 기본값). Thymeleaf 폼(`th:action`)은 토큰을 자동 주입하고, axios 호출은 `common-utils.js`/`http-client.js` 요청 인터셉터가 `<meta name="_csrf">` 값을 헤더로 싣는다.
- 등록된 `AuthenticationProvider`(local 또는 LDAP)를 `http.authenticationProvider(...)`로 연결한다.

미인증 요청 차단(로그인 없이 접근)은 이 Spring Security 1차 인가가 담당하고, **인증된 사용자의 메뉴/기능 권한은 `MenuAuthInterceptor`가 2차로 검증**한다.

---

## 2. 전체 흐름도

### 2.1 로그인 (인증)

```text
[브라우저]  POST /login  (empId, password)
     │
     ▼
[Spring Security 필터체인]  formLogin → AuthenticationProvider 호출
     │
     ├── local  : LocalIdOnlyAuthenticationProvider
     └── dev/prod: LdapAuthenticationProvider
                    └── LDAP bind 성공 → LdapEmployeeContextMapper
     │
     ▼
[ActiveEmployeeResolver]  EMP+DEP 활성 조회 (0건/2건↑ → 실패)
     │
     ▼
[EmployeeRoleService]  (EMP_ID, DEP_ID) → 활성 역할 코드 조회 (없으면 실패)
     │
     ▼
[SmsUserPrincipal 생성]  empId, depId, empNm, depNm, roleCodes
     │
     ▼
[SecurityContext → HTTP 세션 저장]  JSESSIONID 쿠키로 유지
     │
     ▼
[브라우저]  / 로 리다이렉트 (로그인 완료)
```

### 2.2 페이지/API 요청 (인가 + 렌더링)

```text
[브라우저]  GET /campaign/sms  또는  POST /campaign/sms/create
     │
     ▼
[Spring Security 필터체인]  인증 여부 확인
     │  미인증 → /login 으로 리다이렉트 (여기까지가 1차 인가)
     ▼
[MenuAuthInterceptor.preHandle]   ← 인증된 요청만 도달
     │  SecurityContext 에서 SmsUserPrincipal 획득
     │  path = 요청 URI (contextPath 제거)
     │  menuAuthService.checkAccess(path, principal.getRoleCodes())
     │     ├─ 정확 메뉴 일치 → READ 검증
     │     ├─ 메뉴는 있으나 권한 없음 → 즉시 거부
     │     ├─ suffix 매칭 → 부모 화면 권한으로 액션 검증
     │     └─ 어느 메뉴에도 없음 → 거부
     │  권한 없음 → CustomException(ACCESS_DENIED)
     ▼
[Controller]  업무 로직 수행
     │
     ▼
[GlobalModelAdvice.addLayoutAttributes]   ← @Controller 로 응답하는 모든 요청
     │  model 에 user / menus / pageAuth / clientIp 주입
     │  pageAuth = resolvePageAuth(...)   (local 이면 PageAuth.all())
     ▼
[Thymeleaf defaultLayout.html 렌더링]
     │  const SESSION_INFO = { empId, depId, empNm, depNm }   ← ${user.*}
     │  window.PAGE_AUTH = { read, create, ... maskView }     ← ${pageAuth.*}
     │  sidebar 가 ${menus} 로 메뉴 트리 렌더링
     │  th:if="${pageAuth.create}" 로 버튼 노출 제어
     ▼
[브라우저]  HTML 수신 → JS 가 window.PAGE_AUTH 로 버튼/그리드 제어
```

권한 거부 시 `CustomException(ErrorCode.ACCESS_DENIED)`은 `GlobalExceptionHandler`에서 변환된다. **Accept 헤더가 HTML(화면 요청)이면 `error/error` 페이지, JSON(API 요청)이면 `ApiResponse` 403 JSON**으로 응답한다.

---

## 3. 세션 모델 (SESSION_INFO)

### 3.1 무엇이 세션에 저장되는가

인증 성공 시 만들어진 `SmsUserPrincipal`이 Spring Security `SecurityContext`에 담기고, 이 `SecurityContext`가 HTTP 세션에 저장된다. `SmsUserPrincipal`이 들고 있는 사용자 정보는 다음 네 가지다.

| 필드 | 의미 | 출처 |
|---|---|---|
| `empId` | 사번 | `EMP.EMP_ID` |
| `depId` | 부서 ID | `EMP.DEP_ID` |
| `empNm` | 직원명 | `EMP.EMP_NM` |
| `depNm` | 부서명 | `DEP.DEP_NM` |

(`roleCodes`도 principal에 함께 실리지만 화면 전역 변수로는 노출되지 않고 서버 권한 판단에만 쓰인다.)

### 3.2 어디서 세팅되는가

`SmsUserPrincipal` 자체는 인증 단계에서 만들어져 세션에 저장된다. **화면에서 쓰는 `user` 모델 속성은 `GlobalModelAdvice`가 매 요청 채운다.**

```java
// GlobalModelAdvice.addLayoutAttributes(...)
model.addAttribute("user", principal);   // principal = SmsUserPrincipal
```

즉 "세션에 저장된 principal" → "`GlobalModelAdvice`가 `user`로 모델에 노출" → "템플릿과 JS가 소비"의 흐름이다.

### 3.3 JS에서 어떻게 접근하는가

`defaultLayout.html`이 `user` 모델 속성을 읽어 **JS 전역 상수 `SESSION_INFO`** 를 정의한다.

```html
<!-- defaultLayout.html -->
<script th:inline="javascript">
    const SESSION_INFO = {
        empId:  /*[[${user != null ? user.empId : ''}]]*/ '',
        depId:  /*[[${user != null ? user.depId : ''}]]*/ '',
        empNm:  /*[[${user != null ? user.empNm : ''}]]*/ '',
        depNm:  /*[[${user != null ? user.depNm : ''}]]*/ ''
    };
</script>
```

업무 화면 JS에서는 이 전역 상수를 그대로 읽는다.

```javascript
// 등록자 사번을 기본값으로 세팅하는 예
document.querySelector('#regId').value = SESSION_INFO.empId;

// 헤더나 그리드에 사용자 표시
console.log(`${SESSION_INFO.empNm} (${SESSION_INFO.empId}) / ${SESSION_INFO.depNm}`);
```

> `SESSION_INFO`는 `defaultLayout.html`을 decorate하는 모든 화면에서 사용 가능하다. 정적 샘플 페이지(`static/samples/*`)는 서버 렌더링을 거치지 않으므로 더미 값(`SAMPLE`)을 직접 정의해 둔다.

사이드바(`fragments/sidebar.html`)도 같은 `user` 속성으로 사용자/부서/IP를 표시한다.

```html
<!-- fragments/sidebar.html -->
<div class="sidebar-userinfo" th:if="${user != null}">
    <div><strong>사용자:</strong> <span th:text="${user.empNm} + ' (' + ${user.empId} + ')'"></span></div>
    <div><strong>부서:</strong> <span th:text="${user.depNm} + ' (' + ${user.depId} + ')'"></span></div>
    <div><strong>IP :</strong> <span th:text="${clientIp}"></span></div>
</div>
```

---

## 4. 페이지 권한 모델 (PageAuth)

### 4.1 PageAuth란

`PageAuth`는 **현재 화면(메뉴)에서 로그인 사용자가 쓸 수 있는 기능 권한 묶음**이다. `TB_MENU_AUTH`의 `CAN_*` 8개 컬럼과 1:1로 대응하는 `MenuPermission` enum을 boolean으로 풀어 담는다.

| `PageAuth` 필드 | `MenuPermission` | `TB_MENU_AUTH` 컬럼 | 의미 |
|---|---|---|---|
| `read` | `READ` | `CAN_READ` | 화면 접근, 목록/상세 조회 |
| `create` | `CREATE` | `CAN_CREATE` | 신규 등록 |
| `update` | `UPDATE` | `CAN_UPDATE` | 수정 |
| `delete` | `DELETE` | `CAN_DELETE` | 삭제 |
| `approve` | `APPROVE` | `CAN_APPROVE` | 승인/반려 |
| `cancel` | `CANCEL` | `CAN_CANCEL` | 발송취소 등 취소성 업무 |
| `download` | `DOWNLOAD` | `CAN_DOWNLOAD` | 엑셀/파일 다운로드 |
| `maskView` | `MASK_VIEW` | `CAN_MASK_VIEW` | 개인정보 원문/비마스킹 조회 |

팩터리 메서드 세 개가 있다.

- `PageAuth.from(Set<MenuPermission>)` — 권한 Set에서 생성. 비어 있으면 `none()`.
- `PageAuth.all()` — 8개 모두 `true`.
- `PageAuth.none()` — 8개 모두 `false`.

> **중요**: `PageAuth`는 **화면 렌더링(버튼 노출)과 공통 JS 제어용**이다. 실제 API 차단은 `MenuAuthInterceptor`가 별도로 수행한다. 즉 `PageAuth`로 버튼을 숨겨도, 서버는 요청 URL을 다시 검증한다(이중 방어).

### 4.2 GlobalModelAdvice가 PageAuth를 계산하는 법

`GlobalModelAdvice.resolvePageAuth(principal, request)`가 매 화면 요청마다 `pageAuth` 모델 속성을 만든다.

```text
1. local profile 이면 → PageAuth.all() 반환 (개발 편의 특례)
2. 아니면:
   a. 요청 URI 에서 contextPath 를 제거하고 끝의 '/' 를 정규화 (normalizePath)
   b. menuAuthService.resolveBaseMenuPath(path, roleCodes) 로 기준 메뉴 URL 확정
      - path 가 등록된 메뉴이거나 권한이 있으면 → path 그대로
      - 아니면 등록된 suffix 를 떼고 부모 화면 URL 로 (예: /campaign/sms/create → /campaign/sms)
   c. 세션의 pageAuthCache 에서 authPath 키로 조회 → 있으면 재사용
   d. 없으면 menuSource.getPermissions(authPath, roleCodes) 로 권한 Set 조회 후 PageAuth.from(...)
   e. 결과를 세션 pageAuthCache 에 저장
```

`menuSource`는 `sms.menu.source` 설정에 따라 `DbMenuSource`(db) 또는 `StaticMenuSource`(static)가 주입된다.

- **db**: `TB_MENU` + `TB_MENU_AUTH`에서 `(MENU_URL, 역할들)` 기준 조회. `USE_YN = 'Y'` 행만, 역할이 여러 건이면 `CAN_*`별 MAX 집계로 `'Y'` 우선.
- **static**: baseline 메뉴 URL이면 모든 권한 부여(local 화면 검증 용도).

### 4.3 local 특례 (PageAuth.all())

local profile에서는 `resolvePageAuth`가 곧바로 `PageAuth.all()`을 반환한다. **개발 중 모든 버튼을 보며 화면을 만지려는 편의** 때문이다. 하지만 이는 렌더링용 `PageAuth`에만 해당하고, 서버의 실제 URL 차단(`MenuAuthInterceptor` → `MenuAuthService`)은 local에서도 DB 권한을 그대로 적용한다. 그래서 **local에서 버튼이 보인다고 prod에서 보인다는 보장이 없다.** 권한 UI는 반드시 db source 환경(dev/prod 또는 local+`sms.menu.source=db`)에서 확인해야 한다.

### 4.4 템플릿 주입

계산된 `PageAuth`는 `pageAuth`라는 이름으로 모델에 담긴 뒤, `defaultLayout.html`이 두 가지로 소비한다.

1. **Thymeleaf 서버 렌더링** — `th:if="${pageAuth.create}"` 등으로 버튼 노출 제어.
2. **JS 전역 주입** — `window.PAGE_AUTH` 객체로 직렬화([5절](#5-page_auth-js-전역) 참조).

---

## 5. PAGE_AUTH JS 전역

### 5.1 defaultLayout이 주입하는 법

`defaultLayout.html`이 `pageAuth` 모델 속성을 읽어 **JS 전역 `window.PAGE_AUTH`** 를 정의한다. `SESSION_INFO` 정의 바로 아래에 위치한다.

```html
<!-- defaultLayout.html -->
<script th:inline="javascript">
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
</script>
```

`pageAuth`가 null이면(미인증 등) 모든 값이 `false`로 떨어진다.

### 5.2 JS에서 버튼/그리드 제어에 쓰는 법

**strict comparison + fail-closed**가 규칙이다. 값이 문자열 `'true'`로 내려오는 경우까지 차단하려면 `=== true`로 비교하고, `PAGE_AUTH` 자체가 없으면 거부하는 방향으로 가드를 쓴다.

```javascript
// GOOD: strict + fail-closed
const canSave = () => {
    const auth = window.PAGE_AUTH || {};
    return (state.mode === 'create' && auth.create === true)
        || (state.mode === 'update' && auth.update === true);
};

// GOOD: 다운로드 가드 — PAGE_AUTH 가 없거나 download 가 true 가 아니면 차단
if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) {
    Notify.alert('다운로드 권한이 없습니다.');
    return;
}
```

```javascript
// BAD: fail-open — PAGE_AUTH 가 undefined 면 조건이 truthy 가 되어 통과될 수 있음
if (window.PAGE_AUTH && window.PAGE_AUTH.download !== true) { return; }  // 금지

// BAD: 느슨한 비교 — 문자열 'true' 가 통과됨
if (auth.download == true) { ... }  // 금지
```

스캐폴드가 생성하는 CRUD 화면(`scaffold-templates/crud/page.js.tpl`)도 이 패턴을 그대로 따른다. `TuiPageBuilder`는 `PAGE_AUTH.download`가 `true`가 아니면 그리드 우클릭 메뉴를 복사 전용으로 제한한다.

> 팝업 화면(`notice-popup.html`)은 v2 계약상 `window.PAGE_AUTH` 대신 자체 `window.noticePageAuth` 객체를 쓴다. 일반 업무 화면은 `PAGE_AUTH`를 사용한다.

### 5.3 Thymeleaf와 JS의 역할 분담

| 시점 | 수단 | 용도 |
|---|---|---|
| 서버 렌더링 | `th:if="${pageAuth.create}"` | 권한 없는 버튼을 아예 HTML에서 제거 |
| 클라이언트 동작 | `window.PAGE_AUTH.create === true` | 모달 저장/삭제 버튼 동적 표시·숨김, 그리드 제어 |
| 서버 API | `MenuAuthInterceptor` | URL 재검증. 앞 두 단계는 편의, 이것이 최종 방어 |

버튼을 숨기는 것만으로 보안을 충족한 것이 아니다. **서버 검증이 항상 최종 권한 판단**이다.

---

## 6. MenuAuthInterceptor (URL 권한 검증)

### 6.1 역할과 등록

`MenuAuthInterceptor`는 **좌측 메뉴 표시와 별개로 모든 URL/API 요청을 `TB_MENU_AUTH` 기준으로 다시 검증**한다. `WebMvcConfig`가 `/**`에 등록하고 공통 경로만 제외한다.

```text
제외 경로(COMMON_EXCLUDE_PATHS):
  /, /login, /logout, /error,
  /css/**, /js/**, /lib/**, /vendor/**, /img/**, /favicon.ico,
  /api/common-code/**   (로그인한 모든 사용자가 쓰는 화면 보조 API)
  /samples/**           (정적 샘플. prod 는 SamplesBlockInterceptor 가 404 차단)
```

추가 제외가 필요하면 `application*.yml`의 `sms.menu.auth.exclude-paths`에 설정한다. **업무 화면/API URL은 절대 제외 목록에 넣지 않는다.** 메뉴 등록과 권한 부여로 해결한다.

### 6.2 preHandle 동작

```java
// MenuAuthInterceptor.preHandle(...)
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication == null
    || !(authentication.getPrincipal() instanceof SmsUserPrincipal principal)) {
    return true;   // 미인증 차단은 Spring Security 1차 인가가 담당
}
String path = request.getRequestURI().substring(request.getContextPath().length());
menuAuthService.checkAccess(path, principal.getRoleCodes());
return true;
```

인증된 요청(`SmsUserPrincipal`)만 `checkAccess`로 넘어간다. 권한이 없으면 `checkAccess` 안에서 예외가 던져진다.

### 6.3 checkAccess 판정 순서 (suffix 기반)

`MenuAuthService.checkAccess(path, roleCodes)`의 판정 순서는 다음 세 단계다.

```text
1. 요청 URL 이 메뉴 URL 과 정확히 일치 → 화면 접근 → READ 검증
   (예: /campaign/sms/register 같은 화면 URL 이 suffix 규칙보다 먼저 잡힌다)
2. 일치하는 메뉴가 없으면:
   - 활성 메뉴로 등록됐지만 현재 역할에 부여된 권한이 없으면(빈 Set) → 즉시 거부
     (suffix 부모 권한으로 우회하지 않음. UI 권한 상승 방지)
   - 아니면 URL suffix 를 떼고 부모 화면 URL 기준으로 액션 권한 검증
3. 어느 메뉴에도 연결되지 않는 URL → 거부
```

### 6.4 URL suffix → 필요 권한 매핑

| suffix | 필요 권한 |
|---|---|
| `/data`, `/search`, `/detail`, `/popup`, `/tree` | `READ` |
| `/create`, `/register` | `CREATE` |
| `/update` | `UPDATE` |
| `/save` (legacy) | `CREATE` + `UPDATE` **모두** |
| `/delete` | `DELETE` |
| `/approve`, `/reject` | `APPROVE` |
| `/cancel` | `CANCEL` |
| `/excel`, `/download`, `/export` | `DOWNLOAD` |
| `/unmask` | `MASK_VIEW` |

등록과 수정은 `/create`, `/update`로 분리하는 것이 v3 규칙이다. 등록/수정 겸용 `/save`는 신규 코드에서 만들지 않으며, legacy 호환으로 유지할 때만 `CREATE`와 `UPDATE`를 모두 요구한다.

**예시**: `POST /campaign/sms/create` 요청은 정확 일치 메뉴가 없으면 suffix `/create`를 떼어 부모 `/campaign/sms`의 권한을 보고, 그 역할에 `CREATE`가 있어야 통과한다.

### 6.5 권한 없음 시 일어나는 일

`checkAccess`가 실패하면 `CustomException(ErrorCode.ACCESS_DENIED)`를 던진다.

- `ErrorCode.ACCESS_DENIED` = HTTP 403, 코드 `A002`, 메시지 "해당 기능에 대한 접근 권한이 없습니다."
- `GlobalExceptionHandler`가 Accept 헤더로 분기해 **화면 요청은 `error/error` 페이지, JSON 요청은 `ApiResponse` 403 JSON**으로 응답한다.

Controller에서 권한을 중복 검사하지 않는다. URL/API 접근 검증은 이 Interceptor의 단일 책임이다.

---

## 7. GlobalModelAdvice (공통 모델 주입)

### 7.1 역할

`@ControllerAdvice(annotations = Controller.class)`로, **`@Controller`로 응답하는 모든 요청**에 `defaultLayout`(sidebar/header)이 쓰는 공통 모델을 채운다. 메뉴 렌더링은 "Controller가 넘긴 메뉴 tree만 사용한다"는 규칙의 단일 적용 지점이다.

### 7.2 주입하는 모델 속성

`addLayoutAttributes(@AuthenticationPrincipal SmsUserPrincipal principal, Model model, HttpServletRequest request)`가 채우는 속성:

| 속성 | 내용 | 비고 |
|---|---|---|
| `user` | `SmsUserPrincipal` | `SESSION_INFO`, 사이드바 사용자 정보의 원천 |
| `menus` | 권한 기반 메뉴 트리(`List<MenuItemVO>`) | sidebar 렌더링용. 세션에 캐시 |
| `pageAuth` | 현재 화면 기능 권한(`PageAuth`) | 버튼 노출/PAGE_AUTH 주입용. 세션에 캐시 |
| `clientIp` | 접속 IP | `X-Forwarded-For` 우선, 루프백은 `127.0.0.1`로 정규화 |

`principal == null`(미인증 등)이면 `pageAuth = PageAuth.none()`만 넣고 나머지(`user`, `menus`)는 넣지 않는다.

### 7.3 캐싱 동작

매 요청 DB를 치지 않도록 두 가지를 **HTTP 세션**에 캐시한다.

- **메뉴 트리**: 세션 속성 `menuTree`. 없으면 `menuSource.getMenuTree(roleCodes)`로 만들어 저장.
- **페이지 권한**: 세션 속성 `pageAuthCache`(`Map<String, PageAuth>`). 정규화한 요청 경로를 키로 저장. 없으면 기준 메뉴 URL을 해석해 계산한 뒤 저장.

메뉴 등록·수정·삭제 트랜잭션이 커밋되면 `MenuManageService`가 `MenuCacheRevision.invalidateAfterCommit()`을 호출해 애플리케이션 revision을 올린다. `GlobalModelAdvice`는 다음 MVC 화면 요청에서 세션의 revision과 비교하고, 값이 다르면 `menuTree`와 `pageAuthCache`를 함께 제거한 뒤 다시 조회한다. 따라서 단일 애플리케이션 인스턴스에서는 권한 변경 확인을 위해 재로그인할 필요가 없다.

revision은 현재 프로세스의 `AtomicLong`이므로 다중 서버에서는 인스턴스 간에 공유되지 않는다. 다중 서버로 운영할 때는 DB revision이나 공유 캐시 기반 무효화로 교체해야 한다. local profile은 페이지 권한 계산 전에 `PageAuth.all()`로 조기 반환되지만 메뉴 트리 revision 갱신 흐름은 동일하다.

---

## 8. AuthSourceGuard (기동 시 source 검증)

`AuthSourceGuard`는 **메뉴/역할 source 조합을 부팅 시점에 강제**하는 컴포넌트다. 잘못된 조합이면 생성자에서 `IllegalStateException`을 던져 **컨텍스트 기동을 실패**시킨다. 보호하는 불변식은 세 가지다.

| 검증 | 조건 | 실패 메시지 요지 |
|---|---|---|
| auth.mode 제한 | `local` profile이 아닌데 `sms.auth.mode=local` | "sms.auth.mode=local은 spring profile local에서만 허용" |
| prod source 제한 | `prod` profile인데 `sms.menu.source` 또는 `sms.role.source`가 `db`가 아님 | "prod profile은 db source만 허용" |
| 잘못된 조합 | `sms.menu.source=db` 이면서 `sms.role.source=static` | "db 메뉴는 static 역할과 함께 쓸 수 없다" |

배경: `StaticMenuSource`는 baseline URL에 **모든 권한을 부여**하므로 prod에 새면 전 권한 부여 사고가 난다. 또 `role=static`은 `menu=static`(local)과만 함께 쓴다. (`menu=static` + `role=db`는 dev 메뉴 검증용으로 허용되는 정상 조합이다.)

관련 설정 기본값:

```yaml
sms:
  menu:
    source: db      # dev/prod 기본. static 은 local 화면 검증용
  role:
    source: db      # dev/prod 기본
  auth:
    mode: ldap      # local 에서만 local 허용
```

---

## 9. 새 화면에 권한 검사 추가하기

새 업무 화면(예: `/foo/bar`)을 추가할 때 권한 검사를 붙이는 순서다. **Interceptor 제외 경로에 추가하지 않는다.**

### 9.1 서버: 메뉴 등록과 권한 부여 (필수)

1. `TB_MENU`에 화면 메뉴를 등록한다.
   - `MENU_URL = '/foo/bar'`, `MENU_TYPE = 'M'`, `USE_YN = 'Y'`, 좌측 노출이면 `DISPLAY_YN = 'Y'`.
   - `MENU_URL`은 null 제외 유일해야 한다.
2. `TB_MENU_AUTH`에 역할별 권한을 부여한다.
   - 조회만 필요하면 `CAN_READ = 'Y'`. 등록/수정/삭제/다운로드가 필요하면 해당 `CAN_*`를 `'Y'`.
   - `USE_YN = 'Y'` 행만 권한으로 인정된다.
3. seed를 바꾸면 `docs/base/v2-menu-baseline.md`, `StaticMenuSource`, `db/oracle/02_menu_auth_seed.sql`을 함께 갱신한다.

이것만으로 `MenuAuthInterceptor`가 화면 접근(`READ`)과 suffix 액션을 자동으로 검증한다. Controller에 별도 권한 코드를 넣지 않는다.

### 9.2 Controller: suffix 규약 맞추기

endpoint URL을 [6.4절](#64-url-suffix--필요-권한-매핑) suffix 규약에 맞게 짓는다. 그러면 Interceptor가 부모 화면(`/foo/bar`) 권한으로 액션을 검증한다.

```java
// GOOD: suffix 규약 준수 → Interceptor 가 자동 검증
@PostMapping("/foo/bar/create")   // CREATE 필요
@PostMapping("/foo/bar/update")   // UPDATE 필요
@PostMapping("/foo/bar/delete")   // DELETE 필요
@GetMapping("/foo/bar/data")      // READ 필요 (그리드 데이터)
@GetMapping("/foo/bar/excel")     // DOWNLOAD 필요
```

```java
// BAD: suffix 에 걸리지 않는 임의 URL → 어느 메뉴에도 연결되지 않아 403
@PostMapping("/foo/bar/doSave")   // 매핑된 suffix 가 아님 → 거부
```

### 9.3 Thymeleaf: 버튼 노출 제어

`defaultLayout`을 decorate하는 화면에서 `pageAuth` 속성으로 버튼을 제어한다. 권한 없는 버튼은 HTML에서 제거된다.

```html
<!-- 목록 화면 상단 -->
<button type="button" id="btn-create" th:if="${pageAuth.create}"
        class="btn btn-outline-primary">등록</button>
<button type="button" id="btn-excel" th:if="${pageAuth.download}"
        class="btn btn-success">엑셀</button>

<!-- 모달 안 -->
<button type="button" id="btn-save"
        th:if="${pageAuth.create or pageAuth.update}" class="btn btn-primary">저장</button>
<button type="button" id="btn-delete" th:if="${pageAuth.delete}"
        class="btn btn-danger">삭제</button>
```

기존 화면 예시: `templates/system/menu-manage.html`, `templates/basic/notice.html`, `templates/sms/history.html`.

### 9.4 JS: 동적 제어 (strict + fail-closed)

모달 저장/삭제처럼 동적으로 보여주거나, 그리드 다운로드처럼 JS에서 제어할 때는 `window.PAGE_AUTH`를 strict로 읽는다.

```javascript
const auth = window.PAGE_AUTH || {};

// 저장 버튼: 모드별로 create/update 를 strict 검사
const canSave = () =>
    (state.mode === 'create' && auth.create === true)
    || (state.mode === 'update' && auth.update === true);

// 삭제 버튼: update 모드이고 delete 권한이 있어야 노출
deleteBtn.hidden = state.mode !== 'update' || auth.delete !== true;

// 엑셀 다운로드 가드: fail-closed
if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) {
    Notify.alert('다운로드 권한이 없습니다.');
    return;
}
```

### 9.5 체크리스트

```text
[ ] TB_MENU 에 MENU_URL 등록 (USE_YN='Y', MENU_TYPE='M')
[ ] TB_MENU_AUTH 에 역할별 CAN_* 부여 (USE_YN='Y')
[ ] seed 변경 시 baseline/StaticMenuSource/seed.sql 동기화
[ ] Controller endpoint 를 suffix 규약(/create,/update,/delete,/data,/excel ...)에 맞춤
[ ] Thymeleaf 버튼에 th:if="${pageAuth.xxx}" 적용
[ ] JS 동적 제어에 window.PAGE_AUTH strict(=== true) + fail-closed 적용
[ ] Interceptor 제외 경로에 추가하지 않았는지 확인
[ ] db source 환경에서 권한 없는 역할로 버튼 숨김/403 확인
```

---

## 10. 자주 하는 실수

### 10.1 PAGE_AUTH 검사를 빼먹는다

`th:if`로 버튼을 숨겼지만 JS 모달의 저장/삭제 가드를 빼먹으면, 권한 없는 사용자가 개발자 도구로 버튼을 살려 호출할 수 있다. **JS 쓰기 동작마다 `window.PAGE_AUTH` strict 검사를 넣는다.** 그래도 최종 방어는 서버 Interceptor이므로, 검사를 빼먹어도 API는 403으로 막힌다. 하지만 UI가 권한 없는 동작을 허용하는 것처럼 보이는 것 자체가 결함이다.

### 10.2 fail-open 가드를 쓴다

```javascript
// 금지: PAGE_AUTH 가 undefined 면 통과
if (window.PAGE_AUTH && window.PAGE_AUTH.download !== true) { return; }
```

`PAGE_AUTH`가 없는 상황을 "권한 있음"으로 처리하면 안 된다. **항상 `!window.PAGE_AUTH || ... !== true` 형태의 fail-closed**로 쓴다. `== true` 같은 느슨한 비교도 금지(문자열 `'true'` 통과).

### 10.3 suffix에 걸리지 않는 endpoint URL

`/foo/bar/doSave`, `/foo/bar/modify`처럼 매핑된 suffix로 끝나지 않으면, Interceptor는 어느 메뉴에도 연결되지 않는 URL로 보고 **무조건 403**을 던진다. endpoint는 반드시 [6.4절](#64-url-suffix--필요-권한-매핑)의 suffix 규약을 따른다. 신규 코드에서 `/save` 겸용 endpoint도 만들지 않는다.

### 10.4 local과 prod의 동작 차이를 모른다

local은 렌더링용 `PageAuth`가 **`PageAuth.all()`**이라 모든 버튼이 보인다. 따라서 **local에서 버튼이 보인다고 prod에서 보인다는 뜻이 아니다.** 권한 UI(버튼 숨김, 403)는 반드시 db source 환경에서 확인한다.

| 구분 | local | dev/prod |
|---|---|---|
| 인증 | ID-only (비밀번호 없음) | LDAP bind |
| 렌더링 `PageAuth` | 항상 `all()` (모든 버튼 노출) | DB 권한 그대로 |
| 서버 API 차단 | **DB 권한 그대로** | DB 권한 그대로 |
| source 기본 | 설정에 따라 static 가능 | `db` 강제 (`AuthSourceGuard`) |

### 10.5 업무 URL을 Interceptor 제외 경로에 넣는다

"자꾸 403이 난다"며 `sms.menu.auth.exclude-paths`나 `COMMON_EXCLUDE_PATHS`에 업무 URL을 추가하면 권한 검증이 통째로 사라진다. **제외 경로는 로그인/정적 리소스/공통코드 전용**이다. 403이 나면 메뉴 등록·권한 부여·suffix 규약을 먼저 점검한다.

### 10.6 `EMP.PERM_*`로 권한을 판단한다

legacy `EMP.PERM_*` 컬럼은 v3 권한 판단에 사용하지 않는다. 메뉴 노출과 API 권한은 오직 `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH` 기준이다. 사용자 식별도 `EMP_ID` 단독이 아니라 **`(EMP_ID, DEP_ID)` 복합키**로 한다.

### 10.7 권한 변경 후 캐시를 잊는다

메뉴 관리 서비스 경유 변경은 커밋 후 revision이 증가하고 다음 MVC 화면 요청에서 기존 세션 캐시가 자동 갱신된다. 반면 운영자가 DB를 직접 수정하면 revision이 증가하지 않으므로 기존 세션에 즉시 반영되지 않는다. 직접 SQL 변경 후에는 애플리케이션 재기동이나 세션 만료 정책을 사용하고, 다중 서버에서는 공유 revision 도입 여부를 확인한다.

---

## 11. 핵심 파일 지도

| 파일 | 역할 |
|---|---|
| `auth/SmsUserPrincipal.java` | `UserDetails` 구현. empId/depId/empNm/depNm/roleCodes 보유 |
| `auth/ActiveEmployeeResolver.java` | 사번으로 활성 EMP+DEP 단일 건 조회 (0건/2건↑ 실패) |
| `auth/LocalIdOnlyAuthenticationProvider.java` | `@Profile("local")` ID-only 인증 |
| `auth/LdapEmployeeContextMapper.java` | LDAP 인증 후 EMP 조회·역할 조회·principal 생성 |
| `config/LdapAuthenticationConfig.java` | `@Profile({"dev","prod"})` LDAP provider 구성 |
| `config/SecurityConfig.java` | 폼 로그인·CSRF·1차 인가(`authenticated()`) |
| `config/MenuAuthInterceptor.java` | 인증된 요청의 URL 권한 재검증 (2차 인가) |
| `config/WebMvcConfig.java` | Interceptor 등록·제외 경로 관리 |
| `config/AuthSourceGuard.java` | 기동 시 menu/role/auth source 조합 강제 |
| `config/SmsLdapProperties.java` | `sms.ldap.*` 설정 바인딩 |
| `service/menu/MenuPermission.java` | `CAN_*` 8종과 1:1 대응하는 권한 enum |
| `service/menu/PageAuth.java` | 화면 기능 권한 묶음 (from/all/none) |
| `service/menu/MenuAuthService.java` | URL → 권한 판정 (checkAccess, resolveBaseMenuPath) |
| `service/menu/MenuSource.java` | 메뉴 트리+권한 조회 인터페이스 (db/static 공통) |
| `service/menu/MenuCacheRevision.java` | 메뉴·권한 변경 커밋 후 revision 증가, 세션 캐시 갱신 신호 제공 |
| `service/menu/EmployeeRoleService.java` | `(EMP_ID, DEP_ID)` 활성 역할 조회 |
| `controller/GlobalModelAdvice.java` | user/menus/pageAuth/clientIp 공통 모델 주입·세션 캐시 |
| `templates/defaultLayout.html` | `SESSION_INFO`, `window.PAGE_AUTH` JS 전역 주입 |
| `templates/fragments/sidebar.html` | `user`, `menus` 로 사용자 정보·메뉴 트리 렌더링 |
| `exception/ErrorCode.java` | `ACCESS_DENIED` (403, A002) 정의 |
