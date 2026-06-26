# 02-auth-and-security.md — 인증/인가

## 1. SecurityFilterChain 개요

인증/인가의 진입점은 `SecurityConfig`의 `securityFilterChain`이다: `src/main/java/com/scbk/sms/config/SecurityConfig.java:17~49`행.

### 1.1 AuthenticationProvider 등록

`SecurityConfig`는 Spring이 제공하는 `List<AuthenticationProvider>` 전체를 SecurityFilterChain에 등록한다: `SecurityConfig.java:18~22`행.

- local profile: `LocalIdOnlyAuthenticationProvider`가 `@Profile("local")`로 등록된다: `src/main/java/com/scbk/sms/auth/LocalIdOnlyAuthenticationProvider.java:13~15`행.
- dev/prod profile: `LdapAuthenticationConfig`가 `@Profile({"dev", "prod"})`로 등록된다: `src/main/java/com/scbk/sms/config/LdapAuthenticationConfig.java:17~19`행.

`AuthSourceGuard`는 profile별 auth/source 조합을 부팅 시점에 강제한다: `src/main/java/com/scbk/sms/config/AuthSourceGuard.java:24~50`행.

### 1.2 HTTP 접근 규칙

- `/login`, `/error`은 permitAll: `SecurityConfig.java:27~29`행.
- 정적 리소스와 favicon은 permitAll: `SecurityConfig.java:29~30`행.
- 그 외 모든 요청은 authenticated여야 한다: `SecurityConfig.java:27~31`행.

### 1.3 로그인/로그아웃

- 로그인 화면: `/login`
- 로그인 처리: `/login`
- 사용자 인자: `empId`
- 비밀번호 인자: `password`
- 성공 이동: `/`
- 실패 이동: `/login?error`

관련 위치: `SecurityConfig.java:32~40`행.

- 로그아웃 URL: `/logout`
- 로그아웃 성공: `/login?logout`
- 세션 무효화: `invalidateHttpSession(true)`
- `JSESSIONID` 쿠키 삭제

관련 위치: `SecurityConfig.java:41~47`행.

### 1.4 CSRF

`SecurityConfig`는 CSRF를 명시적으로 disable하지 않고 기본값을 유지한다: `SecurityConfig.java:24~26`행. 주석에 따르면 Thymeleaf 폼은 토큰을 자동 주입하고, axios 호출은 공통 JS 요청 인터셉터가 `<meta name="_csrf">`를 헤더로 싣는다: `SecurityConfig.java:24~27`행.

## 2. local ID-only 인증 vs dev/prod LDAP 인증

### 2.1 local 인증 흐름

local에서는 `LocalIdOnlyAuthenticationProvider`가 인증을 처리한다: `LocalIdOnlyAuthenticationProvider.java:13~15`행.

```text
POST /login
  -> UsernamePasswordAuthenticationToken(empId, password)
  -> LocalIdOnlyAuthenticationProvider.authenticate()
       -> ActiveEmployeeResolver.resolveSingleActiveEmployee(empId)
            -> LoginEmployeeMapper.selectActiveEmployeesByEmpId()
            -> EMP/DEP actYn='Y'
       -> EmployeeRoleService.getActiveRoleCodes(empId, depId)
            -> role.source=static이면 StaticRoleProvider
            -> role.source=db이면 EmployeeRoleMapper.selectRoleCodes()
       -> SmsUserPrincipal(employee, roleCodes)
       -> UsernamePasswordAuthenticationToken(principal, credentials, authorities)
```

핵심 위치:

- `LocalIdOnlyAuthenticationProvider.authenticate`: `LocalIdOnlyAuthenticationProvider.java:26~32`행.
- `ActiveEmployeeResolver.resolveSingleActiveEmployee`: `src/main/java/com/scbk/sms/auth/ActiveEmployeeResolver.java:19~33`행.
- `SmsUserPrincipal`: `src/main/java/com/scbk/sms/auth/SmsUserPrincipal.java:19~29`행.
- `SmsUserPrincipal.getAuthorities`: `SmsUserPrincipal.java:51~54`행.

`ActiveEmployeeResolver`는 사번이 비어 있으면 `BadCredentialsException("사번을 입력해야 합니다.")`를 던지고, 사번으로 조회한 활성 사용자 수가 0건이면 `BadCredentialsException("활성 사용자 정보를 찾을 수 없습니다.")`, 1건 초과이면 `BadCredentialsException("동일 사번에 활성 부서가 여러 건입니다. EMP 데이터를 정리해야 합니다.")`를 던진다: `ActiveEmployeeResolver.java:19~32`행.

### 2.2 LDAP 인증 흐름

dev/prod에서는 `LdapAuthenticationConfig`가 `AuthenticationProvider`를 만든다: `LdapAuthenticationConfig.java:22~43`행.

```text
LDAP bind authenticator
  -> DefaultSpringSecurityContextSource(url + "/" + baseDn)
  -> managerDn / managerPassword 설정
  -> FilterBasedLdapUserSearch(userSearchBase, userSearchFilter)
  -> BindAuthenticator
  -> DefaultLdapAuthoritiesPopulator(contextSource, null)
  -> LdapAuthenticationProvider(authenticator, authoritiesPopulator)
  -> userDetailsContextMapper = LdapEmployeeContextMapper
```

핵심 위치:

- `LdapAuthenticationConfig.java:26~31`행: `DefaultSpringSecurityContextSource`와 manager credentials.
- `LdapAuthenticationConfig.java:33~38`행: `BindAuthenticator`와 `FilterBasedLdapUserSearch`.
- `LdapAuthenticationConfig.java:40~43`행: authorities populator와 `LdapEmployeeContextMapper`.
- `SmsLdapProperties`는 `sms.ldap` prefix의 `url`, `baseDn`, `managerDn`, `managerPassword`, `userSearchBase`, `userSearchFilter`를 바인딩한다: `src/main/java/com/scbk/sms/config/SmsLdapProperties.java:5~62`행.

### 2.3 LDAP 사용자 매핑

`LdapEmployeeContextMapper`는 LDAP 인증 성공 후 프로젝트 내부 사용자로 매핑한다: `src/main/java/com/scbk/sms/auth/LdapEmployeeContextMapper.java:24~30`행.

```text
LDAP username
  -> ActiveEmployeeResolver.resolveSingleActiveEmployee(username)
  -> EmployeeRoleService.getActiveRoleCodes(employee.empId, employee.depId)
  -> SmsUserPrincipal(employee, roleCodes)
```

LDAP 사용자 쓰기는 지원하지 않는다. `mapUserToContext`는 `UnsupportedOperationException("LDAP 사용자 쓰기는 지원하지 않습니다.")`를 던진다: `LdapEmployeeContextMapper.java:33~36`행.

### 2.4 profile별 인증 모드 설정

- 기본: `spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}`: `src/main/resources/application.yml:4~5`행.
- local: `sms.auth.mode=local`, `sms.menu.source=static`, `sms.role.source=static`: `src/main/resources/application-local.yml:8~18`행.
- dev: `sms.auth.mode=ldap`, `sms.menu.source=db`, `sms.role.source=db`: `src/main/resources/application-dev.yml:8~15`행.
- prod: `sms.auth.mode=ldap`, `sms.menu.source=db`, `sms.role.source=db`: `src/main/resources/application-prod.yml:5~12`행.

`AuthSourceGuard`는 non-local profile에서 `sms.auth.mode=local`을 금지하고, prod에서 menu/role source가 모두 db가 아니면 실패시킨다: `AuthSourceGuard.java:32~48`행.

## 3. 권한 모델: TB_ROLE / TB_EMP_ROLE / TB_MENU / TB_MENU_AUTH

### 3.1 권한 로딩 구조

인증 성공 시 사용자의 권한 코드는 `EmployeeRoleService`를 통해 로드된다: `src/main/java/com/scbk/sms/service/menu/EmployeeRoleService.java:16~22`행.

```text
EmployeeRoleService
  -> RoleProvider.getActiveRoleCodes(empId, depId)
       -> DbRoleProvider or StaticRoleProvider
```

구현 선택:

- `DbRoleProvider`는 `sms.role.source=db`일 때만 등록된다: `src/main/java/com/scbk/sms/service/menu/DbRoleProvider.java:9~10`행.
- `StaticRoleProvider`는 `sms.role.source=static`일 때만 등록된다: `src/main/java/com/scbk/sms/service/menu/StaticRoleProvider.java:8~10`행.

DB 역할 조회 SQL은 `EmployeeRoleMapper.xml`에서 실행된다: `src/main/resources/mapper/menu/EmployeeRoleMapper.xml:8~18`행.

```sql
SELECT ER.ROLE_CD
FROM SMS.TB_EMP_ROLE ER
INNER JOIN SMS.TB_ROLE R ON R.ROLE_CD = ER.ROLE_CD
WHERE ER.EMP_ID = #{empId}
  AND ER.DEP_ID = #{depId}
  AND ER.USE_YN = 'Y'
  AND R.USE_YN = 'Y'
ORDER BY R.SORT_ORD, R.ROLE_CD
```

역할이 없으면 인증 실패로 처리된다: `EmployeeRoleService.java:18~20`행.

### 3.2 SmsUserPrincipal

인증 후 `SecurityContextHolder`의 principal은 `SmsUserPrincipal`이다: `MenuAuthInterceptor.java:27~30`행.

`SmsUserPrincipal`는 사번, 부서 ID/명, 권한 코드 목록을 저장하고, 권한 코드를 Spring Security authority로 변환한다: `SmsUserPrincipal.java:12~29`행, `SmsUserPrincipal.java:47~54`행.

```text
LoginEmployeeVO
  -> SmsUserPrincipal
       empId
       depId
       empNm
       depNm
       roleCodes[]
       authorities[] = SimpleGrantedAuthority(roleCode)
```

### 3.3 메뉴 권한 판정

`MenuAuthService`는 요청 URL을 기준으로 권한을 판정한다: `src/main/java/com/scbk/sms/service/menu/MenuAuthService.java:21~55`행.

판정 순서:

1. 요청 URL이 메뉴 URL과 정확히 일치하면 화면 접근으로 보고 READ 권한을 검사한다.
2. 정확히 일치하지 않으면 URL suffix를 떼고 부모 화면 URL 기준으로 액션 권한을 검사한다.
3. 어느 메뉴에도 연결되지 않으면 거부한다.

핵심 위치:

- `MenuAuthService.checkAccess`: `MenuAuthService.java:32~55`행.
- suffix 권한 매핑: `MenuAuthService.java:57~76`행.

suffix 예:

| suffix | 필요 권한 |
|---|---|
| `/data`, `/search`, `/detail`, `/tree` | READ |
| `/create`, `/register` | CREATE |
| `/update` | UPDATE |
| `/delete` | DELETE |
| `/approve`, `/reject` | APPROVE |
| `/cancel` | CANCEL |
| `/excel`, `/download`, `/export` | DOWNLOAD |
| `/unmask` | MASK_VIEW |
| `/save` | CREATE + UPDATE |

### 3.4 DB 메뉴 소스

dev/prod에서 `sms.menu.source=db`이면 `DbMenuSource`가 사용된다: `src/main/java/com/scbk/sms/service/menu/DbMenuSource.java:14~16`행.

`DbMenuSource`는 두 가지 조회를 제공한다:

- `getMenuTree`: `TB_MENU`와 `TB_MENU_AUTH`를 JOIN해 읽기 가능한 메뉴 목록을 반환한다: `DbMenuSource.java:30~38`행.
- `getPermissions`: `TB_MENU_AUTH`에서 특정 메뉴 URL의 권한 플래그를 조회한다: `DbMenuSource.java:41~60`행.

SQL:

- `MenuMapper.xml.selectReadableMenus`: `src/main/resources/mapper/menu/MenuMapper.xml:8~33`행.
- `MenuAuthMapper.xml.selectMenuPermissions`: `src/main/resources/mapper/menu/MenuAuthMapper.xml:9~30`행.

`MenuAuthMapper`는 여러 역할의 `CAN_*` 컬럼을 `MAX`로 집계한다. 주석과 SQL에 따르면 'Y' 우선 합산 방식이다: `MenuAuthMapper.xml:8~18`행.

`DbMenuSource`는 `auth.getCanRead()` 등 플래그가 `YES` 문자열과 같을 때 `MenuPermission`을 추가한다: `DbMenuSource.java:63~67`행.

### 3.5 static 메뉴 소스

local에서 `sms.menu.source=static`이면 `StaticMenuSource`가 사용된다: `src/main/java/com/scbk/sms/service/menu/StaticMenuSource.java:17~19`행.

`StaticMenuSource`의 목적은 DB 메뉴 등록 전 local 화면 검증용 baseline이다: `StaticMenuSource.java:12~16`행.

- `getMenuTree`: static baseline menu를 `MenuTreeBuilder`로 트리화한다: `StaticMenuSource.java:32~35`행.
- `getPermissions`: static tree에 메뉴 URL이 있으면 모든 권한을 반환한다: `StaticMenuSource.java:37~45`행.

이 static source는 운영 권한 검증용이 아니며, 최종 권한 검증은 반드시 DB source에서 해야 한다고 주석에 명시되어 있다: `StaticMenuSource.java:12~16`행.

### 3.6 메뉴 트리 빌드

`MenuTreeBuilder`는 flat menu 목록을 `parentMenuId` 기준으로 트리화한다: `src/main/java/com/scbk/sms/service/menu/MenuTreeBuilder.java:13~39`행.

검증:

- `menuId`가 null 또는 blank이면 예외: `MenuTreeBuilder.java:41~44`행.
- 중복 `menuId`가 있으면 예외: `MenuTreeBuilder.java:17~21`행.
- 부모 메뉴를 찾을 수 없으면 예외: `MenuTreeBuilder.java:32~35`행.

## 4. 메뉴 권한 Interceptor

### 4.1 등록 방식

`MenuAuthInterceptor`는 `@Component`로 등록되고 `WebMvcConfig`가 모든 URL에 적용한다: `src/main/java/com/scbk/sms/config/MenuAuthInterceptor.java:12~17`행, `src/main/java/com/scbk/sms/config/WebMvcConfig.java:34~41`행.

### 4.2 제외 대상

`WebMvcConfig`는 다음 경로를 제외한다:

- 공통 제외: `/`, `/login`, `/logout`, `/error`, `/css/**`, `/js/**`, `/lib/**`, `/vendor/**`, `/img/**`, `/favicon.ico`, `/api/common-code/**`: `WebMvcConfig.java:14~20`행.
- 설정값: `sms.menu.auth.exclude-paths`에서 쉼표로 구분한 추가 경로: `WebMvcConfig.java:25~31`행, `WebMvcConfig.java:39~41`행.
- local 설정 예: `/system/scaffold`, `/system/scaffold/**`, `/system/menu-tree`, `/system/menu-tree/**`: `src/main/resources/application-local.yml:14~15`행.

주석은 화면/업무 URL을 `COMMON_EXCLUDE_PATHS`에 추가하지 말라고 명시한다: `WebMvcConfig.java:14~15`행.

### 4.3 검증 로직

```text
preHandle(request, response, handler)
  -> Authentication authentication = SecurityContextHolder.getContext().getAuthentication()
  -> authentication == null 또는 principal이 SmsUserPrincipal이 아니면 true
  -> path = requestURI.substring(contextPath.length())
  -> menuAuthService.checkAccess(path, principal.getRoleCodes())
  -> true
```

관련 위치: `MenuAuthInterceptor.java:25~35`행.

`MenuAuthInterceptor`의 주석은 좌측 메뉴 표시와 별개로 모든 URL/API 요청을 `TB_MENU_AUTH` 기준으로 다시 검증한다고 설명한다: `MenuAuthInterceptor.java:12~15`행.

## 5. 화면 모델과 권한 정보

`GlobalModelAdvice`는 모든 화면 요청에 공통 모델을 채운다: `src/main/java/com/scbk/sms/controller/GlobalModelAdvice.java:18~40`행.

- `user`: `SmsUserPrincipal`
- `menus`: `menuSource.getMenuTree(principal.getRoleCodes())`
- `pageAuth`: `PageAuth.from(menuSource.getPermissions(...))`

local profile에서는 `pageAuth`가 항상 `PageAuth.all()`이다: `GlobalModelAdvice.java:42~52`행.

`PageAuth`는 화면 렌더링과 공통 JS가 사용하는 기능 권한 flag로, 실제 API 차단은 `MenuAuthInterceptor`가 별도로 수행한다고 주석에 명시되어 있다: `src/main/java/com/scbk/sms/service/menu/PageAuth.java:7~10`행.

## 6. 예외 처리와 HTTP 상태

`GlobalExceptionHandler`는 `CustomException`을 비즈니스 예외로 처리한다: `src/main/java/com/scbk/sms/exception/GlobalExceptionHandler.java:27~33`행.

`ErrorCode.ACCESS_DENIED`는 `HttpStatus.FORBIDDEN`이며 코드 `A002`이다: `src/main/java/com/scbk/sms/exception/ErrorCode.java:16~18`행.

HTML 요청이면 `error/error` ModelAndView를, 그 외 요청이면 `ApiResponse.error(status.value(), message)`를 `ResponseEntity`로 반환한다: `GlobalExceptionHandler.java:69~78`행.

## 7. 확인하지 못한 부분 / 불확실한 부분

- `SecurityConfig`는 CSRF를 disable하지 않지만, 공통 JS가 실제로 `_csrf` 헤더를 보내는 구현은 이 문서 작성 대상인 auth/security 핵심 파일 외의 `static`/JS 리소스 확인이 필요하다. 단, `SecurityConfig.java:24~27`행 주석에서 그 의도만 확인했다.
- `DefaultLdapAuthoritiesPopulator(contextSource, null)`의 LDAP group mapping은 `null`로 설정되어 있어 실제 LDAP group 권한 부여 방식은 코드상 확인할 수 없다: `LdapAuthenticationConfig.java:40~43`행.
- `TB_ROLE`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH`의 실제 DDL/테이블 구조는 `docs/analysis/05-data-model.md`에서 별도로 정리한다.
