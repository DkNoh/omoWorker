---
paths:
  - "**/mapper/**/*.xml"
  - "**/resources/mapper/**/*.xml"
  - "**/db/oracle/**/*.sql"
---

# MyBatis / Oracle Rules

- Oracle 19c 기준으로 작성한다.
- 실제 DBObject 확인 없이 컬럼명을 확정하지 않는다.
- `SELECT *`를 사용하지 않는다.
- 조회 컬럼은 VO 필드와 alias를 명확히 맞춘다.
- Oracle snake_case와 Java camelCase 매핑을 전제로 한다.
- LIKE 조건은 `LIKE '%' || #{keyword} || '%'` 형식을 사용한다.
- 페이지 조회는 `OFFSET ... FETCH NEXT ...`를 사용한다.
- 페이지 조회에는 결정적 `ORDER BY`를 둔다.
- count 쿼리와 목록 쿼리는 같은 검색조건을 사용한다.
- update는 수정 컬럼만 SET한다. 전체 컬럼 일괄 update를 만들지 않는다.
- update WHERE에는 PK와 함께 `UPDATE_DTTM = #{beforeUpdateDttm}` 낙관적 잠금 조건을 둔다. nullable 잠금 컬럼은 `(UPDATE_DTTM = #{beforeUpdateDttm} OR (UPDATE_DTTM IS NULL AND #{beforeUpdateDttm} IS NULL))`처럼 null-safe 조건을 사용한다.
- PK/identity 컬럼은 INSERT/UPDATE SET/낙관적 잠금 컬럼으로 사용하지 않는다.
- UPDATE_DTTM 갱신값은 컬럼 타입 확인 후 선택한다: TIMESTAMP면 `SYSTIMESTAMP`, CHAR(14)면 `TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')`.
- 운영 DDL에는 `DROP TABLE`을 넣지 않는다.
- 한글 seed SQL은 UTF-8 실행 경로를 문서화한다.
