# 폐쇄망 반입 체크리스트 (Deployment Checklist)

v3 BASE를 폐쇄망에 반입할 때 순서대로 따라가는 작업서다. Maven 의존성은 반입 확인 완료(2026-06-12) 상태를 전제한다.

## 1. 환경별 수정 대상 파일

DB/LDAP 접속정보는 환경변수 또는 아래 yml 파일을 직접 수정한다.

| 환경 | 수정 파일 | 수정 항목 |
|---|---|---|
| local | `src/main/resources/application-local.yml` | `spring.datasource`의 url / username / password |
| dev | `src/main/resources/application-dev.yml` | `spring.datasource.url`(placeholder 교체), `sms.ldap`의 url / base-dn / manager-dn / manager-password, user-search-base/filter |
| prod | `src/main/resources/application-prod.yml` | `sms.ldap` 4종 (DB는 yml이 아니라 **JBoss EAP에 `java:/comp/env/jdbc/SMS` JNDI datasource 등록**) |

주의: yml에 비밀번호를 직접 기록하면 형상관리에 올리지 않도록 관리한다.
환경변수(`SMS_DB_PASSWORD`, `SMS_LDAP_MANAGER_PASSWORD`) 방식이 규칙상 우선이다.

## 2. EMP / DEP만 존재하는 초기 상태 — 동작 범위와 주의사항

BASE는 `EMP`, `DEP`만으로 기동·로그인·초기 메뉴 화면·scaffold까지 동작하도록 설계됐다 (local + static source).

로그인이 실제로 요구하는 컬럼 (이 컬럼이 실제 DB에 있어야 한다):

```text
SMS.EMP : EMP_ID, DEP_ID, EMP_NM, ACT_YN
SMS.DEP : DEP_ID, DEP_NM, ACT_YN
```

| # | 발생 가능 상황 | 영향 / 대응 |
|---|---|---|
| 1 | `/api/common-code/role` 호출 | `TB_ROLE` 생성 전이면 ORA-00942 오류. role 콤보를 쓰는 화면이 없는 동안은 영향 없음 (`dept`는 정상) |
| 2 | 동일 `EMP_ID`에 활성 부서 2건 이상 | **로그인 실패가 정책이다** (자동 부서 선택 금지). 반입 직후 아래 점검 쿼리로 확인 |
| 3 | 사용자 소속 부서가 `DEP.ACT_YN = 'N'` | 해당 사용자 로그인 불가 (활성 부서 조인 조건) |
| 4 | WAS 접속 계정이 `SMS` 스키마가 아님 | 쿼리가 `SMS.` 접두를 사용하므로 SELECT 권한 + synonym 필요 (DBA 협의) |
| 5 | static 메뉴 클릭 시 404 JSON | 정상 동작 (컨트롤러 미구현 안내 메시지). 화면 생성 전까지 그대로 둔다 |
| 6 | `@PrivacyLog` 사용 | `TB_PRIVACY_AUDIT_LOG`(03 DDL) 적용 전에는 사용 금지 — 저장 실패가 업무 실패로 전파된다 |

중복 활성 사용자 사전 점검 쿼리:

```sql
SELECT E.EMP_ID, COUNT(*) AS ACTIVE_CNT
FROM SMS.EMP E
INNER JOIN SMS.DEP D ON D.DEP_ID = E.DEP_ID AND D.ACT_YN = 'Y'
WHERE E.ACT_YN = 'Y'
GROUP BY E.EMP_ID
HAVING COUNT(*) > 1;
```

## 3. DDL 적용 순서 (DBA 전달)

전제 확인: `EMP`, `DEP`는 기존 운영 테이블 (생성 대상 아님).
`EMP`의 PK `(EMP_ID, DEP_ID)` 존재 확인 — `TB_EMP_ROLE`의 FK가 여기에 걸린다.

```text
1) db/oracle/01_menu_auth_schema.sql        TB_ROLE → TB_EMP_ROLE → TB_MENU → TB_MENU_AUTH
2) db/oracle/02_menu_auth_seed.sql          기본 역할 4종 + v2 baseline 메뉴 + ROLE_ADMIN 전권한
3) db/oracle/03_privacy_audit_log_schema.sql  TB_PRIVACY_AUDIT_LOG
4) 실사용자 역할 부여                         TB_EMP_ROLE에 실제 (EMP_ID, DEP_ID, ROLE_CD) INSERT
```

4번 예시 (실환경 EMP 기준으로 값 조정):

```sql
INSERT INTO SMS.TB_EMP_ROLE (EMP_ID, DEP_ID, ROLE_CD, USE_YN, REG_ID)
VALUES ('실제EMP_ID', '실제DEP_ID', 'ROLE_ADMIN', 'Y', 'SYSTEM');
COMMIT;
```

주의: seed에 한글 메뉴명이 있으므로 **UTF-8 클라이언트로 실행**한다 (NLS_LANG 등).
한글이 깨지면 사이드바 아이콘 분기(메뉴명 기준)까지 연쇄로 어긋난다.

## 4. local static → db 전환 작업서

DDL 적용 직후 메뉴/권한을 DB 기준으로 전환·검증하는 절차다.

전제: 3장의 DDL 1~4번 완료. 특히 4번(본인 계정 역할 INSERT)이 없으면 전환 후 로그인이 실패한다.

```text
1. src/main/resources/application-local.yml 수정

   sms:
     menu:
       source: db        # static -> db
     role:
       source: db        # static -> db
   # role.static-default 줄은 db 모드에서 무시되므로 남겨도 된다

2. 서버 재기동

3. 검증 (순서대로)
   a. 본인 EMP_ID로 로그인 성공
   b. 초기 화면 Sources 카드가 "db / db" 표시
   c. Roles 카드에 TB_EMP_ROLE에 넣은 역할(ROLE_ADMIN) 표시
   d. 사이드바 메뉴가 표시됨 (이제 TB_MENU + TB_MENU_AUTH 기준)
   e. TB_EMP_ROLE 미등록 사용자로 로그인 시도 -> 로그인 실패가 정상
      (역할 없는 사용자는 메뉴 없음이 아니라 로그인 거부다)

4. 실패 시
   - 원인을 수정한다 (역할 미등록, seed 미적용, 권한/synonym 등)
   - 화면 확인이 급하면 source를 static으로 수동 복귀할 수 있다.
     단, 자동 fallback은 없다는 것이 정책이며 static 복귀는 임시 확인용일 뿐이다.
```

## 5. 반입 직후 검증 명령

```text
[빌드 게이트]
mvn test                     -> 전체 PASS (58건 이상, 화면 생성 시 증가)
mvn -DskipTests package      -> WAR 생성 성공

[기동 후 확인 — static 단계]
1. /login 200, EMP 활성 사용자 로그인 성공
2. /(홈)에서 CoreUI 사이드바 + 메뉴 표시, Sources 카드 "static / static"
3. /api/common-code/dept -> 부서 JSON, /api/common-code/bank -> 400(C003)
4. 메뉴에 없는 임의 URL -> 403 (권한 Interceptor)
5. /system/scaffold 접근·생성 동작 (EMP/DEP 대상 쿼리로 확인 가능)

[기동 후 확인 — db 전환 후 (4장 작업서)]
6. Sources 카드 "db / db", 메뉴가 TB_MENU 기준
7. TB_EMP_ROLE 미등록 사용자 -> 로그인 실패
8. @PrivacyLog 부착 메서드 호출 시 TB_PRIVACY_AUDIT_LOG에 (EMP_ID, DEP_ID) 기록
```

## 6. Jenkins 잡 구성

사내 Jenkins에 push마다 빌드 게이트를 실행하는 잡을 만든다.
pipeline 예시와 테스트 자동화 구조는 `docs/base/test-automation-guide.md`를 따른다.
