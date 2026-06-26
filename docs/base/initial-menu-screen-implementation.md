# Initial Menu Screen Implementation

v3 BASE PROJECT의 로그인 이후 초기 메뉴 화면 구현 내용이다.

## 구현 범위

- 로그인 성공 후 `/`를 초기 메뉴 화면으로 사용한다.
- local 초기 환경은 메뉴/역할 테이블 없이도 화면 확인이 가능하도록 `static` source를 사용한다.
- dev/prod와 local DB 검증 환경은 `SMS.TB_ROLE`, `SMS.TB_EMP_ROLE`, `SMS.TB_MENU`, `SMS.TB_MENU_AUTH` 기반 `db` source를 사용한다.
- 사용자 확인은 항상 `SMS.EMP`, `SMS.DEP` 기준이다.
- legacy `EMP.PERM_*` 필드는 사용하지 않는다.
- DB source 실패 시 static으로 자동 대체하지 않는다.

## 주요 흐름

```text
local 로그인 성공
 -> SMS.EMP + SMS.DEP 활성 사용자 확인
 -> EmployeeRoleService가 sms.role.source에 따라 역할 조회
 -> principal에 roleCodes 저장
 -> / 접근 시 MenuSource가 sms.menu.source에 따라 메뉴 tree 생성
 -> 좌측 메뉴와 메뉴 개요 화면 렌더링
```

## Source별 동작

| Source | 역할 | 메뉴 | 목적 |
|---|---|---|---|
| `static` | `sms.role.static-default` 값을 roleCodes로 사용 | 코드에 정의된 v2 baseline 메뉴 사용 | 테이블 생성 전 local 화면 검증 |
| `db` | `TB_EMP_ROLE`, `TB_ROLE` 조회 | `TB_MENU`, `TB_MENU_AUTH` 조회 | local/dev/prod 정식 검증 |

## 생성/수정 파일

| 파일 | 내용 |
|---|---|
| `src/main/java/com/scbk/sms/service/menu/MenuSource.java` | 메뉴 트리/권한 source 공통 인터페이스 |
| `src/main/java/com/scbk/sms/service/menu/StaticMenuSource.java` | v2 baseline 정적 메뉴/권한 |
| `src/main/java/com/scbk/sms/service/menu/DbMenuSource.java` | DB 기반 메뉴/권한 조회 |
| `src/main/java/com/scbk/sms/service/menu/MenuTreeBuilder.java` | flat 메뉴를 tree로 변환하고 부모 누락/중복 검증 |
| `src/main/java/com/scbk/sms/service/menu/RoleProvider.java` | 역할 source 공통 인터페이스 |
| `src/main/java/com/scbk/sms/service/menu/StaticRoleProvider.java` | local 임시 역할 부여 |
| `src/main/java/com/scbk/sms/service/menu/DbRoleProvider.java` | DB 기반 역할 조회 |
| `src/main/java/com/scbk/sms/service/menu/EmployeeRoleService.java` | RoleProvider 위임 및 빈 역할 검증 |
| `src/main/java/com/scbk/sms/controller/HomeController.java` | 메뉴, profile, source 정보를 화면 모델에 전달 |
| `src/main/resources/templates/index.html` | 로그인 이후 초기 메뉴 화면 |
| `src/main/resources/templates/login.html` | 로그인 화면 한글 문구 복구 |
| `src/main/resources/static/css/app.css` | 초기 메뉴 화면 레이아웃 |
| `src/main/resources/application-local.yml` | local 기본 source를 static으로 설정 |
| `src/main/resources/application-dev.yml` | dev 기본 source를 db로 설정 |
| `src/main/resources/application-prod.yml` | prod 기본 source를 db로 설정 |
| `db/oracle/02_menu_auth_seed.sql` | v2 baseline 메뉴/ROLE_ADMIN 권한 seed |

## DB source 적용 전제

DB source를 사용하려면 DBA 또는 권한 보유자가 아래 파일을 적용해야 한다.

```text
db/oracle/01_menu_auth_schema.sql
db/oracle/02_menu_auth_seed.sql
```

초기 DB 검증 사용자 예시는 다음과 같다. 실제 환경에서는 존재하는 `EMP`, `DEP` 기준으로 조정한다.

```text
EMP_ID=admin
DEP_ID=D001
ROLE_CD=ROLE_ADMIN
```

## 아직 하지 않은 일

- 개별 메뉴 URL별 실제 업무 Controller 구현
- 버튼 권한 제어
- 메뉴 관리 화면
- 권한 관리 화면

메뉴 권한 Interceptor는 이후 구현되었다. `docs/base/menu-auth-interceptor-implementation.md`를 따른다.