# Query Scaffold 엔진 분석

## 01. ScaffoldController

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/controller/system/ScaffoldController.java`
- 역할: Query Scaffold UI 진입점이며, local profile에서만 노출되는 REST API 4개(analyze, generate, preview, apply)를 `ScaffoldService`로 위임한다.
- 핵심 메서드:
  - `page()` — 34~37줄: GET `/system/scaffold` 처리. `system/scaffold` 뷰 이름을 반환한다.
  - `analyze(Map<String, String> request)` — 39~44줄: POST `/system/scaffold/analyze` 처리. `rawQuery`와 `targetTable`만 추출해 `ScaffoldService.analyze`로 넘긴다.
  - `generate(ScaffoldRequestDTO request)` — 46~51줄: POST `/system/scaffold/generate` 처리. `@Valid` 요청 바디를 받아 `ScaffoldService.generate` 결과를 Map<String, String>으로 응답한다.
  - `preview(ScaffoldRequestDTO request)` — 53~58줄: POST `/system/scaffold/preview` 처리. `@Valid` 요청 바디를 받아 `ScaffoldService.preview` 결과를 `ScaffoldApplyFileResultDTO` 목록으로 응답한다.
  - `apply(ScaffoldRequestDTO request)` — 60~65줄: POST `/system/scaffold/apply` 처리. `@Valid` 요청 바디를 받아 `ScaffoldService.apply` 결과를 `ScaffoldApplyFileResultDTO` 목록으로 응답한다.
- 생성/변환 동작 요약: 이 클래스 자체는 코드 생성을 하지 않는다. 입력 JSON을 서비스 레이어로 전달하고, 결과를 `ApiResponse`로 감싸 HTTP 응답으로 반환한다.

## 02. ScaffoldService

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/ScaffoldService.java`
- 역할: Query Scaffold의 오케스트레이터. `analyze`, `generate`, `preview`, `apply`를 처리하고, SQL 컬럼 추출, 타입 추론, 모델 생성, 템플릿 실행, 파일 미리보기/적용을 연결한다.
- 핵심 메서드:
  - `generate(ScaffoldRequestDTO request)` — 54~56줄: 외부 REST 요청을 받아 내부 `generateFiles(request)`를 실행하고, 생성된 파일 목록 `Map<String, String>`을 반환한다.
  - `analyze(String rawQuery, String targetTable)` — 58~69줄: `targetTable`이 있으면 그것을 사용하고 없으면 `QueryColumnExtractor.extractPrimaryTable(rawQuery)`로 첫 번째 테이블을 추출한다. DB 메타데이터(`pkColumns`, `nullableColumns`)와 SQL에서 추출한 컬럼/검색 조건을 `columns`, `searchVars`, `targetTable`, `dbPlatform` 키로 구성해 반환한다.
  - `preview(ScaffoldRequestDTO request)` — 71~74줄: `generateFiles(request)`로 생성한 파일 내용을 먼저 만들고, `ScaffoldFileApplier.preview(request, generatedFiles)`로 실제 적용 없이 최종 경로별 미리보기 결과를 반환한다.
  - `apply(ScaffoldRequestDTO request)` — 76~79줄: `generateFiles(request)`로 생성한 파일 내용을 먼저 만들고, `ScaffoldFileApplier.apply(request, generatedFiles)`로 지정된 프로젝트 경로에 파일을 적용한 결과를 반환한다.
  - `generateFiles(ScaffoldRequestDTO request)` — 81~110줄: scaffold의 핵심 생성 파이프라인. `rawQuery`의 SELECT 컬럼과 검색 조건을 추출하고, `ColumnTypeInferrer`로 컬럼 타입을 추론하며, `ScaffoldModel`을 만든 뒤 DTO/VO/Mapper/Service/Controller/테스트/HTML/JS/메뉴 SQL 템플릿을 순서대로 실행해 파일명→내용 Map을 반환한다.
  - `enrichAndValidateCrudRequest(ScaffoldRequestDTO request, List<String> columns)` — 112~153줄: CRUD 생성 옵션이 활성화된 경우 PK, PK 컬럼 포함 여부, lock column 존재 여부/NULL 여부/PK 제외 조건을 검증하고, 요청값이 없으면 DB 메타데이터의 PK를 채운다.
- 생성/변환 동작 요약:
  - `analyze`: 입력 `rawQuery`, 선택 입력 `targetTable` → DB 메타데이터 + SQL 컬럼/검색 조건 요약 Map.
  - `generate`: `ScaffoldRequestDTO` → 생성 파일 내용 Map. 실제 파일 생성은 하지 않고 JSON 응답용 문자열을 만든다.
  - `preview`: `ScaffoldRequestDTO` → 적용 전 파일별 예상 경로와 내용 목록.
  - `apply`: `ScaffoldRequestDTO` → `ScaffoldFileApplier`를 통해 실제 프로젝트 파일 적용 결과 목록.
- 참고: ServiceTemplate, UpdateRequestDtoTemplate, ServiceTestTemplate은 `generateFiles`에서 각각 `cls + "Service.java"`, `cls + "UpdateRequestDTO.java"`, `cls + "ServiceTest.java"` 파일명으로 호출된다.

## 03. ColumnTypeInferrer

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ColumnTypeInferrer.java`
- 역할: `rawQuery`를 실제 DB에서 0행 결과로 실행해 `ResultSetMetaData`를 보고, `ScaffoldModel`이 사용할 Java 타입 맵을 만든다. 기본값은 `String`이지만 DB 타입 기반 추론 실패 시 `String`으로 강제하지 않고 오류를 반환한다.
- 핵심 메서드:
  - `inferTypes(String rawQuery, List<String> columns)` — 30~74줄: 입력 `rawQuery`와 SELECT 컬럼 목록을 받는다. 먼저 모든 컬럼을 `String`으로 초기화한 뒤, MyBatis/EL表达式(`<...>`, `#{...}`, `${...}`, `$var`)를 안전 쿼리용 `NULL` 또는 `1=1`로 변환한다. `ScaffoldDialect.emptyResultQuery`로 0행 결과 쿼리를 만들고 `JdbcTemplate`로 실행해 `ResultSetMetaData`의 라벨/타입/정밀도/소수점 정보를 읽는다. 라벨과 입력 컬럼을 case-insensitive로 매칭해 `Map<String, String>`에 Java 타입을 저장한다.
  - `rootCauseMessage(Throwable e)` — 76~82줄: 예외 원인 체인에서 가장 안쪽 메시지를 추출해 사용자 오류 메시지에 포함한다.
  - `findMatchingColumn(List<String> columns, String label)` — 84~91줄: `ResultSetMetaData.getColumnLabel()` 값을 입력 컬럼 목록과 case-insensitive로 비교해 실제 요청/모델에 사용할 컬럼명을 반환한다.
- 생성/변환 동작 요약: 입력 `rawQuery`, `columns` → `Map<String, String>` 컬럼 타입 맵. 예: 날짜/숫자/문자열 타입을 `ScaffoldDialect.javaType`을 통해 Java 타입으로 매핑한다.
- 미확인: `ScaffoldDialect.javaType`의 세부 매핑 규칙은 이 클래스 내부에 직접 나와 있지 않으므로 별도 `ScaffoldDialect` 문서에서 확인해야 한다.

## 04. ScaffoldModel

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ScaffoldModel.java`
- 역할: `ScaffoldRequestDTO`와 분석 결과(`columns`, `searchVars`, `typeMap`)를 묶어 모든 템플릿이 공유할 데이터 모델로 제공하는 DTO/모델 계층이다.
- 핵심 메서드:
  - 생성자 `ScaffoldModel(ScaffoldRequestDTO request, List<String> columns, List<String> searchVars, Map<String, String> typeMap, ScaffoldDialect dialect)` — 31~39줄: 요청 DTO, SELECT 컬럼, 검색 변수, 타입 맵, dialect를 저장한다. 컬럼/검색 변수는 `List.copyOf`, 타입 맵은 `Map.copyOf`, dialect는 null이면 `ORACLE`로 기본 처리한다.
  - `moduleName/domainId/domainClass/domainName/rawQuery/orderBy` — 61~83줄: `ScaffoldRequestDTO`의 기본 식별/쿼리/정렬 정보를 템플릿에 제공한다.
  - `includeCreateUpdate/includeExcel/includeDetailModal/includePrivacy` — 85~99줄: 화면 모드 또는 요청 옵션을 기반으로 CRUD, Excel, 상세 모달, 개인정보 표시 옵션을 계산한다.
  - `targetTable` — 101~106줄: 요청의 `targetTable`이 있으면 대문자로 정리해 사용하고, 없으면 `rawQuery`에서 첫 번째 테이블을 추출한다.
  - `screenMode` — 108~119줄: 요청의 `screenMode`가 있으면 대문자로 사용하고, 없으면 CRUD/Excel 옵션이 있으면 각각 `CRUD`/`EXCEL`, 아니면 `LIST`로 판단한다.
  - `pkColumns/pkColumn/pkFieldNames` — 121~150줄: 요청의 `pkColumns` 또는 `pkColumn`을 정규화해 PK 정보를 제공한다.
  - `pkParamImports/pkJavaType/lockJavaType` — 152~179줄: PK와 lock 컬럼의 Java 타입, 테스트 코드에 필요한 import 목록을 계산한다.
  - `lockColumn/beforeLockFieldName/lockFieldName` — 181~200줄: lock column 설정이 있으면 그 컬럼명을 사용하고, 없으면 `updateDttm` 계열 필드를 기본값으로 삼는다.
  - `screenUrl` — 202~204줄: 모듈명/도메인 ID로 화면 URL `/module/domainId`를 만든다.
  - `searchParams` — 206~230줄: 검색 변수 목록과 요청의 `searchParamOptions`를 조합해 검색 파라미터 목록을 만든다. 기본 검색어는 `searchKeyword`이며, 날짜형 변수는 `DATE`, 선택/라디오 옵션은 요청 옵션에 따라 처리한다.
  - `columnConfigs` — 232~276줄: SELECT 컬럼과 요청의 `columnOptions`를 조합해 각 컬럼의 Java 타입, 필드명, 헤더명, 폭, 정렬, 날짜 형식, mask, 표시/편집 여부를 계산한다.
  - `menuId/parentMenuId/roleCode/menuSortOrd` — 278~308줄: 메뉴 옵션이 있으면 그 값을 사용하고, 없으면 기본값을 생성한다.
  - 내부 보조: `isDateVar` — 310~313줄, `normalizeColumn` — 315~317줄, `normalizeTable` — 319~321줄, `isProtectedUpdateColumn` — 323~335줄, `isDefaultEditableCandidate` — 337~346줄.
- 생성/변환 동작 요약: 입력 `ScaffoldRequestDTO + columns + searchVars + typeMap + dialect` → 템플릿이 읽을 수 있는 `SearchParam`, `ColumnConfig`, 메뉴/CRUD/URL 정보 등 구조화된 데이터.
- 미확인: 이 파일 자체는 실제 파일 생성을 하지 않는다. 실제 생성은 `ServiceTemplate`, `UpdateRequestDtoTemplate`, `ServiceTestTemplate` 등 각 템플릿이 `ScaffoldModel`을 읽어서 수행한다.

## 05. ServiceTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ServiceTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 실제 Spring Service 클래스를 생성한다. 기본 검색, CRUD, Excel 다운로드 코드를 템플릿 문자열로 만든다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 9~87줄: 입력 `ScaffoldModel`을 읽고 생성할 Service 클래스의 패키지/수입/클래스/메서드를 구성한다. 항상 `search` 메서드를 만들고, `includeCreateUpdate()`가 참이면 `create`, `update`, `delete`를, `includeExcel()`가 참이면 `downloadExcel`을 추가한다.
  - `joinQuoted(ScaffoldModel model, boolean upperCase)` — 89~99줄: Excel 다운로드용 헤더/키 문자열을 컬럼 목록에서 만든다. `upperCase=false`는 원문 컬럼, `true`는 대문자 컬럼을 사용한다.
  - `deleteMethodParams(ScaffoldModel model)` — 101~112줄: Delete 요청용 매개변수를 PK 타입과 camelCase 필드명으로 만든다.
  - `deleteCallArgs(ScaffoldModel model)` — 114~116줄: Delete 호출 시 전달할 필드명 목록을 만든다.
  - `maskExcelRows(ScaffoldModel model)` — 118~133줄: Excel 다운로드 시 mask 타입이 있는 컬럼에 대해 row별 마스킹 보류를 위한 가짜 처리를 삽입한다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.service.<module>.<DomainClass>Service.java` 소스 문자열.
- 주요 생성 코드 특징:
  - `search`: `request.validate()` 후 `mapper.count(request)`와 `mapper.selectList(request)`를 호출하고 `PageResponseDTO.of(list, request, totalCount)`를 반환한다.
  - `create/update/delete`: CRUD 모드에서 생성/수정/삭제 메서드를 만든다. `update`는 `mapper.update(request)`가 0이면 `ErrorCode.UPDATE_CONFLICT`를 던진다.
  - `downloadExcel`: `mapper.selectListForExcel(request)`를 호출하고 `ExcelUtil.downloadExcel(response, cls + "_export", headers, list, keys)`를 실행한다.
- 미확인: `maskExcelRows`는 실제 마스킹 로직이 아니라 TODO 주석과 임시 `value.toString()`을 넣는 임시 보류 코드이다.

## 06. UpdateRequestDtoTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/UpdateRequestDtoTemplate.java`
- 역할: CRUD 모드에서 사용할 수정 요청용 화이트리스트 DTO를 생성한다. VO를 직접 쓰기보다 수정 가능한 필드만 선언해 mass assignment를 방지하는 구조를 만든다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 12~63줄: 입력 `ScaffoldModel`을 읽고 `UpdateRequestDTO` 클래스를 생성한다. 날짜/숫자 타입이 있으면 필요한 Java import를 추가하고, PK 필드, editable 컬럼, lock column을 순서대로 필드로 만든다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.dto.<module>.<DomainClass>UpdateRequestDTO.java` 소스 문자열.
- 주요 생성 코드 특징:
  - PK 필드는 `WHERE` 조건용 필드로 선언한다.
  - `column.editable()`이 참인 컬럼만 포함한다.
  - lock column이 있으면 `before<FieldName>` 형태의 hidden lock 필드를 추가한다.
- 미확인: 실제 수정 가능 필드 목록은 `ScaffoldModel.columnConfigs()`의 `editable()` 값을 통해 결정된다.

## 07. ServiceTestTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ServiceTestTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 Service 단위 테스트 파일을 생성한다. Mapper는 Mockito mock으로 대체하고, 목록 조회/수정 실패/삭제 위임 테스트를 만든다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 9~84줄: 입력 `ScaffoldModel`을 읽고 `ServiceTest` 클래스를 생성한다. 항상 목록 조회 테스트를 만들고, `includeCreateUpdate()`가 참이면 수정 충돌 테스트와 삭제 위임 테스트를 추가한다.
  - `samplePkArgs(ScaffoldModel model)` — 86~90줄: PK 컬럼 목록을 기반으로 테스트에서 사용할 인자 문자열을 만든다.
  - `sampleValue(String javaType)` — 92~102줄: Java 타입별 안정된 샘플 리터럴을 생성한다. `Integer`, `Long`, `LocalDate`, `LocalDateTime`, `BigDecimal`, 기본 문자열을 처리한다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.service.<module>.<DomainClass>ServiceTest.java` 소스 문자열.
- 주요 생성 코드 특징:
  - 목록 조회 테스트는 `mapper.count(request)`를 1, `mapper.selectList(request)`를 하나의 VO 목록으로 mock 처리한다.
  - CRUD 모드에서는 `mapper.update(any())`를 0으로 mock해 `CustomException` 발생을 검증한다.
  - 삭제 테스트는 `service.delete(...)`가 호출되면 `mapper.delete(...)`가 호출됐다고 검증한다.

## 08. JSqlParser 사용 위치

- 검색 범위: `/Users/dk/Work/smsTest-v3/src/main/java/**/*.java` 전체와 `pom.xml`
- 확인 결과: 명시적인 `JSqlParser` 또는 `net.sf.jsqlparser` 문구는 `QueryColumnExtractor`에서만 사용된다. 전체 코드베이스에서 `JSqlParser`, `net.sf.jsqlparser`, `jsqlparser` 문자열 검색은 해당 파일 외에는 결과를 주지 않았다.
- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/QueryColumnExtractor.java`
- 사용 위치와 역할:
  - import — 7~12줄: `CCJSqlParserUtil`, `Column`, `Table`, `PlainSelect`, `Select`, `SelectItem`을 가져온다.
  - `extractColumns(String query)` — 30~64줄: `rawQuery`를 `safeParseQuery`로 MyBatis 동적 구문을 `NULL`로 치환한 뒤 `CCJSqlParserUtil.parse`로 파싱한다. `Select`의 `SelectItem`을 순회해 alias가 있으면 alias를, `Column`이면 컬럼명을, 그 외 식이면 마지막 식별자를 컬럼 목록에 추가한다. 파싱 실패 시 `extractColumnsFallback(query)`로 정규식 기반 추출로 우회한다.
  - `extractPrimaryTable(String query)` — 66~89줄: `rawQuery`를 JSqlParser로 파싱해 `PlainSelect.getFromItem()`이 `Table`이면 `table.getFullyQualifiedName()`을 정규화해 반환한다. 파싱 실패 또는 `Table`이 아니면 `FROM` 정규식 패턴으로 우회한다.
  - `safeParseQuery(String query)` — 177~182줄: JSqlParser가 MyBatis `#{...}`, `${...}`, `$var` 구문을 제대로 처리하지 못하지 않도록 `NULL` 또는 빈 문자열로 변환한다.
- 사용 방식 요약:
  - JSqlParser는 Query Scaffold의 SQL 파싱 우선 수단으로 사용된다.
  - 컬럼 추출과 기본 테이블 추출에 사용되며, 실패하면 정규식 fallback이 적용된다.
  - `$검색변수` 추출과 동적 SQL 변환은 JSqlParser가 아니라 정규식으로 처리한다.
- 미확인: `pom.xml` 내 JSqlParser 의존성 확인은 별도 검증에서 필요하지만, 현재 작업 범위에서는 사용 위치와 호출 방식만 확인했다.

## 09. ControllerTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ControllerTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 실제 Spring MVC Controller를 생성한다. 목록 조회, CRUD 엔드포인트, Excel 다운로드, 개인정보 로깅 옵션을 템플릿화한다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 12~107줄: 입력 `ScaffoldModel`을 읽고 `Controller` 클래스, `@RequestMapping`, `@GetMapping`, `@PostMapping` 등 API 구조를 만든다.
  - `deleteRequestParams(ScaffoldModel model)` — 109~120줄: 삭제 요청용 `@RequestParam` 매개변수 목록을 만든다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.controller.<module>.<DomainClass>Controller.java` 소스 문자열.
- 주요 생성 코드 특징:
  - 목록 조회는 `GET /data`
  - CRUD 모드에서는 `POST /create`, `POST /update`, `POST /delete`
  - Excel 모드는 `GET /excel`
  - 개인정보 옵션이 있으면 `@PrivacyLog`를 붙인다.

## 10. ControllerTestTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ControllerTestTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 Controller 단위 테스트 파일을 생성한다. MockMvc standalone setup으로 `ApiResponse` 포맷을 검증한다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 9~53줄: 입력 `ScaffoldModel`을 읽고 `ControllerTest` 클래스를 생성한다. `Service`를 Mockito mock으로 설정하고 `GET <screenUrl>/data` 호출 결과를 검증한다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.controller.<module>.<DomainClass>ControllerTest.java` 소스 문자열.
- 주요 생성 코드 특징: `service.search(any())`를 `PageResponseDTO.of(List.of(), new <DomainClass>SearchRequestDTO(), 0)`으로 mock 처리하고, `status().isOk()`, `$.code == 200`, `$.data.totalCount == 0`을 검증한다.

## 11. DtoTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/DtoTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 검색 요청 DTO를 생성한다. `PageRequestDTO`를 상속하고 검색 파라미터를 필드로 만든다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 11~28줄: 입력 `ScaffoldModel`의 `searchParams()`를 읽고 `SearchRequestDTO` 클래스를 생성한다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.dto.<module>.<DomainClass>SearchRequestDTO.java` 소스 문자열.
- 주요 생성 코드 특징: 검색 파라미터는 모두 `String` 필드로 생성되며, `@Data`, `@EqualsAndHashCode(callSuper = true)`를 붙인다.

## 12. HtmlTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/HtmlTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 Thymeleaf 목록 화면 HTML을 생성한다. 검색 조건 카드, 버튼, Excel 버튼, JS 포함 태그를 만든다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 13~86줄: 입력 `ScaffoldModel`의 검색 파라미터와 화면 옵션을 읽고 HTML 구조를 생성한다.
  - `appendInput(StringBuilder sb, ScaffoldModel.SearchParam param)` — 88~117줄: 검색 파라미터를 input/select/radio/date picker로 변환한다.
  - `createButton(ScaffoldModel model)` — 119~124줄: CRUD 모드에서 등록 버튼을 생성한다.
  - `appendDatePickerInput(StringBuilder sb, String inputId, int width)` — 126~137줄: 날짜 입력 UI와 picker layer를 생성한다.
  - `findRangeTo(ScaffoldModel.SearchParam fromParam, List<ScaffoldModel.SearchParam> params)` — 147~162줄: 날짜 검색 파라미터의 `From`/`To` 쌍을 찾는다.
  - `rangeToName`, `rangeLabel`, `parseOptions`, `parseOption`, `escape` — 164~214줄: 범위 라벨/옵션 파싱/HTML escape를 담당한다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `<domainId>.html` 소스 문자열.
- 주요 생성 코드 특징:
  - 검색 조건은 `scaffold-search-card`
  - 날짜 필드는 `tui-datepicker-input`와 `scaffold-date-picker-layer`
  - 검색 버튼은 `btn-search`
  - CRUD 모드에서는 `btn-create`
  - Excel 모드는 `btn-excel`

## 13. JsTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/JsTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 화면 JS를 생성한다. TuiPageBuilder로 그리드, 검색 파라미터, 컬럼, 모달, Excel 버튼을 초기화한다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 11~103줄: 입력 `ScaffoldModel`을 읽고 `TuiPageBuilder` 구성 객체를 만든다.
  - `appendFormatter(StringBuilder sb, ScaffoldModel.ColumnConfig column)` — 105~121줄: 컬럼의 mask/날짜 형식/날짜 타입에 따라 formatter를 붙인다.
  - `escapeJs(String value)` — 123~125줄: JS 문자열 내부의 backslash/quote를 이스케이프한다.
  - `pkFields(ScaffoldModel model)` — 127~136줄: 모달 action의 PK field 목록을 만든다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `<domainId>.js` 소스 문자열.
- 주요 생성 코드 특징:
  - `apiUrl`은 `<screenUrl>/data`
  - 검색 입력은 `searchInputs`
  - 검색 기본값은 `searchDefaults`
  - 컬럼은 `header/name/align/width/hidden/modalVisible/editable/inputMask/validate/formatter`
  - 상세 모달이 있으면 `autoModal: true`와 create/update/delete URL을 생성한다.
  - Excel 모드가 있으면 `#btn-excel` 클릭 시 `/excel`로 이동한다.

## 14. MapperInterfaceTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/MapperInterfaceTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 MyBatis Mapper 인터페이스를 생성한다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 9~48줄: 입력 `ScaffoldModel`을 읽고 `@Mapper` 인터페이스와 count/selectList/CRUD/Excel 메서드를 만든다.
  - `deleteParams(ScaffoldModel model)` — 50~62줄: DELETE 메서드 파라미터를 `@Param`과 camelCase 필드명으로 만든다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.mapper.<module>.<DomainClass>Mapper.java` 소스 문자열.
- 주요 생성 코드 특징:
  - 기본 CRUD는 `count`, `selectList`, `insert`, `update`, `delete`
  - Excel 모드는 `selectListForExcel` 추가
  - PK import는 `model.pkParamImports()`를 사용

## 15. MapperXmlTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/MapperXmlTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 MyBatis Mapper XML을 생성한다. 조회 SQL을 래핑하고 `$검색변수`를 동적 `<if>` 조건으로 변환하며 CRUD/Excel XML을 만든다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 20~99줄: 입력 `ScaffoldModel`을 읽고 Mapper namespace, count/selectList/CRUD/selectListForExcel XML을 생성한다.
  - `convertToDynamicSql(ScaffoldModel model, String indent)` — 101~134줄: `rawQuery`를 줄 단위로 읽고 `$변수`를 `<if test="...">`로 감싼다.
  - `replaceBindVariables(...)` — 136~153줄: `COLUMN_COMPARE_PATTERN`으로 컬럼과 `$변수`의 비교식을 찾아 `#{var}` 또는 날짜 범위 표현으로 변환한다.
  - `compareReplacement(...)`, `bindExpression(...)` — 155~197줄: 날짜 타입에 따라 `TO_DATE`/`TO_TIMESTAMP`와 `+ INTERVAL '1' DAY` 같은 날짜 범위를 만든다.
  - `escapeSqlText`, `xmlOperator`, `isUpperBound` — 199~221줄: XML/SQL 텍스트와 연산자 표현을 보조한다.
  - `insertSql`, `updateSetClause`, `editableColumns` — 223~285줄: INSERT/UPDATE SET 절을 생성한다.
  - `hasColumn`, `currentValueExpression`, `pkWhereClause`, `lockWhereClause`, `validateCrudModel`, `bindParameter` — 287~356줄: 컬럼/현재값/PK/잠금/타입 매핑 보조를 담당한다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `Mapper.xml` 소스 문자열.
- 주요 생성 코드 특징:
  - 조회 SQL은 항상 `SELECT A.* FROM (<dynamicQuery>) A` 구조로 감싼다.
  - 검색 조건은 `searchConditions` SQL id에 포함된다.
  - `$start_dt` 같은 변수는 `startDt`로 camelCase 변환되어 `#{startDt}`로 바인딩된다.
  - 날짜 검색은 `TO_DATE`/`TO_TIMESTAMP`와 하루 단위 범위 조건으로 변환된다.
  - CRUD가 켜지면 INSERT/UPDATE/DELETE XML을 추가한다.

## 16. MenuSqlTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/MenuSqlTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 메뉴/권한 등록 SQL을 생성한다. v3 스키마의 `SMS.TB_MENU`와 `SMS.TB_MENU_AUTH`를 기준으로 한다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 10~50줄: 입력 `ScaffoldModel`의 menuId, parentMenuId, roleCode, screenUrl, domainName 등을 읽고 SQL 문자열을 만든다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → 메뉴/권한 등록 SQL 문자열.
- 주요 생성 코드 특징: `TB_MENU`에 메뉴 정보, `TB_MENU_AUTH`에 `CAN_READ/CAN_CREATE/CAN_UPDATE/CAN_DELETE/CAN_APPROVE/CAN_CANCEL/CAN_DOWNLOAD/CAN_MASK_VIEW` 권한을 등록하고 `COMMIT`한다.

## 17. VoTemplate

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/VoTemplate.java`
- 역할: `ScaffoldModel`을 기반으로 화면 VO를 생성한다. 컬럼 타입 추론 결과를 반영한 Lombok VO를 만든다.
- 핵심 메서드:
  - `generate(ScaffoldModel model)` — 10~43줄: 입력 `ScaffoldModel`의 컬럼 목록과 타입 맵을 읽고 `VO` 클래스를 생성한다.
- 생성/변환 동작 요약: 입력 `ScaffoldModel` → `com.scbk.sms.vo.<module>.<DomainClass>VO.java` 소스 문자열.
- 주요 생성 코드 특징: `LocalDate`, `LocalDateTime`, `BigDecimal`은 import를 추가하고, 컬럼명은 `QueryColumnExtractor.toCamelCase`로 변환한다. 개인정보 옵션이 있으면 마스킹 주석을 붙인다.

## 18. ScaffoldRequestDTO

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/dto/system/ScaffoldRequestDTO.java`
- 역할: Query Scaffold 생성 요청을 받는 DTO이다. `rawQuery` 안의 `$검색변수`는 검색조건 규약으로 사용된다.
- 핵심 메서드:
  - `getRawQuery()/setRawQuery(String rawQuery)` — 80~86줄: 원본 SQL을 받는다.
  - `getOrderBy()/setOrderBy(String orderBy)` — 88~94줄: 목록 정렬 기준을 받는다.
  - `getPkColumns()` — 152~157줄: `pkColumn` 또는 `pkColumns` 중 하나를 기본 PK 목록으로 반환한다.
  - `getSearchParamOptions()`, `getColumnOptions()`, `getMenuOption()` — 171~193줄: 검색 옵션, 컬럼 옵션, 메뉴 옵션을 받는다.
- 생성/변환 동작 요약: 직접 생성물은 만들지 않고, `ScaffoldService`가 이 DTO를 읽어 `ScaffoldModel`로 변환한다.
- 주요 생성 코드 특징: `moduleName`, `domainId`, `domainClass`, `domainName`, `rawQuery`, `orderBy`는 `@NotBlank` 또는 `@Pattern`으로 입력을 제한한다.

## 19. QueryColumnExtractor

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/QueryColumnExtractor.java`
- 역할: `rawQuery`에서 SELECT 컬럼, 기본 테이블, `$검색변수`를 추출하고 `$검색변수` 라인을 MyBatis 동적 SQL로 변환한다.
- 핵심 메서드:
  - `extractColumns(String query)` — 30~64줄: JSqlParser로 `Select`의 `SelectItem`을 파싱해 alias/컬럼/식별자를 추출한다. 실패 시 정규식 fallback을 사용한다.
  - `extractPrimaryTable(String query)` — 66~89줄: JSqlParser로 `FROM` 테이블을 추출하고 정규화한다. 실패 시 `FROM` 정규식 fallback을 사용한다.
  - `extractSearchVars(String query)` — 91~105줄: `$변수`를 camelCase로 변환하고 중복 제거한다.
  - `convertToDynamicSql(String rawQuery, String indent)` — 112~141줄: `$변수`가 포함된 SQL 줄을 `<if test="...">`로 감싸고 `#{camelCase}`로 변환한다.
  - `toCamelCase(String value)` — 143~154줄: snake_case를 camelCase로 변환한다.
  - `extractColumnsFallback`, `safeParseQuery`, `normalizeIdentifier`, `splitTopLevel`, `lastIdentifier` — 156~225줄: 정규식 fallback, 파싱 전 치환, 식별자 정규화, top-level split, 식별자 추출을 담당한다.
- 생성/변환 동작 요약: 직접 파일을 생성하지 않고, `ColumnTypeInferrer`, `ServiceTemplate`, `MapperXmlTemplate`, `ControllerTemplate`, `MapperInterfaceTemplate`, `VoTemplate` 등에서 사용된다.
- 주요 생성 코드 특징: `rawQuery`의 `$start_dt`는 `startDt`로 변환되어 DTO 필드명, MyBatis bind parameter, 컨트롤러 파라미터명에 적용된다.

## 20. ScaffoldDialect

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ScaffoldDialect.java`
- 역할: DB 플랫폼별 SQL fragment와 JDBC 타입 → Java 타입 추론을 제공한다.
- 핵심 메서드:
  - `emptyResultQuery(String sql)` — 12~126줄: Oracle/Postgres/DB2별 빈 결과 조회용 SQL fragment를 만든다.
  - `pageClause()` — 17~100줄: Oracle/Postgres/DB2별 paging clause를 만든다.
  - `currentTimestamp()`, `currentDate()`, `currentTimestampString()` — 22~115줄: 현재값/현재문자열을 DB별 SQL로 만든다.
  - `dateExpression(String fieldName)`, `timestampExpression(String fieldName, String suffix)`, `plusOneDay(String expression)` — 37~130줄: 날짜/시간 검색과 하루 추가 조건을 만든다.
  - `javaType(String typeName, int jdbcType, int precision, int scale)` — 150~160줄: JDBC 타입을 Java 타입으로 추론한다.
  - `from(String value)` — 163~175줄: `oracle/postgres/db2` 문자열을 enum으로 변환한다.
  - `integerType`, `typeNameFallback` — 177~191줄: 정수/타입명 기반 Java 타입 fallback을 제공한다.
- 생성/변환 동작 요약: 직접 파일을 생성하지 않고, 조회 SQL paging, 날짜 bind, CRUD 현재값, 타입 추론에 사용된다.
- 주요 생성 코드 특징: 기본 DB는 `ORACLE`이며, `sms.scaffold.db-platform` 설정으로 전환한다.

## 21. ScaffoldTableMetadata

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ScaffoldTableMetadata.java`
- 역할: 테이블 메타데이터 중 PK 컬럼과 NULL 가능 여부를 담는 레코드이다.
- 핵심 메서드:
  - `isNullable(String columnName)` — 10~15줄: 컬럼명을 대문자로 정규화해 nullable 여부를 반환한다. 누락 시 `true`로 처리한다.
- 생성/변환 동작 요약: 직접 생성하지 않고, `ColumnTypeInferrer`가 컬럼 타입과 null 정보를 함께 전달하기 위한 보조 구조로 사용된다.
- 주요 생성 코드 특징: `pkColumns`와 `nullableByColumn`만 가진 단순 record로, 복잡한 로직은 없다.

## 22. ScaffoldMetadataReader

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ScaffoldMetadataReader.java`
- 역할: DBMS `DatabaseMetaData`를 통해 `targetTable`의 PK와 컬럼 null 가능 여부를 읽는다. `@Profile("local")`로 local 프로필에서만 활성화된다.
- 핵심 메서드:
  - `read(String targetTable)` — 28~44줄: 입력 테이블명을 파싱하고, 존재 여부를 확인한 뒤 PK/NULL 정보를 읽는다.
  - `resolveTableName(...)`, `tableExists(...)` — 46~60줄: catalog/schema/table 또는 schema/table 형태로 후보를 만들어 실제 테이블 존재 여부를 확인한다.
  - `readPrimaryKeys(...)`, `readNullability(...)` — 62~88줄: `getPrimaryKeys`와 `getColumns`를 사용해 PK 순서와 null 가능 여부를 수집한다.
  - `normalizeColumn`, `TableName.parse`, `TableName.candidates`, `upper/lower` — 90~132줄: 테이블/컬럼명 정규화와 후보 생성을 보조한다.
- 생성/변환 동작 요약: 직접 생성하지 않고, `ColumnTypeInferrer`가 컬럼 타입과 함께 null 정보를 읽기 위해 사용한다.
- 주요 생성 코드 특징: 테이블이 없으면 입력값을 그대로 사용하고, `getColumns`의 `NULLABLE`이 `columnNoNulls`가 아니면 nullable로 본다.

## 23. ScaffoldFileApplier

- 파일: `/Users/dk/Work/smsTest-v3/src/main/java/com/scbk/sms/service/system/scaffold/ScaffoldFileApplier.java`
- 역할: `ScaffoldService`가 생성한 파일 문자열을 실제 프로젝트 경로의 예상 위치로 preview/apply한다. `@Profile("local")`에서만 사용된다.
- 핵심 메서드:
  - `preview(ScaffoldRequestDTO request, Map<String, String> generatedFiles)` — 42~51줄: 각 산출물 이름으로 대상 경로를 계산하고 NEW/UNCHANGED/OVERWRITE 상태를 반환한다.
  - `apply(ScaffoldRequestDTO request, Map<String, String> generatedFiles)` — 53~60줄: preview 결과를 반환하면서 각 파일을 작성한다.
  - `resolveTargetPath(ScaffoldRequestDTO request, String generatedName)` — 62~100줄: DTO/VO/Mapper/XML/Service/Controller/Test/HTML/JS/Menu SQL 산출물별 경로를 매핑한다.
  - `writeFile(Path targetPath, String content)` — 102~112줄: 기존 내용이 같으면 쓰지 않고, 다르면 디렉터리 생성 후 덮어쓴다.
  - `result(...)`, `toDisplayPath`, `resolveOutputRoot`, `validateRequest`, `validate` — 114~152줄: 파일 상태 표시, 경로 표시, output-root 설정, 입력값 패턴 검사를 담당한다.
- 생성/변환 동작 요약: 직접 생성하지 않고, 산출물 이름과 request 정보로 실제 적용 경로를 계산한다.
- 주요 생성 코드 특징: `sms.scaffold.output-root` 설정이 없으면 현재 작업 디렉토리를 기준으로 한다. 경로가 `outputRoot` 밖으로 벗어나면 거부한다.
