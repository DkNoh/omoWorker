# V2 미이관 항목 추적

v2(`sms-project-v2`)의 규칙/문서 중 v3에 아직 이관하지 않은 항목을 추적한다.

항목을 이관하면 이 문서에서 해당 행을 제거하고, 신규 문서를 만들었으면 `docs/base/README.md` 문서 목록을 함께 갱신한다.

## 이관 대기 항목

없음 — v2 미이관 항목 전부 완료 (2026-06-12, git 저장소 초기화 + vcs 규칙 이관으로 종결).

## 이관 완료 (기록)

| 항목 | v3 위치 |
|---|---|
| 화면 규약 | `screen-convention.md` + 공통 UI 자산 이식 |
| 공통 응답 계약 | `common-response-contract.md` + `dto/common`, `exception` 코드 |
| 감사 로그 / 마스킹 정책 | `audit-masking-policy.md` |
| 공통 유틸 (@PrivacyLog/MaskingUtil/ExcelUtil) | `common-utils-implementation.md` + `annotation`, `aop`, `util` 코드 |
| Query Scaffold | `query-scaffold-implementation.md` + `controller/system`, `service/system/scaffold` 코드 (jsqlparser 4.9, 회사 repo 보유 확인) |
| git / 커밋 규칙 | `.claude/rules/vcs.md` (2026-06-12 git init + origin 연결과 함께 이관) |
| 테스트 작성 규칙 | `.claude/rules/testing.md` |
| REST API 표준 | `rest-api-standard.md` |
| 도메인 규칙 | `domain-rules.md` (legacy `TB_*` 업무 테이블 미사용 방침 반영) |

## 이관하지 않기로 한 항목

| 항목 | v2 원본 | 사유 |
|---|---|---|
| legacy `TB_*` 업무 테이블 참조 | features 문서 전반 | 2026-06-10 사용자 확정: `TB_SMS_HISTORY`, `TB_DEPT`, `TB_CAMPAIGN` 등은 v3에서 사용하지 않음. 실테이블 기준으로 `domain-rules.md`에 반영 |
| `TB_AUTHORITY` 기반 권한 모델 | `docs/authority-model.md` | v3는 `TB_ROLE`/`TB_EMP_ROLE`과 8종 `CAN_*` 권한 모델로 대체 확정 |
| `EMP.PERM_*` 기반 권한 판단 | v2 legacy 코드 | v3 확정 정책으로 배제. `docs/base/emp-dep-identity-policy.md` 참고 |
| `HANDOFF.md` 운영 방식 | `HANDOFF.md`, `docs/HANDOFF_GUIDE.md` | v3는 `docs/base/*-implementation.md`로 구현 조각 단위 기록 방식을 사용 |
| 주소록/대시보드 도메인 규칙 | `.claude/rules/features/contact.md`, `dashboard.md` | v3 메뉴 baseline에 해당 메뉴 없음 |
