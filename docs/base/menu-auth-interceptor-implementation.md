# Menu Auth Interceptor Implementation

좌측 메뉴 표시와 별개로 모든 URL/API 요청을 서버에서 다시 검증하는 메뉴 권한 Interceptor 구현 기록이다.

설계 원문은 `docs/base/menu-authority-table-design.md`의 "URL suffix 권한 매핑"을 따른다.

## 판정 규칙

```text
1. 요청 URL이 메뉴 URL과 정확히 일치 -> 화면 접근 -> CAN_READ 검증
2. 일치하는 메뉴가 없으면 URL suffix를 분리 -> 부모 화면 URL 기준 액션 권한 검증
3. 어느 메뉴에도 연결되지 않는 URL -> 거부 (403)
```

정확 일치를 suffix보다 먼저 검사하므로 `/campaign/sms/register`, `/campaign/sms/approve`처럼
suffix와 겹치는 화면 URL이 잘못 분해되지 않는다.

## URL suffix 매핑

| suffix | 필요 권한 |
|---|---|
| `/data`, `/search`, `/detail`, `/tree` | READ |
| `/create`, `/register` | CREATE |
| `/update` | UPDATE |
| `/save` (legacy) | CREATE + UPDATE 모두 |
| `/delete` | DELETE |
| `/approve`, `/reject` | APPROVE |
| `/cancel` | CANCEL |
| `/excel`, `/download`, `/export` | DOWNLOAD |
| `/unmask` | MASK_VIEW |

## 검증 제외 경로

코드 고정 제외: `/`, `/login`, `/logout`, `/error`, 정적 리소스(`/css/**`, `/js/**`, `/lib/**`, `/vendor/**`, `/img/**`, `/favicon.ico`), 공통코드 API(`/api/common-code/**` — 로그인한 모든 사용자가 쓰는 화면 보조 API)

추가 제외가 필요하면(예: 추후 공통코드 API) `application*.yml`에 설정한다.

```yaml
sms:
  menu:
    auth:
      exclude-paths: /api/common-code/**
```

업무 화면/API URL은 제외 목록에 추가하지 않는다. 메뉴 등록과 권한 부여로 해결한다.

## Source별 동작

| Source | 권한 판단 |
|---|---|
| `db` | `TB_MENU` + `TB_MENU_AUTH`에서 `(MENU_URL, 역할들)` 기준 조회. 역할 여러 건은 MAX 집계로 'Y' 우선 |
| `static` | baseline 메뉴 URL이면 모든 권한 부여. local 화면 검증 용도이며 최종 권한 검증은 db source에서 한다 |

`db` 조회 조건: `TB_MENU.USE_YN = 'Y'`, `TB_MENU_AUTH.USE_YN = 'Y'`.
`DISPLAY_YN`은 좌측 메뉴 노출 조건일 뿐이므로 접근 검증에서는 사용하지 않는다 (숨김 화면도 권한이 있으면 접근 가능).

## 생성 파일

| 파일 | 역할 |
|---|---|
| `service/menu/MenuPermission.java` | CAN_* 8종과 1:1 대응하는 권한 enum |
| `service/menu/MenuSource.java` | 메뉴 트리+권한 조회 인터페이스 (source 공통) |
| `service/menu/DbMenuSource.java` | `sms.menu.source=db` 전용. TB_MENU/TB_MENU_AUTH 조회 |
| `service/menu/StaticMenuSource.java` | `sms.menu.source=static` 전용. baseline URL 전체 권한 |
| `service/menu/MenuAuthService.java` | URL -> 권한 판정. 실패 시 `CustomException(ACCESS_DENIED)` |
| `mapper/menu/MenuAuthMapper.java` / `.xml` | URL + 역할 기준 CAN_* MAX 집계 조회 |
| `vo/menu/MenuAuthVO.java` | 권한 조회 결과 VO |
| `config/MenuAuthInterceptor.java` | preHandle에서 `(EMP_ID, DEP_ID)` principal의 역할로 검증 |
| `config/WebMvcConfig.java` | Interceptor 등록과 제외 경로 관리 |

권한 거부는 `GlobalExceptionHandler`가 `ApiResponse` 403 JSON으로 변환한다 (`ErrorCode.ACCESS_DENIED`).

## 검증 결과

2026-06-10 기준:

```text
mvn test                  PASS (18 tests, 신규 10: MenuAuthService 8, StaticMenuAuthProvider 2)
mvn -DskipTests package   PASS
```

테스트로 고정한 규칙: 정확 일치 우선, suffix -> 부모 화면 권한, `/save` 이중 권한, READ 없는 화면 거부, 미등록 URL 거부.

## 남은 고려사항

- 버튼 단위 화면 제어(권한 없는 버튼 숨김)는 별도 작업이다. 서버 검증은 이 Interceptor가 담당한다.
- `MASK_VIEW`는 `/unmask` suffix만 매핑되어 있다. 개인정보 원문 조회 화면이 생기면 감사 로그 정책과 함께 구체화한다.
- ~~화면 요청의 403도 JSON으로 응답된다~~ → 해소됨 (2026-06-12). `GlobalExceptionHandler`가 Accept 헤더로 분기해 화면 요청은 `error/error` 페이지, JSON 요청은 `ApiResponse`로 응답한다.
