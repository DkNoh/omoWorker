# Menu Source Policy

v3 메뉴와 역할은 자동 대체 없이 명시 설정으로 source를 선택한다. 이 정책은 메뉴 테이블 생성 전 임시 화면과 local/dev/prod 공통 DB 메뉴 구조를 한 프로젝트에서 분리하기 위한 기준이다.

## 결론

잘못된 방식:

```text
DB 조회 실패 -> static 메뉴로 자동 전환
```

허용 방식:

```text
sms.menu.source 값에 따라 static 또는 db를 명시 선택
sms.role.source 값에 따라 static 또는 db를 명시 선택
```

실패는 숨기지 않는다. `db` source에서 테이블, 데이터, 권한이 없으면 실패해야 하며 원인을 수정한다.

## Source 종류

| 설정 | 값 | 용도 | 허용 환경 |
|---|---|---|---|
| `sms.menu.source` | `static` | 메뉴 테이블 생성 전 임시 메인 화면 표시 | local, dev(사유 기록 후 일시 검증) |
| `sms.menu.source` | `db` | `TB_MENU`, `TB_MENU_AUTH` 기반 정식 메뉴 | local, dev, prod |
| `sms.role.source` | `static` | 역할 테이블 생성 전 임시 `ROLE_ADMIN` 부여 | local 전용 |
| `sms.role.source` | `db` | `TB_EMP_ROLE`, `TB_ROLE` 기반 정식 역할 | local, dev, prod |

prod는 `db`만 사용한다. prod에서 `static`이 필요하다면 별도 승인과 사유 문서화가 필요하다.

dev에서 `sms.menu.source=static`을 사용할 때는 사유와 사용 기간을 문서로 남기고, `sms.role.source`는 `db`를 유지한다. `sms.role.source=static`은 local 전용이다.

## 공통 흐름

```text
GlobalModelAdvice -> MenuSource -> MenuItemVO tree
Local/Ldap 인증 -> EmployeeRoleService -> RoleProvider -> roleCodes
```

Controller와 Thymeleaf는 메뉴 source가 `static`인지 `db`인지 몰라야 한다. 화면은 항상 `MenuItemVO tree`만 받는다.

## static 규칙

- v2 baseline 메뉴 목록을 코드에 고정한다.
- 메뉴 테이블 생성 전 UI와 로그인 이후 흐름 확인 목적으로만 사용한다.
- 로그인 사용자 확인은 기존 `SMS.EMP`, `SMS.DEP` 기준을 그대로 사용한다.
- `EMP.PERM_*` 필드는 사용하지 않는다.
- `sms.role.source=static`은 local 임시 역할 부여일 뿐 최종 권한 검증이 아니다.
- 최종 권한 검증 완료 기준은 반드시 `db` source에서 확인한다.

## db 규칙

- 사용자 역할은 `(EMP_ID, DEP_ID)` 기준 `SMS.TB_EMP_ROLE`에서 조회한다.
- 역할은 `SMS.TB_ROLE.USE_YN = 'Y'`인 값만 사용한다.
- 메뉴는 `SMS.TB_MENU.USE_YN = 'Y'` 및 `DISPLAY_YN = 'Y'` 기준이다.
- 메뉴 노출은 `SMS.TB_MENU_AUTH.USE_YN = 'Y'`이고 `CAN_READ = 'Y'`인 행 기준이다.
- 버튼/API 권한은 READ/CREATE/UPDATE/DELETE/APPROVE/CANCEL/DOWNLOAD/MASK_VIEW로 분리한다.

## 기본 설정

local 초기:

```yaml
sms:
  menu:
    source: static
  role:
    source: static
    static-default: ROLE_ADMIN
```

local DB 검증 이후:

```yaml
sms:
  menu:
    source: db
  role:
    source: db
```

dev/prod:

```yaml
sms:
  menu:
    source: db
  role:
    source: db
```

## 성능 고려사항

GlobalModelAdvice의 `@ModelAttribute`는 화면(HTML) 요청마다 실행되므로 source별 DB 부하가 다르다.

- local: `sms.menu.source=static`이므로 `StaticMenuSource`를 사용하고, `PageAuth.all()`로 권한 쿼리를 skip한다. 메뉴/권한 조회로 DB를 치지 않으므로 성능 이슈가 없다.
- dev/prod: `sms.menu.source=db`이므로 `DbMenuSource`를 사용한다. `GlobalModelAdvice`는 역할별 `menuTree`와 요청 URL별 `pageAuth`를 HTTP 세션에 저장한다. 같은 메뉴 revision에서는 메뉴 트리는 세션당 1회, 화면 권한은 URL당 1회만 계산한다.

`MenuManageService`의 등록·수정·삭제가 커밋되면 애플리케이션의 메뉴 revision이 증가한다. 기존 로그인 세션은 다음 화면 요청에서 revision 불일치를 확인해 `menuTree`와 `pageAuthCache`를 함께 비우고 최신 DB 값을 다시 조회한다. 실패하거나 롤백된 변경은 revision을 올리지 않는다.

현재 revision은 단일 애플리케이션 인스턴스 메모리에 있다. 여러 서버 인스턴스로 운영한다면 다음 중 하나를 추가해야 한다.

- DB에 공유 메뉴 revision을 저장하고 각 인스턴스가 비교
- Redis 등 공유 캐시의 version/evict 이벤트 사용
- 메시지 브로커로 커밋 후 무효화 이벤트 전파

역할 목록 자체는 로그인 principal에 들어간다. `TB_EMP_ROLE` 변경은 재로그인 후 반영하며 메뉴 revision으로 principal의 역할 목록을 바꾸지 않는다.
