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
| `archive/role-menu-permission-memo.md` | (보관) 역할 기반 권한 모델 해설 (base 문서 파생 설명 문서) |
| `login-page-implementation.md` | 첫 로그인 구현 조각과 검증 결과 |
| `initial-menu-screen-implementation.md` | DB 기반 초기 메뉴 화면 구현과 검증 결과 |
| `ui-assets-implementation.md` | v2 공통 UI 자산(레이아웃/그리드/공통 JS) 이식 기록 |
| `common-response-contract.md` | JSON 공통 응답 계약(ApiResponse/PageResponseDTO/예외 처리) |
| `menu-auth-interceptor-implementation.md` | URL suffix 기반 메뉴 권한 Interceptor 구현 기록 |
| `screen-generation-guide.md` | Query Scaffold 최초 생성 → 개발자 소유 전환 표준 절차 |
| `domain-boundary-guide.md` | 도메인 경계 판별 기준 (쓰기 소유 테이블 기반) |
| `domain-rules.md` | 도메인별 테이블/마스킹/상태값 규칙 (v2 이관) |
| `audit-masking-policy.md` | 감사 로그 대상과 마스킹 기준 (v2 이관) |
| `rest-api-standard.md` | 외부 연계 REST API 설계 기준 (v2 이관) |
| `v2-scaffold-reference.md` | v2 스캐폴드 생성기 분석. Query Scaffold 설계 참고 |
| `common-utils-implementation.md` | @PrivacyLog/MaskingUtil/ExcelUtil 구현 기록 |
| `common-code-api-implementation.md` | 화면 콤보/자동완성용 공통코드 API 구현 기록 |
| `query-scaffold-implementation.md` | Query Scaffold(local 전용 화면 생성 도구) 구현 기록 |
| `scaffold-contract.md` | Query Scaffold 생성 계약 (1회 생성, 개발자 소유, 재생성은 교체) |
| `grid-formatter-guide.md` | 그리드 셀 포매터 사용 가이드 (배지/날짜/행 스타일) |
| `remaining-work.md` | 현재 미수정/미비 사항과 의도적으로 미룬 작업 목록 |
| `deployment-checklist.md` | 폐쇄망 반입 체크리스트 (환경변수/DDL/소스 전환/검증) |
| `git-basics-cheatsheet.md` | Git 기본 명령어 참고 (개발자용) |
| `test-automation-guide.md` | 테스트 자동화 3층 구조 (scaffold 생성/컨벤션/Jenkins) |
| `../../DESIGN.md` | 디자인 토큰 계층 설계 (`--sms-*` 정의와 CoreUI 별칭) |
| `../메뉴.md` | 메뉴/역할/권한 관리 화면 설계 메모와 Oracle DDL 예시 |
| `../승인.md` | 메뉴 권한과 분리된 공통 승인 시스템 설계 메모와 Oracle DDL 예시 |
| `../js분석.md` | `/sms/customer-search` 화면 JavaScript 분석(초기 로드, 검색, 페이징, 행 클릭 모달, CRUD 흐름). CustomerSearch/공통 그리드·모달·HTTP 모듈 작업 시에만 읽는다. |

### 매뉴얼 (`docs/menual/`)

개발자용 사용 매뉴얼이다. 설계 근거(`docs/base/`)와 달리 "이것을 어떻게 쓰는지"를 다룬다.

| 문서 | 목적 |
|---|---|
| `menual/구조및구현_manual.md` | 프로젝트 전체 구조 (패키지/템플릿/정적자산/빌드/프로파일) |
| `menual/스케폴드_manual.md` | Query Scaffold 쿼리 작성법, 오류 패턴, 옵션 계약, 생성물 구조 |
| `menual/tui_manual.md` | TUI 공통 모듈 (`tui-common.js` + `tui-page-builder.js`) 구조·API·확장 |
| `menual/common-js_manual.md` | 공통 JS 6모듈 (http-client/notify/modal-manager/common-utils/form-binder/field-format) |
| `menual/css-design_manual.md` | 디자인 토큰(`--sms-*`), CSS 스택, 상세폼 패턴, 새 CSS 추가 가이드 |
| `menual/auth-page_manual.md` | 인증/권한 (PageAuth, SESSION_INFO, MenuAuthInterceptor, GlobalModelAdvice) |
| `menual/SMS_GIT_COMMIT_CONVENTION.md` | Git 커밋 메시지 형식, 변경 분리, 검증·원복 규칙 |
| `menual/sample-to-screen_manual.md` | Scaffold 미지원 UI를 위한 정적 샘플 참고 절차 (표준 생성 경로 아님) |
| `menual/tui-grid_manual.md` | TOAST UI Grid 4.21.22 프로젝트 사용법 |
| `menual/tui-date-picker_manual.md` | TOAST UI Date Picker 4.3.3 프로젝트 사용법 |
| `menual/tui-pagination_manual.md` | TOAST UI Pagination 3.4.1 (Grid 의존성 + 커스텀 페이징) |
| `menual/axios_manual.md` | axios 1.16.1 + HttpClient 래퍼 |
| `menual/dayjs_manual.md` | Day.js + ko 로케일 |
| `menual/imask_manual.md` | IMask 7.6.1 + data-mask 패턴 |
| `menual/just-validate_manual.md` | JustValidate 4.3.0 + data-validate 패턴 |
| `menual/lucide_manual.md` | 커스텀 lucide 아이콘 서브셋 (공식 교체 금지) |
| `menual/xlsx_manual.md` | SheetJS (TUI Grid 클라이언트 export) |
| `menual/toastui-editor_manual.md` | TOAST UI Editor 3.2.2 (notice-popup 전용) |
| `menual/coreui_manual.md` | CoreUI (vendor) + admin-ui-bridge 브리지 |

### 검증·문제 해결 기록 (`docs/menual/`)

다음 문서는 상시 사용 매뉴얼이 아니라 특정 문제의 분석·수정·브라우저 검증 기록이다. 해당 컴포넌트의 회귀를 조사할 때만 읽는다.

| 문서 | 목적 |
|---|---|
| `menual/클라이언트사이드 유효성 검사 테스트.md` | 필수값 오류 미표시 원인과 FieldFormat·브라우저 회귀 검증 기록 |
| `menual/중첩모달이슈.md` | CoreUI 업무 모달 위 확인 팝업 문제와 SweetAlert2 전환 검증 기록 |
| `menual/메뉴얼오류탐지.md` | 2026-07-28 기준 매뉴얼 계약과 코드 대조 보고서. 현재 규칙의 원본으로 사용하지 않음 |

### 업무 화면 생성 문서 권위

```text
scaffold-contract.md          생성물 소유권과 재생성 원칙
        ↓
screen-generation-guide.md    실제 작업 순서
        ↓
menual/스케폴드_manual.md      입력 SQL과 옵션 사용법
```

표준 화면은 `.tpl`로 한 번 생성한 뒤 개발자가 직접 수정한다. 정적 샘플과 개별 라이브러리 매뉴얼은 생성된 화면을 확장할 때만 참고하며 생성 계약을 바꾸지 않는다.

현재 메뉴에 등록된 `CustomerSearch`, `SmsHistory`, `Notice` 등의 업무 화면은 테스트·참고 구현이다. 매뉴얼에서 이 이름을 사용하는 예시는 공통 API와 계층 구조 설명용이며 운영 업무 요구사항이나 하위 호환 기준이 아니다.

`docs/plan/`은 작업 전 검토안과 실행 계획을 보관한다. 계획 문서는 현행 설계 계약이 아니며, 완료된 결정은 `docs/base` 계약 또는 해당 매뉴얼에 반영한다.

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
