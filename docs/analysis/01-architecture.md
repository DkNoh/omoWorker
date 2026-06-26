# 01-architecture.md — 전체 아키텍처

## 1. 프로젝트 기본 구조

- Spring Boot WAR 프로젝트: `pom.xml` 7~17행.
- Java 21, Spring Boot 3.3.0: `pom.xml` 19~20행, `pom.xml` 7~12행.
- 주요 시작 클래스: `com.scbk.sms.SmsV3Application`, `@SpringBootApplication`은 `src/main/java/com/scbk/sms/SmsV3Application.java:6~10`행.
- MyBatis mapper 위치: `classpath:/mapper/**/*.xml` 설정은 `src/main/resources/application.yml:9~13`행.
- Lombok과 JSQLParser 의존: `pom.xml` 62~69행.

## 2. 레이어 구조와 패키지 의존 방향

확인된 패키지별 역할은 다음과 같다.

| 레이어 | 패키지 | 확인 위치 |
|---|---|---|
| Application | `com.scbk.sms` | `SmsV3Application.java:6~10`행 |
| Config | `com.scbk.sms.config` | `SecurityConfig.java:13~55`행, `WebMvcConfig.java:11~42`행 |
| Controller | `com.scbk.sms.controller.*` | `SmsHistoryController.java:21~60`행, `ScaffoldController.java:23~65`행 |
| Service | `com.scbk.sms.service.*` | `SmsHistoryService.java:15~47`행, `ScaffoldService.java:18~65`행 |
| Mapper | `com.scbk.sms.mapper.*` | `SmsHistoryMapper.java:10~22`행 |
| DTO | `com.scbk.sms.dto.*` | `SmsHistorySearchRequestDTO.java:7~15`행, `PageResponseDTO.java:9~70`행 |
| VO | `com.scbk.sms.vo.*` | `SmsHistoryVO.java:6~19`행 |
| Auth | `com.scbk.sms.auth` | `SmsUserPrincipal.java:6~19`행 |
| Exception | `com.scbk.sms.exception` | `CustomException.java:10~22`행, `ErrorCode.java:8~46`행 |
| AOP | `com.scbk.sms.aop` | `PrivacyLogAspect.java:27~42`행 |
| Util | `com.scbk.sms.util` | `MaskingUtil.java:1~12`행 |

의존 방향은 코드상 다음과 같이 확인된다.

- Controller는 Service, DTO, VO를 import한다. 예: `SmsHistoryController.java:3~8`행.
- Service는 Mapper, DTO, VO, Exception을 import한다. 예: `SmsHistoryService.java:3~10`행.
- Mapper는 DTO와 VO를 import하지만 Service를 import하지 않는다. 예: `SmsHistoryMapper.java:3~5`행.
- Config는 Security/MyBatis/WebMvc 관련 Spring 타입과 일부 프로젝트 타입을 import한다. 예: `SecurityConfig.java:3~11`행, `WebMvcConfig.java:3~9`행.
- Auth 관련 Service는 Mapper/VO를 통해 권한 테이블을 읽는다. 예: `DbMenuSource.java:3~10`행, `MenuAuthService.java:3~13`행.

## 3. 요청 1건 흐름

### 3.1 일반 화면 요청 흐름

1. `SecurityFilterChain`이 `/login`, `/error`과 정적 리소스를 제외하고 나머지 요청을 인증 대상으로 만든다: `SecurityConfig.java:24~31`행.
2. `/`는 `HomeController`가 처리하고 `index` 템플릿을 반환한다: `HomeController.java:8~23`행.
3. `/system/scaffold`와 `/system/menu-tree`는 `@Profile("local")`이며 local profile에서만 등록된다: `ScaffoldController.java:23~25`행, `MenuTreeController.java:14~16`행.
4. `MenuAuthInterceptor`는 모든 URL/API 요청을 가로채 `MenuAuthService.checkAccess()`를 호출한다: `MenuAuthInterceptor.java:25~36`행.
5. `WebMvcConfig`는 `/`, `/login`, `/logout`, `/error`, 정적 리소스, `/api/common-code/**`를 제외하고, 구성된 `sms.menu.auth.exclude-paths`도 제외한다: `WebMvcConfig.java:14~41`행.

### 3.2 `/sms/history` 데이터 요청 흐름

`SmsHistoryController`가 `/sms/history`를 컨트롤한다: `SmsHistoryController.java:21~24`행.

```text
HTTP Request
  -> WebMvcConfig excludePathPatterns 검사
  -> MenuAuthInterceptor.preHandle()
       -> SecurityContextHolder.getAuthentication()
       -> SmsUserPrincipal.principal.roleCodes
       -> MenuAuthService.checkAccess(path, roleCodes)
  -> SmsHistoryController
       -> SmsHistoryService.search(request)
            -> request.validate()
            -> SmsHistoryMapper.count(request)
            -> SmsHistoryMapper.selectList(request)
            -> PageResponseDTO.of(list, request, totalCount)
  -> ResponseEntity.ok(ApiResponse.success(...))
```

관련 위치:

- Controller: `SmsHistoryController.java:33~38`행.
- Service: `SmsHistoryService.java:21~27`행.
- Mapper interface: `SmsHistoryMapper.java:13~15`행.
- XML SQL: `SmsHistoryMapper.xml:17~45`행, `SmsHistoryMapper.xml:47~78`행.
- 공통 응답: `ApiResponse.java:12~53`행, `PageResponseDTO.java:9~70`행.

### 3.3 create/update/delete 흐름

```text
POST /sms/history/create
  -> MenuAuthInterceptor
  -> SmsHistoryController.create(@Valid @RequestBody SmsHistoryUpdateRequestDTO)
  -> SmsHistoryService.create()
  -> SmsHistoryMapper.insert()
  -> SMS.SMS_HISTORY insert SQL
  -> ApiResponse.success("등록되었습니다.", null)

POST /sms/history/update
  -> MenuAuthInterceptor
  -> SmsHistoryController.update(@Valid @RequestBody SmsHistoryUpdateRequestDTO)
  -> SmsHistoryService.update()
  -> SmsHistoryMapper.update()
  -> UPDATE SQL WHERE SMS_HISTORY_ID AND REQUEST_ID
  -> update count가 0이면 CustomException(UPDATE_CONFLICT)
  -> ApiResponse.success("수정되었습니다.", null)

POST /sms/history/delete
  -> MenuAuthInterceptor
  -> SmsHistoryController.delete(@RequestParam smsHistoryId, @RequestParam requestId)
  -> SmsHistoryService.delete()
  -> SmsHistoryMapper.delete()
  -> DELETE SQL WHERE SMS_HISTORY_ID AND REQUEST_ID
  -> ApiResponse.success("삭제되었습니다.", null)
```

관련 위치:

- Controller: `SmsHistoryController.java:40~59`행.
- Service: `SmsHistoryService.java:29~46`행.
- Mapper: `SmsHistoryMapper.java:17~21`행.
- SQL: `SmsHistoryMapper.xml:80~117`행.

## 4. profile 분기

### 4.1 기본 profile

- 기본 profile은 `local`: `src/main/resources/application.yml:4~5`행.
- 기본 DB는 `localhost:1521/SMS`: `src/main/resources/application-local.yml:2~6`행.
- local auth는 ID-only: `src/main/resources/application-local.yml:8~18`행.
- local menu/role source는 static: `src/main/resources/application-local.yml:10~18`행.

### 4.2 dev/profile

- dev profile은 `@Profile({"dev", "prod"})`로 LDAP 인증 구성이 등록된다: `LdapAuthenticationConfig.java:17~19`행.
- dev DB URL은 `dev-db.example.internal`: `src/main/resources/application-dev.yml:2~6`행.
- dev auth는 LDAP, menu/role source는 DB: `src/main/resources/application-dev.yml:8~15`행.

### 4.3 prod/profile

- prod DB는 JNDI: `src/main/resources/application-prod.yml:2~4`행.
- prod auth는 LDAP, menu/role source는 DB: `src/main/resources/application-prod.yml:6~12`행.
- `AuthSourceGuard`는 prod에서 `sms.menu.source=db`와 `sms.role.source=db`만 허용한다: `AuthSourceGuard.java:27~49`행.
- `AuthSourceGuard`는 non-local profile에서 `sms.auth.mode=local`을 금지한다: `AuthSourceGuard.java:31~36`행.

### 4.4 local auth

- local profile에서는 `LocalIdOnlyAuthenticationProvider`가 `@Profile("local")`으로 등록된다: `LocalIdOnlyAuthenticationProvider.java:15~40`행.
- dev/prod에서는 `LdapAuthenticationConfig`가 LDAP `AuthenticationProvider`를 등록한다: `LdapAuthenticationConfig.java:23~43`행.

## 5. 전체 아키텍처 요약

- SSR/REST 하이브리드 구조다. 화면은 Thymeleaf 템플릿, 데이터는 JSON API로 받는다: `SmsHistoryController.java:28~38`행.
- Spring Security는 인증/인가의 1차 필터다: `SecurityConfig.java:24~47`행.
- `MenuAuthInterceptor`는 화면 메뉴 표시와 별도로 실제 URL/API 접근을 `TB_MENU_AUTH` 기반으로 재검증한다: `MenuAuthInterceptor.java:12~36`행.
- Service는 MyBatis Mapper를 호출하고 Mapper는 XML SQL과 직접 연결된다: `SmsHistoryService.java:19~46`행, `SmsHistoryMapper.java:10~22`행, `SmsHistoryMapper.xml:17~117`행.
- profile별 분기는 config class의 `@Profile`과 `AuthSourceGuard`가 핵심이다: `LdapAuthenticationConfig.java:17~43`행, `AuthSourceGuard.java:27~49`행.

## 6. 확인하지 못한 부분 / 불확실한 부분

- 프로젝트 전체의 모든 의존 방향을 import 그래프로 자동 계산한 것은 아니다. 위 방향은 확인한 핵심 클래스의 import와 호출 관계를 기준으로 한 것이다.
- `target/`에 존재하는 빌드 결과물과 `static/lib/`, `static/vendor/`는 진입하지 않았으므로 배포 WAR 내부의 실제 정적 리소스 배치는 별도 검증이 필요하다.
