# Query Scaffold Implementation

QuerySpec(조회 SQL + `$변수`)에서 화면 1세트의 복사용 코드와 적용 대상 파일 목록을 생성하는 local 전용 도구 구현 기록이다.

설계 근거는 `docs/base/v2-scaffold-reference.md`(v2 생성기 분석)를 따른다.

## 접근

- URL: `/system/scaffold`
- API: `POST /system/scaffold/analyze`, `POST /system/scaffold/generate`, `POST /system/scaffold/preview`, `POST /system/scaffold/apply`
- `@Profile("local")` — dev/prod에서는 빈이 등록되지 않는다.
- 메뉴에 등록하지 않는 개발 도구다. `application-local.yml`의
  `sms.menu.auth.exclude-paths: /system/scaffold,/system/scaffold/**`로 local에서만 접근을 연다.

## 입력 (QuerySpec)

| 입력 | 예 |
|---|---|
| moduleName / domainId / domainClass / domainName | `sms` / `history` / `SmsHistory` / `발송이력조회` |
| domainId (3단계 URL) | v2 baseline 중 `/campaign/sms/register`처럼 3단계 URL인 화면은 `domainId`에 내부 슬래시 1개를 허용해 `sms/register`로 입력한다. `screenUrl`은 `/{moduleName}/{domainId}`이므로 그대로 `/campaign/sms/register`가 된다 |
| rawQuery | `$변수` 검색조건 규약 포함 SQL |
| orderBy | `A.SEND_DT DESC, A.HIST_ID DESC` — 결정적 정렬 입력 필수 (v2에 없던 신규 입력) |
| screenMode | `LIST`, `EXCEL`, `DETAIL`, `CRUD` |
| targetTable | CRUD 기준 수정 대상 테이블. 미입력 시 `FROM`의 첫 테이블을 서버에서 추론 |
| includeModal | `LIST`/`EXCEL`에서도 상세 자동 모달을 사용할지 여부. `DETAIL`/`CRUD`는 자동 활성화 |
| includePrivacy | 개인정보 포함 시 `@PrivacyLog` 생성 |
| searchParamOptions | 검색 파라미터별 입력 타입/기본값/콤보·라디오 옵션 |
| columnOptions | 컬럼별 그리드 표시여부/모달 표시여부/수정 가능여부/헤더명/너비/정렬/날짜 포맷/마스킹 타입 |
| pkColumns / lockColumn | update/delete WHERE 기준 PK 목록, 낙관적 잠금 컬럼. `pkColumn` 단일 입력은 하위 호환용으로만 유지 |
| menuOption | `menuId`, `parentMenuId`, `roleCode`, `sortOrd` |

DB 문법은 자동 fallback하지 않고 `application.yml`의 `sms.scaffold.db-platform` 값으로 명시한다. 허용값은 `oracle`, `postgres`, `db2`다.

`$start_dt` 하나가 SearchRequestDTO 필드(`startDt`) + 화면 검색 input + XML `<if>` 동적조건 + `#{startDt}` 바인딩으로 동시 생성된다.

## 동작

```text
1. QueryColumnExtractor : JSQLParser로 SELECT 컬럼/alias, 검색변수, CRUD 기준 테이블 추출 (실패 시 fallback)
2. ColumnTypeInferrer   : `sms.scaffold.db-platform` Dialect의 빈 결과 쿼리 실행 후 메타데이터로 타입 추론
                          실패 시 String 강행하지 않고 오류 보고 (v2와 다른 v3 확정 동작)
3. ScaffoldService      : targetTable 메타데이터에서 PK/nullable 정보 조회, CRUD 검증 후 산출물 생성
4. 미리보기             : `/system/scaffold/preview` 호출 -> 신규/변경없음/덮어쓰기 표시
5. 적용 버튼            : local 전용 `/system/scaffold/apply` 호출 -> 생성물을 정해진 프로젝트 경로로 저장
```

산출물: SearchRequestDTO, VO, Mapper interface, Mapper XML, Service, Controller, ServiceTest, ControllerTest, HTML, JS, 메뉴등록 SQL — 기본 11종. `screenMode=CRUD` 선택 시 UpdateRequestDTO(화이트리스트)가 추가되어 12종. (테스트 생성 전략은 `test-automation-guide.md`, 수정 요청 규약은 `screen-convention.md` "상세폼 화면 규약")

## 적용 경로

`미리보기`와 `적용`은 같은 요청을 서버에서 다시 생성한 뒤 아래 경로를 계산한다. UI에는 각 산출물별로 `신규 파일`, `기존 파일 덮어쓰기`, `변경 없음`을 표시한다. `적용` 실행 시 UTF-8로 저장하며, 같은 경로의 기존 파일은 산출물 내용으로 덮어쓴다.

| 산출물 | 적용 경로 |
|---|---|
| `*SearchRequestDTO.java`, `*UpdateRequestDTO.java` | `src/main/java/com/scbk/sms/dto/{moduleName}/` |
| `*VO.java` | `src/main/java/com/scbk/sms/vo/{moduleName}/` |
| `*Mapper.java` | `src/main/java/com/scbk/sms/mapper/{moduleName}/` |
| `*Mapper.xml` | `src/main/resources/mapper/{moduleName}/` |
| `*Service.java` | `src/main/java/com/scbk/sms/service/{moduleName}/` |
| `*Controller.java` | `src/main/java/com/scbk/sms/controller/{moduleName}/` |
| `*ServiceTest.java` | `src/test/java/com/scbk/sms/service/{moduleName}/` |
| `*ControllerTest.java` | `src/test/java/com/scbk/sms/controller/{moduleName}/` |
| `{domainId}.html` | `src/main/resources/templates/{moduleName}/` |
| `{domainId}.js` | `src/main/resources/static/js/{moduleName}/` |
| `메뉴등록.sql` | `db/oracle/{moduleName}_{domainId}_menu_seed.sql` |

파일 적용은 `local` profile에서만 가능하다. `moduleName`, `domainId`, `domainClass`는 저장 경로 오염을 막기 위해 각각 패턴 검증을 통과해야 한다. `domainId`에 슬래시가 있으면(`sms/register`) `{domainId}.html`/`{domainId}.js`도 `templates/{moduleName}/sms/register.html`처럼 한 단계 더 깊은 경로에 저장된다.

## 생성물의 v3 규약 (테스트로 고정)

- Lombok 기반: DTO/VO `@Data`, Service/Controller `@RequiredArgsConstructor` (2026-06-12 폐쇄망 Lombok 보유 확인으로 plain Java에서 전환)
- `ApiResponse<PageResponseDTO<VO>>` 응답 계약, `request.validate()` 호출
- `/create`, `/update` 분리. `/save` 생성 금지
- 메뉴 SQL은 v3 스키마 (`TB_MENU.MENU_ID/PARENT_MENU_ID`, `TB_MENU_AUTH.ROLE_CD` + `CAN_*` 8종)
- 메뉴 SQL은 입력한 `parentMenuId/menuId/roleCode/sortOrd`를 반영한다. 미입력 시 `menuId={module}_{domain}`, `roleCode=ROLE_ADMIN`, `sortOrd=99`를 기본값으로 둔다.
- HTML은 `screen-convention.md` 카드 골격, JS는 `TuiPageBuilder`
- 검색 input의 날짜 타입은 변수명이 `date`, `dt`, `at`으로 끝나는 경우에만 사용한다. `sendType`처럼 중간에 `dt` 글자 조합이 있는 일반 필드는 text로 생성한다.
- 날짜 검색조건은 native `type="date"`가 아니라 Toast UI DatePicker (`data-search-type="date"` + `{field}PickerLayer`)로 생성한다.
- 검색조건 `xxxFrom/xxxTo`, `startX/endX`, `fromX/toX` 날짜쌍은 HTML에서 `from ~ to` Toast UI DatePicker로 묶어 표시한다.
- 검색조건 입력 타입은 텍스트/날짜/콤보/라디오 중 선택 가능하며, 기본값은 없음/오늘/어제/최근 7일/이번 달/현재월 1일~오늘 중 선택한다.
- 컬럼 옵션은 그리드 표시여부, 모달 표시여부, 수정 가능여부, 헤더명, 너비, 정렬, 날짜 포맷, 마스킹 타입을 반영한다.
  - `gridVisible=false`: 그리드에서는 `hidden: true`로 유지한다. PK/상세/삭제 기준으로 사용할 수 있다.
  - `modalVisible=false`: 상세 모달에도 표시하지 않는다. 단, PK와 낙관적 잠금 값은 hidden으로 보관할 수 있다.
  - `editable=true`: CRUD 모드에서만 의미가 있다. 수정 모달 input, `*UpdateRequestDTO`, Mapper XML `UPDATE SET` 대상에 포함한다.
  - `editable=false`: 상세 모달에는 읽기전용으로 표시할 수 있지만 update payload와 `*UpdateRequestDTO`에는 포함하지 않는다.
- `screenMode`는 목록 조회만, 목록+엑셀, 목록+상세 모달, 목록+등록/수정/삭제를 분리한다.
- 상세 모달은 `TuiPageBuilder.autoModal` 공통 기능을 사용한다. CRUD 모드에서는 같은 모달 footer에 수정/삭제 버튼을 생성하고 `/update`, `/delete` endpoint를 호출한다. 실제 권한 판정은 기존 `MenuAuthInterceptor`의 URL suffix 권한 규칙을 따른다.
- CRUD 모드는 실제 DB 메타데이터의 PK를 기본값으로 사용한다. 단일 PK와 복합 PK를 모두 `pkColumns`로 다루며, 조회 SQL 결과에 모든 PK 컬럼이 없으면 생성을 막는다.
- PK가 없는 테이블은 CRUD 생성을 막고 LIST/EXCEL/DETAIL 조회 전용만 허용한다. 임의 `_ID` 컬럼을 PK처럼 추정하지 않는다.
- 선택한 `lockColumn`은 낙관적 잠금 조건으로 사용한다. 조회 SQL 결과와 targetTable 메타데이터에 포함되어야 하며, PK 컬럼은 lockColumn으로 선택할 수 없다. nullable 컬럼은 null-safe WHERE 조건으로 생성한다.
- CRUD 모드의 수정 요청은 **화이트리스트 원칙을 유지한다.** `*UpdateRequestDTO`는 `pkColumns`, `before{LockColumn}`, `editable=true` 컬럼만 선언한다. VO 전체 컬럼이나 모달에 표시된 전체 컬럼을 update 요청 DTO로 사용하지 않는다.
- CRUD 모드의 프론트 update payload도 `editable=true` 컬럼만 전송한다. 서버 DTO 화이트리스트가 최종 방어선이지만, 프론트에서도 불필요한 읽기전용/시스템 컬럼을 보내지 않는다.
- Mapper XML의 `UPDATE SET`도 `editable=true` 컬럼만 생성한다. `REG_ID`, `REG_DTTM`, PK, 권한/소유자/감사 컬럼은 기본적으로 editable 대상에서 제외한다.
- 생성 HTML 버튼은 lucide 로컬 아이콘(`data-lucide`)을 사용한다. CDN은 사용하지 않는다.
- CRUD Mapper XML은 `targetTable`, editable 컬럼, `pkColumns`, lock 컬럼 기준으로 `INSERT/UPDATE/DELETE`를 생성한다.
- DB별 현재시각/날짜 변환/페이징 SQL은 `ScaffoldDialect`가 생성한다. Oracle은 `SYSTIMESTAMP`, Postgres는 `CURRENT_TIMESTAMP`, DB2는 `CURRENT TIMESTAMP`를 사용한다.
- 개인정보 Y이면 `/data`, `/excel`에 `@PrivacyLog` 자동 부착 + `MaskingUtil` 적용 지점 TODO 표시
- 엑셀은 `ExcelUtil` 연결. Mapper의 Map 반환은 ExcelUtil 계약상 예외로 허용

## 생성 파일

| 파일 | 역할 |
|---|---|
| `controller/system/ScaffoldController.java` | 화면 + 생성/적용 API |
| `service/system/ScaffoldService.java` | 생성/적용 오케스트레이션 |
| `service/system/scaffold/ScaffoldFileApplier.java` | 생성물 경로 매핑 및 파일 저장 |
| `service/system/scaffold/QueryColumnExtractor.java` | 컬럼/검색변수 추출, 동적 SQL 변환 |
| `service/system/scaffold/ColumnTypeInferrer.java` | 실제 DB 기준 타입 추론 |
| `service/system/scaffold/ScaffoldDialect.java` | Oracle/Postgres/DB2별 페이징, 날짜 변환, 현재시각 SQL 분기 |
| `service/system/scaffold/ScaffoldMetadataReader.java` | JDBC 메타데이터 기반 PK/nullable 컬럼 조회 |
| `service/system/scaffold/*Template.java` (11종) | 산출물별 템플릿 (테스트 2종 포함) |
| `dto/system/ScaffoldRequestDTO.java` | QuerySpec 입력 |
| `dto/system/Scaffold*OptionDTO.java`, `ScaffoldApplyFileResultDTO.java` | 검색/컬럼/메뉴 옵션, 적용 미리보기 결과 |
| `templates/system/scaffold.html`, `static/js/system/scaffold.js` | 생성기 화면 |

v2는 컨트롤러 1파일 800줄이었으나, v3는 파일 300줄 규칙에 따라 분리해 핵심 로직을 단위 테스트로 고정했다.

## 의존성 (폐쇄망 반입 확인 완료)

| 의존성 | 버전 | 비고 |
|---|---|---|
| `com.github.jsqlparser:jsqlparser` | 4.9 | 회사 repo 보유 확인 (2026-06-11) |

## 검증 결과

2026-06-19 기준:

```text
mvn -Dtest=ScaffoldTemplateTest,QueryColumnExtractorTest,GlobalModelAdviceTest,AuthSourceGuardTest test  PASS
mvn test                                                                                                  PASS
```

Mockito 테스트는 `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`에서 subclass mock maker를 사용하도록 고정해 JDK self-attach 문제를 피한다.

화면 검증(사용자, 서버 기동 후):

- local 로그인 후 `http://localhost:8081/system/scaffold` 접근
- 샘플 SQL + `$변수` 입력, 정렬 컬럼 입력 후 생성 -> 탭 표시 확인
- 생성 결과 검토 후 적용 -> 적용 파일 목록 표시 확인
- 생성된 코드가 v3 규약(카드 골격, /create·/update, v3 메뉴 SQL)인지 확인

## 사용 절차

생성된 코드는 그대로 완료가 아니다. `screen-generation-guide.md`의 절차에 편입해서 사용한다.

1. 절차서 0~1단계(대상 확정, 실제 테이블 확인)를 먼저 수행한다.
2. scaffold로 2~7단계 산출물을 생성하고 검토 후 적용한다.
3. CRUD 화면은 `targetTable`, PK, lock 컬럼, editable 컬럼을 확인한다. 개인정보 화면은 마스킹 TODO를 보정한다.
4. 절차서 8~10단계(권한, mvn 검증, 문서 갱신)를 수행한다.
