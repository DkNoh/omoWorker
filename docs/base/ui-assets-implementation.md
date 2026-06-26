# UI Assets Port Implementation

v2 공통 UI 자산을 v3로 이식한 구현 기록이다. 이 작업으로 `docs/base/screen-convention.md`의 적용 전제가 충족되었다.

## 이식 범위

| 구분 | 파일 | 출처 |
|---|---|---|
| 라이브러리 | `static/lib/*` (tui-grid, tui-pagination, axios, xlsx) | v2 그대로 복사 |
| 라이브러리(신규) | `static/lib/dayjs.min.js`, `static/lib/ko.js` | 2026-06-12 사용자 반입. 날짜 공통 처리용. fmt.date/setDefaultDateTime/PageBuilder 날짜 변환이 dayjs 기반으로 전환됨. 한국어 locale은 defaultLayout에서 `dayjs.locale('ko')`로 전역 활성화 |
| 공통 JS(신규) | `static/js/common/form-binder.js` | 2026-06-12 작성. 상세폼 화면의 자동 바인딩(bind/toObject). 규약은 `screen-convention.md` "상세폼 화면 규약" |
| CoreUI | `static/vendor/coreui/*` | v2 그대로 복사 |
| 공통 JS | `static/js/common/common-utils.js`, `tui-common.js`, `tui-page-builder.js` | v2 그대로 복사 |
| 공통 CSS | `static/css/admin-common.css` | v2 그대로 복사 |
| 이미지 | `static/img/SC.png`, `static/favicon.ico` | v2 그대로 복사 |
| 공통 레이아웃 | `templates/defaultLayout.html` | v2 기반, v3 조정 |
| Fragment | `templates/fragments/sidebar.html`, `templates/fragments/header.html` | v2 기반, v3 조정 |

## v2 대비 조정 사항

| 항목 | v2 | v3 |
|---|---|---|
| `SESSION_INFO` 전역변수 | `userName`, `loginIp`, `loginTime` | `empId`, `depId`, `empNm`, `depNm` (EMP_ID + DEP_ID 식별 정책) |
| 사이드바 메뉴 데이터 | `session.userMenus` + `subMenus` | 모델 attribute `menus`(`MenuItemVO` tree) + `children` |
| 사이드바 접속 정보 | IP / TIME | 사용자(empNm/empId) / 부서(depNm/depId) |
| 사이드바 아이콘 분기 | v2 메뉴명 기준 | `v2-menu-baseline.md` 메뉴명 기준 (`캠페인SMS`, `시스템관리 계정관리` 등) |
| 헤더 사용자 표시 | `session.userName` | `user.empNm` + `user.depNm` |
| 그리드 공통 옵션 | `gridDefaults` 정의만 있고 PageBuilder는 자체 하드코딩(minBodyHeight 300 vs 400 불일치) | PageBuilder가 `gridDefaults`를 병합하는 단일 통제점으로 통일. 화면별 예외는 `config.gridOptions` |
| 총 건수 selector | `.total-count strong` (구형 마크업 기준) | `#total-count` (v3 화면 골격 기준) |
| `fmt.date` | 원시값 전용이라 TUI formatter로 쓰면 깨짐 | `{value}` 객체/원시값 양쪽 지원 |
| 업무 오류(code≠200) | 침묵으로 무시 | 토스트로 표시 |
| HTTP 호출 | PageBuilder 목록/자동완성은 fetch, 나머지는 axios 혼용 | **axios로 통일** (2026-06-12). 그리드 조회에도 인터셉터(스피너/언래핑/오류 모달) 적용 |

## 신규/수정 코드

| 파일 | 내용 |
|---|---|
| `pom.xml` | `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect` 의존성 추가. 이 의존성이 없으면 `layout:decorate`가 무시되어 화면이 레이아웃/CSS 없이 렌더링된다 |
| `controller/GlobalModelAdvice.java` | 신규. 인증된 요청의 화면 모델에 `user`(principal), `menus`(메뉴 tree)를 공통 주입 |
| `controller/HomeController.java` | `user`/`menus` 주입을 `GlobalModelAdvice`로 이관. profile/source 정보만 유지 |
| `config/SecurityConfig.java` | `/js/**`, `/lib/**`, `/vendor/**`, `/img/**`, `/favicon.ico` 정적 경로 permitAll 추가 |
| `templates/index.html` | `defaultLayout` decorate 구조로 재작성. 요약 카드와 메뉴 개요를 CoreUI 카드로 정렬 |
| `static/css/app.css` | 삭제. `defaultLayout` + `admin-common.css`로 대체 (`auth.css`는 login.html 전용으로 유지) |

## 검증 결과

2026-06-10 기준:

```text
mvn test                  PASS
mvn -DskipTests package   PASS
```

화면 확인 결과 (2026-06-10, 사용자 직접 확인):

```text
로그인 후 / 화면에서 CoreUI 레이아웃 정상 표시 확인 완료
```

## 남은 고려사항

- `GlobalModelAdvice`는 매 화면 요청마다 메뉴 tree를 조회한다. `/data` 계열 JSON endpoint가 추가되는 단계에서 advice 적용 범위 제한 또는 메뉴 캐싱을 검토한다.
- 사이드바 아이콘은 메뉴명 `th:switch` 분기다. 메뉴명이 바뀌면 `fragments/sidebar.html`도 함께 갱신해야 한다. 추후 `TB_MENU.ICON_NM` 기반으로 전환을 검토한다.

## 공통 자산 개선 후보 (폐쇄망 배포 전 검토)

| 후보 | 내용 | 우선순위 |
|---|---|---|
| 기간 검증 일반화 | `startDate`/`endDate` 고정 ID 검증을 `config.dateRangePairs: [['startDt','endDt']]`처럼 설정 기반으로 전환 | 중간 |
| `fmt` 상태값 외부화 | SUCCESS/FAIL/WAIT, SMS/LMS/ALIMTALK이 JS에 하드코딩. 상태값 추가 시 tui-common.js 수정 필요 — 공통코드 API 연동 검토 | 중간 |
| 엑셀 버튼 규약 통일 | 서버 대용량 엑셀(`btn-excel` → `/excel`)과 화면 엑셀(`TuiCommon.exportExcel`) 두 종류의 버튼 명칭/배치 규약 확정 | 중간 |
| `renderPagination` 전역 함수 제거 | `window[fnName]` + inline onclick 방식을 addEventListener로 전환 (전역 오염 제거) | 낮음 |
