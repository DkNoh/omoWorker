# 프로젝트 구조 및 구현 패턴 매뉴얼

> **대상 독자**: omoWorker(SMS전송시스템 v3) 코드베이스에 처음 진입하는 개발자, 폐쇄망에서 화면/API를 추가·수정해야 하는 개발자
> **문서 성격**: 프로젝트의 전체 구조(디렉터리·패키지·템플릿·정적 자산·설정·빌드)와 핵심 구현 패턴을 한 곳에서 파악하는 입문서. 개별 주제의 설계 근거는 `docs/base/` 원문을 참조한다.
> **관련 문서**: 문서 인덱스 `docs/base/README.md`, 환경/인증 `docs/base/environment-auth-policy.md`, 화면 규약 `docs/base/screen-convention.md`, 응답 계약 `docs/base/common-response-contract.md`, 메뉴/권한 `docs/base/menu-authority-table-design.md`, 디자인 토큰 `DESIGN.md`, 스캐폴드 `docs/menual/스케폴드_manual.md`, TUI 래퍼 `docs/menual/tui_manual.md`

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [디렉터리 구조](#2-디렉터리-구조)
3. [Java 패키지 구조](#3-java-패키지-구조)
4. [템플릿 구조 (Thymeleaf)](#4-템플릿-구조-thymeleaf)
5. [정적 자산 (Static Assets)](#5-정적-자산-static-assets)
6. [설정 (Configuration)](#6-설정-configuration)
7. [빌드 (Build)](#7-빌드-build)
8. [핵심 구현 패턴](#8-핵심-구현-패턴)
9. [명명 규약](#9-명명-규약)
10. [관련 문서 지도](#10-관련-문서-지도)

---

## 1. 프로젝트 개요

### 1.1 무엇인가

omoWorker(`artifactId: omo-worker`)는 **SMS/LMS/알림톡 발송과 발송 이력 조회를 다루는 백오피스 웹 시스템**이다. `smsTest-v3`에서 fork되어 hardened된 v3 BASE PROJECT로, 화면을 먼저 많이 만드는 대신 **폐쇄망에서도 안정적으로 반복 생성할 수 있는 기반(인증·권한·메뉴·응답 계약·화면 생성 도구)**을 먼저 확립하는 것을 목표로 한다.

### 1.2 기술 스택


| 계층              | 기술                                                                                           | 비고                                |
| ----------------- | ---------------------------------------------------------------------------------------------- | ----------------------------------- |
| 언어 / 런타임     | Java 21                                                                                        | `pom.xml` `java.version=21`         |
| 프레임워크        | Spring Boot 3.3.0                                                                              | `spring-boot-starter-parent`        |
| 웹 / 템플릿       | Spring MVC + Thymeleaf +`thymeleaf-layout-dialect`                                             | 서버 사이드 렌더링                  |
| 보안 / 인증       | Spring Security 6 +`spring-security-ldap`                                                      | profile별 인증 방식 분리            |
| 영속성            | MyBatis(`mybatis-spring-boot-starter` 3.0.3) + Oracle(`ojdbc11`)                               | Mapper XML 사용                     |
| SQL 로깅          | p6spy 3.9.1                                                                                    | local/dev 전용. prod(JNDI)는 미사용 |
| 검증 / AOP        | `spring-boot-starter-validation`, `spring-boot-starter-aop`                                    | `@Valid`, `@PrivacyLog`             |
| 관측성            | `spring-boot-starter-actuator` + `micrometer-registry-prometheus` + `logstash-logback-encoder` | JSON 구조화 로그(prod)              |
| API 문서          | `springdoc-openapi-starter-webmvc-ui` 2.6.0                                                    | Swagger UI`/docs`                   |
| 도메인 라이브러리 | Apache POI 5.2.3(엑셀), jsqlparser 4.9(스캐폴드 쿼리 분석)                                     |                                     |
| 편의              | Lombok(provided), spring-boot-devtools(runtime)                                                |                                     |
| 패키징            | **WAR** (`spring-boot-starter-tomcat` provided)                                                | 외부 Tomcat 배포                    |
| 코드 품질         | spotless(google-java-format), jacoco, surefire                                                 | 빌드 게이트                         |

### 1.3 폐쇄망(closed-network) 제약

이 프로젝트는 외부 네트워크 접근이 차단된 폐쇄망 배포를 전제로 한다. 다음 제약이 코드와 규약 전반에 관통한다.

- **CDN 참조 금지.** 모든 프론트엔드 라이브러리는 `static/lib/`, `static/vendor/`에 로컬 박제(vendoring)한다. 템플릿은 로컬 파일만 참조한다(근거: `.claude/rules/thymeleaf.md`, `static/lib/MANIFEST.md`).
- **프론트엔드 빌드 파이프라인 부재.** npm/webpack/bundler를 쓰지 않는다. 프로젝트가 직접 작성한 `static/css/*`, `static/js/**`는 minify하지 않고 원본 그대로 서빙한다.
- **비밀값은 환경변수/JNDI로만.** DB 비밀번호, LDAP manager password는 `application-*.yml`에 하드코딩하지 않고 `${ENV_VAR}` placeholder 또는 JNDI로 주입한다(근거: `docs/base/environment-auth-policy.md`).
- LDAP -> 그룹인증방식으로 변경될 예정
- **반입/배포는 체크리스트 기반.** 환경변수·DDL·소스 전환·검증 절차는 `docs/base/deployment-checklist.md`를 따른다.

### 1.4 설계 방향(인증과 권한의 분리)

인증 방식은 profile별로 다르게 두고, 권한 판단은 profile과 무관하게 하나의 v3 권한 모델로 통일한다.

```text
local  -> ID-only 인증 -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
dev    -> LDAP 인증    -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
prod   -> LDAP 인증    -> EMP 조회 -> v3 역할 조회 -> v3 메뉴 권한 조회
```

- 사용자는 항상 `(EMP_ID, DEP_ID)` 복합키로 식별한다.
- v3 권한은 `TB_ROLE`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH`로 분리하며, legacy `EMP.PERM_*` 컬럼은 사용하지 않는다.
- LDAP -> 그룹인증방식으로 변경될 예정(*)

---

## 2. 디렉터리 구조

프로젝트 루트 기준 전체 구조다.

```text
  omoWorker/
├── pom.xml                          # Maven 빌드 정의 (WAR, Java 21, Spring Boot 3.3.0)
├── mvnw, mvnw.cmd, .mvn/            # Maven Wrapper
├── docs/
│   ├── base/                        # 공통 설계 원문 + 구현 기록 (rules-index.md가 라우팅)
│   │   └── archive/                 # 보관 문서
│   ├── menual/                      # 개발자 매뉴얼 (본 문서, tui_manual.md, 스케폴드_manual.md)
│   ├── 메뉴.md, 승인.md, js분석.md     # 주제별 설계/분석 메모
├── src/
│   ├── main/
│   │   ├── java/com/scbk/sms/       # 애플리케이션 소스 (3장 참조)
│   │   └── resources/
│   │       ├── application.yml           # 공통 설정 + profile 라우팅
│   │       ├── application-local.yml     # local profile
│   │       ├── application-dev.yml       # dev profile
│   │       ├── application-prod.yml      # prod profile
│   │       ├── logback-spring.xml        # 로그 설정 (prod JSON 구조화)
│   │       ├── spy.properties            # p6spy 설정
│   │       ├── mapper/                   # MyBatis Mapper XML (auth/basic/menu/sms/system)
│   │       ├── scaffold-templates/       # Query Scaffold 코드 템플릿 (.tpl)
│   │       ├── scaffold-cases/           # 스캐폴드 생성 사례 기록 (.json)
│   │       ├── static/                   # 정적 자산 (5장 참조)
│   │       └── templates/                # Thymeleaf 템플릿 (4장 참조)
│   └── test/java/com/scbk/sms/      # 단위/계약 테스트 (ConventionTest 포함)
└── target/                          # 빌드 산출물 (omoWorker.war)
```

### 2.1 주요 디렉터리 책임


| 디렉터리                                 | 책임                                                                                                                              |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `src/main/java/com/scbk/sms/`            | 서버 애플리케이션 전체. 계층별 패키지는 3장 참조                                                                                  |
| `src/main/resources/mapper/`             | MyBatis Mapper XML. Java Mapper 인터페이스와`namespace`로 1:1 대응. `mybatis.mapper-locations: classpath:/mapper/**/*.xml`로 로드 |
| `src/main/resources/templates/`          | Thymeleaf HTML.`defaultLayout.html` + `fragments/` + 도메인별 페이지                                                              |
| `src/main/resources/static/`             | 브라우저에 서빙되는 정적 자산. CSS/JS/라이브러리/이미지                                                                           |
| `src/main/resources/scaffold-templates/` | Query Scaffold가 화면 생성 시 사용하는`.tpl` 템플릿                                                                               |
| `db/oracle/`                             | DBA 전달용 DDL/seed. 운영 DDL에는`DROP TABLE`을 넣지 않는다                                                                       |
| `docs/base/`                             | 설계 근거 원문.`rules-index.md`가 작업별 읽을 문서를 라우팅                                                                       |
| `docs/menual/`                           | 개발자 매뉴얼. 본 문서가 속한 디렉터리                                                                                            |

---

## 3. Java 패키지 구조

루트 패키지는 `com.scbk.sms`다. 진입점은 `SmsV3Application.java`(Spring Boot main), WAR 배포를 위해 `ServletInitializer.java`가 함께 있다. 계층별·도메인별 하위 패키지는 다음 표와 같다.

### 3.1 최상위 패키지


| 패키지       | 책임                                     | 대표 클래스                                                                                                             |
| ------------ | ---------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `annotation` | 사용자 정의 어노테이션 정의              | `PrivacyLog`(민감 정보 조회 감사 기록)                                                                                  |
| `aop`        | 어노테이션 구동 AOP Aspect               | `PrivacyLogAspect`(`@PrivacyLog` 메서드 호출을 `TB_PRIVACY_AUDIT_LOG`에 기록)                                           |
| `auth`       | 인증 주체(principal)와 인증 provider     | `SmsUserPrincipal`, `LocalIdOnlyAuthenticationProvider`, `LdapEmployeeContextMapper`, `ActiveEmployeeResolver`          |
| `config`     | Spring 설정·인터셉터·필터·가드        | `SecurityConfig`, `WebMvcConfig`, `MenuAuthInterceptor`, `AuthSourceGuard`, `LdapAuthenticationConfig`, `TraceIdFilter` |
| `controller` | HTTP 진입점. 화면 렌더링 + JSON API      | `HomeController`, `LoginController`, `GlobalModelAdvice` + 도메인별 하위                                                |
| `dto`        | 요청/응답 전송 객체.`@Valid` 검증 대상   | `ApiResponse`, `PageRequestDTO`, `PageResponseDTO` + 도메인별 하위                                                      |
| `exception`  | 전역 예외/에러 코드                      | `CustomException`, `ErrorCode`, `GlobalExceptionHandler`                                                                |
| `mapper`     | MyBatis Mapper 인터페이스 (DB 접근)      | 도메인별`*Mapper`                                                                                                       |
| `service`    | 비즈니스 로직. 트랜잭션 경계             | 도메인별`*Service` + `menu` 권한 서비스                                                                                 |
| `util`       | 상태 없는 공통 유틸                      | `MaskingUtil`(개인정보 마스킹), `ExcelUtil`(엑셀 다운로드)                                                              |
| `vo`         | DB 행(row) 매핑 객체. Mapper`resultType` | 도메인별`*VO`                                                                                                           |

### 3.2 도메인별 하위 패키지

`controller`, `dto`, `mapper`, `service`, `vo`는 쓰기 소유권 기준의 도메인별로 다시 나뉜다. 메뉴와 도메인은 서로 다른 축이며 상세 판단은 `docs/base/domain-boundary-guide.md`를 따른다.


| 도메인   | 의미                                           | 해당 패키지 예시                                                                 |
| -------- | ---------------------------------------------- | -------------------------------------------------------------------------------- |
| `basic`  | 기본메뉴 (공지사항 등)                         | `controller.basic.NoticeController`, `service.basic.NoticeService`               |
| `sms`    | SMS발송조회/캠페인SMS (고객조회, 발송이력)     | `controller.sms.CustomerSearchController`, `controller.sms.SmsHistoryController` |
| `system` | 시스템관리 (메뉴관리, 공통코드, 스캐폴드)      | `controller.system.MenuManageController`, `controller.system.ScaffoldController` |
| `menu`   | 메뉴/역할/권한 공통 (service·mapper·vo 전용) | `service.menu.MenuAuthService`, `mapper.menu.MenuAuthMapper`                     |
| `auth`   | 로그인 사용자 조회 (mapper·vo 전용)           | `mapper.auth.LoginEmployeeMapper`, `vo.auth.LoginEmployeeVO`                     |
| `common` | 공통 코드/페이지 공통 (dto·vo 전용)           | `dto.common.ApiResponse`, `vo.common.CommonCodeVO`                               |

> `service.system.scaffold` 하위 패키지에는 Query Scaffold 구현체(`ScaffoldService`, `QueryColumnExtractor`, `ScaffoldFileApplier` 등)가 모여 있다. local 전용 도구이며 상세는 `docs/menual/스케폴드_manual.md` 참조.

> 표의 `basic`, `sms`, `system` 화면은 현재 구조를 설명하기 위한 테스트·참고 구현이다. 운영 업무 요구사항이나 신규 화면 설계의 기준으로 사용하지 않는다.

### 3.3 VO와 DTO의 구분

- **VO** (`vo.*`): DB 조회 결과를 담는 객체. Mapper XML의 `resultType`으로 지정되며, Oracle snake_case 컬럼이 `map-underscore-to-camel-case`로 camelCase 필드에 매핑된다.
- **DTO** (`dto.*`): HTTP 요청/응답을 담는 객체. 검색 요청(`*SearchRequestDTO`), 등록/수정 요청(`*UpdateRequestDTO`), 공통 응답(`ApiResponse`, `PageResponseDTO`)으로 나뉜다. 수정 요청 DTO는 **화이트리스트 방식**으로 서버가 받는 필드만 명시한다.

---

## 4. 템플릿 구조 (Thymeleaf)

### 4.1 디렉터리 구성

```text
templates/
├── defaultLayout.html      # 전체 공통 레이아웃 (사이드바/헤더/콘텐츠 + 공통 CSS/JS 로드)
├── index.html              # 로그인 후 기본 홈
├── login.html              # 로그인 페이지 (유일하게 layout을 decorate 하지 않음)
├── error/
│   └── error.html          # 공통 에러 페이지 (GlobalExceptionHandler가 text/html 요청 시 렌더)
├── fragments/              # 재사용 조각
│   ├── sidebar.html        # 좌측 메뉴 (Controller가 넘긴 메뉴 tree만 렌더)
│   ├── header.html         # 상단 헤더 (사용자 정보/로그아웃)
│   ├── toast-grid.html     # 목록 그리드 카드 뼈대 (#grid, #pagination, #total-count)
│   └── modal-base.html     # 비즈니스 모달 표준 뼈대 (modal-manager.js와 DOM id 계약)
├── basic/                  # 기본메뉴 화면 (notice.html, notice-popup.html)
├── sms/                    # SMS 화면 (customer-search.html, history.html)
└── system/                 # 시스템관리 화면 (menu-manage.html, menu-tree.html, scaffold.html)
```

### 4.2 Layout Dialect 패턴

업무 화면은 `thymeleaf-layout-dialect`로 `defaultLayout.html`을 장식(decorate)한다. 페이지는 콘텐츠와 스크립트만 채우고, 공통 셸(사이드바·헤더)과 공통 CSS/JS는 레이아웃이 책임진다.

```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{defaultLayout}">
<body>
<main layout:fragment="content">
    <!-- 화면 고유 콘텐츠 -->
    <div th:replace="~{fragments/toast-grid :: gridCard}"></div>
</main>
<th:block layout:fragment="script">
    <script th:src="@{/js/sms/history.js}"></script>
</th:block>
</body>
</html>
```

- `layout:fragment="content"` — 페이지 본문이 끼워지는 자리.
- `layout:fragment="css"` / `layout:fragment="script"` — 페이지 전용 CSS/JS를 추가하는 자리(head/body 말미).
- `login.html`만 예외적으로 레이아웃을 사용하지 않는다.

### 4.3 defaultLayout.html이 로드하는 공통 자산

`defaultLayout.html`은 모든 화면의 공통 기반을 한 곳에서 로드한다. 로드 순서가 계약이므로 임의 변경 금지.

1. **라이브러리** (`/lib/*`): tui-pagination, xlsx, tui-date-picker, tui-grid, axios, dayjs(+ko 로케일), lucide, imask, just-validate
2. **CoreUI** (`/vendor/coreui/css/coreui.min.css`) — base component library
3. **프로젝트 CSS** (순서 중요): `admin-common.css`(토큰) → `admin-layout.css`(셸) → `admin-ui-bridge.css`(브리지) → `admin-form-detail.css`(상세폼)
4. **공통 JS** (의존 순서): `notify.js` → `http-client.js` → `modal-manager.js` → `common-utils.js` → `form-binder.js` → `field-format.js` → `tui-common.js` → `tui-page-builder.js`
5. **전역 변수 주입**: `SESSION_INFO`(empId/depId/empNm/depNm)와 `PAGE_AUTH`(read/create/update/delete/approve/cancel/download/maskView)을 `th:inline="javascript"`로 노출. 사용자 식별은 `EMP_ID + DEP_ID`.
6. **CSRF 메타**: `<meta name="_csrf">` / `<meta name="_csrf_header">` — `http-client.js` 인터셉터가 axios 요청 헤더로 싣는다.

### 4.4 Fragment 계약

- **`fragments/sidebar.html`** — 메뉴 렌더링은 Controller가 넘긴 메뉴 tree(`menus`)만 사용한다. 화면에서 권한을 임의 계산하지 않는다.
- **`fragments/toast-grid.html`** — `#grid`는 **빈 마운트 포인트**로 유지해야 한다(TUI Grid가 직접 DOM을 구성). 총 건수 `#total-count`, 페이지 크기 `#pageSizeSelect`, 페이징 `#pagination` id가 고정 계약이다.
- **`fragments/modal-base.html`** — `modal-manager.js`와 DOM id 계약을 맞춘다: 모달 `${modalId}`, 제목 `${modalId}-title`, 저장 `${modalId}-btn-save`, 삭제 `${modalId}-btn-delete`.

---

## 5. 정적 자산 (Static Assets)

```text
static/
├── css/        # 프로젝트 작성 CSS (6종, minify 안 함)
├── js/
│   ├── common/ # 공통 모듈 8종 (모든 화면이 defaultLayout에서 로드)
│   ├── basic/  # 기본메뉴 화면 JS
│   ├── sms/    # SMS 화면 JS
│   └── system/ # 시스템관리 화면 JS
├── lib/        # vendored 라이브러리 (CDN 대체, MANIFEST.md로 관리)
├── vendor/     # CoreUI (css + bundle js)
├── img/        # 이미지 (SC.png 로고)
└── samples/    # 정적 샘플 페이지 (상세폼 행 패턴 참조용. prod는 404 차단)
```

### 5.1 CSS 파일 (6종)

CSS는 계층(layer) 구조로 로드되며, 프로젝트 CSS/템플릿은 `--sms-*` 토큰만 참조한다(직접 `--cui-*` 참조는 토큰 파일과 브리지 계층에만 한정). 상세는 `DESIGN.md` 참조.


| 파일                    | 계층       | 역할                                                                                                                                                         |
| ----------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `admin-common.css`      | 2 (토큰)   | **디자인 토큰 레이어.** 색상/타이포그래피/간격/반경/그림자/모션의 단일 출처. `--sms-*` 토큰을 정의하고 CoreUI `--cui-*` 변수에 별칭 매핑. CoreUI 직후에 로드 |
| `admin-layout.css`      | 3 (셸)     | **관리자 셸 레이아웃.** 사이드바/헤더/콘텐츠 영역의 geometry와 페이지 타이포그래피 앵커. `--sms-*` 토큰 소비                                                 |
| `admin-ui-bridge.css`   | 4 (브리지) | **UI 브리지.** CoreUI 컴포넌트와 TUI grid/picker, Lucide 아이콘 사이의 시각적 연결. 토큰 기반 보강만 하고 CoreUI 클래스를 대체하지 않음                      |
| `admin-form-detail.css` | 5 (상세폼) | **상세폼 행 패턴.** 등록/수정 화면의 공통 폼 행 레이아웃(`.form-detail-row`). `static/samples/`가 라이브 참조                                                |
| `auth.css`              | 로그인     | **인증 페이지.** 로그인 화면 스타일. `admin-common.css` 이후 로드하여 동일 디자인 시스템 공유                                                                |
| `scaffold-tool.css`     | 도구       | **Query Scaffold 페이지 전용.** 스캐폴드 화면만의 소규모 보정                                                                                                |

### 5.2 JS 공통 모듈 (8종)

`js/common/`의 8개 모듈은 라이브러리와 화면 JS 사이의 프로젝트 전용 래퍼/인프라 계층이다. `defaultLayout.html`이 의존 순서대로 모두 로드한다.


| 모듈                  | 노출 전역                              | 역할                                                                                                                                                                  |
| --------------------- | -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `notify.js`           | `window.Notify`                        | 화면 공통 알림 — toast/alert/confirm(큐 기반 순차 처리), CoreUI Modal 도우미, Lucide 아이콘 갱신                                                                     |
| `http-client.js`      | `window.HttpClient`(+`ApiClient` 호환) | 전역 HTTP 인프라 — axios interceptor, 글로벌 spinner, 세션 만료 처리,`get/post/put/delete/remove` 래퍼. CSRF 메타를 헤더로 적재                                      |
| `modal-manager.js`    | `window.ModalManager`                  | 개발자 수동 비즈니스 모달 lifecycle — 폼/검증 훅(onMount/beforeOpen/onOpen/onSubmit/onDelete/onClose)과 CoreUI Modal 인스턴스 중앙화.`modal-base.html`과 DOM id 계약 |
| `common-utils.js`     | `window.CommonUtils`                   | 순수 유틸 — 공통코드 콤보박스 자동 생성(`.common-combo[data-code-type]`), 날짜/검색/포맷/autocomplete. toast/alert/confirm은 Notify로 하위 호환 연결                 |
| `form-binder.js`      | `window.FormBinder`                    | 상세폼 자동 바인딩 — 조회 응답을 form에 채우고(`name` = JSON 필드 = UpdateRequestDTO 프로퍼티), 폼 값을 객체로 직렬화. 마스크 필드는 unmaskedValue 전송              |
| `field-format.js`     | `window.FieldFormat`                   | 입력 포맷 마스킹(IMask) + 클라이언트 검증(JustValidate) —`data-mask`, `data-validate` 속성 선언 방식. 서버 `@Valid`가 최종 권위                                      |
| `tui-common.js`       | `window.TuiCommon`                     | TUI 공통 래퍼 — formatter(날짜/배지/마스킹), 그리드 기본 옵션, 총 건수 갱신, 페이징 렌더링, 엑셀 내보내기                                                            |
| `tui-page-builder.js` | `window.TuiPageBuilder`                | 목록 화면 Facade — 그리드 초기화/검색/페이징/비동기 조회를 한 곳에서 자동화.**모든 목록 그리드는 이 모듈로만 초기화**                                                |

> `tui-common.js`·`tui-page-builder.js`의 상세 계약은 `docs/menual/tui_manual.md` 참조.

### 5.3 vendored 라이브러리 (`lib/`, `vendor/`)

CDN을 쓰지 않고 로컬에 박제한 라이브러리다. 추가/교체 시 `static/lib/MANIFEST.md`의 버전·출처·라이선스를 함께 갱신해야 한다.


| 디렉터리                    | 주요 라이브러리                                               | 용도                                                           |
| --------------------------- | ------------------------------------------------------------- | -------------------------------------------------------------- |
| `lib/`                      | tui-grid 4.21.22, tui-date-picker 4.3.3, tui-pagination 3.4.1 | 목록 그리드/날짜선택/페이징                                    |
| `lib/`                      | axios 1.16.1, dayjs(+ko), xlsx(SheetJS)                       | HTTP/날짜/엑셀                                                 |
| `lib/`                      | imask 7.6.1, just-validate 4.3.0                              | 입력 마스킹/클라 검증                                          |
| `lib/`                      | lucide.js (**커스텀 서브셋**)                                 | 아이콘. 공식`.min`으로 교체 금지(사용 아이콘만 추린 자체 구현) |
| `lib/toastui-editor/3.2.2/` | TOAST UI Editor                                               | 공지 팝업 본문 에디터 (`all` 번들, LICENSE 동봉)               |
| `vendor/coreui/`            | CoreUI (css + bundle js)                                      | base component library (.btn/.card/.form-control/.modal 소유)  |

---

## 6. 설정 (Configuration)

### 6.1 application.yml (공통)

`application.yml`은 모든 profile의 공통 설정을 담고, `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`로 profile을 라우팅한다(기본 local).


| 설정                                                 | 값                                         | 의미                                          |
| ---------------------------------------------------- | ------------------------------------------ | --------------------------------------------- |
| `spring.thymeleaf.cache`                             | `false`                                    | 템플릿 캐시 비활성 (개발 편의)                |
| `mybatis.mapper-locations`                           | `classpath:/mapper/**/*.xml`               | Mapper XML 로드 경로                          |
| `mybatis.configuration.map-underscore-to-camel-case` | `true`                                     | Oracle snake_case → Java camelCase 자동 매핑 |
| `mybatis.configuration.jdbc-type-for-null`           | `NULL`                                     | null 파라미터 JDBC 타입                       |
| `server.port`                                        | `${SERVER_PORT:8081}`                      | 기본 8081                                     |
| `server.servlet.session.tracking-modes`              | `cookie`                                   | 세션 추적은 쿠키만 (URL 재작성 비활성)        |
| `management.endpoints...`                            | health/info/metrics/prometheus/env/loggers | Actuator 노출                                 |
| `springdoc.swagger-ui.path`                          | `/docs`                                    | Swagger UI 경로                               |

### 6.2 profile별 차이 (local / dev / prod)

환경별 차이는 **인증 방식과 외부 접속정보에 한정**한다. 사용자/부서/역할/메뉴 권한 판단 로직은 모든 환경에서 동일하다. 상세는 `docs/base/environment-auth-policy.md` 참조.


| 항목                            | local                                                     | dev                            | prod                                    |
| ------------------------------- | --------------------------------------------------------- | ------------------------------ | --------------------------------------- |
| 인증 방식 (`sms.auth.mode`)     | `local` (ID-only)                                         | `ldap`                         | `ldap`                                  |
| 비밀번호 검증                   | 하지 않음 (`EMP_ID`만 입력)                               | LDAP                           | LDAP                                    |
| 메뉴 source (`sms.menu.source`) | `static`                                                  | `db`                           | `db`                                    |
| 역할 source (`sms.role.source`) | `static` (기본 ROLE_ADMIN)                                | `db`                           | `db`                                    |
| 데이터소스                      | p6spy + Oracle thin (로컬)                                | p6spy + Oracle thin (dev)      | **JNDI** (`java:/comp/env/jdbc/SMS`)    |
| LDAP 비밀번호                   | —                                                        | `${SMS_LDAP_MANAGER_PASSWORD}` | `${SMS_LDAP_MANAGER_PASSWORD}`          |
| 로그 레벨                       | `com.scbk.sms: DEBUG`                                     | 기본                           | `root/com.scbk.sms: INFO` (JSON 구조화) |
| 메뉴 권한 제외 경로             | `/system/scaffold/**`, `/system/menu-tree/**` (개발 도구) | —                             | —                                      |

> `AuthSourceGuard`가 부팅 시 source 조합을 강제한다. prod에 `static`이 들어가거나 `menu=db`+`role=static` 조합이면 부팅이 실패한다.

### 6.3 WebMvcConfig와 인터셉터

`config.WebMvcConfig`는 `WebMvcConfigurer` 구현으로 인터셉터를 등록한다.

- **`MenuAuthInterceptor`** — `/**`에 적용. 좌측 메뉴 표시와 별개로 **모든 URL/API 요청을 `TB_MENU_AUTH` 기준으로 다시 검증**한다. 권한이 없으면 `CustomException(ACCESS_DENIED)`을 던지고 `GlobalExceptionHandler`가 403으로 변환한다. 공통 경로(`/`, `/login`, `/error`, 정적 자산 `/css·/js·/lib·/vendor·/img/**`, `/api/common-code/**`, `/samples/**`)는 제외하며, `sms.menu.auth.exclude-paths` 설정으로 추가 제외할 수 있다. **새 화면 URL은 제외 경로에 추가하지 않고 메뉴 등록+권한 부여로 접근을 연다.**
- **`SamplesBlockInterceptor`** — `/samples/**`를 prod에서 404로 차단(빈이 존재할 때만 등록).
- **`TraceIdFilter`** (`TraceIdFilterConfig`) — 요청 추적용 trace id 필터.

### 6.4 SecurityConfig

`config.SecurityConfig`는 Spring Security 필터 체인을 정의한다.

- CSRF 활성화(기본값). Thymeleaf 폼(`th:action`)은 토큰 자동 주입, axios는 `http-client.js`가 `<meta name="_csrf">`를 헤더로 적재.
- `/login`, `/error`, 정적 자산은 `permitAll`, 그 외는 `authenticated()`.
- 폼 로그인: `usernameParameter=empId`, `passwordParameter=password`, 성공 시 `/`, 실패 시 `/login?error`.
- 인증 provider는 profile에 따라 `LocalIdOnlyAuthenticationProvider`(local) 또는 LDAP provider(dev/prod)가 주입된다.

---

## 7. 빌드 (Build)

### 7.1 빌드 도구와 명령

빌드는 **Maven**만 사용한다. 프론트엔드 빌드 파이프라인(npm/bundler)은 없다.

```bash
mvn test                    # 전체 테스트 실행 (완료 필수 조건)
mvn -DskipTests package     # WAR 패키징 (target/omoWorker.war, finalName=omoWorker)
```

코드 변경이 있는 작업은 `mvn test`와 `mvn -DskipTests package`를 **모두 통과**해야 완료다. 문서만 변경한 작업은 Maven 실행 대상이 아니다. 서버 기동은 AI가 직접 하지 않고 사용자가 담당한다.

### 7.2 빌드 게이트 (plugins)


| 플러그인                   | 역할                                                                                                                 |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `spring-boot-maven-plugin` | 실행 가능한 WAR 패키징                                                                                               |
| `maven-surefire-plugin`    | 테스트 실행 (`-Djava.awt.headless=true`, jacoco argLine 연동)                                                        |
| `spotless-maven-plugin`    | 코드 스타일 게이트 — google-java-format(GOOGLE style), 미사용 import 제거, 후행 공백 제거.`verify` 단계에서 `check` |
| `jacoco-maven-plugin`      | 커버리지 리포트 (`test` 단계)                                                                                        |
| `exec-maven-plugin`        | 스캐폴드 진단·폐기 가능한 화면의 명시적 재생성 등 유틸 실행                                                         |

### 7.3 테스트 구조

`src/test/java/com/scbk/sms/`에 단위/계약 테스트가 있다. 주요 규약(근거: `.claude/rules/testing.md`, `docs/base/test-automation-guide.md`):

- Service 단위 테스트는 Mapper를 **Mockito로 mock** 처리한다.
- Controller 테스트는 `@Valid` 실패와 공통 `ApiResponse` 에러 포맷을 확인한다.
- **`ConventionTest`**가 규약(DTO 상속, `/save` 금지, `SELECT *` 금지, 정렬 필수, Lombok)을 자동 검증한다.
- 테스트는 given/when/then 구조를 사용한다. LDAP은 local에서 사용할 수 없으므로 mock 또는 profile로 분리한다.

---

## 8. 핵심 구현 패턴

### 8.1 Controller → Service → Mapper 흐름

모든 업무 기능은 3계층 흐름을 따른다. 아래 `CustomerSearch`는 계층 연결을 설명하는 테스트·참고 구현이며 운영 업무 정의의 근거가 아니다.

```text
HTTP 요청
   │
   ▼
Controller (controller.sms.CustomerSearchController)
   │  - @Controller + @RequestMapping("/sms/customer-search")
   │  - 화면: @GetMapping → "sms/customer-search" (템플릿명 반환)
   │  - API : @ResponseBody + ResponseEntity<ApiResponse<...>>
   │  - @Valid로 요청 DTO 검증, 업무 로직은 Service 위임
   ▼
Service (service.sms.CustomerSearchService)
   │  - @Service + @Transactional (조회는 readOnly=true)
   │  - request.validate() 호출, Mapper 결과로 PageResponseDTO 조립
   │  - 영향 행 0건이면 CustomException(ErrorCode.*) 발생
   ▼
Mapper (mapper.sms.CustomerSearchMapper + mapper/sms/CustomerSearchMapper.xml)
      - 인터페이스 + XML(namespace로 1:1 대응)
      - count / selectList / selectDetail / insert / update / delete
```

**Controller 규칙**

- 화면 메서드는 템플릿 경로 문자열을 반환하고, JSON API 메서드는 `@ResponseBody` + `ResponseEntity<ApiResponse<T>>`를 반환한다.
- 등록/수정 endpoint는 **`/create`, `/update`로 분리**한다. 등록/수정 겸용 `/save`는 신규 코드에서 만들지 않는다.
- Controller에서 권한을 중복 검사하지 않는다(URL/API 접근 검증은 `MenuAuthInterceptor` 담당).

**Service 규칙**

- 트랜잭션 경계는 Service에 둔다. 조회는 `@Transactional(readOnly = true)`.
- 업무 규칙 검증(중복 체크, 상태 전이 등)은 TODO 위치에 직접 추가한다.
- update/delete는 영향 행 수를 확인해 0건이면 `UPDATE_CONFLICT`/`DELETE_CONFLICT`를 던진다(낙관적 잠금/대상 없음 처리).

**Mapper 규칙** (근거: `.claude/rules/mybatis-oracle.md`)

- Oracle 19c 기준. `SELECT *` 금지(명시 컬럼 파생 테이블의 `SELECT A.*`는 허용).
- LIKE는 `LIKE '%' || #{keyword} || '%'`, 페이지 조회는 `OFFSET ... FETCH NEXT ...` + 결정적 `ORDER BY`.
- count 쿼리와 목록 쿼리는 같은 검색조건(`<sql id="searchConditions">` 재사용)을 쓴다.
- update는 수정 컬럼만 SET하고, WHERE에 PK + `UPDATE_DTTM` 낙관적 잠금 조건을 둔다.

### 8.2 ApiResponse 공통 응답 계약

모든 JSON API 응답은 `dto.common.ApiResponse<T>` 규격으로 감싼다. 성공/실패와 관계없이 프론트엔드는 항상 이 규격을 받는다(근거: `docs/base/common-response-contract.md`).

```json
{
  "timestamp": "2026-07-28T10:30:00",
  "code": 200,
  "message": "SUCCESS",
  "data": { ... },
  "errors": null
}
```


| 필드      | 의미                                                               |
| --------- | ------------------------------------------------------------------ |
| `code`    | HTTP 상태 코드 (성공 200)                                          |
| `message` | 메시지 (성공 시 "SUCCESS" 또는 등록/수정 안내 문구)                |
| `data`    | 응답 본문 (목록은`PageResponseDTO<VO>`)                            |
| `errors`  | `@Valid` 실패 시 필드별 에러 `[{field, message}]` (그 외에는 null) |

- 성공: `ApiResponse.success(data)` / `ApiResponse.success("등록되었습니다.", null)`
- 실패: `ApiResponse.error(code, message)` / `ApiResponse.error(code, message, errors)`
- 목록 페이지 응답은 `PageResponseDTO.of(list, request, totalCount)`로만 생성한다(`contents`, `page`, `size`, `totalCount`, `totalPages`, `hasNext`, `hasPrev`).
- 검색 요청 DTO는 `PageRequestDTO`를 상속한다(`page`, `size`, `keyword`, `searchType` + `validate()`로 size 1~100 보정).

### 8.3 전역 예외 처리 (GlobalExceptionHandler)

`exception.GlobalExceptionHandler`(`@ControllerAdvice`)는 Controller가 삼키지 않은 예외를 한 곳에서 변환한다. **요청의 `Accept` 헤더로 응답 형태를 분기**한다.

- `Accept: text/html` (브라우저 화면 이동) → `error/error` ModelAndView(공통 에러 페이지)
- 그 외 (axios/fetch JSON) → `ApiResponse.error(...)` ResponseEntity

처리 대상: `CustomException`(비즈니스), `MethodArgumentNotValidException`(`@Valid` 실패 — 필드별 errors 포함), 요청값 변환 실패, 메서드/미디어타입 미지원, `NoResourceFoundException`(404 — 컨트롤러 미등록/메뉴 URL 오타/파일 미배치 안내), 그 외 `Exception`(500). 비즈니스 에러 코드는 `ErrorCode` enum에 모아둔다(공통 Cxxx, 권한 Axxx, 사용자 Uxxx, 메뉴 Mxxx).

### 8.4 PageAuth 모델과 GlobalModelAdvice

**`GlobalModelAdvice`** (`controller.GlobalModelAdvice`, `@ControllerAdvice(annotations = Controller.class)`)는 defaultLayout(sidebar/header)이 모든 화면에서 쓰는 공통 모델을 채우는 **단일 적용 지점**이다. 모든 `@Controller` 요청에 `@ModelAttribute`로 다음을 주입한다.


| 모델 속성  | 내용                                                |
| ---------- | --------------------------------------------------- |
| `user`     | 로그인 principal (`SmsUserPrincipal`)               |
| `menus`    | 역할 기반 메뉴 tree (세션 캐시)                     |
| `pageAuth` | 현재 메뉴의 기능 권한 (`PageAuth`)                  |
| `clientIp` | 클라이언트 IP (X-Forwarded-For 우선, 루프백 정규화) |

**`PageAuth`** (`service.menu.PageAuth`)는 화면 렌더링과 공통 JS가 사용하는 현재 메뉴의 기능 권한이다. 8개 불리언(`read/create/update/delete/approve/cancel/download/maskView`)을 가지며, `MenuPermission` enum 집합에서 `PageAuth.from(...)`으로 생성한다. local profile은 `PageAuth.all()`(모두 허용), 미인증은 `PageAuth.none()`이다.

- 주입된 `pageAuth`는 `defaultLayout.html`이 `window.PAGE_AUTH` 전역 변수로 노출한다.
- 화면은 이 값으로 버튼 표시를 제어한다(예: `th:if="${pageAuth.download}"` 엑셀 버튼).
- **화면 표시 권한(PageAuth)과 실제 API 차단(MenuAuthInterceptor)은 분리**되어 있다. PageAuth는 UI 보조일 뿐, 최종 접근 통제는 Interceptor가 수행한다.

### 8.5 인증/권한 부가 패턴

- **`@PrivacyLog` + `PrivacyLogAspect`** — 민감 정보 조회/다운로드 메서드에 `@PrivacyLog(action="...")`를 붙이면 AOP가 `SMS.TB_PRIVACY_AUDIT_LOG`에 감사 이력을 자동 기록한다. 적용 기준은 `docs/base/audit-masking-policy.md` 참조.
- **`MaskingUtil`** — 이름/전화/주민번호/카드번호를 화면 표시·로그 기록 전에 마스킹한다(예: `010-****-5678`).
- **`ExcelUtil`** — 목록 엑셀 다운로드(POI). 최대 건수 초과 시 `EXCEL_ROW_LIMIT_EXCEEDED`.

---

## 9. 명명 규약

### 9.1 언어 규칙

- **주석/문서/메시지는 한국어**, **식별자(클래스·메서드·변수·파일명)는 영어**를 사용한다.
- 에러 메시지, 화면 라벨, 로그 메시지 본문은 한국어로 작성한다.
- Javadoc도 한국어로 작성한다(예: `/** 목록 조회 공통 페이지 응답. */`).

### 9.2 파일/클래스 명명 패턴


| 대상               | 패턴                                                              | 예시                                  |
| ------------------ | ----------------------------------------------------------------- | ------------------------------------- |
| Controller         | `{Domain}Controller`                                              | `CustomerSearchController`            |
| Service            | `{Domain}Service`                                                 | `CustomerSearchService`               |
| Mapper 인터페이스  | `{Domain}Mapper`                                                  | `CustomerSearchMapper`                |
| Mapper XML         | `{Domain}Mapper.xml` (인터페이스와 동명, `mapper/{domain}/` 하위) | `mapper/sms/CustomerSearchMapper.xml` |
| VO                 | `{Domain}VO`                                                      | `CustomerSearchVO`                    |
| 검색 요청 DTO      | `{Domain}SearchRequestDTO`                                        | `CustomerSearchSearchRequestDTO`      |
| 등록/수정 요청 DTO | `{Domain}UpdateRequestDTO`                                        | `CustomerSearchUpdateRequestDTO`      |
| 테스트             | `{Class}Test`                                                     | `CustomerSearchServiceTest`           |
| 페이지 템플릿      | `{domainId}.html` (URL 말미와 일치)                               | `customer-search.html`                |
| 화면 JS            | `{domainId}.js` (`js/{domain}/` 하위)                             | `js/sms/customer-search.js`           |

> 여기서 `{Domain}`은 PascalCase 도메인 클래스명, `{domainId}`는 URL/파일명에 쓰는 kebab-case 식별자다.

### 9.3 URL/endpoint 규약

- 화면 URL과 메뉴 URL은 `TB_MENU` 또는 static baseline과 일치해야 한다.
- 목록 데이터 API: `GET {화면URL}/data`
- 등록/수정/삭제: `POST {화면URL}/create`, `/update`, `/delete` (`/save` 금지)
- 원문(마스크 해제) 조회: `GET {화면URL}/unmask` (`@PrivacyLog` 부착)
- 고정 버튼/그리드 id: `btn-search`, `btn-reset`, `btn-excel`, `grid`, `pagination`, `total-count`

### 9.4 코드 스타일

- Lombok 사용: `@RequiredArgsConstructor`(생성자 주입), DTO는 `@Data`.
- google-java-format(GOOGLE style) — spotless가 `verify` 단계에서 강제.
- Controller는 생성자 주입(`@RequiredArgsConstructor` + `private final`).

---

## 10. 관련 문서 지도

작업 유형별로 읽을 문서는 `docs/base/rules-index.md`가 라우팅한다. 본 문서와 직접 연관된 문서는 다음이다.


| 주제                             | 문서                                        |
| -------------------------------- | ------------------------------------------- |
| 문서 인덱스/프로젝트 목표        | `docs/base/README.md`                       |
| 작업별 읽을 문서 라우팅          | `docs/base/rules-index.md`                  |
| 환경/인증 정책 (local/dev/prod)  | `docs/base/environment-auth-policy.md`      |
| 사용자/부서 식별 (EMP/DEP)       | `docs/base/emp-dep-identity-policy.md`      |
| 화면 규약 (레이아웃/그리드/명명) | `docs/base/screen-convention.md`            |
| JSON 공통 응답 계약              | `docs/base/common-response-contract.md`     |
| 메뉴/권한 테이블 설계            | `docs/base/menu-authority-table-design.md`  |
| 메뉴 source 정책 (static/db)     | `docs/base/menu-source-policy.md`           |
| 디자인 토큰 계층                 | `DESIGN.md`                                 |
| 화면 생성 절차 체크리스트        | `docs/base/screen-generation-guide.md`      |
| 폐쇄망 반입 체크리스트           | `docs/base/deployment-checklist.md`         |
| 테스트 자동화 가이드             | `docs/base/test-automation-guide.md`        |
| vendored 라이브러리 목록         | `src/main/resources/static/lib/MANIFEST.md` |
| Query Scaffold 매뉴얼            | `docs/menual/스케폴드_manual.md`            |
| TUI 공통 모듈 매뉴얼             | `docs/menual/tui_manual.md`                 |
