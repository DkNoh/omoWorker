# 08-conventions-and-debt.md

## 분석 범위

- 대상: `.claude/rules/*.md`, `docs/base/*.md`, `src/test/java/**/*.java`, 핵심 공통 서비스/컨트롤러/에이전트
- 목적: 현재 프로젝트가 강제하는 코드 규칙, 검증 가능한 테스트, 아직 보완해야 하는 기술부채/미완성 영역을 정리
- 접근 방식: 완료된 01~04 문서는 다시 분석하지 않고, 현재 항목에 필요한 현재 소스/정책/테스트만 확인

## 강제 규칙과 자동 검증

### 프로젝트 규칙

- `docs/base/rules-index.md`와 `.claude/rules/project.md`가 v3 BASE PROJECT의 기본 운영 기준이다.
- `EMP`는 사용자, `DEP`는 부서의 운영 기준 테이블이다. 권한 판단은 직원에게 직접 주지 않고 역할 기반으로 한다.
- `EMP.PERM_*`는 v3 권한 판단에 사용하지 않는다.
- local만 LDAP 없이 ID 입력으로 로그인하고, dev/prod는 LDAP 인증을 사용한다.
- 로깅/권한/경로/한도 등 운영 중 바뀔 값은 `application*.yml`로 외부화한다.
- 신규 도메인 DTO/VO는 Lombok `@Data`를 사용한다. BASE 공통 DTO/VO는 plain Java를 유지한다.

### 메뉴/권한 규칙

- `sms.menu.source`와 `sms.role.source`는 `static|db`로 명시 선택한다.
- DB source 실패를 static 메뉴로 자동 대체하지 않는다.
- 메뉴 노출은 `TB_MENU_AUTH.USE_YN = 'Y'`이고 `CAN_READ = 'Y'`인 기준이다.
- 버튼/API 권한은 READ/CREATE/UPDATE/DELETE/APPROVE/CANCEL/DOWNLOAD/MASK_VIEW로 분리한다.
- `/save` 같은 등록/수정 겸용 endpoint는 신규 v3 코드에서 만들지 않는다. legacy가 필요하면 CREATE와 UPDATE를 모두 요구한다.
- 서버는 좌측 메뉴 표시와 별도로 URL/API 요청 시 권한을 다시 검증한다.

### MyBatis/Oracle 규칙

- `SELECT *`는 사용하지 않는다. 파생 테이블 별칭(`SELECT A.*`)은 허용한다.
- `OFFSET`/`FETCH` 기반 페이지 조회에는 `ORDER BY`가 반드시 필요하다.
- Mapper XML의 SQL은 명확한 테이블 별칭과 대상 컬럼을 명시하는 방향이 권장된다.

### 공통 응답 규칙

- 모든 JSON endpoint는 `ApiResponse<T>`를 사용한다.
- `PageResponseDTO.of(list, request, totalCount)`를 사용하며, 결과가 0건이어도 totalPages는 최소 1이다.
- `PageRequestDTO.validate()`가 page와 size를 보정한다.
- `GlobalExceptionHandler`는 HTTP 요청의 Accept 헤더에 따라 화면 에러 페이지 또는 JSON 응답으로 분기한다.
- Controller는 `try-catch`로 예외를 삼키지 않고 `CustomException`을 전파한다.

## 테스트로 검증되는 규칙

| 테스트 | 검증 내용 |
|---|---|
| `ConventionTest` | SearchRequestDTO는 PageRequestDTO 상속, Controller는 `/save` 금지, Controller는 VO 직접 요청 본문 금지, Mapper XML은 SELECT * 금지, 페이지 조회는 ORDER BY 필요, 신규 도메인 DTO/VO는 Lombok @Data 필요 |
| `AuthSourceGuardTest` | `sms.auth.mode=local`은 local profile에서만 허용 |
| `MenuAuthServiceTest` | URL 정확 일치 우선, data suffix는 부모 READ 허용, excel suffix는 DOWNLOAD 필요, legacy `/save`는 CREATE+UPDATE 필요, READ 없는 화면 URL 거부, 연결되지 않은 URL 거부 |
| `GlobalModelAdviceTest` | local에서는 화면용 권한을 모두 허용, nonLocal에서는 현재 URL 권한을 pageAuth로 전달 |
| `GlobalExceptionHandlerTest` | 화면 요청은 error/error, JSON 요청은 ApiResponse, Accept 없는 요청은 JSON |
| `PrivacyLogAspectTest` | 감사 로그는 EMP_ID와 DEP_ID를 함께 기록, 파라미터를 마스킹, 감사 로그 저장 실패는 본래 로직 실행 전 차단 |
| `SmsHistoryServiceTest` | 목록 조회는 PageResponseDTO로 감싸고, 수정 0건은 UPDATE_CONFLICT, 삭제는 Mapper 위임 |
| `SmsHistoryControllerTest` | `/sms/history/data`는 ApiResponse 형식의 목록 응답 |
| `ScaffoldServiceTest` | PK 없는 테이블 CRUD 생성 차단, nullable lock column은 null-safe WHERE 가능, PK는 lock column로 선택 불가 |

## 핵심 설계 흐름

```text
GlobalModelAdvice -> MenuSource -> MenuItemVO tree
Local/Ldap 인증 -> EmployeeRoleService -> RoleProvider -> roleCodes
Controller -> Service -> Mapper -> VO -> PageResponseDTO
PrivacyLogAspect -> AuditLogService -> SMS.TB_PRIVACY_AUDIT_LOG
```

## 발견된 기술부채/주의사항

1. `SmsHistoryService`는 현재 CRUD를 Mapper 위임 중심으로만 구성하고 있어 업무 상태 전이/검증 테스트가 부족하다. 현재 테스트는 목록 응답, 낙관적 잠금, Mapper 위임 정도다.
2. `PrivacyLogAspect`는 감사 로그 실패 시 본래 로직을 실행하지 않는 강력한 정책이므로, 운영 환경에서 감사 로그 저장 장애가 실제 장애로 이어질 가능성을 검토해야 한다.
3. local에서만 화면용 권한을 모두 허용한다. 이 로직은 개발 편의성 측면이 크므로 local profile 한정임을 문서/설정에서 명확히 유지해야 한다.
4. `AuthSourceGuard`는 local ID-only 인증을 보호하지만, dev/prod에서 잘못된 auth mode가 들어오면 명시적으로 실패하므로 설정 검증이 중요하다.
5. `GlobalModelAdvice`는 화면 모델에 권한을 내려주지만, 실제 API 요청은 별도 `MenuAuthService`에서 검증하므로 화면/서버 권한이 분리되어 있다. 이 이중 검증이 유지되어야 한다.
6. Mapper XML 카탈로그는 아직 문서화 중이며, 실제 DB 적용 전 DDL/시드 데이터와 Mapper 조회 결과를 대조하는 작업이 남아 있다.
7. `docs/analysis/05-data-model.md`에 문서화한 EMP/DEP 시드와 v3 권한 테이블은 운영 DB 적용 여부와 DBA 전달용 DDL 완료 여부를 별도로 확인해야 한다.
8. 현재 분석은 코드와 문서 기반이며, 실제 DBA 적용된 Oracle 스키마, local DB, dev/prod 설정을 대조한 검증을 포함하지 않는다.

## 미확인/후속 확인

- `application-local.yml`, `application-dev.yml`, `application-prod.yml`의 `sms.menu.source`, `sms.role.source`, LDAP placeholder 설정은 실제 환경별 차이 검토가 필요하다.
- `db/oracle` DDL과 `docker/local-emp-dep-seed.sql`의 EMP/DEP 시드가 실제 운영 구조와 일치하는지 DBA 적용 전 확인이 필요하다.
- Mapper XML의 SELECT 문이 실제 테이블 컬럼과 매핑되는지, 특히 `SMS_HISTORY` 상세 조회/수정 조건은 운영 DB 테스트에서 확인해야 한다.
- `docs/analysis/05-data-model.md`와 `07-mybatis-sql-inventory.md`는 문서 작성 후 인덱스 완료 표시만 남았으며, 실제 DB 적용 검증은 별도 작업이다.
