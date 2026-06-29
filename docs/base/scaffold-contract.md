# Scaffold Contract v1 (철학 A: 생성 후 소유 + 자동 재생성)

이 문서는 Query Scaffold가 **생성하는 코드의 품질 기준과 개발자 워크플로우**를 정의한다.

> **핵심 원칙**: 스캐폴드는 고품질 출발점을 생성한다. 개발자는 생성된 코드를 직접 수정한다. 템플릿 구조가 바뀌면 `ScaffoldRegenerateMain` 일괄 재생성으로 모든 산출물을 자동 갱신한다.

## 1. 생성 범위 (11~12종)

| 산출물 | 비고 |
|---|---|
| {Domain}SearchRequestDTO.java | PageRequestDTO 상속, Lombok @Data |
| {Domain}UpdateRequestDTO.java | CRUD 모드만. 화이트리스트. validate=required 시 @NotBlank/@NotNull |
| {Domain}VO.java | Lombok @Data. maskType 컬럼은 MaskingUtil 적용 |
| {Domain}Mapper.java | interface |
| {Domain}Mapper.xml | baseQuery include, searchConditions 공통화, 쿼리 시그니처 |
| {Domain}Service.java | 마스킹, 검증 TODO, getUnmaskedDetail (privacy 시) |
| {Domain}Controller.java | /data, /create, /update, /delete, /excel, /unmask (옵션별) |
| {Domain}ServiceTest.java | Mapper mock, given/when/then |
| {Domain}ControllerTest.java | MockMvc, ApiResponse 포맷 검증 |
| {domainId}.html | screen-convention 골격 |
| {domainId}.js | TuiPageBuilder |
| 메뉴등록.sql | screenMode별 최소 권한 CAN_* |

## 2. 개발자 워크플로우

```
1. 스캐폴드로 기본 코드 생성 (local 전용 /system/scaffold)
   - apply 시 src/main/resources/scaffold-cases/{module}_{domainId}.json 메타 파일이 자동 저장됨
2. TODO 주석 위치에 업무 로직 추가
   - Service create() : // TODO: 등록 전 업무 규칙 검증(중복 체크, 필수값 보정 등)
   - Service update() : // TODO: 수정 전 업무 규칙 검증(상태 전이, 권한 확인 등)
3. 필요시 컬럼, 검색조건, 화면 레이아웃 직접 수정
4. mvn test + mvn -DskipTests package 통과 확인
5. 배포
```

**재생성 워크플로우 (템플릿 구조 변경 시)**

```
1. Scaffold*Template.java 코드 수정 (예: baseQuery/searchConditions 분리)
2. mvn compile exec:java -Dexec.mainClass=...ScaffoldRegenerateMain
   - scaffold-cases/*.json 읽어 모든 산출물을 자동 재생성 + 파일 덮어쓰기
3. mvn spotless:apply
4. mvn test (드리프트 게이트가 산출물-템플릿 일치 검증)
5. commit
```

주의: 개발자가 TODO를 채운 부분은 ScaffoldRegenerateMain 실행 시 덮어쓰기 된다. 업무 로직이 많이 쌓인 화면은 메타 파일을 지우거나 `screenMode`를 조정하여 재생성 대상에서 제외한다.

## 3. 생성 품질 기준 (스캐폴드가 보장하는 것)

| 항목 | 보장 내용 |
|---|---|
| 마스킹 | includePrivacy + maskType -> MaskingUtil 실제 호출 (목록/엑셀) |
| 검증 | validate=required -> @NotBlank/@NotNull 서버 어노테이션 |
| 원문 상세 | includePrivacy -> /unmask endpoint + getUnmaskedDetail + @PrivacyLog |
| 권한 | screenMode별 최소 CAN_* (LIST->READ, CRUD->CRUD 4종, privacy->+MASK_VIEW) |
| SQL | 쿼리 시그니처(/* Mapper.method */), baseQuery DRY, searchConditions 공통화 |
| 적용 | 미리보기는 신규/변경없음/덮어쓰기를 표시한다. 사용자가 적용을 확정하면 기존 파일은 생성 결과로 덮어쓴다. 메타 파일(scaffold-cases/*.json)도 함께 저장된다. |

## 4. 생성 파일 소유권

모든 생성 파일에는 `Scaffold 생성(v1)` 헤더가 있다.
이 헤더는 생성 출처를 표시하고, ConventionTest 드리프트 게이트가 이 헤더가 있는 파일만
현재 템플릿 규칙을 따르는지 검증한다. 개발자는 Service, Controller, Mapper, 화면 파일을
직접 수정할 수 있으며, ScaffoldRegenerateMain 실행 시 다시 템플릿 출력으로 덮어쓰기 된다.

## 5. 일괄 재생성 (ScaffoldRegenerateMain)

`src/main/resources/scaffold-cases/*.json` 파일이 ScaffoldService.apply() 시 자동 저장된다.
각 파일은 ScaffoldRequestDTO + columns + searchVars + typeMap + dialect를 포함한다.

```bash
mvn compile exec:java -Dexec.mainClass=com.scbk.sms.service.system.scaffold.ScaffoldRegenerateMain
mvn spotless:apply
mvn test
```

DB 접속이 필요 없다 (typeMap이 메타에 저장됨).

## 6. screen-generation-guide.md가 핵심

폐쇄망 개발자는 AI를 사용할 수 없다. 따라서 screen-generation-guide.md의 절차서가
개발자가 스캐폴드를 사용하고 TODO를 채우는 데 가장 중요한 자산이다.
