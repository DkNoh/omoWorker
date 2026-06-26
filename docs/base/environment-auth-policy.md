# 환경 및 인증 정책

v3는 `local`, `dev`, `prod` 세 profile을 분리한다.

## 공통 원칙

- 환경별 차이는 인증 방식과 외부 접속정보에 한정한다.
- 사용자 존재 여부, 부서, 역할, 메뉴 권한 판단은 모든 환경에서 동일한 DB 로직을 사용한다.
- local 전용 사용자 테이블이나 local 전용 권한 테이블을 만들지 않는다.
- 실제 비밀번호, LDAP bind password, 운영 DB 접속정보는 문서와 코드에 기록하지 않는다.

## local

| 항목 | 정책 |
|---|---|
| 인증 방식 | ID-only |
| LDAP | 사용하지 않음 |
| 로그인 입력 | `EMP_ID`만 입력 |
| 비밀번호 검증 | 하지 않음 |
| 사용자 확인 | `EMP.ACT_YN = 'Y'`이고 소속 `DEP.ACT_YN = 'Y'`인 사용자 조회 |
| 권한 확인 | `TB_EMP_ROLE`, `TB_MENU_AUTH` 사용 |

local 로그인은 개발 편의용이다. 하지만 권한 로직까지 우회하지 않는다.

local 로그인 처리 규칙:

1. 사용자가 `EMP_ID`를 입력한다.
2. `EMP`에서 `EMP_ID = 입력값` and `EMP.ACT_YN = 'Y'` 조건으로 조회하고, `DEP`를 조인해 `DEP.ACT_YN = 'Y'`인 행만 남긴다.
3. 결과가 1건이면 로그인 성공으로 처리한다.
4. 결과가 0건이면 로그인 실패로 처리한다.
5. 결과가 2건 이상이면 로그인 실패로 처리하고, 동일 `EMP_ID`에 활성 부서가 여러 건이라고 보고한다.
6. 로그인 성공 후 `TB_EMP_ROLE`에서 `(EMP_ID, DEP_ID)` 기준 역할을 조회한다.
7. 메뉴와 버튼 권한은 `TB_MENU_AUTH` 기준으로만 판단한다.

3~5번의 건수 판단은 2번의 `EMP.ACT_YN = 'Y'` and `DEP.ACT_YN = 'Y'` 조인 결과를 기준으로 한다.

## dev

| 항목 | 정책 |
|---|---|
| 인증 방식 | LDAP |
| LDAP 설정 | placeholder 사용 |
| 사용자 확인 | LDAP 성공 후 `EMP` 조회 |
| 권한 확인 | `TB_EMP_ROLE`, `TB_MENU_AUTH` 사용 |

예시 설정값은 구조를 보여주기 위한 placeholder다.

```yaml
spring:
  profiles:
    active: dev

sms:
  auth:
    mode: ldap
  ldap:
    url: ldap://ldap-dev.example.internal:389
    base-dn: dc=example,dc=internal
    manager-dn: cn=sms-service,ou=system,dc=example,dc=internal
    manager-password: ${SMS_LDAP_MANAGER_PASSWORD}
    user-search-base: ou=users
    user-search-filter: "(uid={0})"
```

## prod

| 항목 | 정책 |
|---|---|
| 인증 방식 | LDAP |
| LDAP 설정 | 운영 placeholder 사용 |
| 사용자 확인 | LDAP 성공 후 `EMP` 조회 |
| 권한 확인 | `TB_EMP_ROLE`, `TB_MENU_AUTH` 사용 |
| 비밀값 | 환경변수, JNDI, 외부 secret 기준 |

```yaml
spring:
  profiles:
    active: prod

sms:
  auth:
    mode: ldap
  ldap:
    url: ldap://ldap-prod.example.internal:389
    base-dn: dc=example,dc=internal
    manager-dn: cn=sms-service,ou=system,dc=example,dc=internal
    manager-password: ${SMS_LDAP_MANAGER_PASSWORD}
    user-search-base: ou=users
    user-search-filter: "(uid={0})"
```

## 인증과 권한의 경계

인증은 사용자가 누구인지 확인하는 절차다.
권한은 확인된 사용자가 어떤 메뉴와 기능을 사용할 수 있는지 판단하는 절차다.

v3에서는 인증과 권한을 섞지 않는다.

- local은 비밀번호 검증만 생략한다.
- dev/prod는 LDAP으로 비밀번호를 검증한다.
- 모든 환경은 동일하게 `EMP`, `DEP`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH`를 사용한다.
