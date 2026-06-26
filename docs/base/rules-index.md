# Rules Index

이 문서는 v3 BASE PROJECT에서 Codex, Claude, **폐쇄망 소형 모델(Qwen3.6-35B-A3B + Hermes agent)** 이 작업마다 어떤 문서를 읽을지 정하는 단일 라우팅 기준이다.

## 읽기 원칙 (반드시 지킨다)

- **아래 표에 적힌 문서만 읽는다.** 작업과 무관해 보이는 문서를 추측으로 열지 않는다.
- **한 작업에서 읽는 docs/base 문서는 4개를 넘기지 않는다.** 넘으면 작업을 더 작게 쪼갠다.
- `.claude/rules/*.md`는 짧은 실행 규칙이다. 먼저 읽는다. `docs/base/*.md`는 설계 근거다. 필요한 행만 읽는다.
- 진입 문서 간 순서가 달라 보이면 이 문서를 기준으로 한다.

## 0. 세션 시작 시 1회

1. `CLAUDE.md` 또는 `AGENTS.md`
2. 이 문서 (`docs/base/rules-index.md`)
3. `docs/base/README.md` (전체 그림)

## 1. 모든 코드 작업 공통

- `.claude/rules/project.md` (계층/코딩/응답 규칙)
- `docs/base/emp-dep-identity-policy.md` (`EMP`,`DEP`,`EMP_ID+DEP_ID` 식별 — 거의 모든 쿼리/화면의 전제)

## 2. 작업별 읽을 문서 (이 표만, 그 외 금지)

| 작업 유형 | 읽을 문서 |
|---|---|
| **도메인 묶기 판단** (화면을 몇 개 도메인으로?) | `docs/base/domain-boundary-guide.md`, `docs/base/domain-rules.md`(해당 섹션) |
| **업무 화면 생성** (목록/CRUD) | `docs/base/screen-generation-guide.md`, `docs/base/screen-convention.md`, `docs/base/domain-boundary-guide.md`, `docs/base/domain-rules.md`(해당 섹션) |
| **화면/UI만** (Thymeleaf/JS/CSS) | `.claude/rules/thymeleaf.md`, `docs/base/screen-convention.md` |
| **JSON endpoint** (목록조회/저장/삭제 API) | `docs/base/common-response-contract.md` |
| **외부 연계 REST API** | `docs/base/rest-api-standard.md`, `docs/base/common-response-contract.md` |
| **DB/SQL 작업** (Mapper/XML) | `.claude/rules/mybatis-oracle.md` |
| **개인정보 포함 화면** | `docs/base/audit-masking-policy.md` (+ 위 화면 생성 세트) |
| **메뉴/권한** (seed·DDL·인터셉터) | `.claude/rules/menu-authority.md`, `docs/base/menu-authority-table-design.md`, `docs/base/menu-source-policy.md`, `docs/base/v2-menu-baseline.md`, `docs/base/dba-request-menu-auth-ddl.md` |
| **Query Scaffold 작업** | `.claude/rules/scaffold-query.md`, `docs/base/query-scaffold-implementation.md`, `docs/base/v2-scaffold-reference.md` |
| **인증/환경 작업** (profile/LDAP) | `.claude/rules/environment-auth.md`, `docs/base/environment-auth-policy.md` |
| **테스트 작성** | `.claude/rules/testing.md`, `docs/base/test-automation-guide.md` |
| **폐쇄망 반입/배포** | `docs/base/deployment-checklist.md` |
| **커밋/VCS** | `.claude/rules/vcs.md` |

## 3. 구현 기록 (해당 컴포넌트를 만질 때만 참고)

아래는 과거 구현+검증 기록이다. 매 작업에 읽지 않는다. 그 컴포넌트를 수정할 때만 참고한다.

`login-page-implementation.md`, `initial-menu-screen-implementation.md`, `ui-assets-implementation.md`,
`menu-auth-interceptor-implementation.md`, `common-utils-implementation.md`, `common-code-api-implementation.md`

## 문서 계층

| 위치 | 용도 |
|---|---|
| `AGENTS.md` | Codex repo-level 진입 규칙 |
| `CLAUDE.md` | Claude Code repo-level 진입 규칙 |
| `docs/base/*.md` | 공통 설계 원문 / 구현 기록 |
| `.claude/rules/*.md` | 짧은 실행 규칙 (path-scoped) |

## 규칙 작성 원칙

- `docs/base`는 상세 설계와 결정 근거를 담는다.
- `.claude/rules`는 낮은 모델도 바로 따를 수 있는 짧은 실행 규칙만 담는다.
- 같은 내용이 충돌하면 `docs/base`의 설계 원문을 우선하고, `.claude/rules`를 즉시 갱신한다.
- 문서와 코드가 충돌하면 작업을 멈추고 충돌 내용을 보고한다. 임의로 한쪽을 선택하지 않는다.
- **새 문서를 만들 때는 README 문서 목록과 이 라우팅 표(2장 또는 3장)에 반드시 등록한다.** 등록하지 않은 문서는 모델이 찾지 못하므로 없는 것과 같다.

## 완료 기준

코드 변경이 있는 작업은 다음을 모두 통과해야 완료다.

```text
mvn test
mvn -DskipTests package
```

문서만 변경한 작업은 Maven 실행 대상이 아니다. 단, 문서가 코드 동작을 바꾸도록 지시하는 경우에는 코드 반영 전까지 부분 완료로 보고한다.
