# 화면 생성 절차서 (Screen Generation Guide)

폐쇄망에서 메뉴 1개를 동작하는 화면 1개로 만드는 표준 절차다.

이 문서는 순서대로 실행하는 체크리스트다. 단계를 건너뛰지 않고, 각 단계의 완료 조건을 만족한 뒤 다음 단계로 간다. 판단이 필요한 항목이 미확정이면 임의 구현하지 않고 미확정으로 기록하고 보고한다.

## 참조 문서

| 단계 | 문서 |
|---|---|
| 도메인 지식 | `docs/base/domain-rules.md` 해당 섹션 |
| 화면 골격 | `docs/base/screen-convention.md` |
| 응답 규격 | `docs/base/common-response-contract.md` |
| SQL 규칙 | `.claude/rules/mybatis-oracle.md` |
| 권한 | `docs/base/menu-auth-interceptor-implementation.md` |
| 개인정보 | `docs/base/audit-masking-policy.md` |

## 0. 대상 확정

- 생성할 메뉴의 `MENU_ID`, `MENU_URL`, 메뉴명을 `docs/base/v2-menu-baseline.md` 또는 `TB_MENU`에서 확인한다.
- `domain-rules.md`에서 해당 도메인 섹션을 읽는다.
- 개인정보 포함 여부 / 엑셀 다운로드 여부 / 승인 기능 여부를 Y/N으로 기록한다.
- 개인정보 Y이면 조회/엑셀 메서드에 `@PrivacyLog`를 붙이고, 개인정보 컬럼은 `MaskingUtil`로 마스킹해 조회한다. (`audit-masking-policy.md`)
- 개인정보 Y이면 `SMS.TB_PRIVACY_AUDIT_LOG` 테이블이 적용되어 있는지 확인한다. 없으면 중단하고 보고한다.

완료 조건: URL, 도메인, 세 가지 Y/N이 기록됨.

## 1. 실제 테이블 확인

- 조회 대상 테이블과 컬럼을 실제 DB에서 확인한다. (`DESC 테이블명` 또는 DBA 제공 명세)
- legacy `TB_*` 업무 테이블은 사용하지 않는다. (`domain-rules.md` 테이블 사용 방침)
- 확인하지 못한 컬럼명은 사용하지 않는다.

완료 조건: 화면에 표시할 컬럼 목록과 검색 조건 컬럼이 실제 DB 기준으로 확정됨.

## 2. DTO / VO 생성

- `dto/<도메인>/<Domain>SearchRequestDTO.java` — `PageRequestDTO`를 상속. 검색 조건 필드만 추가.
- `vo/<도메인>/<Domain>VO.java` — 1단계에서 확정한 컬럼과 1:1. Oracle snake_case -> Java camelCase.
- Lombok을 사용한다: DTO/VO는 `@Data`(상속 DTO는 `@EqualsAndHashCode(callSuper = true)` 추가), Service/Controller는 `@RequiredArgsConstructor`. (2026-06-12 폐쇄망 Lombok 보유 확인으로 확정)

완료 조건: 두 파일이 컴파일됨.

## 3. Mapper 생성

- `mapper/<도메인>/<Domain>Mapper.java` — `count(request)`, `selectList(request)` 두 메서드부터 시작.
- `resources/mapper/<도메인>/<Domain>Mapper.xml`:
  - 검색 조건은 `<sql id="searchConditions"><where>...</where></sql>`로 공통화한다.
  - count 쿼리와 목록 쿼리는 같은 `searchConditions`를 사용한다.
  - 목록 쿼리는 결정적 `ORDER BY`(tie-breaker 포함) + `OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY`.
  - `SELECT *` 금지. LIKE는 `'%' || #{keyword} || '%'`.

완료 조건: XML의 컬럼이 전부 1단계 확정 컬럼이고, VO 필드와 alias가 일치함.

## 4. Service 생성

- `service/<도메인>/<Domain>Service.java`
- 조회는 `@Transactional(readOnly = true)`.
- 목록은 `PageResponseDTO.of(list, request, totalCount)`로 반환한다.
- 예측 가능한 업무 오류는 `CustomException(ErrorCode)`로 던진다. try-catch로 삼키지 않는다.

## 5. Controller 생성 (two-track)

- `controller/<도메인>/<Domain>Controller.java`, `@RequestMapping("<MENU_URL>")`
- 화면: `@GetMapping` -> Thymeleaf view 이름 반환.
- 목록: `@ResponseBody @GetMapping("/data")` -> `ResponseEntity<ApiResponse<PageResponseDTO<VO>>>`.
- 등록/수정이 필요하면 `/create`, `/update`로 분리한다. `/save`를 만들지 않는다.
- 수정/등록 요청은 `*UpdateRequestDTO`(화이트리스트)로만 받는다. VO를 `@RequestBody`로 받지 않는다.
- 상세폼 화면(조회 -> 폼 바인딩 -> 저장)은 `screen-convention.md`의 "상세폼 화면 규약"(FormBinder, 낙관적 잠금)을 따른다.
- 입력 검증은 `@Valid`를 사용한다. 권한 검사는 작성하지 않는다(Interceptor가 담당).

## 6. Template 생성

- `templates/<도메인>/<화면명>.html`
- `screen-convention.md`의 "목록 화면 표준 골격"을 복사해서 시작한다.
- 검색조건 input id는 SearchRequestDTO 필드명과 동일하게 둔다.
- 버튼/그리드 ID 규칙(`btn-search`, `grid`, `total-count` 등)을 그대로 사용한다.

## 7. 화면 JS 생성

- `static/js/<도메인>/<화면명>.js`
- `TuiPageBuilder`로만 그리드를 초기화한다.
- `columns.name`은 VO 필드명과 1:1, `searchInputs`는 SearchRequestDTO 필드명과 1:1.
- 알림/확인은 `CommonUtils.toast`, `CommonUtils.confirm`을 사용한다.

## 8. 메뉴/권한 확인

- `TB_MENU`에 해당 URL 메뉴가 있고 `USE_YN = 'Y'`인지 확인한다.
- 접근할 역할에 `TB_MENU_AUTH` 권한(`CAN_READ` 등)이 있는지 확인한다.
- Interceptor 제외 경로에 업무 URL을 추가하지 않는다. 권한은 메뉴 등록으로 연다.

## 9. 검증

- scaffold가 생성한 `{Domain}ServiceTest`, `{Domain}ControllerTest`를 `src/test/java`에 배치한다.
- 테스트의 `// TODO` 부분에 업무 규칙 테스트를 채운다. 업무 규칙이 있는데 채우지 않았으면 부분 완료다.
- `ConventionTest`가 규약 위반을 자동 검출한다. 위반이 나오면 화면 코드를 수정한다 (테스트를 수정하지 않는다).

```text
mvn test                  PASS
mvn -DskipTests package   PASS
```

서버 기동 후 수동 확인 (서버 실행은 사용자 담당):

- 화면 진입 200, 사이드바 메뉴 활성화 표시
- 조회 버튼 -> 그리드 데이터 + 총 건수 + 페이징 동작
- 검색 조건 적용/초기화 동작
- 권한 없는 역할로 접근 시 403
- 개인정보 컬럼 마스킹 표시 (해당 시)

미검증 항목이 있으면 완료가 아니라 부분 완료로 보고한다.

## 10. 문서 갱신

- `docs/base/v2-menu-baseline.md` 비고(필요 시), 신규 검색조건/그리드 컬럼 기록
- 같은 오류 3회 반복 시: 원문 오류, 시도한 3가지, 추정 원인을 보고하고 중단한다.
