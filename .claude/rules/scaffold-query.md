---
paths:
  - "**/scaffold/**/*.java"
  - "**/system/Scaffold*.java"
  - "**/templates/**/scaffold*.html"
  - "**/js/system/scaffold.js"
  - "**/docs/base/**/*scaffold*.md"
---

# Query Scaffold Rules

- Query Scaffold는 local 전용 도구다. `@Profile("local")`을 유지하고 메뉴에 등록하지 않는다.
- 구현 기준은 `docs/base/query-scaffold-implementation.md`, 설계 근거는 `docs/base/v2-scaffold-reference.md`를 따른다.
- QuerySpec 입력 계약은 `rawQuery + $변수` 규약이다.
- SELECT 컬럼은 alias를 필수로 둔다. `SELECT *`는 금지한다.
- 정렬 컬럼(orderBy)은 입력 필수다. 결정적 정렬 없이 생성하지 않는다.
- 타입 추론 실패를 String으로 강행하지 않는다. 오류를 보고한다.
- 생성물은 v3 규약을 따라야 한다: Lombok(@Data/@RequiredArgsConstructor) 기반, `/create`·`/update` 분리, v3 메뉴 스키마, screen-convention 골격.
- 생성물 규약을 바꾸면 `ScaffoldTemplateTest`를 함께 갱신한다.
- bind parameter는 SearchRequestDTO 필드와 1:1, grid 컬럼은 VO 필드와 1:1이다.
- 생성 직후 Controller, Service, Mapper XML, Template 파라미터 매핑을 검증한다.
- scaffold가 만든 코드는 바로 완료가 아니다. `screen-generation-guide.md` 절차에 편입해 검증한다.
