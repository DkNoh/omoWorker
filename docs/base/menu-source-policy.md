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
- dev/prod: `sms.menu.source=db`이므로 `DbMenuSource`를 사용한다. `GlobalModelAdvice`가 매 HTTP 요청마다 `selectReadableMenus`(메뉴 트리)와 `getPermissions`(페이지 권한)을 호출한다. 현재 `DbMenuSource`에는 캐시가 없다.

메뉴와 권한은 세션 단위로 거의 변하지 않으므로, dev/prod 트래픽이 늘면 `DbMenuSource`에 캐시 후보다:

1. (권장) `@Cacheable`을 `roleCodes` 키로 적용 — 동일 사용자 반복 요청은 DB 호출을 생략한다. TTL/evict 정책만 정한다.
2. 메뉴 트리는 전역 캐시 — 메뉴 자체는 정적이므로 트리 전체를 1회 로드 후 메모리에서 `roleCodes`로 필터링한다.
3. `getPermissions` 결과를 세션(`HttpSession`)에 캐시 — 권한은 세션 수명 동안 불변이므로 요청당 쿼리를 세션당 1회로 줄인다.

운영 메뉴가 자주 바뀌지 않는 정적 구조라면 2번이 가장 효율적이다. 트래픽이 적은 현 단계에서는 그대로 두고, 부하가 생기면 적용한다.