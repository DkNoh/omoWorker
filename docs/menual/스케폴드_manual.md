# Query Scaffold 사용 매뉴얼

> **대상 독자**: 폐쇄망에서 업무 화면(목록/엑셀/CRUD)을 만들어야 하는 개발자
> **도구 성격**: local 전용 최초 생성 도구. SQL 조회 쿼리 한 개를 입력하면 개발자가 이어서 완성할 화면 1세트의 기본 파일을 만든다.
> **관련 문서**: 설계 근거 `docs/base/v2-scaffold-reference.md`, 구현 기록 `docs/base/query-scaffold-implementation.md`, 생성 계약 `docs/base/scaffold-contract.md`, 포매터 `docs/base/grid-formatter-guide.md`, 화면 생성 절차 `docs/base/screen-generation-guide.md`

---

## 목차

1. [개요 — Scaffold가 만드는 것과 쓰는 시점](#1-개요--scaffold가-만드는-것과-쓰는-시점)
2. [접속과 화면 흐름](#2-접속과-화면-흐름)
3. [조회 SQL 작성 가이드 (QueryColumnExtractor 동작)](#3-조회-sql-작성-가이드-querycolumnextractor-동작)
4. [추출 실패 / 주의 SQL 패턴 (에러 패턴)](#4-추출-실패--주의-sql-패턴-에러-패턴)
5. [옵션 계약 (Options Contract)](#5-옵션-계약-options-contract)
6. [생성 파일 구조와 각 파일의 역할](#6-생성-파일-구조와-각-파일의-역할)
7. [생성 후 체크리스트](#7-생성-후-체크리스트)
8. [자주 하는 실수와 트러블슈팅](#8-자주-하는-실수와-트러블슈팅)
9. [부록](#9-부록)

---

## 1. 개요 — Scaffold가 만드는 것과 쓰는 시점

### 1.1 무엇인가

Query Scaffold는 **조회 SQL 1개(`rawQuery`)와 검색조건 규약(`$변수`)을 입력**받아, 그 쿼리를 분석해서 화면 1세트를 구성하는 Java/HTML/JS/SQL 파일을 한꺼번에 생성하는 **local 전용 개발 도구**다.

핵심 아이디어는 다음 한 줄이다.

```text
$변수 1개  →  SearchRequestDTO 필드 + 화면 검색 input + Mapper XML <if> 동적조건 + #{바인딩}
SELECT alias 1개  →  VO 필드 + 그리드 컬럼
```

즉 검색조건과 컬럼을 각각 **하나의 규약으로 선언하면 DTO·화면·XML이 동시에 정합**된다. 개발자가 이 세 곳을 손으로 맞춰 쓸 때 나는 불일치를 원천적으로 줄이는 것이 목적이다.

### 1.2 무엇을 생성하나

`screenMode`에 따라 **기본 11종**, CRUD 모드일 때 `UpdateRequestDTO`가 추가되어 **12종**을 생성한다.

| 구분 | 생성 파일 |
|---|---|
| DTO | `{Domain}SearchRequestDTO.java`, `{Domain}UpdateRequestDTO.java`(CRUD만) |
| VO | `{Domain}VO.java` |
| Mapper | `{Domain}Mapper.java` (interface), `{Domain}Mapper.xml` |
| Service / Controller | `{Domain}Service.java`, `{Domain}Controller.java` |
| Test | `{Domain}ServiceTest.java`, `{Domain}ControllerTest.java` |
| 화면 | `{domainId}.html`, `{domainId}.js` |
| 메뉴 | `메뉴등록.sql` |

> 여기서 `{Domain}`은 입력값 `domainClass`(PascalCase, 예: `SmsHistory`), `{domainId}`는 URL/파일명(예: `history`)이다.

생성물은 v3 규약을 따른다. Lombok(`@Data`, `@RequiredArgsConstructor`) 기반, 응답 계약 `ApiResponse<PageResponseDTO<VO>>`, 등록/수정 endpoint `/create`·`/update` 분리(`/save` 생성 금지), v3 메뉴 스키마(`TB_MENU.MENU_ID/PARENT_MENU_ID`, `TB_MENU_AUTH.ROLE_CD` + `CAN_*`), HTML은 `screen-convention.md` 카드 골격, JS는 `TuiPageBuilder`를 사용한다.

### 1.3 언제 쓰나 / 언제 쓰지 않나

**쓰면 좋은 경우**

- 단일 테이블 또는 단순 조인 기반의 표준 목록/엑셀/CRUD 화면을 빠르게 시작할 때
- 검색조건·컬럼·DTO·XML 동적조건을 정합 맞게 한 번에 잡고 싶을 때
- 고품질 "출발점"을 얻은 뒤 TODO 자리에 업무 로직을 채우는 방식이 맞을 때

**생성 후 개발이 필요한 경우**

- `UNION`, `WITH`(CTE), 다중 서브쿼리 등 복잡한 집합 쿼리 — 조회는 동작해도 컬럼/테이블 자동 추출이 부정확해질 수 있다(4장 참조).
- CRUD의 기본 행 클릭 편집 모달을 넘어서는 상세보기, 이중 편집 모달, 게시판 팝업, 미리보기 패널 등 화면별 특수 동작 — 기본 골격 생성 후 개발자가 직접 추가한다(`basic/notice` 수동 수정 예제 참조).

### 1.4 중요한 원칙 세 가지

1. **local 전용**. `@Profile("local")`이 붙어 있어 dev/prod에서는 빈 자체가 등록되지 않는다. 메뉴에 등록하지 않는 개발 도구이며, `application-local.yml`의 `sms.menu.auth.exclude-paths: /system/scaffold,/system/scaffold/**`로 local에서만 접근을 연다.
2. **한 번 생성한 뒤 개발자 소유**. scaffold는 고품질 출발점을 생성할 뿐, 생성된 코드가 그대로 완성이 아니다. 적용 후에는 개발자가 직접 수정하며 템플릿과 계속 동기화하지 않는다.
3. **재생성은 교체**. 재생성에는 병합이나 사용자 수정 보존 기능이 없다. 같은 경로로 다시 적용하는 것은 기존 수정본을 버리고 새 생성물로 교체하는 작업이다.

---

## 2. 접속과 화면 흐름

### 2.1 접속

1. local 프로필로 서버를 기동한다(서버 기동은 개발자가 직접 수행).
2. local 로그인 후 `http://localhost:8081/system/scaffold` 로 접근한다.
3. 세션이 없으면 로그인 페이지로 리다이렉트된다. 응답이 HTML 문자열로 돌아오면 F12 Console에서 최종 URL이 `/login`인지 확인한다.

### 2.2 API (참고)

화면 뒤에서 호출되는 endpoint는 다음 4개다. 직접 호출할 필요는 없지만 동작을 이해하는 데 도움이 된다.

| Method / URL | 요청 | 응답 | 역할 |
|---|---|---|---|
| `GET /system/scaffold` | — | HTML | 생성기 화면 |
| `POST /system/scaffold/analyze` | `{ rawQuery, targetTable }` | `columns`, `searchVars`, `targetTable`, `pkColumns`, `nullableColumns`, `columnComments`, `dbPlatform` | 쿼리 분석(컬럼/검색변수/테이블/PK/comment 추출) |
| `POST /system/scaffold/generate` | `ScaffoldRequestDTO` | `{ 파일명: 내용 }` Map | 산출물 코드 생성(탭 표시용) |
| `POST /system/scaffold/preview` | `ScaffoldRequestDTO` | 적용 미리보기 목록 | 파일별 신규/변경없음/덮어쓰기 표시 |
| `POST /system/scaffold/apply` | `ScaffoldRequestDTO` | 적용 결과 목록 | 실제 프로젝트 경로에 파일 저장. 기존 파일은 병합 없이 덮어쓸 수 있음 |

### 2.3 화면 버튼과 순서

```text
① 입력 카드 작성 (moduleName, domainId, domainClass, domainName, 조회 SQL, 정렬 컬럼, 화면 모드 …)
② [옵션 갱신]  → analyze 호출 → $변수+원본 컬럼 comment 기준 "조회조건 옵션", alias+DB comment 기준 "컬럼 옵션" 자동 구성
③ [생성]       → generate 호출 → 결과 카드에 파일별 탭 표시 (코드 검토)
④ [적용 미리보기] → preview 호출 → 파일별 [신규 파일]/[변경 없음]/[기존 파일 덮어쓰기] 표시
⑤ [적용]       → 최초 생성 파일임을 확인 → preview로 건수 확인 → apply 호출 → 실제 저장 + 메타 파일 저장
⑥ [현재 탭 복사] → 검토 중인 파일 내용을 클립보드에 복사
```

> **옵션 갱신을 먼저 누르는 이유**: 조회 SQL 안의 `$변수`와 SELECT alias를 서버가 추출해서 옵션 테이블을 만들어 준다. SQL을 고쳤다면 반드시 [옵션 갱신]을 다시 눌러 옵션 테이블을 최신화한다. [생성]을 누를 때 옵션 테이블이 비어 있으면 자동으로 한 번 갱신한다.

---

## 3. 조회 SQL 작성 가이드 (QueryColumnExtractor 동작)

이 장이 매뉴얼의 핵심이다. scaffold 결과의 정확도는 **입력 SQL을 얼마나 규약에 맞게 쓰느냐**에 달려 있다. 서버가 SQL을 어떻게 읽는지(`QueryColumnExtractor`) 알고 쓰면 실패를 미리 피할 수 있다.

### 3.1 표준 형태 (이 형태를 지키면 거의 항상 정상 동작)

```sql
SELECT A.SEND_DT,
       A.RECEIVER_NO,
       A.SEND_STATUS,
       COUNT(1) AS SEND_CNT
FROM SMS_HISTORY A
WHERE 1=1
  AND A.SEND_DT >= $start_dt
  AND A.RECEIVER_NO = $receiver_no
GROUP BY A.SEND_DT, A.RECEIVER_NO, A.SEND_STATUS
```

작성 규칙 요약:

| 규칙 | 이유 |
|---|---|
| **표현식·함수·CASE·서브쿼리 컬럼은 반드시 alias를 둔다** | 서버는 alias를 최우선으로 추출한다. 단순 컬럼 참조(`A.COL_NAME`)는 alias 없이도 `Column` 분기에서 정확히 추출되므로 불필요하다. |
| **`SELECT *`, `A.*` 와일드카드 금지** | `*`/`A.*`는 정적 파싱으로 컬럼을 풀지 못한다. `ConventionTest`에서도 `SELECT *`를 금지 규약으로 검증한다. |
| **검색조건은 `$변수`로 쓴다** | `$start_dt` → DTO 필드 `startDt` + 화면 input + XML `<if>` + `#{startDt}` 로 일괄 변환된다. |
| **`WHERE 1=1` 로 시작** | 동적 `<if>` 조건을 `AND ...`로 안전하게 덧붙이기 위한 관용 구조. |
| **결정적 정렬은 별도 `orderBy` 입력에 쓴다** | SQL 본문이 아니라 화면의 "정렬 컬럼" 입력란에 tie-breaker까지 포함해 입력한다(예: `A.SEND_DT DESC, A.HIST_ID DESC`). `orderBy`는 필수값이다. |

### 3.2 서버가 컬럼을 추출하는 순서 (`extractColumns`)

서버는 다음 순서로 SELECT 컬럼명(alias 우선)을 뽑는다.

1. **전처리(`safeParseQuery`)**: `#{...}`, `${...}`, `$변수`를 모두 `NULL`로 치환한다. 즉 `$변수`·MyBatis 바인딩은 파서를 깨뜨리지 않는다.
2. **JSQLParser 파싱**: 파싱에 성공하고 `PlainSelect` 구조이면, 각 SELECT 항목을 다음 우선순위로 추출한다.
   - ① **alias**가 있으면 alias(따옴표 제거) → 예: `COUNT(1) AS SEND_CNT` → `SEND_CNT`
   - ② alias가 없고 단순 컬럼(`Column`)이면 컬럼명 → 예: `A.SEND_DT` → `SEND_DT`
   - ③ 그 외 표현식이면 표현식의 **마지막 식별자**를 사용
3. **fallback(정규식)**: 위 파싱이 **예외로 실패하면** 정규식 `SELECT(.*?)\s+FROM\s`(대소문자 무시, 첫 매칭)로 SELECT 절을 잘라, 최상위 콤마로 분리한 뒤 각 항목의 마지막 토큰을 컬럼명으로 사용한다.

> **실무 결론**: 단순 컬럼 참조(`A.SEND_DT`)는 alias 없이도 2-② 경로로 정확히 추출된다. 함수·CASE·서브쿼리·계산식 컬럼만 alias를 붙이면 된다(2-①). alias 없는 표현식은 2-③ 또는 fallback에서 이름이 깨질 수 있다(4장). 100개 컬럼 전부 alias를 강제할 필요는 없다.

### 3.3 검색변수 추출 (`extractSearchVars`)

- 정규식 `\$([a-zA-Z0-9_]+)`로 `$변수`를 모두 찾아 **camelCase**로 변환한다. `map-underscore-to-camel-case` 설정과 일치한다.
  - `$base_dt` → `baseDt`, `$receiver_no` → `receiverNo`, `$status` → `status`
- 중복은 제거한다. 같은 변수를 여러 줄에 써도 DTO 필드는 하나다.
- `$변수`가 하나도 없으면 화면 검색조건은 기본값 `searchKeyword` 1개로 생성된다.
- **날짜 타입 자동 판정**: 변수명이 `date`, `dt`, `at`으로 **끝나면** 날짜 입력(DatePicker)으로 생성한다. `sendType`처럼 중간에 `dt` 글자가 들어간 필드는 텍스트로 생성한다.
- **날짜 범위 자동 묶음**: `BETWEEN $start_dt AND $end_dt`처럼 쓰면 `~` 끝 변수(`endDt`)를 범위 끝으로 인식해 HTML에서 `from ~ to` DatePicker 쌍으로 묶는다.

### 3.4 CRUD 기준 테이블 추출 (`extractPrimaryTable`)

- `FROM`의 첫 항목이 **테이블**이면 그것을 기준 테이블로 잡는다. 스키마 접두는 유지, 따옴표 제거, 대문자 정규화.
  - `FROM SMS.SMS_HISTORY A` → `SMS.SMS_HISTORY`
  - `FROM SMS_HISTORY A` → `SMS_HISTORY`
- `FROM`이 서브쿼리(인라인 뷰)이거나 구조를 잡지 못하면 빈 문자열을 반환한다. **이 경우 CRUD는 `targetTable`을 직접 입력해야 한다**(5장, 4장 참조).
- 조인 화면은 첫 테이블이 수정 대상과 다를 수 있으므로 **CRUD에서는 `targetTable`을 명시하는 것이 안전**하다.

### 3.5 타입 추론 (ColumnTypeInferrer) — "실제 DB 근거 우선"

컬럼의 Java 타입은 추측이 아니라 **실제 DB 메타데이터**로 결정한다.

1. 원쿼리를 빈 결과 쿼리로 감싼다. Oracle 기준 `SELECT * FROM (원쿼리) WHERE ROWNUM = 0`.
2. 실행 전 MyBatis 태그(`<if>` 등) 제거, 바인딩/`$변수` → `NULL` 치환, 외톨이 `AND`/`OR` 줄 제거, `WHERE AND|OR` → `WHERE 1=1` 보정을 거친다.
3. `ResultSetMetaData`로 컬럼 라벨을 읽어 추출된 컬럼명과 매칭(대소문자 무시)해 타입을 정한다.

주요 타입 매핑(`ScaffoldDialect.javaType`):

| DB 타입(조건) | Java 타입 |
|---|---|
| `NUMBER`/`NUMERIC`/`DECIMAL` (scale > 0) | `BigDecimal` |
| `NUMBER` (precision > 9, scale=0) | `Long` |
| `NUMBER` (precision ≤ 9, scale=0), `INTEGER` 계열 | `Integer` |
| `DATE` | `LocalDate` |
| `TIMESTAMP` 계열 | `LocalDateTime` |
| `CLOB`/`NCLOB`/`LONGVARCHAR` | `String` |
| `BLOB`/`BINARY` 계열 | `byte[]` |
| 그 외/알 수 없음 | `String` |

> **중요**: 타입 추론이 **실패하면 String으로 강행하지 않고 오류를 보고**한다. `컬럼 타입 추론에 실패했습니다. 쿼리를 확인하세요: ORA-XXXXX ...` 메시지가 뜨면 쿼리 자체를 수정해야 한다(4.2절, 8장).

### 3.6 DB 방언 (db-platform)

타입 추론·페이징·현재시각·날짜 변환 SQL은 `application.yml`의 `sms.scaffold.db-platform` 값으로 결정된다. 자동 fallback하지 않는다.

| 값 | 현재시각 | 페이징 | 빈 결과 쿼리 |
|---|---|---|---|
| `oracle` (기본) | `SYSTIMESTAMP` | `OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY` | `SELECT * FROM (q) WHERE ROWNUM = 0` |
| `postgres` | `CURRENT_TIMESTAMP` | `OFFSET #{offset} LIMIT #{size}` | `SELECT * FROM (q) scaffold_src WHERE 1 = 0` |
| `db2` | `CURRENT TIMESTAMP` | `OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY` | `SELECT * FROM (q) scaffold_src FETCH FIRST 0 ROWS ONLY` |
| `mssql` (2012+) | `SYSDATETIME()` | `OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY` | `SELECT * FROM (q) scaffold_src WHERE 1 = 0` |

허용되지 않는 값이면 `Unsupported sms.scaffold.db-platform: ... (allowed: oracle, postgres, db2, mssql)` 오류가 난다. 이 프로젝트의 기본 운영 DB는 Oracle이다.

---

## 4. 추출 실패 / 주의 SQL 패턴 (에러 패턴)

`QueryColumnExtractor`는 "JSQLParser 우선, 실패 시 정규식 fallback" 구조다. 그래서 **완전히 실패하기보다, 조용히 부정확한 결과를 내는 경우**가 더 위험하다. 아래 패턴을 알고 피한다.

### 4.1 컬럼 추출이 깨지는 패턴

#### (a) alias 없는 함수 / Oracle 전용 함수

```sql
-- 나쁨: NVL 표현식의 "마지막 식별자"를 잡아 SEND_CNT 자리에 'FAIL' 같은 쓰레기 이름이 들어갈 수 있음
SELECT NVL(A.RESULT_MSG, 'OK,FAIL'), COUNT(1)
FROM SMS_HISTORY A

-- 좋음: alias 부여
SELECT NVL(A.RESULT_MSG, 'OK') AS RESULT_MSG,
       COUNT(1) AS SEND_CNT
FROM SMS_HISTORY A
```

`NVL`, `DECODE`, `TO_CHAR`, `TO_DATE`, `SUBSTR`, `TRIM` 등 함수 컬럼은 **반드시 alias**를 붙인다. 서버 테스트에서 `NVL(A.RESULT_MSG, 'OK,FAIL') AS RESULT_MSG`가 정상 추출됨을 확인했다.

#### (b) alias 없는 CASE

```sql
-- 나쁨: CASE ... END의 마지막 식별자가 'END'로 잡혀 컬럼명이 END가 됨
SELECT CASE WHEN A.STATUS = '1' THEN 'Y' ELSE 'N' END
FROM TB_A A

-- 좋음
SELECT CASE WHEN A.STATUS = '1' THEN 'Y' ELSE 'N' END AS STATUS_YN
FROM TB_A A
```

#### (c) alias 없는 스칼라 서브쿼리

```sql
-- 나쁨: 서브쿼리 문자열의 마지막 토큰이 컬럼명으로 잡힘
SELECT (SELECT COUNT(1) FROM SMS_HISTORY X WHERE X.REQUEST_ID = A.REQUEST_ID)
FROM SMS_HISTORY A

-- 좋음: alias 부여 (서버 테스트에서 RETRY_CNT 정상 추출 확인)
SELECT (SELECT COUNT(1) FROM SMS_HISTORY X WHERE X.REQUEST_ID = A.REQUEST_ID) AS RETRY_CNT
FROM SMS_HISTORY A
```

#### (d) `SELECT *` / `A.*` 와일드카드

```sql
-- 금지: * 와일드카드는 정적 파싱으로 컬럼을 풀지 못함. ConventionTest에서도 차단.
SELECT * FROM SMS_HISTORY A

-- 금지: A.* 도 동일하게 실패. AllTableColumns는 Column이 아니라 lastIdentifier가 "A"를 반환.
SELECT A.* FROM (SELECT AA, BB, CC FROM TBL) A

-- 좋음: 컬럼을 명시 나열 (단순 참조는 alias 불필요)
SELECT A.SEND_DT, A.RECEIVER_NO, A.SEND_STATUS FROM SMS_HISTORY A

-- 좋음: 표현식만 alias
SELECT A.SEND_DT, NVL(A.RESULT_MSG, 'OK') AS RESULT_MSG FROM SMS_HISTORY A
```

`columns`가 비면 생성 시 `rawQuery에서 SELECT 컬럼을 추출하지 못했습니다. 쿼리를 확인하세요.` 오류가 난다.

#### (e) `UNION` / `UNION ALL` / `INTERSECT` / `MINUS`

```sql
-- 주의: 집합 연산은 PlainSelect가 아니라 JSQLParser 파싱이 실패 → 정규식 fallback으로 빠짐
SELECT A.ID, A.NAME FROM TBL_A A
UNION
SELECT B.ID, B.NAME FROM TBL_B B
```

fallback 정규식은 **첫 번째 `SELECT ... FROM` 구간만** 잡는다. 양쪽 컬럼이 동일하면 우연히 맞지만, 가지 수가 다르거나 첫 SELECT와 최종 결과 컬럼이 다르면 **잘못된 컬럼이 추출**된다. 서버 테스트도 UNION은 "비어있지 않다"까지만 보증한다.

**권장**: scaffold 입력 쿼리에서는 `UNION`을 피한다. 집합이 꼭 필요하면 DB에 **VIEW**를 만들고 그 VIEW를 상대로 scaffold를 돌리거나, 단일 `SELECT` + `JOIN`/`CASE`로 재구성한다.

#### (f) `WITH` (CTE)

```sql
-- 주의: WITH cte AS (SELECT A.ID, A.NAME FROM TBL_A A) SELECT ID, NAME FROM cte
```

CTE도 파싱 경로가 불안정해 fallback으로 빠질 수 있고, fallback은 **CTE 안쪽의 첫 `SELECT ... FROM`**을 잡는다. CTE 내부 컬럼과 바깥 SELECT 컬럼이 다르면 잘못된 컬럼이 추출된다.

**권장**: CTE 대신 VIEW 또는 인라인 서브쿼리 + alias로 재구성한다. 부득이 CTE를 쓰면 [옵션 갱신] 후 컬럼 옵션 테이블에 의도한 컬럼이 정확히 나왔는지 **반드시 눈으로 확인**한다.

#### (g) `FROM`에 인라인 뷰(서브쿼리)

```sql
-- 컬럼 추출은 alias 덕분에 동작하지만, CRUD 기준 테이블 추론이 빈 값이 됨
SELECT X.ID, X.NAME FROM (SELECT A.ID, A.NAME FROM TB_A A) X
```

`extractPrimaryTable`은 `FROM`이 테이블이 아니면 빈 값을 반환한다. **LIST/EXCEL은 그대로 가능**하지만, **CRUD는 `targetTable`을 직접 입력**해야 한다.

### 4.2 타입 추론이 실패하는 패턴 (하드 실패)

타입 추론은 쿼리를 **실제 DB에서 실행**한다. 따라서 "쿼리가 local Oracle에서 그대로 실행 가능한가"가 기준이다. 실패하면 String으로 덮지 않고 다음 메시지로 막는다.

```text
컬럼 타입 추론에 실패했습니다. 쿼리를 확인하세요: ORA-XXXXX ...
```

대표 원인:

| 원인 | 대표 ORA | 조치 |
|---|---|---|
| 없는 테이블/뷰 | `ORA-00942` | 테이블명·스키마 접두·접근 권한 확인 |
| 없는 컬럼 | `ORA-00904` | 컬럼명 철자, alias 오타 확인 |
| Oracle이 아닌 문법(다른 DB 전용 함수 등) | 구문 오류 | `db-platform`에 맞는 문법 사용 |
| `$변수` → `NULL` 치환 후 비문법이 되는 구조 | 구문 오류 | 치환 후에도 유효한 SQL이 되도록 재구성 |
| GROUP BY 없는 집계, SELECT 절 비일치 | `ORA-00937` | GROUP BY 보정 |

> **점검 팁**: 입력 SQL을 메모장에서 `$변수`를 모두 `NULL`로 치환한 뒤 그대로 실행해 본다. 에러 없이 0건이라도 결과셋 메타가 나오면 타입 추론은 통과한다.

`$변수` 치환은 대개 안전하다. `A.COL = $x` → `A.COL = NULL`, `IN ($x)` → `IN (NULL)`, `BETWEEN $a AND $b` → `BETWEEN NULL AND NULL`, `TO_DATE($d, 'YYYYMMDD')` → `TO_DATE(NULL, 'YYYYMMDD')` 모두 유효 SQL이다. 문제는 치환 자체가 아니라 **원쿼리의 문법 오류**다.

### 4.3 CRUD 검증에서 막히는 패턴 (ScaffoldService)

`screenMode=CRUD`(또는 legacy `includeCreateUpdate=true`)면 생성 전에 다음 검증을 통과해야 한다. 메시지 원문과 함께 싣는다.

| 조건 | 오류 메시지 |
|---|---|
| `targetTable` 없음 | `CRUD mode requires targetTable.` |
| 대상 테이블에 실제 PK 없음 | `CRUD mode requires a real primary key. Table has no PK: {table}` |
| 조회 결과에 PK 컬럼 없음 | `CRUD query must include PK column: {col}` |
| 조회 결과에 lock 컬럼 없음 | `CRUD query must include lock column: {col}` |
| lock 컬럼이 대상 테이블에 없음 | `Lock column does not exist in target table: {col}` |
| lock 컬럼을 PK로 지정 | `Optimistic lock column must not be a PK column: {col}` |

핵심 원칙:

- **PK는 실제 DB 메타데이터 기준**이다. 단일 PK·복합 PK 모두 `pkColumns`로 다룬다. 임의 `_ID` 컬럼을 PK처럼 추정하지 않는다.
- **PK가 없는 테이블은 CRUD 생성이 막힌다.** LIST/EXCEL 조회 전용으로만 생성한다.
- **조회 SQL 결과에 모든 PK 컬럼이 있어야** 한다. 복합 PK면 전부가 SELECT에 있어야 한다.
- **lock 컬럼**은 조회 결과와 대상 테이블 메타 양쪽에 있어야 하고, PK는 lock으로 쓸 수 없다. nullable lock 컬럼은 null-safe WHERE 조건으로 생성된다.

### 4.4 지원되지 않는 screenMode

`screenMode`는 `LIST`/`EXCEL`/`CRUD` 3종만 지원한다. 그 외 값은 렌더러에서 다음 메시지로 거부한다.

```text
지원하지 않는 screenMode입니다: {값} (지원: LIST, EXCEL, CRUD)
```

---

## 5. 옵션 계약 (Options Contract)

서버 입력 DTO는 `ScaffoldRequestDTO`이다. 화면에서 채우는 값이 그대로 이 DTO에 실린다. **이 장의 기본값·제약은 코드 기준 실제 동작**이다.

### 5.1 기본 입력 (ScaffoldRequestDTO)

| 옵션 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `moduleName` | String | **예** | — | 패키지/경로 도메인명. `^[a-z][a-z0-9]*$` (영문 소문자+숫자, 소문자 시작). 예: `sms` |
| `domainId` | String | **예** | — | URL/파일명(kebab-case). `^[a-z][a-z0-9-]*(/[a-z][a-z0-9-]*)?$`. **내부 슬래시 1개 허용**(3단계 URL, 예: `sms/register` → `/campaign/sms/register`). 예: `history` |
| `domainClass` | String | **예** | — | 클래스 접두(PascalCase). `^[A-Z][A-Za-z0-9]*$`. 예: `SmsHistory`. 기존 화면을 교체할 때는 약어를 포함한 대소문자를 기존 클래스명과 정확히 맞춘다 (`Smshistory`와 `SmsHistory`는 다른 이름) |
| `domainName` | String | **예** | — | 화면 한글명. 예: `발송이력조회` |
| `rawQuery` | String | **예** | — | 조회 SQL 원문 + `$변수` 검색조건 규약 |
| `orderBy` | String | **예** | — | 결정적 정렬 컬럼(tie-breaker 포함). 예: `A.SEND_DT DESC, A.HIST_ID DESC` |
| `screenMode` | String | 아니오 | `LIST`(또는 legacy boolean에서 유도) | `LIST` / `EXCEL` / `CRUD`. **명시하면 이 값이 권위**를 가진다 |
| `includeCreateUpdate` | boolean | 아니오 | `false` | **legacy**. `screenMode`가 비어 있을 때만 fallback으로 동작. `CRUD`면 무시 |
| `includeExcel` | boolean | 아니오 | `false` | **legacy**. `screenMode`가 비어 있을 때만 fallback. `EXCEL`면 무시 |
| `includePrivacy` | boolean | 아니오 | `false` | 개인정보 포함. `@PrivacyLog` + 마스킹 + `/unmask` endpoint 생성 |
| `showRowNumber` | boolean | 아니오 | `true` | 그리드 행 번호(No). `true` → `rowHeaders: ['rowNum']`, `false` → `[]` |
| `targetTable` | String | CRUD **예** | `FROM` 첫 테이블 추론 | 수정 대상 테이블. 조인/서브쿼리면 명시 권장 |
| `pkColumn` | String | 아니오 | — | **legacy 단일 PK**. `pkColumns`가 비어 있을 때만 사용 |
| `pkColumns` | List\<String\> | 아니오 | CRUD: DB 메타 PK | PK 컬럼 목록(단일/복합). 미입력 시 실제 DB 메타데이터 PK를 기본 사용 |
| `lockColumn` | String | 아니오 | —(UI가 `UPD_DTTM`/`UPDATE_DTTM` 추측) | 낙관적 잠금 컬럼 |
| `searchParamOptions` | List | 아니오 | `[]` | 검색 파라미터별 옵션 (5.2) |
| `columnOptions` | List | 아니오 | `[]` | 컬럼별 옵션 (5.3) |
| `menuOption` | Object | 아니오 | 기본값 객체 | 메뉴 등록 옵션 (5.4) |

> **screenMode 권위 규칙**: `screenMode`에 값이 있으면 `includeCreateUpdate`/`includeExcel` boolean은 모순돼도 무시된다. `screenMode`가 비어 있을 때만 legacy boolean으로 `CRUD`/`EXCEL`/`LIST`를 유도한다(하위 호환). 화면 UI는 `screenMode` 선택에 따라 두 boolean을 자동으로 맞춰 보낸다.

### 5.2 조회조건 옵션 (`searchParamOptions` / ScaffoldSearchParamOptionDTO)

`$변수`마다 한 행씩 생성된다. [옵션 갱신]을 누르면 analyze 결과의 `searchVars`와 `searchParamLabels`로 테이블이 자동 구성된다. 직접 검색조건 컬럼은 DB comment를 화면명 기본값으로 사용하고, comment가 없으면 SELECT alias 또는 검색 변수명으로 fallback한다. 화면명은 생성 전에 직접 수정할 수 있으며, 전부 지우고 포커스를 빼면 DB comment/alias 기본값으로 복원된다.

| 필드 | 타입 | 기본값 | 허용값 / 설명 |
|---|---|---|---|
| `name` | String | — | 검색 변수명(camelCase). analyze가 자동 채움 |
| `label` | String | DB column comment → SELECT alias/검색 변수명 | 생성 화면의 조회조건 라벨과 `aria-label`. 직접 수정 가능 |
| `inputType` | String | 변수명이 `date/dt/at`으로 끝나면 `DATE`, 아니면 `TEXT` | `TEXT`(텍스트) / `DATE`(날짜, Toast UI DatePicker) / `SELECT`(콤보) / `RADIO`(라디오) |
| `defaultValue` | String | `NONE` | `NONE`(없음) / `TODAY`(오늘) / `YESTERDAY`(어제) / `RECENT_7_DAYS`(최근 7일) / `THIS_MONTH`(이번 달) / `CURRENT_MONTH_TO_TODAY`(현재월 1일~오늘) |
| `optionsText` | String | — | `SELECT`/`RADIO`의 항목. `값:라벨,값:라벨` 형식. 예: `SMS:SMS,LMS:LMS`. `:` 또는 `=` 구분, 라벨 생략 시 값=라벨 |

> 날짜 검색조건은 native `type="date"`가 아니라 Toast UI DatePicker(`data-search-type="date"` + `{field}PickerLayer`)로 생성된다. `xxxFrom/xxxTo`, `startX/endX`, `fromX/toX` 쌍과 `BETWEEN $a AND $b`는 `from ~ to` 묶음으로 표시된다. `BETWEEN`의 두 변수는 각각 `시작일자`, `종료일자`를 기본 화면명으로 사용한다.

### 5.3 컬럼 옵션 (`columnOptions` / ScaffoldColumnOptionDTO)

SELECT alias마다 한 행씩 생성된다. 컬럼명 매칭은 대문자 정규화 기준이다. DB comment가 있는 직접 컬럼은 comment를 화면명 기본값으로 사용하며, 사용자가 생성 전에 수정할 수 있다. alias가 있어도 원본 직접 컬럼을 추적하지만 계산식·집계식은 원본을 확정하지 않고 alias/컬럼명으로 fallback한다.

| 필드 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `columnName` | String | — | 대상 컬럼(alias). analyze가 자동 채움 |
| `visible` | boolean | 일반 컬럼 `true`, 실제 PK `false` | 그리드 표시. `false`여도 조회 row에는 남아 수정/삭제 식별값으로 사용 가능 |
| `modalVisible` | boolean | 일반 컬럼 `true`, 실제 PK `false` | CRUD 등록·수정 화면 표시. `false`면 모달에 미표시(PK/lock 값은 hidden 보관 가능) |
| `editable` | boolean | 자동 후보 판정 | CRUD 등록·수정 입력 허용. `true` → input + `UpdateRequestDTO` + XML `INSERT/UPDATE` 대상. **CRUD 모드에서만 의미** |
| `headerName` | String | DB column comment → alias/컬럼명 | 그리드 헤더와 CRUD 등록·수정 모달에 공통 적용하는 화면명. 직접 수정 가능 |
| `width` | Integer | `150` | 그리드 컬럼 너비(UI 최소 60) |
| `align` | String | `center` | `center` / `left` / `right` |
| `dateFormat` | String | 날짜 컬럼 `AUTO`, 그 외 `NONE` | `AUTO`(자동) / `NONE`(없음) / `DATE`(`YYYY-MM-DD`) / `DATETIME`(`YYYY-MM-DD HH:mm`) / 임의 포맷 문자열 |
| `maskType` | String | `NONE` | `NONE` / `PHONE` / `NAME` / `RRN` / `EMAIL` (백엔드는 `BIRTH`, `CARD`, `BIZNO` 등도 매핑) |
| `inputMask` | String | `""` | 입력 마스크 |
| `validate` | String | `""` | `required` 포함 시 서버 `@NotBlank`(String) / `@NotNull`(비-String) 부여 |
| `optionsText` | String | — | 수정 가능 컬럼의 콤보 항목 (`값:라벨,...`) |

**`editable` 자동 후보 판정 규칙** (옵션을 명시하지 않았을 때):

- 다음 컬럼은 **절대 editable이 되지 않는다**(protected): PK 컬럼, `lockColumn`, `REG_ID`, `REG_DTTM`, `UPD_ID`, `UPD_DTTM`, `REQUEST_ID`.
- 기본 후보에서 제외: `_ID`로 끝나는 컬럼, `CREATED_AT`, `UPDATED_AT`.
- 그 외 컬럼은 기본적으로 editable 후보로 체크된다.

> **화이트리스트 원칙**: CRUD 수정 요청은 `pkColumns` + `before{LockColumn}` + `editable=true` 컬럼만 다룬다. VO 전체 컬럼을 update 요청으로 쓰지 않는다. 프론트 update payload도 `editable=true` 컬럼만 전송하며, Mapper XML `UPDATE SET`도 `editable=true` 컬럼만 생성한다.

> 등록과 수정은 현재 동일한 `editable` 화이트리스트를 사용한다. 등록 전용/수정 전용 필드가 서로 달라야 하는 업무는 최초 생성 후 개발자가 DTO·폼·Service 계약을 분리한다.

#### 마스킹 타입 → 메서드 매핑 (백엔드)

| `maskType` | 호출 메서드 |
|---|---|
| `name`, `nm` | `maskName` |
| `email` | `maskEmail` |
| `birth`, `birthdate`, `birth_date` | `maskBirthDate` |
| `rrn`, `ssn` | `maskRrn` |
| `card`, `bizno` | `maskCard` |
| 그 외(`phone` 포함 기본) | `maskPhone` |

`includePrivacy=true` + `maskType` 지정 시 목록/엑셀에서 `MaskingUtil` 실제 호출이 생성되고, 원문 조회용 `/unmask` endpoint + `getUnmaskedDetail` + `@PrivacyLog`가 함께 생성된다.

#### formatter는 scaffold 옵션이 아니다 (주의)

과거 문서·요구사항에서 언급되는 그리드 셀 `formatter`(배지, 색상, 조건부 강조 등)는 **scaffold 입력 옵션에 없다**. `columnOptions`에는 `dateFormat`까지만 있고 `formatter` 필드는 존재하지 않는다.

그리드 셀 포매터는 **생성 후 개발자가 생성된 `{domainId}.js`의 컬럼 정의에 직접 추가**한다. `TuiCommon.badgeByValue({ labels, tones })`, `TuiCommon.fmt.date` 등 사용법은 `docs/base/grid-formatter-guide.md`를 참조한다. (예: 코드값별 자동 색상 배지는 `badgeByValue`, 날짜 표시는 `fmt.date`.)

### 5.4 메뉴 등록 옵션 (`menuOption` / ScaffoldMenuOptionDTO)

| 필드 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `menuId` | String | `{MODULE}_{DOMAIN}` (대문자, `-`·`/` → `_`) | 메뉴 ID. 예: `sms` + `history` → `SMS_HISTORY` |
| `parentMenuId` | String | `/* TODO: 상위 메뉴 ID */` | 상위 메뉴 ID. 미입력 시 SQL에 TODO 주석으로 남는다 |
| `roleCode` | String | `ROLE_ADMIN` | 권한 부여 대상 역할 |
| `sortOrd` | Integer | `99` | 정렬 순서 |

메뉴 SQL은 v3 스키마(`TB_MENU.MENU_ID/PARENT_MENU_ID`, `TB_MENU_AUTH.ROLE_CD` + `CAN_*` 8종)로 생성되며, `screenMode`에 따라 최소 권한이 결정된다.

| `CAN_*` | 부여 조건 |
|---|---|
| `CAN_READ` | 항상 (목록 조회) |
| `CAN_CREATE` / `CAN_UPDATE` / `CAN_DELETE` | `CRUD` 모드 |
| `CAN_DOWNLOAD` | `EXCEL` 모드 |
| `CAN_MASK_VIEW` | `includePrivacy=true` |

> 메뉴/권한 테이블의 전체 설계는 `docs/base/menu-authority-table-design.md`, seed 정책은 `docs/base/menu-source-policy.md`를 참조한다.

---

## 6. 생성 파일 구조와 각 파일의 역할

### 6.1 파일별 역할

| 파일 | 역할 |
|---|---|
| `{Domain}SearchRequestDTO.java` | 검색 요청 DTO. `PageRequestDTO` 상속, Lombok `@Data`. `$변수`가 필드로 생성되고 `request.validate()` 호출 계약 |
| `{Domain}UpdateRequestDTO.java` | **CRUD만**. 수정 요청 DTO(화이트리스트). `pkColumns` + `before{LockColumn}` + `editable=true` 컬럼만 선언. `validate=required`면 `@NotBlank`/`@NotNull` |
| `{Domain}VO.java` | 조회 결과 VO. Lombok `@Data`. SELECT alias가 필드로 생성. `maskType` 컬럼은 마스킹 적용 대상 |
| `{Domain}Mapper.java` | MyBatis Mapper interface |
| `{Domain}Mapper.xml` | SQL. `baseQuery` include(DRY), `searchConditions` 공통화, 쿼리 시그니처(`/* Mapper.method */`), 페이징 `pageClause`. 직접 컬럼 기반 단순 SELECT는 검색조건을 내부에 배치하고, 그중 단일 테이블만 직접 COUNT를 생성한다. 복합 쿼리는 외부 검색조건/래퍼 COUNT를 유지한다. CRUD면 `INSERT/UPDATE/DELETE` 추가. 엑셀용 `selectListForExcel`은 `resultType=HashMap`(Map 반환, ExcelUtil 계약상 예외) |
| `{Domain}Service.java` | 업무 로직. 마스킹 호출, `// TODO` 업무 규칙 검증 자리, privacy 시 `getUnmaskedDetail` |
| `{Domain}Controller.java` | endpoint. `screenMode`·옵션에 따라 `/data`, `/create`, `/update`, `/delete`, `/excel`, `/unmask` 조합 |
| `{Domain}ServiceTest.java` | Service 단위 테스트. Mapper를 Mockito mock, given/when/then |
| `{Domain}ControllerTest.java` | Controller 테스트. MockMvc, `ApiResponse` 포맷·`@Valid` 실패 검증 |
| `{domainId}.html` | 화면. `screen-convention.md` 카드 골격, `layout:decorate="~{defaultLayout}"`, lucide 로컬 아이콘(`data-lucide`, CDN 금지) |
| `{domainId}.js` | 화면 JS. `TuiPageBuilder`(그리드/페이징), `ApiClient`(통신), `FormBinder`(폼 바인딩), `ModalManager`(CRUD 모달). `rowHeaders`는 `showRowNumber`에 따라 결정 |
| `메뉴등록.sql` | v3 메뉴/권한 seed. 입력한 `menuOption` 반영 |

### 6.2 적용 경로 (`apply` 시 저장 위치)

`미리보기`와 `적용`은 같은 요청을 서버가 다시 생성한 뒤 아래 경로를 계산한다. `domainId`에 슬래시가 있으면(`sms/register`) HTML/JS는 `templates/{moduleName}/sms/register.html`처럼 한 단계 더 깊은 경로에 저장된다.

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

적용은 **local profile에서만** 가능하고, `moduleName`·`domainId`·`domainClass`는 경로 오염 방지를 위해 패턴 검증을 통과해야 한다. 적용 경로는 프로젝트 루트를 벗어날 수 없도록 차단된다. 또한 기존 산출물과 파일명이 대소문자만 다르면 macOS 같은 대소문자 비구분 파일시스템에서 다른 `public` 타입이 기존 파일을 덮어쓸 수 있으므로 미리보기부터 적용을 거부한다.

### 6.3 적용 상태 라벨

미리보기/적용 결과에서 각 파일은 다음 상태 중 하나를 가진다.

| status | statusLabel | 의미 |
|---|---|---|
| `NEW` | 신규 파일 | 해당 경로에 파일이 없음 → 새로 생성 |
| `UNCHANGED` | 변경 없음 | 기존 파일과 생성 내용이 동일 → 스킵 |
| `OVERWRITE` | 기존 파일 덮어쓰기 | 기존 파일과 내용이 다름 → 생성 내용으로 덮어씀 |

> **덮어쓰기 주의**: `적용`은 파일 단위 병합을 하지 않는다. `OVERWRITE`는 개발자가 작성한 업무 로직과 UI도 사라질 수 있다는 뜻이다. 개발자 소유로 전환된 화면에는 갱신 목적으로 다시 적용하지 않는다.

### 6.4 메타 파일과 의도적인 재생성

`apply` 시 `src/main/resources/scaffold-cases/{module}_{domainId}.json` 메타 파일이 자동 저장된다. 이 파일에는 요청 + columns + searchVars + typeMap + dialect가 들어 있다.

메타 파일은 **최초 생성 입력을 재현하기 위한 기록**이다. 개발자가 수정한 소스를 템플릿과 계속 동기화하거나, 수정 내용을 보존해 재생성하는 파일이 아니다.

```bash
# 파일을 쓰지 않고 현재 case와 산출물 차이만 확인
mvn compile exec:java \
  -Dexec.mainClass=com.scbk.sms.service.system.scaffold.ScaffoldRegenerateMain \
  -Dexec.args=--dry-run
```

`ScaffoldRegenerateMain`을 인자 없이 실행하면 모든 활성 case의 산출물을 병합 없이 덮어쓴다. 테스트·데모 화면뿐 아니라 개발자 수정 화면도 기존 수정본을 버리고 새 생성물로 교체하기로 명시적으로 결정했다면 사용할 수 있다. 단, 활성 case 중 하나라도 보존해야 한다면 전체 실행하면 안 된다.

`ConventionTest`는 공통 규약을 검사할 뿐, 개발자 소유 화면과 현재 `.tpl`의 완전한 동일성을 요구하지 않는다. 자세한 소유권과 재생성 기준은 `docs/base/scaffold-contract.md`를 따른다.

---

## 7. 생성 후 체크리스트

생성된 코드는 그대로 완료가 아니다. `screen-generation-guide.md` 절차에 편입해 다음을 수행한다.

### 7.1 생성 전 (절차서 1~2단계)

- [ ] 대상 화면과 도메인 경계를 확정한다 (`docs/base/domain-boundary-guide.md`).
- [ ] 실제 테이블 구조·PK·컬럼명을 확인한다 (CRUD면 특히 PK 존재 여부).
- [ ] 조회 SQL을 local Oracle에서 `$변수` → `NULL` 치환 후 실행해 0건이라도 메타가 나오는지 확인한다.

### 7.2 생성 직후 검토

- [ ] [옵션 갱신] 후 **컬럼 옵션 테이블**의 alias와 DB comment 기반 화면명이 정확한지 확인 (특히 함수/CASE/서브쿼리/UNION/CTE를 쓴 경우).
- [ ] **조회조건 옵션 테이블**의 `$변수` 목록·DB comment/alias 기반 화면명·입력 타입(날짜 자동 판정 포함)을 확인하고, `BETWEEN`은 시작일자/종료일자로 표시되는지 본다.
- [ ] `screenMode`가 의도와 일치하는지 (LIST/EXCEL/CRUD).
- [ ] CRUD: `targetTable`, `pkColumns`, `lockColumn`, 그리드/모달 표시 및 `editable` 컬럼이 맞는지 확인.
- [ ] 개인정보 화면: `includePrivacy` + 컬럼별 `maskType` 지정, 마스킹 TODO 보정.
- [ ] [적용 미리보기]로 신규/덮어쓰기 건수를 확인한 뒤 적용.

### 7.3 적용 후 보완

- [ ] 이 시점부터 생성 파일을 개발자 소유로 관리하고 템플릿 자동 동기화 대상으로 취급하지 않는다.
- [ ] Service의 `// TODO` 자리에 업무 규칙 추가 (등록 전 중복 체크·필수값 보정, 수정 전 상태 전이·권한 확인 등).
- [ ] 필요 시 컬럼·검색조건·화면 레이아웃 직접 수정.
- [ ] 그리드 셀 포매터(배지/색상/강조)가 필요하면 생성 JS에 직접 추가 (`docs/base/grid-formatter-guide.md`).
- [ ] scaffold 기본 산출물에 없는 상세보기/팝업/이중 편집이 필요하면 화면 요구사항에 맞게 직접 구현 (`basic/notice` 예제 참조).
- [ ] `parentMenuId`가 TODO 주석으로 남아 있으면 실제 상위 메뉴 ID로 보정.
- [ ] 메뉴등록.sql을 실행해 메뉴/권한을 부여한다 (URL 접근은 `MenuAuthInterceptor`가 담당).

### 7.4 검증 (완료 기준)

```bash
mvn test
mvn -DskipTests package
```

- [ ] 두 명령 모두 통과해야 완료. 미실행 상태는 부분 완료다.
- [ ] 생성된 ServiceTest/ControllerTest에 `// TODO` 업무 규칙 테스트를 채운다.
- [ ] 절차서 6~7단계(메뉴·권한 연결과 검증)를 마무리한다.

---

## 8. 자주 하는 실수와 트러블슈팅

### 8.1 증상 → 원인 → 조치

| 증상 | 원인 | 조치 |
|---|---|---|
| `rawQuery에서 SELECT 컬럼을 추출하지 못했습니다.` | `SELECT *` 또는 `A.*` 와일드카드 사용, 또는 SELECT 절을 전혀 인식 못함 | 컬럼을 명시 나열. `*`/`A.*` 금지 |
| 컬럼 옵션 테이블에 쓰레기 이름(`END`, `FAIL`, `A` 등)이 나옴 | alias 없는 CASE/함수/서브쿼리, 또는 `A.*` 사용 | 표현식 컬럼에 alias 부여, 와일드카드 제거 후 [옵션 갱신] |
| UNION/CTE인데 컬럼이 일부만/잘못 나옴 | fallback 정규식이 첫 `SELECT...FROM`만 잡음 | VIEW/단일 SELECT로 재구성, 또는 생성 결과 눈검증 |
| `컬럼 타입 추론에 실패했습니다. ... ORA-XXXXX` | 쿼리가 local Oracle에서 실행 불가 | `$변수`→`NULL` 치환 후 실행해 ORA 원인 제거 (8.2) |
| `CRUD mode requires targetTable.` | CRUD인데 `targetTable` 미입력(추론 실패) | 수정 대상 테이블을 직접 입력 |
| `CRUD mode requires a real primary key. Table has no PK: {t}` | 대상 테이블에 실제 PK 없음 | LIST/EXCEL로 생성하거나 PK 있는 테이블로 재설계 |
| `CRUD query must include PK column: {c}` | SELECT 결과에 PK 컬럼 누락 | SELECT에 PK 컬럼(복합 PK면 전부) 추가 |
| `Optimistic lock column must not be a PK column: {c}` | lock 컬럼을 PK로 지정 | lock은 PK가 아닌 컬럼(예: `UPD_DTTM`)으로 지정 |
| `Lock column does not exist in target table: {c}` | lock 컬럼이 대상 테이블에 없음 | 실제 테이블에 있는 컬럼으로 지정 |
| `지원하지 않는 screenMode입니다: {값}` | LIST/EXCEL/CRUD 외 값 | 3종 중 하나로 수정 |
| `Unsupported sms.scaffold.db-platform: {값}` | 허용되지 않는 DB 방언 | `oracle`/`postgres`/`db2`/`mssql` 중 사용 |
| `scaffold 적용 입력값이 올바르지 않습니다: {field}` | moduleName/domainId/domainClass 패턴 위반 | 5.1의 정규식 규칙 준수 |
| `scaffold 적용 경로가 프로젝트 루트를 벗어났습니다.` | 경로 이탈 감지 | 입력값 재점검 |
| `scaffold 적용 경로 대소문자 충돌: ...` | `SmsHistory`가 있는 경로에 `Smshistory`처럼 대소문자만 다른 `domainClass`를 적용 | 기존 클래스·파일명과 `domainClass` 표기를 정확히 일치시킨 뒤 다시 미리보기 |
| 생성 응답이 HTML 문자열로 옴 | 세션 만료로 로그인 리다이렉트 | F12에서 최종 URL `/login` 확인 → 재로그인 |
| 적용했는데 내 수정이 사라짐 | `OVERWRITE`로 파일 전체가 교체됨 | 버전 관리에서 복구. 재생성 결과와 병합되지 않으므로 개발자 소유 화면에는 다시 적용하지 않음 |

### 8.2 ORA 오류 1차 진단

| ORA | 의미 | 확인할 곳 |
|---|---|---|
| `ORA-00942` | table or view does not exist | 테이블명·스키마 접두(`SMS.TB_X`)·접근 권한 |
| `ORA-00904` | invalid identifier | 컬럼명 철자·alias 오타 |
| `ORA-00937` | not a single-group group function | GROUP BY 누락/비일치 |
| `ORA-00936` | missing expression | SELECT/WHERE 절 빈 표현식 |
| 그 외 구문 오류 | 문법 오류 | `db-platform` 문법 일치 여부, `$변수`→`NULL` 치환 후 실행 |

### 8.3 실수를 줄이는 작성 습관

1. **표현식·함수·CASE·서브쿼리 컬럼에 alias** — 단순 컬럼 참조(`A.COL`)는 alias 불필요.
2. **`SELECT *`, `A.*` 와일드카드 금지**, 컬럼 명시 나열.
3. **`WHERE 1=1`** 로 시작, 검색조건은 `AND ... $변수`.
4. **정렬은 `orderBy` 입력란**에 tie-breaker까지 (`A.SEND_DT DESC, A.HIST_ID DESC`).
5. **UNION/CTE는 VIEW로** 우회.
6. **CRUD는 `targetTable`·PK·lock을 명시**하고, 조회 결과에 PK가 포함됐는지 확인.
7. **타입 추론 오류는 쿼리 수정으로** 해결 — String 폴백은 없다.
8. 적용 전 **미리보기로 덮어쓰기 건수** 확인.
9. `OVERWRITE`가 개발자 소유 화면이면 적용을 중단. 새 위치에 생성해 비교하거나 기존 화면 폐기를 먼저 결정.

---

## 9. 부록

### 9.1 최소 입력 예제 — 목록 조회 (LIST)

```text
moduleName   : sms
domainId     : history
domainClass  : SmsHistory
domainName   : 발송이력조회
screenMode   : LIST
orderBy      : A.SEND_DT DESC, A.HIST_ID DESC
```

```sql
SELECT A.HIST_ID,
       A.SEND_DT,
       A.RECEIVER_NO,
       A.SEND_STATUS,
       COUNT(1) AS SEND_CNT
FROM SMS.SMS_HISTORY A
WHERE 1=1
  AND A.SEND_DT >= $start_dt
  AND A.RECEIVER_NO = $receiver_no
GROUP BY A.HIST_ID, A.SEND_DT, A.RECEIVER_NO, A.SEND_STATUS
```

→ `startDt`(날짜 자동 판정), `receiverNo`(텍스트) 검색조건과 5개 그리드 컬럼이 생성된다.

### 9.2 CRUD 입력 예제

```text
screenMode   : CRUD
targetTable  : SMS.SMS_HISTORY
pkColumns    : HIST_ID
lockColumn   : UPD_DTTM
```

- `HIST_ID`는 SELECT 결과에 반드시 포함되어야 한다.
- `UPD_DTTM`은 PK가 아니어야 하고, 수정 요청에는 `beforeUpdDttm`으로 전달되어 낙관적 잠금 WHERE 조건에 사용된다.
- `editable=true` 컬럼만 수정 input·`UpdateRequestDTO`·`UPDATE SET`에 포함된다. `REG_ID`/`REG_DTTM`/PK 등은 자동으로 제외된다.

### 9.3 개인정보 화면 예제

```text
includePrivacy : true
columnOptions  : RECEIVER_NO → maskType=PHONE
                 CUST_NM     → maskType=NAME
```

→ 목록/엑셀에 `MaskingUtil` 호출이 생성되고, `/unmask` + `getUnmaskedDetail` + `@PrivacyLog`가 붙으며, 메뉴 SQL에 `CAN_MASK_VIEW='Y'`가 부여된다. 마스킹 정책 상세는 `docs/base/audit-masking-policy.md` 참조.

### 9.4 설정 키

| 키 | 위치 | 기본 | 설명 |
|---|---|---|---|
| `sms.scaffold.db-platform` | `application.yml` | `oracle` | DB 방언 (`oracle`/`postgres`/`db2`/`mssql`) |
| `sms.scaffold.output-root` | `application.yml` | `""`(프로젝트 루트) | 적용 파일 저장 루트 |
| `sms.menu.auth.exclude-paths` | `application-local.yml` | `/system/scaffold,/system/scaffold/**` | local 전용 접근 허용 |

### 9.5 함께 읽는 문서

| 문서 | 언제 |
|---|---|
| `docs/base/query-scaffold-implementation.md` | 구현 내부/검증 기록을 볼 때 |
| `docs/base/scaffold-contract.md` | 생성 품질 기준·생성물 소유권·재생성 금지 기준 |
| `docs/base/v2-scaffold-reference.md` | 설계 근거·v2 대비 변경점 |
| `docs/base/grid-formatter-guide.md` | 생성 후 그리드 셀 포매터 추가 |
| `docs/base/screen-generation-guide.md` | 화면 생성 표준 절차(편입 필수) |
| `docs/base/screen-convention.md` | 화면 레이아웃/그리드/명명 규약 |
| `docs/base/common-response-contract.md` | JSON 응답 계약 |
| `docs/base/audit-masking-policy.md` | 개인정보/마스킹 정책 |
| `docs/base/menu-authority-table-design.md` | 메뉴/권한 테이블 설계 |

---

> **문서 성격**: 이 매뉴얼은 코드(`ScaffoldController`, `ScaffoldService`, `QueryColumnExtractor`, `ColumnTypeInferrer`, `ScaffoldDialect`, `ScaffoldModel`, `ScaffoldArtifactRenderer`, `ScaffoldFileApplier`, `ScaffoldRequestDTO` 및 옵션 DTO, `scaffold.js`/`scaffold.html`)와 `docs/base` 설계 문서를 근거로 작성했다. 코드와 충돌하면 코드가 우선이며, 충돌 발견 시 즉시 보고한다.
