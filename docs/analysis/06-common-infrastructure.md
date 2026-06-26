# 06-common-infrastructure.md

## 분석 범위

- 대상: 로그인, 권한, 메뉴, 에러 처리, 공통 DTO/API 응답, 감사 로그, 마스킹, 엑셀 다운로드 등 공통 인프라
- 범위: `src/main/java/com/scbk/sms/{auth,config,dto,exception,service,util}` 및 관련 Mapper/VO
- 제외: 화면 컨트롤러의 도메인 로직, 05에서 이미 다룬 DB DDL/시드 구조는 필요할 때만 연결선으로 언급

## 주요 공통 인프라 구성

### 1. 인증/권한 진입점

| 구성요소 | 경로:줄 | 역할 |
|---|---:|---|
| `SecurityConfig` | `src/main/java/com/scbk/sms/config/SecurityConfig.java:6` | Spring Security 설정: 기본 CSRF/세션/로그아웃/폼 로그인 제외, `/login`, `/api/**`, `/v3/**` 등 경로별 인증 정책 |
| `LdapAuthenticationConfig` | `src/main/java/com/scbk/sms/config/LdapAuthenticationConfig.java:6` | LDAP 인증 Provider 등록 |
| `LocalIdOnlyAuthenticationProvider` | `src/main/java/com/scbk/sms/auth/LocalIdOnlyAuthenticationProvider.java:10` | local 환경에서 ID만 입력해 `EMP_ID` 기준으로 사용자 조회 |
| `LdapEmployeeContextMapper` | `src/main/java/com/scbk/sms/auth/LdapEmployeeContextMapper.java:10` | LDAP 로그인 결과를 `EMP_ID`로 매핑 |
| `ActiveEmployeeResolver` | `src/main/java/com/scbk/sms/auth/ActiveEmployeeResolver.java:10` | `EMP.ACT_YN = 'Y'`가 아니면 인증 실패 처리 |
| `SmsUserPrincipal` | `src/main/java/com/scbk/sms/auth/SmsUserPrincipal.java:10` | Spring Security 인증 객체. `(EMP_ID, DEP_ID, EMP_NM, DEP_NM, ROLE_CODES)`를 화면/권한 로직에 전달 |

핵심 흐름은 `SecurityConfig`가 인증 경로를 분기하고, local은 `LocalIdOnlyAuthenticationProvider`, dev/prod는 `LdapAuthenticationConfig`를 통해 `EMP_ID`를 확보한 뒤, `LoginEmployeeMapper`/LDAP 결과를 `SmsUserPrincipal`로 만든다. 권한 판단은 인증 정보 생성 이후 `MenuAuthInterceptor`와 `GlobalModelAdvice`에서 `ROLE_CODES`를 사용한다.

### 2. 메뉴/권한 공통 서비스

| 구성요소 | 경로:줄 | 역할 |
|---|---:|---|
| `RoleProvider` | `src/main/java/com/scbk/sms/service/menu/RoleProvider.java:5` | `EMP_ID, DEP_ID` 기준으로 활성 ROLE_CODE 목록을 반환하는 추상화 |
| `StaticRoleProvider` | `src/main/java/com/scbk/sms/service/menu/StaticRoleProvider.java:9` | `sms.role.source=static`일 때 고정 역할 반환 |
| `DbRoleProvider` | `src/main/java/com/scbk/sms/service/menu/DbRoleProvider.java:9` | `sms.role.source=db`일 때 `TB_EMP_ROLE` 기반 역할 조회 |
| `EmployeeRoleService` | `src/main/java/com/scbk/sms/service/menu/EmployeeRoleService.java:16` | 역할 목록이 비어 있으면 인증 실패 처리 |
| `MenuSource` | `src/main/java/com/scbk/sms/service/menu/MenuSource.java:11` | 메뉴 트리/권한 조회의 단일 출처 |
| `DbMenuSource` | `src/main/java/com/scbk/sms/service/menu/DbMenuSource.java:15` | `sms.menu.source=db`일 때 `TB_MENU`/`TB_MENU_AUTH` 기반 메뉴와 권한 조회 |
| `StaticMenuSource` | `src/main/java/com/scbk/sms/service/menu/StaticMenuSource.java:18` | `sms.menu.source=static`일 때 로컬 화면 검증용 baseline 메뉴와 전권한 반환 |
| `MenuTreeBuilder` | `src/main/java/com/scbk/sms/service/menu/MenuTreeBuilder.java:13` | 플랫 메뉴 목록을 트리 구조로 변환 |
| `MenuAuthInterceptor` | `src/main/java/com/scbk/sms/config/MenuAuthInterceptor.java:12` | 화면 경로별 `READ` 권한을 강제 검증 |
| `GlobalModelAdvice` | `src/main/java/com/scbk/sms/controller/GlobalModelAdvice.java:29` | sidebar/header에 들어갈 메뉴 트리와 `pageAuth`를 공통 모델로 추가 |

권한 흐름은 `DbRoleProvider -> EmployeeRoleService -> SmsUserPrincipal -> GlobalModelAdvice/MenuAuthInterceptor`이다. `MenuAuthInterceptor`는 실제 화면 진입을 막는 보안 게이트이고, `GlobalModelAdvice`는 화면 렌더링 전 `pageAuth`를 채우는 UI 보조 계층이다. `MenuAuthInterceptor`와 `GlobalModelAdvice`는 같은 `MenuSource`를 공유하므로 메뉴 트리/권한의 출처가 하나다.

### 3. 에러 처리/응답 규격

| 구성요소 | 경로:줄 | 역할 |
|---|---:|---|
| `ErrorCode` | `src/main/java/com/scbk/sms/exception/ErrorCode.java:6` | 커스텀 예외 코드 |
| `CustomException` | `src/main/java/com/scbk/sms/exception/CustomException.java:10` | `ErrorCode`를 가진 비즈니스 예외 |
| `GlobalExceptionHandler` | `src/main/java/com/scbk/sms/exception/GlobalExceptionHandler.java:14` | `CustomException`은 HTTP 상태와 `ErrorCode`를 응답하고, 기타 예외는 서버 오류로 처리 |
| `ApiResponse` | `src/main/java/com/scbk/sms/dto/common/ApiResponse.java:12` | 모든 JSON API 응답의 공통 감싸기 |
| `PageRequestDTO` | `src/main/java/com/scbk/sms/dto/common/PageRequestDTO.java:9` | 목록 조회 공통 검색 조건과 페이징 값 |
| `PageResponseDTO` | `src/main/java/com/scbk/sms/dto/common/PageResponseDTO.java:9` | 목록 조회 공통 응답 |

`ApiResponse`는 성공/실패 여부와 관계없이 `timestamp`, `code`, `message`, `data`를 고정 구조로 제공한다. 목록 조회는 `PageRequestDTO`와 `PageResponseDTO`를 통해 `page`, `size`, `keyword`, `searchType`을 공통화한다.

### 4. 개인정보 보호/감사 로그

| 구성요소 | 경로:줄 | 역할 |
|---|---:|---|
| `@PrivacyLog` | `src/main/java/com/scbk/sms/annotation/PrivacyLog.java:10` | 개인정보 조회/반출/변경 행위를 표시하는 어노테이션 |
| `PrivacyLogAspect` | `src/main/java/com/scbk/sms/aop/PrivacyLogAspect.java:29` | 어노테이션 붙은 메서드 실행 시 감사 로그 생성 |
| `AuditLogService` | `src/main/java/com/scbk/sms/service/system/AuditLogService.java:14` | 감사 로그 저장 |
| `PrivacyAuditLogMapper` | `src/main/java/com/scbk/sms/mapper/system/PrivacyAuditLogMapper.java:7` | `TB_PRIVACY_AUDIT_LOG` 삽입 |
| `MaskingUtil` | `src/main/java/com/scbk/sms/util/MaskingUtil.java:13` | 이름, 전화번호, 주민번호, 카드번호를 마스킹 |
| `ExcelUtil` | `src/main/java/com/scbk/sms/util/ExcelUtil.java:29` | 대용량 엑셀 다운로드 처리 |

감사 로그는 `@PrivacyLog`가 붙은 서비스 메서드 실행 시 자동으로 기록된다. `PrivacyLogAspect`는 `SmsUserPrincipal`의 `(EMP_ID, DEP_ID)`를 기록하고, 파라미터 JSON을 `MaskingUtil`로 마스킹한 뒤 `AuditLogService`에 전달한다. `AuditLogService`는 저장 실패를 삼키지 않고 전파한다.

### 5. 공통 코드/부서/역할 조회

| 구성요소 | 경로:줄 | 역할 |
|---|---:|---|
| `CommonCodeApiController` | `src/main/java/com/scbk/sms/controller/system/CommonCodeApiController.java:21` | `/api/common-code/{codeType}` API |
| `CommonCodeService` | `src/main/java/com/scbk/sms/service/system/CommonCodeService.java:18` | `dept`, `role` 타입별 조회 분기 |
| `CommonCodeMapper` | `src/main/java/com/scbk/sms/mapper/system/CommonCodeMapper.java:7` | 부서/역할 목록 조회 Mapper |
| `CommonCodeVO` | `src/main/java/com/scbk/sms/vo/common/CommonCodeVO.java:10` | 콤보/자동완성용 공통 코드 VO |

`CommonCodeService`는 지원하지 않는 `codeType`을 빈 목록으로 숨기지 않고 `CustomException`으로 실패 처리한다. `CommonCodeApiController`는 화면 콤보/자동완성용 API로, 공통 제외 경로에 해당한다.

## Mapper 연결 요약

| Mapper | 경로:줄 | 주요 호출 서비스 |
|---|---:|---|
| `LoginEmployeeMapper` | `src/main/java/com/scbk/sms/mapper/auth/LoginEmployeeMapper.java:9` | `LocalIdOnlyAuthenticationProvider`, `LdapEmployeeContextMapper` |
| `EmployeeRoleMapper` | `src/main/java/com/scbk/sms/mapper/menu/EmployeeRoleMapper.java:9` | `DbRoleProvider` |
| `MenuMapper` | `src/main/java/com/scbk/sms/mapper/menu/MenuMapper.java:9` | `DbMenuSource` |
| `MenuAuthMapper` | `src/main/java/com/scbk/sms/mapper/menu/MenuAuthMapper.java:9` | `DbMenuSource` |
| `CommonCodeMapper` | `src/main/java/com/scbk/sms/mapper/system/CommonCodeMapper.java:7` | `CommonCodeService` |
| `PrivacyAuditLogMapper` | `src/main/java/com/scbk/sms/mapper/system/PrivacyAuditLogMapper.java:7` | `AuditLogService` |

## 주요 VO/DTO 관계

| VO/DTO | 역할 | 관련 테이블/서비스 |
|---|---|---|
| `LoginEmployeeVO` | 로그인 사용자 정보 | `EMP` |
| `SmsUserPrincipal` | 인증 컨텍스트 | `LoginEmployeeVO + ROLE_CODES` |
| `MenuItemVO` | 메뉴 트리와 권한 표시 | `TB_MENU`, `TB_MENU_AUTH` |
| `MenuAuthVO` | 메뉴별 권한 플래그 | `TB_MENU_AUTH` |
| `CommonCodeVO` | 콤보/자동완성 공통 코드 | `DEP`, `TB_ROLE` |
| `PrivacyAuditLogVO` | 개인정보 접근 감사 로그 | `TB_PRIVACY_AUDIT_LOG` |
| `PageRequestDTO/PageResponseDTO` | 공통 목록 조회 | 도메인별 Mapper 결과 |
| `SmsHistorySearchRequestDTO` | 발송이력 검색 조건 | `SMS_HISTORY` |
| `SmsHistoryUpdateRequestDTO` | 발송이력 수정 화이트리스트 | `SMS_HISTORY` |

## 결론

- 인증/권한은 `EMP_ID` 단독이 아니라 `EMP_ID + DEP_ID + ROLE_CODES`를 기준으로 흐른다.
- `sms.role.source`와 `sms.menu.source`는 각각 역할/메뉴 출처를 분리한다.
- `MenuSource`를 단일 출처로 유지해 메뉴 트리/권한이 DB 또는 static 중 한 쪽에서만 나오도록 설계했다.
- `MenuAuthInterceptor`는 화면 진입 보안을 담당하고, `GlobalModelAdvice`는 화면 렌더링용 권한 정보를 제공한다.
- 개인정보 보호는 `MaskingUtil`과 `@PrivacyLog` AOP로 공통화했고, 감사 로그 실패는 삼키지 않는다.
- JSON API와 목록 조회는 `ApiResponse`, `PageRequestDTO`, `PageResponseDTO`로 공통화했다.
