# Login Page Implementation Slice

이 문서는 v3 BASE PROJECT의 첫 구현 조각인 로그인 페이지와 local 인증 흐름을 설명한다.

## 구현 범위

- Spring Boot 3.3 / Java 21 / WAR 프로젝트 골격
- Thymeleaf 로그인 페이지
- local profile ID-only 인증
- Oracle `SMS.EMP`, `SMS.DEP` 기반 활성 사용자 확인
- dev/prod LDAP 인증 Provider 골격
- 로그인 성공 시 `SMS.TB_EMP_ROLE`, `SMS.TB_ROLE` 기준 역할 조회
- local/dev/prod profile 설정 분리

## 핵심 흐름

```text
GET /login
POST /login empId=admin
 -> local profile: 비밀번호 검증 생략
 -> SMS.EMP + SMS.DEP 조회
 -> 활성 사용자 1건이면 인증 성공
 -> / 로 이동
```

## 생성된 주요 파일

| 파일 | 역할 |
|---|---|
| `pom.xml` | Spring Boot WAR 프로젝트 설정 |
| `src/main/resources/application.yml` | 공통 설정 |
| `src/main/resources/application-local.yml` | local Oracle JDBC + ID-only 인증 |
| `src/main/resources/application-dev.yml` | dev JDBC + LDAP placeholder |
| `src/main/resources/application-prod.yml` | prod JNDI + LDAP placeholder |
| `src/main/java/com/scbk/sms/config/SecurityConfig.java` | Spring Security 공통 필터 체인 |
| `src/main/java/com/scbk/sms/auth/LocalIdOnlyAuthenticationProvider.java` | local ID-only 인증 Provider |
| `src/main/java/com/scbk/sms/auth/ActiveEmployeeResolver.java` | `EMP_ID` 기준 활성 사용자 단건 검증 |
| `src/main/java/com/scbk/sms/config/LdapAuthenticationConfig.java` | dev/prod LDAP Provider 골격 |
| `src/main/resources/mapper/auth/LoginEmployeeMapper.xml` | `SMS.EMP`, `SMS.DEP` 조회 SQL |
| `src/main/resources/templates/login.html` | 로그인 화면 |
| `src/main/resources/templates/index.html` | 로그인 후 확인 화면 |

## local DB 설정

DB 비밀번호는 파일에 저장하지 않는다.

필수 환경변수:

```text
SMS_DB_PASSWORD
```

선택 환경변수:

```text
SMS_DB_URL
SMS_DB_USERNAME
SERVER_PORT
SPRING_PROFILES_ACTIVE
```

기본 local 값:

```text
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8081
SMS_DB_URL=jdbc:oracle:thin:@localhost:1521/SMS
SMS_DB_USERNAME=SYSTEM
```

## local 로그인 정책

- 로그인 화면에서는 `EMP_ID`만 입력한다.
- local profile은 비밀번호를 검증하지 않는다.
- 단, `SMS.EMP`와 `SMS.DEP`에서 활성 사용자/부서 1건이 확인되어야 한다.
- 동일 `EMP_ID`로 활성 부서가 2건 이상이면 로그인 실패로 처리한다.
- `EMP.PERM_*`는 사용하지 않는다.

## 검증 결과

2026-06-10 기준 검증 결과:

```text
mvn test                  PASS
mvn -DskipTests package   PASS
```

local Oracle 확인:

```text
SMS.EMP 1건
SMS.DEP 5건
admin / D001 / 최고관리자 / ACT_YN=Y
```

HTTP 확인:

```text
GET  /login 200
POST /login empId=admin 200
GET  /      200
홈 화면에서 admin, 최고관리자, D001 확인
브라우저 접속 URL: http://localhost:8081/login
```

## 다음 단계

- `TB_ROLE`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH` 실제 생성 후 메뉴 권한 로딩 연결
- dev/prod 실제 LDAP 정보 반영
- CSRF 정책 재검토
- 로그인 실패 메시지 세분화
- 감사로그/마지막 로그인 일시 갱신 정책 추가