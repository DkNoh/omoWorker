# REST API 설계 기준

외부 연계/API 거래를 RESTful 방식으로 만들 때 따르는 기준이다. v2 `docs/rest-api-standard.md`를 이관했다.

Thymeleaf 화면 거래에는 적용하지 않는다. 화면 내부 데이터 조회(`/data` 계열)는 화면 URL 아래에 두고, 외부 연계 거래만 `/api/**` 아래에 둔다.

현재 v3 BASE에는 `/api/**` 거래가 없다. 첫 외부 연계 API를 만들 때 이 기준을 적용한다.

## 기본 원칙

| 항목 | 기준 |
|---|---|
| URL | 행위가 아니라 자원 기준으로 작성 (`/api/sms/send-results`) |
| Method | 조회 `GET`, 생성 `POST`, 전체 수정 `PUT`, 부분 수정 `PATCH`, 삭제 `DELETE` |
| Controller | `@RestController` 사용. 화면 Controller와 분리 |
| 응답 | `ApiResponse<T>` 공통 포맷 사용 |
| 데이터 노출 | 화면 VO를 직접 노출하지 않고 API 전용 DTO 사용 |
| SQL | API 응답에 필요한 컬럼만 조회하는 Mapper SQL 사용 |

## 화면 거래와 API 거래 분리

| 구분 | URL | 역할 |
|---|---|---|
| 화면 | `/sms/history` | Thymeleaf 화면 반환 |
| 화면 데이터 | `/sms/history/data` | 화면 그리드용 JSON (메뉴 권한 Interceptor 검증 대상) |
| 외부 연계 API | `/api/sms/send-results` | 외부 시스템용 JSON 조회 |

`/api/sms/history/send-result`처럼 업무 행위명을 URL에 넣지 않는다.

## 권한 주의

`/api/**`는 메뉴 권한 Interceptor의 메뉴 URL 매핑과 맞지 않으므로 인증/인가 방식을 유형별로 확정한다.

| 유형 | 인증/인가 | 상태 |
|---|---|---|
| 화면 보조 API (`/api/common-code/**`) | 세션 로그인 필수 + Interceptor 제외 (모든 로그인 사용자 사용) | 확정 (2026-06-12) |
| 외부 연계 API | 토큰, 내부망 IP 제한 등 별도 확정 필요 | 미확정. 확정 전에는 만들지 않는다 |
