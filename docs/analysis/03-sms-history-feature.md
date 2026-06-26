# 03-sms-history-feature.md — /sms/history 대표 기능 end-to-end

## 1. 엔드포인트와 사용 DTO

컨트롤러: `src/main/java/com/scbk/sms/controller/sms/SmsHistoryController.java`

- 화면: `GET /sms/history` → `sms/history` 템플릿 반환 (`Controller.java:28~30`행).
- 목록 조회: `GET /sms/history/data` → `SmsHistorySearchRequestDTO`를 `@ModelAttribute`로 받는다 (`Controller.java:33~37`행).
- 등록: `POST /sms/history/create` → `SmsHistoryUpdateRequestDTO`를 `@Valid @RequestBody`로 받는다 (`Controller.java:40~44`행).
- 수정: `POST /sms/history/update` → `SmsHistoryUpdateRequestDTO`를 `@Valid @RequestBody`로 받는다 (`Controller.java:47~51`행).
- 삭제: `POST /sms/history/delete` → `Integer smsHistoryId`, `String requestId`를 `@RequestParam`으로 받는다 (`Controller.java:54~58`행).

응답은 모두 `ApiResponse`로 감싸고 `ResponseEntity.ok(...)`를 반환한다: `Controller.java:33~58`행.

DTO:

- 검색 요청: `src/main/java/com/scbk/sms/dto/sms/SmsHistorySearchRequestDTO.java:7~15`행.
  - `PageRequestDTO`를 상속하므로 `page`, `size`, `keyword`, `searchType`을 가진다.
  - SMS history만의 검색 조건은 `sendType`, `sendStatus`, `sentAt`, `receiverNo`이다.
- 수정 요청: `src/main/java/com/scbk/sms/dto/sms/SmsHistoryUpdateRequestDTO.java:12~25`행.
  - 화이트리스트 방식으로 PK와 수정 가능 필드만 선언한다.
  - 주석에 따라 `REG_ID`, `REG_DTTM`, 시스템 필드, 권한 필드는 선언하지 않는다: `SmsHistoryUpdateRequestDTO.java:6~10`행.
- 결과 VO: `src/main/java/com/scbk/sms/vo/sms/SmsHistoryVO.java:7~18`행.
  - `smsHistoryId`, `requestId`, `sentAt`, `receiverNo`, `senderNo`, `sendType`, `sendStatus`, `resultCd`, `resultMsg`를 가진다.
- 페이지 응답: `src/main/java/com/scbk/sms/dto/common/PageResponseDTO.java:9~18`행.
  - `contents`, `page`, `size`, `totalCount`, `totalPages`, `hasNext`, `hasPrev`를 제공한다.

## 2. 전체 흐름 다이어그램

```text
[요청]
GET /sms/history/data?sendType=...&sendStatus=...&sentAt=...&receiverNo=...&page=...&size=...
  -> SmsHistoryController.getData(@ModelAttribute SmsHistorySearchRequestDTO request)
     src/main/java/com/scbk/sms/controller/sms/SmsHistoryController.java:33~37행
  -> SmsHistoryService.search(request)
     src/main/java/com/scbk/sms/service/sms/SmsHistoryService.java:21~26행
     request.validate()
     mapper.count(request)
     mapper.selectList(request)
     PageResponseDTO.of(list, request, totalCount)
  -> SmsHistoryMapper.count(sqlId=count)
     src/main/java/com/scbk/sms/mapper/sms/SmsHistoryMapper.java:13행
  -> SmsHistoryMapper.xml count SQL
     src/main/resources/mapper/sms/SmsHistoryMapper.xml:17~45행
  -> SmsHistoryMapper.selectList(sqlId=selectList)
     src/main/java/com/scbk/sms/mapper/sms/SmsHistoryMapper.java:15행
  -> SmsHistoryMapper.xml selectList SQL
     src/main/resources/mapper/sms/SmsHistoryMapper.xml:47~78행
  -> PageResponseDTO<SmsHistoryVO>
  -> ApiResponse<PageResponseDTO<SmsHistoryVO>>
  -> JSON

POST /sms/history/create
  -> SmsHistoryController.create(@Valid @RequestBody SmsHistoryUpdateRequestDTO request)
     Controller.java:40~44행
  -> SmsHistoryService.create(request)
     SmsHistoryService.java:29~31행
  -> SmsHistoryMapper.insert(request)
     SmsHistoryMapper.java:17행
  -> SmsHistoryMapper.xml insert SQL
     SmsHistoryMapper.xml:80~98행
  -> ApiResponse<String>

POST /sms/history/update
  -> SmsHistoryController.update(@Valid @RequestBody SmsHistoryUpdateRequestDTO request)
     Controller.java:47~51행
  -> SmsHistoryService.update(request)
     SmsHistoryService.java:34~40행
     mapper.update(request)
     updated == 0이면 CustomException(UPDATE_CONFLICT)
  -> SmsHistoryMapper.update(request)
     SmsHistoryMapper.java:19행
  -> SmsHistoryMapper.xml update SQL
     SmsHistoryMapper.xml:100~112행
  -> ApiResponse<String>

POST /sms/history/delete
  -> SmsHistoryController.delete(@RequestParam Integer smsHistoryId, @RequestParam String requestId)
     Controller.java:54~58행
  -> SmsHistoryService.delete(smsHistoryId, requestId)
     SmsHistoryService.java:43~45행
  -> SmsHistoryMapper.delete(smsHistoryId, requestId)
     SmsHistoryMapper.java:21행
  -> SmsHistoryMapper.xml delete SQL
     SmsHistoryMapper.xml:114~117행
  -> ApiResponse<String>
```

## 3. Controller → Service → Mapper → SQL 상세

### 3.1 목록 조회 `/data`

Controller는 검색 DTO를 `@ModelAttribute`로 받는다: `SmsHistoryController.java:33~37`행. 이 방식은 쿼리 스트링 또는 폼 파라미터를 DTO 필드에 바인딩한다.

Service는 세 단계를 수행한다: `SmsHistoryService.java:21~26`행.

1. `request.validate()`로 페이지 범위를 정규화한다: `SmsHistoryService.java:23`행.
2. `mapper.count(request)`로 전체 건수를 조회한다: `SmsHistoryService.java:24`행.
3. `mapper.selectList(request)`로 현재 페이지 데이터를 조회한다: `SmsHistoryService.java:25`행.
4. `PageResponseDTO.of(list, request, totalCount)`로 페이징 메타를 계산한다: `SmsHistoryService.java:26`행.

Mapper 인터페이스는 `count`와 `selectList`를 각각 정의한다: `SmsHistoryMapper.java:13~15`행.

SQL의 검색 조건:

- `sendType`이 있으면 `SMS.SMS_HISTORY.SEND_TYPE`을 같다: `SmsHistoryMapper.xml:31~33`행, `SmsHistoryMapper.xml:62~64`행.
- `sendStatus`가 있으면 `SMS.SMS_HISTORY.SEND_STATUS`를 같다: `SmsHistoryMapper.xml:34~36`행, `SmsHistoryMapper.xml:65~67`행.
- `sentAt`가 있으면 `YYYYMMDD` 문자열을 `TO_TIMESTAMP(..., 'YYYYMMDDHH24MISS')`로 변환하고, 다음 날 00:00:00보다 작게 처리한다: `SmsHistoryMapper.xml:37~39`행, `SmsHistoryMapper.xml:68~70`행. XML 상단 주석도 DatePicker 검색값이 `YYYYMMDD` 문자열로 온다고 설명한다: `SmsHistoryMapper.xml:8~10`행.
- `receiverNo`가 있으면 `RECEIVER_NO`가 LIKE `%#{receiverNo}%`이다: `SmsHistoryMapper.xml:40~42`행, `SmsHistoryMapper.xml:71~73`행.

페이징:

- 목록 SQL은 `OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY`를 사용한다: `SmsHistoryMapper.xml:76~77`행.
- `offset`은 `PageRequestDTO.getOffset()`에서 계산된다: `src/main/java/com/scbk/sms/dto/common/PageRequestDTO.java:14~16`행.
- `PageResponseDTO.of`는 `Math.ceil(totalCount / size)`로 `totalPages`를 계산하고, `hasNext`/`hasPrev`를 계산한다: `PageResponseDTO.java:30~40`행.

### 3.2 등록 `/create`

Controller는 `SmsHistoryUpdateRequestDTO`를 `@Valid @RequestBody`로 받는다: `SmsHistoryController.java:40~44`행.

Service는 `mapper.insert(request)`만 호출한다: `SmsHistoryService.java:29~31`행.

Mapper는 `insert(SmsHistoryUpdateRequestDTO request)`를 정의한다: `SmsHistoryMapper.java:17`행.

SQL은 `SMS.SMS_HISTORY`에 다음 필드를 삽입한다: `SmsHistoryMapper.xml:80~98`행.

- `SENT_AT`
- `RECEIVER_NO`
- `SENDER_NO`
- `SEND_TYPE`
- `SEND_STATUS`
- `RESULT_CD`
- `RESULT_MSG`

`SMS_HISTORY_ID`는 SQL에서 명시하지 않으므로 DB side PK 또는 default를 가정한다. 코드만으로는 실제 자동 생성 방식을 확인하지 못했다.

### 3.3 수정 `/update`

Controller는 `SmsHistoryUpdateRequestDTO`를 `@Valid @RequestBody`로 받는다: `SmsHistoryController.java:47~51`행.

Service는 `mapper.update(request)` 결과를 확인한다: `SmsHistoryService.java:34~40`행.

- `updated == 0`이면 `CustomException(ErrorCode.UPDATE_CONFLICT)`를 던진다: `SmsHistoryService.java:37~39`행.
- `UPDATE_CONFLICT`는 HTTP 409, 코드 `C004`이다: `src/main/java/com/scbk/sms/exception/ErrorCode.java:14`행.

Mapper는 `update(SmsHistoryUpdateRequestDTO request)`를 정의한다: `SmsHistoryMapper.java:19`행.

SQL은 `SMS.SMS_HISTORY`를 수정하고 `SMS_HISTORY_ID`와 `REQUEST_ID`를 WHERE 조건으로 둔다: `SmsHistoryMapper.xml:100~112`행.

주석에 따르면 잠금/고정 컬럼을 지정할 경우 WHERE에 함께 두는 구조로 설계되어 있다: `SmsHistoryMapper.xml:100`행. 다만 현재 DTO와 SQL에는 잠금 컬럼이 추가되지 않았다.

### 3.4 삭제 `/delete`

Controller는 `smsHistoryId`와 `requestId`를 `@RequestParam`으로 받는다: `SmsHistoryController.java:54~58`행.

Service는 `mapper.delete(smsHistoryId, requestId)`만 호출한다: `SmsHistoryService.java:43~45`행.

Mapper는 `delete(@Param("smsHistoryId") Integer smsHistoryId, @Param("requestId") String requestId)`를 정의한다: `SmsHistoryMapper.java:21`행.

SQL은 `SMS.SMS_HISTORY`에서 `SMS_HISTORY_ID`와 `REQUEST_ID`가 모두 일치하는 행만 삭제한다: `SmsHistoryMapper.xml:114~117`행.

## 4. 검증(@Valid)이 걸리는 지점

- `GET /data`: Controller에 `@Valid`가 없다. 목록 조회 검증은 Service의 `request.validate()`로 처리한다: `SmsHistoryService.java:23`행.
- `POST /create`: Controller에 `@Valid @RequestBody`가 있다: `SmsHistoryController.java:42`행.
- `POST /update`: Controller에 `@Valid @RequestBody`가 있다: `SmsHistoryController.java:49`행.
- `POST /delete`: `@RequestParam`만 있고 `@Valid`는 없다. Service 내부 검증도 확인되지 않았다: `SmsHistoryController.java:56`행, `SmsHistoryService.java:43~45`행.

`PageRequestDTO.validate()`는 다음 범위를 정규화한다: `src/main/java/com/scbk/sms/dto/common/PageRequestDTO.java:18~27`행.

```text
page < 1  -> page = 1
size < 1  -> size = 10
size > 100 -> size = 100
```

`@Valid`가 붙은 DTO에는 현재 `jakarta.validation` 어노테이션이 확인되지 않았다. 따라서 Spring `@Valid`는 DTO 필드가 null인지 정도만 검사하고, 비즈니스 규칙은 Service의 `validate()` 또는 Mapper SQL 조건에서 처리되는 구조로 보인다: `SmsHistoryUpdateRequestDTO.java:12~25`행.

## 5. SQL의 동적 조건과 페이징

`SmsHistoryMapper.xml`은 `searchConditions` id를 정의하지만 현재 본문은 비어 있다: `src/main/resources/mapper/sms/SmsHistoryMapper.xml:12~15`행.

`count`와 `selectList` SQL은 거의 동일한 조건을 가진다:

- `WHERE 1=1`로 시작한다: `SmsHistoryMapper.xml:30~31`행, `SmsHistoryMapper.xml:61~62`행.
- 검색 조건은 `<if>`로 동적 삽입한다: `SmsHistoryMapper.xml:31~73`행.
- `ORDER BY A.RESULT_MSG`로 고정 정렬한다: `SmsHistoryMapper.xml:76`행.
- Oracle fetch paging을 사용한다: `SmsHistoryMapper.xml:77`행.

`count` SQL은 `SELECT COUNT(1) FROM (SELECT ... FROM SMS.SMS_HISTORY A WHERE ...) A` 형태로 래핑한다: `SmsHistoryMapper.xml:17~45`행.

`selectList` SQL은 `SELECT A.* FROM (SELECT ... FROM SMS.SMS_HISTORY A WHERE ...) A` 형태로 래핑하고, 바깥쪽에서 `ORDER BY`와 `OFFSET/FETCH`를 적용한다: `SmsHistoryMapper.xml:47~78`행.

## 6. 마스킹 처리 방식

현재 `SmsHistoryMapper.xml`에서 `RECEIVER_NO`, `SENDER_NO`, `RESULT_MSG`를 마스킹하는 SQL 처리는 확인되지 않았다. SQL은 원본 값을 그대로 조회하고 있다: `SmsHistoryMapper.xml:50~59`행, `SmsHistoryMapper.xml:68~73`행.

VO도 `receiverNo`, `senderNo`, `resultMsg`를 그대로 가진다: `SmsHistoryVO.java:12~18`행.

따라서 이 문서에서 확인한 코드 기준으로는 마스킹은 `SmsHistory` 기능의 Mapper/XML 단계에서 처리되지 않는다. 마스킹이 있다면 HTML/JS 템플릿, 공통 응답 필터, 또는 다른 공통 인프라에서 처리될 가능성이 있다.

## 7. 확인하지 못한 부분 / 불확실한 부분

- `/sms/history` 화면 템플릿과 JS가 현재 프로젝트에서 확인되지 않았다. `Controller`는 `sms/history`를 반환하지만 `src/main/resources` 내 HTML 템플릿 검색 결과가 없었다.
- `SMS.SMS_HISTORY.SMS_HISTORY_ID`의 자동 생성 방식은 SQL에서 명시되지 않았다.
- `@Valid`가 적용된 DTO에 실제 `jakarta.validation` 어노테이션이 없는 것으로 보이며, 입력값 형식 검증은 Mapper SQL 또는 프론트엔드에서 처리되는지 확인이 필요하다.
- SQL 내 마스킹은 확인되지 않았으며, 프론트엔드 또는 공통 인프라에서 처리되는지 별도 확인이 필요하다.
