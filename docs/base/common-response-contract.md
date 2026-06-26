# 공통 응답 계약 (Common Response Contract)

v2의 공통 응답 규격을 v3로 이관한 설계 원문이다. 모든 JSON endpoint는 이 계약을 따른다.

## 응답 규격

모든 JSON 응답은 `ApiResponse<T>`로 감싼다. 성공/실패와 관계없이 프론트엔드는 항상 같은 모양의 JSON을 받는다.

```json
{
  "timestamp": "2026-06-10T16:49:00.000",
  "code": 200,
  "message": "SUCCESS",
  "data": { }
}
```

| 필드 | 의미 |
|---|---|
| `timestamp` | 응답 생성 시각 |
| `code` | HTTP 상태값과 동일한 결과 코드 |
| `message` | 결과 메시지. 성공 기본값 `SUCCESS` |
| `data` | 실제 데이터. 오류 시 `null` |

## 목록 응답 규격

목록 조회는 `ApiResponse<PageResponseDTO<VO>>`를 사용한다.

```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "contents": [],
    "page": 1,
    "size": 10,
    "totalCount": 25,
    "totalPages": 3,
    "hasNext": true,
    "hasPrev": false
  }
}
```

- `PageResponseDTO`는 `PageResponseDTO.of(list, request, totalCount)`로만 생성한다.
- 결과가 0건이어도 `totalPages`는 최소 1이다.
- 도메인 검색 DTO는 `PageRequestDTO`를 상속한다. `offset = (page - 1) * size`는 `PageRequestDTO.getOffset()`이 계산한다.
- `PageRequestDTO.validate()`는 `page >= 1`, `1 <= size <= 100`으로 보정한다.

## 클래스 구성

| 클래스 | 역할 |
|---|---|
| `dto/common/ApiResponse.java` | 공통 응답 래퍼. `success(data)`, `success(message, data)`, `error(code, message)` |
| `dto/common/PageRequestDTO.java` | 공통 페이지 요청. 도메인 검색 DTO의 부모 |
| `dto/common/PageResponseDTO.java` | 공통 페이지 응답. `of(...)` 정적 팩토리만 사용 |
| `exception/ErrorCode.java` | HTTP status + 코드 + 메시지를 가진 업무 에러 enum |
| `exception/CustomException.java` | Service에서 던지는 예측 가능한 업무 예외 |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`. 예외 -> `ApiResponse` 변환의 단일 지점 |

## 클라이언트(화면 JS) 처리 규약

`common-utils.js`의 전역 axios 인터셉터가 `ApiResponse`를 처리한다.

- 성공 시 껍데기를 벗겨 `response.data`에 알맹이(data)만 남긴다. 화면 JS는 `response.data.data`로 접근하지 않는다.
- 오류 시 서버 메시지를 모달로 표시하고 reject한다. 화면 JS에서 알림을 중복 표시하지 않는다.
- 상세는 `screen-convention.md`의 "axios 응답 처리 규약"을 따른다.

## 예외 처리 규칙

- Controller에서 try-catch로 예외를 삼키지 않는다. `GlobalExceptionHandler`로 전파한다.
- `GlobalExceptionHandler`는 요청 `Accept` 헤더로 분기한다 (2026-06-12): 화면 요청(`text/html`)은 공통 에러 페이지(`templates/error/error.html`), JSON 요청은 `ApiResponse`로 응답한다.
- 예측 가능한 업무 오류는 `throw new CustomException(ErrorCode.XXX)`로 던진다.
- 새 업무 오류가 필요하면 `ErrorCode`에 항목을 추가한다. 코드 체계: 공통 `C***`, 인증/권한 `A***`, 사용자 `U***`, 메뉴 `M***`.
- `@Valid` 검증 실패는 `GlobalExceptionHandler`가 400 + 첫 번째 검증 메시지로 변환한다.
- 매핑되지 않은 URL(404)은 원인 후보(컨트롤러 미등록/메뉴 URL 오타/파일 미배치)를 포함한 메시지로 응답한다.

## v2 대비 조정 사항

| 항목 | v2 | v3 |
|---|---|---|
| Lombok | `@Getter`, `@Builder`, `@Data` 사용 | 이 공통 클래스들은 plain Java로 구현 (검증 완료된 코드라 유지). 신규 도메인/생성 코드는 Lombok 사용 — 2026-06-12 도입 |
| 동작/JSON 모양 | — | v2와 동일 |

## 검증 결과

2026-06-10 기준:

```text
mvn test                  PASS (8 tests: ApiResponse 2, PageRequestDTO 3, PageResponseDTO 3)
mvn -DskipTests package   PASS
```

테스트는 `src/test/java/com/scbk/sms/dto/common/`에 있으며 페이지 계산과 보정 규칙을 고정한다.
