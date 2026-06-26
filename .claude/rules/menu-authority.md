---
paths:
  - "**/menu/**/*.java"
  - "**/mapper/menu/**/*.xml"
  - "**/templates/**/*.html"
  - "**/db/oracle/*menu*.sql"
  - "**/db/oracle/*auth*.sql"
---

# Menu / Authority Rules

- 상세 기준은 `docs/base/menu-authority-table-design.md`와 `docs/base/menu-source-policy.md`를 따른다.
- v3 권한 판단은 `TB_ROLE`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH` 기준이다.
- legacy `EMP.PERM_*` 필드는 사용하지 않는다.
- 사용자 역할 조회는 `(EMP_ID, DEP_ID)` 기준이다.
- 메뉴 노출은 `TB_MENU_AUTH.USE_YN = 'Y'`이고 `CAN_READ = 'Y'`인 행 기준이다.
- 버튼/API 권한은 READ/CREATE/UPDATE/DELETE/APPROVE/CANCEL/DOWNLOAD/MASK_VIEW로 분리한다.
- 등록과 수정 endpoint는 `/create`, `/update`로 분리한다. 등록/수정 겸용 `/save`는 신규 코드에서 만들지 않는다.
- URL/API 접근 검증은 `MenuAuthInterceptor`가 담당한다. Controller에서 권한을 중복 검사하지 않는다.
- 새 화면 URL은 메뉴 등록과 권한 부여로 접근을 연다. Interceptor 제외 경로에 추가하지 않는다.
- Interceptor 동작에 영향을 주는 변경은 `MenuAuthServiceTest`로 반드시 검증한다.
- `static` 메뉴와 `db` 메뉴는 명시 설정으로만 선택한다.
- `db` 메뉴 실패 시 `static` 메뉴로 자동 대체하지 않는다.
- `static` 역할은 local 테이블 생성 전 임시 로그인 이후 화면 확인 용도다.
- dev/prod 기본값은 `sms.menu.source=db`, `sms.role.source=db`다.
- dev에서 `static` 메뉴 검증이 필요하면 사유를 문서에 기록하고 일시적으로만 사용한다. 이 경우에도 `sms.role.source`는 `db`를 유지한다.
- prod는 `db`만 사용한다.
- 위 source 조합은 `AuthSourceGuard`가 부팅 시 강제한다. prod에 static이 들어가거나 `menu=db`+`role=static` 조합이면 부팅이 실패한다.
- 메뉴 seed 변경 시 `docs/base/v2-menu-baseline.md`, `StaticMenuSource`, `db/oracle/02_menu_auth_seed.sql`을 함께 갱신한다.