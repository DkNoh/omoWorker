# Rules Index

이 문서는 v3 BASE PROJECT에서 Codex, Claude, **폐쇄망 소형 모델(Qwen3.6-35B-A3B + Hermes agent)** 이 작업마다 어떤 문서를 읽을지 정하는 단일 라우팅 기준이다.

## 읽기 원칙 (반드시 지킨다)

- **아래 표에 적힌 문서만 읽는다.** 작업과 무관해 보이는 문서를 추측으로 열지 않는다.
- **한 작업에서 읽는 docs/base 문서는 4개를 넘기지 않는다.** 넘으면 작업을 더 작게 쪼갠다.
- `.claude/rules/*.md`는 짧은 실행 규칙이다. 먼저 읽는다. `docs/base/*.md`는 설계 근거다. 필요한 행만 읽는다.
- 진입 문서 간 순서가 달라 보이면 이 문서를 기준으로 한다.

## 0. 세션 시작 시 1회

1. `AGENTS.md`
2. 이 문서 (`docs/base/rules-index.md`)
3. `docs/base/README.md` (전체 그림)

## 1. 모든 코드 작업 공통

- `.claude/rules/project.md` (계층/코딩/응답 규칙)
- `docs/base/emp-dep-identity-policy.md` (`EMP`,`DEP`,`EMP_ID+DEP_ID` 식별 — 거의 모든 쿼리/화면의 전제)

## 2. 작업별 읽을 문서 (이 표만, 그 외 금지)

| 작업 유형 | 읽을 문서 |
|---|---|
| **도메인 묶기 판단** (화면을 몇 개 도메인으로?) | `docs/base/domain-boundary-guide.md`, `docs/base/domain-rules.md`(해당 섹션) |
| **업무 화면 생성** (목록/CRUD) | `.claude/rules/scaffold-query.md`, `docs/base/scaffold-contract.md`, `docs/base/screen-generation-guide.md`, `docs/base/domain-boundary-guide.md`, `docs/base/domain-rules.md`(해당 섹션), `docs/menual/스케폴드_manual.md` |
| **화면/UI만** (Thymeleaf/JS/CSS) | `.claude/rules/thymeleaf.md`, `docs/base/screen-convention.md` |
| **JSON endpoint** (목록조회/저장/삭제 API) | `docs/base/common-response-contract.md` |
| **외부 연계 REST API** | `docs/base/rest-api-standard.md`, `docs/base/common-response-contract.md` |
| **DB/SQL 작업** (Mapper/XML) | `.claude/rules/mybatis-oracle.md` |
| **개인정보 포함 화면** | `docs/base/audit-masking-policy.md` (+ 위 화면 생성 세트) |
| **메뉴/권한** (seed·DDL·인터셉터) | `.claude/rules/menu-authority.md`, `docs/base/menu-authority-table-design.md`, `docs/base/menu-source-policy.md`, `docs/base/v2-menu-baseline.md`, `docs/base/dba-request-menu-auth-ddl.md` |
| **메뉴/권한 관리 화면 설계** | `.claude/rules/menu-authority.md`, `docs/base/menu-authority-table-design.md`, `docs/base/menu-source-policy.md`, `docs/메뉴.md` |
| **승인 시스템 설계** | `.claude/rules/menu-authority.md`, `docs/메뉴.md`, `docs/승인.md` |
| **Query Scaffold 작업** | `.claude/rules/scaffold-query.md`, `docs/base/scaffold-contract.md`, `docs/base/query-scaffold-implementation.md`, `docs/base/screen-convention.md`, `docs/menual/스케폴드_manual.md` |
| **인증/환경 작업** (profile/LDAP) | `.claude/rules/environment-auth.md`, `docs/base/environment-auth-policy.md` |
| **테스트 작성** | `.claude/rules/testing.md`, `docs/base/test-automation-guide.md` |
| **폐쇄망 반입/배포** | `docs/base/deployment-checklist.md` |
| **커밋/VCS** | `.claude/rules/vcs.md` |

## 3. 구현 기록 (해당 컴포넌트를 만질 때만 참고)

아래는 과거 구현+검증 기록이다. 매 작업에 읽지 않는다. 그 컴포넌트를 수정할 때만 참고한다.

`login-page-implementation.md`, `initial-menu-screen-implementation.md`, `ui-assets-implementation.md`,
`menu-auth-interceptor-implementation.md`, `common-utils-implementation.md`, `common-code-api-implementation.md`

화면 단위 분석(예: `../js분석.md`)은 CustomerSearch 화면이나 공통 그리드·모달·HTTP JavaScript를 다룰 때만 참고한다.

`v2-scaffold-reference.md`는 v2와 설계 차이를 비교할 때만 읽는 역사적 참고 문서다. 현재 생성 계약이나 작업 절차의 권위 문서로 사용하지 않는다. 그리드 formatter 작업은 `grid-formatter-guide.md`를 별도로 읽는다.

## 4. 개발자 매뉴얼 (`docs/menual/`)

사용법 중심 매뉴얼이다. 설계 근거(`docs/base/`)와 달리 "이것을 어떻게 쓰는지"를 다룬다. 해당 자산/컴포넌트를 처음 사용하거나 사용법이 헷갈릴 때 읽는다. 매 작업마다 전부 읽지 않는다.

| 작업 | 읽을 매뉴얼 |
|---|---|
| 프로젝트 구조 파악 | `menual/구조및구현_manual.md` |
| Query Scaffold 사용 | `menual/스케폴드_manual.md` |
| TUI 공통 모듈 (그리드/페이징 래퍼) | `menual/tui_manual.md` |
| 공통 JS (HTTP/모달/검증/포맷) | `menual/common-js_manual.md` |
| 클라이언트 검증 이슈 재현/회귀 확인 | `menual/just-validate_manual.md`, `menual/클라이언트사이드 유효성 검사 테스트.md` |
| 확인 팝업·중첩 모달 회귀 조사 | `menual/common-js_manual.md`, `menual/coreui_manual.md`, `menual/중첩모달이슈.md` |
| 매뉴얼과 코드의 과거 대조 결과 확인 | `menual/메뉴얼오류탐지.md` (기록용, 현재 계약은 `docs/base`와 해당 `_manual.md` 우선) |
| CSS/디자인 토큰 | `menual/css-design_manual.md` |
| 인증/권한 (PageAuth) | `menual/auth-page_manual.md` |
| Scaffold 미지원 UI를 정적 샘플에서 참고 | `menual/sample-to-screen_manual.md`, `docs/base/screen-convention.md` |
| 개별 라이브러리 (tui-grid/date-picker/pagination, axios, dayjs, imask, just-validate, lucide, xlsx, toastui-editor, coreui) | `menual/{name}_manual.md` |

## 문서 계층

| 위치 | 용도 |
|---|---|
| `AGENTS.md` | repo-level 진입 규칙 |
| `docs/base/*.md` | 공통 설계 원문 / 구현 기록 |
| `docs/menual/*.md` | 개발자 사용 매뉴얼 (how-to) |
| `docs/plan/*.md` | 작업 전 검토안·실행 계획. 현행 계약으로 사용하지 않음 |
| `.claude/rules/*.md` | 짧은 실행 규칙 (path-scoped) |

## 규칙 작성 원칙

- `docs/base`는 상세 설계와 결정 근거를 담는다.
- Scaffold 생성물 소유권은 `docs/base/scaffold-contract.md`를 단일 기준으로 삼는다. 정적 샘플이나 과거 구현 기록이 이 계약을 덮어쓰지 않는다.
- `.claude/rules`는 낮은 모델도 바로 따를 수 있는 짧은 실행 규칙만 담는다.
- 같은 내용이 충돌하면 `docs/base`의 설계 원문을 우선하고, `.claude/rules`를 즉시 갱신한다.
- 문서와 코드가 충돌하면 작업을 멈추고 충돌 내용을 보고한다. 임의로 한쪽을 선택하지 않는다.
- **새 계약·매뉴얼을 만들 때는 README 문서 목록과 이 라우팅 표(2장 또는 3장)에 반드시 등록한다.** `docs/plan`의 일회성 작업 계획은 개별 등록하지 않는다.

## 완료 기준

코드 변경이 있는 작업은 다음을 모두 통과해야 완료다.

```text
mvn test
mvn -DskipTests package
```

문서만 변경한 작업은 Maven 실행 대상이 아니다. 단, 문서가 코드 동작을 바꾸도록 지시하는 경우에는 코드 반영 전까지 부분 완료로 보고한다.
