# V2 Scaffold 생성기 분석 (Query Scaffold 설계 참고)

v2에 이미 구현되어 있는 스캐폴드 생성기 화면을 분석한 문서다. v3 Query Scaffold를 구현할 때 이 동작을 기준으로 설계한다.

원본: `sms-project-v2/src/main/java/com/scbk/sms/controller/system/SystemScaffoldController.java` + `templates/system/scaffold.html`

## 접근/보호

- URL: `/system/scaffold`, 생성 API: `POST /system/scaffold/generate`
- `@Profile("local")` — dev/prod에서는 빈 자체가 등록되지 않는다. v3도 동일하게 유지한다.

## 입력 계약

| 입력 | 의미 | 예 |
|---|---|---|
| `moduleName` | 패키지/경로의 도메인명 | `basic` |
| `domainId` | URL/파일명 (kebab-case) | `currency` -> `/basic/currency` |
| `domainClass` | 클래스 접두 (PascalCase) | `Currency` |
| `domainName` | 화면 한글명 | `환율조회` |
| `rawQuery` | 조회 SQL 원문 + `$변수` 검색조건 규약 | 아래 참조 |
| `includeCud` / `includeExcel` / `includeExcelGrid` | 선택 생성 옵션 | |

`rawQuery`의 `$변수` 규약이 핵심이다 (scaffold-query.md의 "QuerySpec 입력 계약"의 실체):

```sql
SELECT A.BASE_DT, A.CURRENCY_CD, A.RATE
FROM EXCHANGE_RATE A
WHERE 1=1
AND A.BASE_DT = $base_dt
AND A.CURRENCY_CD = $currency_cd
```

- `$base_dt` -> SearchRequestDTO 필드 `baseDt`(camelCase 변환) + 화면 검색 input + XML `<if>` 동적 조건 + `#{baseDt}` 바인딩으로 일괄 변환된다.
- `$변수`가 포함된 SQL 라인은 통째로 `<if test="... != null and ... != ''">` 블록이 된다.

## 동작 파이프라인

```text
1. SELECT 컬럼 추출  : JSQLParser 파싱 (alias 우선), 실패 시 정규식 fallback
2. 타입 추론         : SELECT * FROM (원쿼리) WHERE ROWNUM = 0 실행 후 ResultSetMetaData로
                       NUMBER(scale>0)->BigDecimal, NUMBER(p>9)->Long, NUMBER->Integer,
                       DATE->LocalDate, TIMESTAMP->LocalDateTime, CLOB->String, 실패 시 전부 String
3. 산출물 생성       : 아래 10종을 탭으로 표시 (파일 직접 쓰기가 아니라 복사용 코드 제공)
```

산출물: SearchRequestDTO, VO, Mapper interface, Mapper XML(count/selectList/searchConditions, OFFSET-FETCH), Service, Controller(two-track), HTML, JS(TuiPageBuilder), 메뉴등록 SQL, 문서 snippet 2종.

## v3 Query Scaffold 구현 시 조정 목록

v2 생성기를 그대로 가져오면 안 된다. 다음을 반드시 조정한다.

| 항목 | v2 생성 결과 | v3 기준 |
|---|---|---|
| Lombok | `@Data`, `@RequiredArgsConstructor` 사용 | v2와 동일하게 Lombok 생성 (초기에는 plain Java였으나 2026-06-12 폐쇄망 Lombok 보유 확인으로 v2 방식 복귀) |
| 메뉴 SQL | `TB_MENU(MENU_CD, UP_MENU_CD)` + `TB_MENU_AUTH(AUTH_CD, CAN_WRITE, CAN_EXCEL)` | v3 스키마: `TB_MENU(MENU_ID, PARENT_MENU_ID, MENU_TYPE...)` + `TB_MENU_AUTH(ROLE_CD, CAN_* 8종)` |
| 저장 endpoint | `/save` 단일 생성 | `/create`, `/update` 분리 생성 |
| HTML 골격 | 구버전 `.search-section` 클래스 구조 | `screen-convention.md` 카드 골격으로 교체 |
| 엑셀 | `ExcelUtil` 호출 + `Map` 반환 Mapper | `ExcelUtil`은 v3에 없음. 구현 후 연결. `Map` 반환은 규칙 위반이므로 VO 기반 검토 |
| 타입 추론 실패 | 경고 로그 후 전부 String으로 진행 | 실패를 숨기지 않는 v3 원칙에 따라 실패 보고 후 사용자 판단 검토 |
| 정렬 | `ORDER BY A.ROWID DESC /* TODO */` | 결정적 정렬 컬럼을 입력에서 받도록 개선 검토 |

## 의존성 (폐쇄망 반입 목록에 추가)

- `net.sf.jsqlparser:jsqlparser` — SELECT 컬럼 파싱용. v3 pom에 없음. Query Scaffold 구현 시 반입 필요.

## v3에서 유지할 가치가 있는 설계

- `$변수` 한 가지 규약으로 DTO/화면/XML 동적조건이 동시에 정합되는 구조
- `ROWNUM = 0` 메타데이터 조회로 실제 DB 기준 타입을 얻는 방식 ("실제 DB 근거 우선" 원칙과 일치)
- 생성물 직접 파일 쓰기가 아니라 복사용 코드 제시 (사람 검토 후 반영)
- 메뉴 등록 SQL과 문서 snippet을 코드와 함께 생성 (문서 동기화 원칙과 일치)
