# V3 BASE PROJECT 문서 인덱스

v3는 화면을 먼저 많이 만드는 프로젝트가 아니다. 먼저 폐쇄망에서도 안정적으로 반복 생성할 수 있는 BASE PROJECT를 만든다.

## 목표

- local/dev/prod profile 분리
- local ID-only 인증과 dev/prod LDAP 인증 분리
- `EMP`, `DEP` 기반 사용자/부서 모델 확정
- `EMP.PERM_*`를 버리고 v3 신규 역할/메뉴 권한 모델 설계
- v2 운영 메뉴와 동일한 메뉴 seed 설계
- DBA에게 전달 가능한 메뉴/권한 DDL 문서 확보
- 이후 Query Scaffold와 loop engineering을 붙일 수 있는 기반 마련

## 문서 목록

| 문서 | 목적 |
|---|---|
| `rules-index.md` | Codex/Claude/폐쇄망 AI 규칙 로딩 순서 |
| `environment-auth-policy.md` | local/dev/prod 환경과 인증 정책 |
| `emp-dep-identity-policy.md` | 실제 `EMP`, `DEP` 구조와 v3 사용자 식별 기준 |
| `menu-authority-table-design.md` | v3 메뉴/역할/권한 테이블 설계 |
| `menu-source-policy.md` | static/db 메뉴 source 분리 정책 |
| `v2-menu-baseline.md` | v2와 동일하게 유지할 운영 메뉴 baseline |
| `dba-request-menu-auth-ddl.md` | DBA 전달용 신규 테이블 DDL |
| `screen-convention.md` | v2 기준 화면 규약. 레이아웃/그리드/명명 규칙 |
| `archive/v2-migration-backlog.md` | (보관) v2에서 아직 이관하지 않은 규칙/문서 추적 |
| `login-page-implementation.md` | 첫 로그인 구현 조각과 검증 결과 |
| `initial-menu-screen-implementation.md` | DB 기반 초기 메뉴 화면 구현과 검증 결과 |
| `ui-assets-implementation.md` | v2 공통 UI 자산(레이아웃/그리드/공통 JS) 이식 기록 |
| `common-response-contract.md` | JSON 공통 응답 계약(ApiResponse/PageResponseDTO/예외 처리) |
| `menu-auth-interceptor-implementation.md` | URL suffix 기반 메뉴 권한 Interceptor 구현 기록 |
| `screen-generation-guide.md` | 폐쇄망 화면 생성 표준 절차 체크리스트 |
| `domain-rules.md` | 도메인별 테이블/마스킹/상태값 규칙 (v2 이관) |
| `audit-masking-policy.md` | 감사 로그 대상과 마스킹 기준 (v2 이관) |
| `rest-api-standard.md` | 외부 연계 REST API 설계 기준 (v2 이관) |
| `v2-scaffold-reference.md` | v2 스캐폴드 생성기 분석. Query Scaffold 설계 참고 |
| `common-utils-implementation.md` | @PrivacyLog/MaskingUtil/ExcelUtil 구현 기록 |
| `common-code-api-implementation.md` | 화면 콤보/자동완성용 공통코드 API 구현 기록 |
| `query-scaffold-implementation.md` | Query Scaffold(local 전용 화면 생성 도구) 구현 기록 |
| `remaining-work.md` | 현재 미수정/미비 사항과 의도적으로 미룬 작업 목록 |
| `deployment-checklist.md` | 폐쇄망 반입 체크리스트 (환경변수/DDL/소스 전환/검증) |
| `test-automation-guide.md` | 테스트 자동화 3층 구조 (scaffold 생성/컨벤션/Jenkins) |

## 설계 방향

```text
인증 방식은 profile별로 다르게 둔다.
권한 판단은 profile과 무관하게 하나의 v3 권한 모델로 통일한다.
```

```text
local  -> ID-only 인증 -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
dev    -> LDAP 인증    -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
prod   -> LDAP 인증    -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
```

## 확정 사항

- `EMP`, `DEP`는 신규로 재설계하지 않는다. 운영에서 전달받은 실제 테이블을 기준으로 사용한다.
- `EMP.PERM_*` 컬럼은 v3 권한 판단에 사용하지 않는다.
- v3 권한은 `TB_ROLE`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH`로 분리한다.
- `TB_EMP_ROLE`은 `EMP_ID + DEP_ID` 복합키를 기준으로 `EMP`와 연결한다.
- 메뉴는 v2 운영 메뉴 목록을 기준으로 seed한다.

## 아직 하지 않는 일

- 실제 업무 화면 생성 (폐쇄망에서 Query Scaffold + `screen-generation-guide.md`로 생성한다)
- loop engineering 프롬프트 생성
- 운영 LDAP 실제 접속 정보 반영
- 운영 DB 직접 작업
