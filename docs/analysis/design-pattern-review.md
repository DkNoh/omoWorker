# SMS V3 디자인 패턴 및 현대 웹표준 분석 보고서

> 작성일: 2026-06-24 (최종 정정: 2026-06-24)
> 분석 대상: `src/main/java/com/scbk/sms/**` 및 `src/main/resources/**`
> 목적: 현재 프로젝트의 디자인 패턴을 점검하고, 현대 웹표준 대비 **실질적으로 가치 있는** 개선점 도출
> 참고: 본 보고서는 분석 결과만 제공하며 **코드 수정은 수행하지 않음** (사용자 지시 "수정금지")

---

## 0. 중요 정정 (사용자 피드백 반영)

본 보고서 초안의 2.1~2.3절은 **잘못된 전제**를 깔고 있었습니다. 정정합니다.

| 항목 | 초안(잘못됨) | 정정(실제) |
|------|--------------|------------|
| 시스템 성격 | 외부 API 서비스 | **폐쇄망 내부 SSR 시스템** (원래 API 거래 없음) |
| Controller/API 분리(2.3) | "혼합은 문제, v2처럼 분리 권장" | **의도적 설계**. 공통코드 같은 단발성 고정 조회만 REST API(`/api/common-code`)로 분리. 비즈니스 화면은 서버 사이드 렌더링 + 폼/AJAX. |
| RESTful URL(2.1) | "동사형 URL 위반, PUT/DELETE 권장" | 외부 API 소비자가 없으므로 Richardson 성숙도 기준은 **과잉**. 관찰 사항으로만 남김. |
| API 버전 관리(2.2) | "`/api/v1/` 권장" | 외부 API 거래가 없으므로 **불필요**. |
| v2 마이그레이션 | "v2 패턴으로 전환 권장" | **철회**. v3 자체가 최종 구조. v2는 단순 과거 버전 참고일 뿐. |

> "v1/v2"라는 표현은 초안에서 Richardson 모델 레벨과 API 버전을 혼동해 쓴 표현으로, 버전 번호가 아닌 아키텍처 성숙도/버저닝 개념이었습니다. 오해를 줄여 정정합니다.

---

## 1. 현재 아키텍처 개요 (의도적 설계)

### 1.1 시스템 성격

SMS V3는 **폐쇄망 내부에서 동작하는 서버 사이드 렌더링(SSR) 애플리케이션**입니다.

- 외부에 공개하는 REST API 서비스가 아님
- 화면(Thymeleaf) + 서버 사이드 세션/권한 기반 운영 도구
- "API 거래(외부 연동)"는 원래 존재하지 않음

### 1.2 계층 구조

```
Controller(@Controller, SSR) → Service → Mapper(MyBatis) → Oracle DB
                  │
                  └─ 공통코드 등 단발성 고정 조회만 @RestController(/api/...)로 분리
```

### 1.3 Controller 분류 (실제 코드 기준)

| Controller | 어노테이션 | 역할 | 비고 |
|------------|-----------|------|------|
| `CommonCodeApiController` | `@RestController` | **진짜 REST API** (화면 콤보/자동완성용 공통코드) | `/api/common-code`, 메뉴 권한 Interceptor 제외 대상 |
| `SmsHistoryController` | `@Controller` | SSR 화면 + 화면 내 AJAX(`/data`, `/create`...) | 폼/AJAX 기반, 독립 API 서비스 아님 |
| `NoticeController` | `@Controller` | SSR 화면 | |
| `LoginController` / `HomeController` | `@Controller` | SSR 화면 | |
| `ScaffoldController` / `MenuTreeController` | `@Controller` + `@Profile("local")` | local 전용 | |

→ `CommonCodeApiController`는 코드 주석에 명시된 대로 *"로그인한 모든 사용자가 사용하는 화면 보조 API"* 이며, 이것만이 **의도적으로 REST API로 분리된 유일한 사례**입니다.

### 1.4 확인된 디자인 패턴 (양호)

| 패턴 | 적용 위치 | 평가 |
|------|-----------|------|
| **Layered Architecture** | Controller/Service/Mapper | ✅ 적절 |
| **SSR + Template Method** | Thymeleaf `defaultLayout.html` | ✅ 폐쇄망 내부 도구로 적절 |
| **DTO/VO 분리** | `*RequestDTO`, `PageResponseDTO`, `*VO` | ✅ 적절 |
| **공통 응답 래퍼** | `ApiResponse<T>` | ✅ 적절 |
| **Global Exception Handler** | `@ControllerAdvice` | ✅ 적절 |
| **Strategy (인증)** | `List<AuthenticationProvider>` 주입 | ✅ 적절 |
| **Front Controller** | Spring MVC `DispatcherServlet` | ✅ 적절 |
| **DI** | Lombok `@RequiredArgsConstructor` | ✅ 적절 |
| **Builder** | Lombok `@Builder` (DTO) | ✅ 적절 |
| **화면 보조 API 분리** | `CommonCodeApiController`만 `@RestController` | ✅ 의도적 설계 |

---

## 2. 개선점 후보 (재평가)

> 아래 항목은 "현대 웹표준"이라는 외부 기준을 기계적으로 적용한 것이 아니라, **폐쇄망 SSR 내부 도구**라는 실제 맥락에서 가치가 있는지 재검토했습니다.

### 2.1 ✅ 철회: Controller/API 분리는 "문제"가 아님

- **초안 주장**: 하나의 `@Controller`가 HTML과 JSON을 동시 담당 → SRP 위반 → 분리 권장
- **정정**: v3는 SSR 앱이므로 `SmsHistoryController`의 `/data`, `/create` 등은 **화면 구성 요소(AJAX)** 일 뿐, 독립 API 서비스가 아님. 이를 억지로 `@RestController`로 분리하면 BASE 프로젝트의 일관성이 깨지고, 공통코드 API와 비즈니스 화면의 경계가 흐려짐.
- **결론**: 현재 구조(`CommonCodeApiController`만 분리)가 **의도적이고 합리적**. 분리 권장은 철회.

### 2.2 ✅ 철회: RESTful URL / API 버전 관리 과잉 요구

- 폐쇄망 내부 도구이고 외부 API 소비자가 없으므로 Richardson 성숙도 레벨이나 `/api/v1/` 버저닝은 **맥락에 맞지 않음**.
- 다만, 향후 BASE 프로젝트를 재사용해 **외부 연동이 추가될 경우**에만 그때 검토하면 됨. 현재는 권장하지 않음.

### 2.3 🟡 관찰 사항(권장 아님): AJAX 엔드포인트 네이밍

`SmsHistoryController`의 `/create`, `/update`, `/delete`는 동사형 URL이지만, SSR 앱의 내부 AJAX이므로 **문제가 아님**. 단지 BASE를 재사용할 때 외부 API로 노출할 일이 생기면 그때 재설계하면 됨. **지금은 손 댈 필요 없음.**

---

## 3. 실질적으로 가치 있는 개선점 (P0~P2)

맥락에 맞게 남은 항목만 정리했습니다.

### 3.1 🟡 보안 헤더 (Security Headers) — 폐쇄망 기준: 기본값만으로 충분

#### 현재 상태 — `SecurityConfig.java` 실제 코드

```java
// src/main/java/com/scbk/sms/config/SecurityConfig.java
http
    .authorizeHttpRequests(auth -> auth ...)
    .formLogin(form -> form ...)
    .logout(logout -> logout ...);

return http.build();
```

`.headers(...)` 설정이 **전혀 없습니다**. 따라서 Spring Security 기본값만 적용되는데, Spring Security 6 기본값은 다음과 같습니다:

| 헤더 | 기본값 | 상태 |
|------|--------|------|
| `Cache-Control` | `no-cache, no-store, must-revalidate` | ✅ 기본 적용 |
| `X-Content-Type-Options` | `nosniff` | ✅ **기본 적용됨** (초안 오류 정정) |
| `X-Frame-Options` | `DENY` | ✅ 기본 적용됨 |
| `Content-Security-Policy` | 없음 | ❌ **미설정** |
| `Referrer-Policy` | 없음 | ❌ 미설정 |
| `Strict-Transport-Security` | 없음 | ⚠️ HTTPS 확정 전 보류 |
| `Permissions-Policy` | 없음 | ❌ 미설정 |

> **정정**: 초안에서 "X-Content-Type-Options, X-Frame-Options 부재"라고 했으나, 실제로는 Spring Security 6가 기본 적용합니다. `.headers()`를 명시하지 않아도 `.contentTypeOptions()`와 `.frameOptions()`는 기본 활성화됨. 따라서 **실질적으로 누락된 핵심은 CSP와 Referrer-Policy**입니다.

#### 각 헤더의 의미와 필요성 (폐쇄망 SSR 맥락)

##### ① Content-Security-Policy (CSP) — **가장 중요**

**무엇인가?**
- 브라우저에게 "이 페이지는 어디에서 스크립트/스타일/이미지를 불러와도 되는가?"를 알려주는 화이트리스트 정책
- XSS(크로스사이트 스크립팅) 공격의 핵심 방어선

**왜 필요한가? (v3 맥락)**
- v3는 SSR이지만, Thymeleaf 템플릿에 사용자 입력이 렌더링됨 (예: SMS 발송 내용, 사원명 등)
- 사용자가 입력한 내용에 `<script>`가 포함되어 저장되고, 이후 조회 화면에서 렌더링되면 **Stored XSS** 발생 가능
- CSP가 있으면 `script-src 'self'`로 지정하여, 악성 스크립트가 외부 CDN이나 인라인으로 실행되는 것을 차단

**v3 적용 시 주의점**
- 현재 Thymeleaf/TUI Grid/Toast UI 등에서 **인라인 스크립트**(`onclick`, `<script>...</script>`)를 사용할 가능성이 높음
- `script-src 'self'`만 지정하면 인라인 스크립트가 차단되어 **화면 깨짐** 발생
- 따라서 **점진적 도입**이 필요:
  1. 1단계: `Content-Security-Policy-Report-Only` 모드로 위반 사항만 로깅 (차단 안 함)
  2. 2단계: 위반 로그를 보며 인라인 스크립트를 외부 파일로 이관 또는 nonce 할당
  3. 3단계: 실제 CSP 적용 (`script-src 'self' 'nonce-xxx'`)

```java
// 1단계 (Report-Only): 차단 없이 위반만 로깅
http.headers(h -> h
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")
    )
);
```

##### ② Referrer-Policy

**무엇인가?**
- 사용자가 링크를 클릭해 다른 페이지로 이동할 때, 이전 페이지의 URL(Referer)을 얼마나 노출할지 제어

**왜 필요한가? (v3 맥락)**
- 폐쇄망 내부라 외부 유출 위험은 낮지만, 내부 시스템 간 이동 시 **세션 URL 파라미터**가 Referer로 노출될 수 있음
- `strict-origin-when-cross-origin` 권장 (HTTPS→HTTPS에서만 origin 전송, 그 외는 생략)

```java
http.headers(h -> h
    .referrerPolicy(rp -> rp.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
);
```

##### ③ Permissions-Policy (구 Feature-Policy)

**무엇인가?**
- 브라우저 기능(카메라, 마이크, GPS 등)의 사용을 페이지 단위로 제한

**왜 필요한가? (v3 맥락)**
- SMS 관리 도구는 카메라/마이크/위치정보가 **불필요** → 명시적으로 차단하여 만일의 악성 스크립트 실행 시 피해 최소화

```java
http.headers(h -> h
    .permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=()"))
);
```

##### ④ Strict-Transport-Security (HSTS) — **보류**

**무엇인가?**
- 브라우저에게 "이 사이트는 항상 HTTPS로만 접속해라"를 강제
- HTTP→HTTPS 리다이렉트 과정의 중간자 공격 방어

**왜 보류하는가?**
- 폐쇄망에서 HTTPS 종단(인증서, 로드밸런서) 구성이 확정되지 않았을 수 있음
- HTTP로만 운영되는 환경에서 HSTS를 설정하면 브라우저가 접속을 거부할 수 있음
- **HTTPS 적용이 확정된 후 도입**

##### ⑤ X-Content-Type-Options / X-Frame-Options — **이미 적용됨 (정정)**

- 앞서 정정했듯, Spring Security 6 기본값으로 이미 적용 중
- `nosniff`: 브라우저가 MIME 타입을 추측하여 실행하는 것 방지
- `X-Frame-Options: DENY`: 클릭재킹(clickjacking) 방지 (다른 사이트에 iframe으로 삽입 차단)
- 명시적으로 관리하고 싶다면 `.headers()`에 작성:

```java
http.headers(h -> h
    .contentTypeOptions(ct -> {})              // nosniff (기본값과 동일)
    .frameOptions(fo -> fo.deny())             // DENY (기본값과 동일)
);
```

#### 폐쇄망 SSR 맥락에서의 정직한 평가

> **결론부터**: 폐쇄망 내부 시스템에서는 **"다 적용할 필요 없다"**. Spring Security 기본값 + (선택) CSP 정도면 충분.

**위협 모델의 차이**

| 위협 | 외부 인터넷 서비스 | 폐쇄망 내부 시스템 (v3) |
|------|-------------------|------------------------|
| 외부 공격자 직접 공격 | 높음 | **불가능** (망 단절) |
| MITM(중간자) 공격 | 가능 | **거의 불가능** (스위치 기반 내부망) |
| Stored XSS (내부 사용자 입력) | 높음 | **존재 가능** (사원명, SMS 내용 등) |
| 외부 CDN 악성 스크립트 주입 | 높음 | **불가능** (외부 접근 차단) |

**폐쇄망에서 보통 적용하는 수준 (관행)**

대부분의 폐쇄망 내부 운영 시스템은 **Spring Security 기본값만 유지**합니다:

| 헤더 | 폐쇄망 권장 | 이유 |
|------|-------------|------|
| `X-Content-Type-Options` (nosniff) | ✅ **기본값 유지** (아무것도 안 해도 됨) | Spring Security 6 기본 적용 |
| `X-Frame-Options` (DENY) | ✅ **기본값 유지** | 클릭재킹 방어, 비용 0 |
| `Cache-Control` | ✅ **기본값 유지** | 민감 화면 캐싱 방지 |
| `Content-Security-Policy` | 🟡 **선택** | Stored XSS 대비용. 인라인 스크립트 때문에 비용 발생 → 굳이 안 해도 됨 |
| `Referrer-Policy` | 🟡 **선택** | 외부 유출 위험 없음. 설정 1줄이라 보너스 |
| `Permissions-Policy` | ⚪ **보통 안 함** | 보너스 방어. 폐쇄망 관행상 거의 적용 안 함 |
| `Strict-Transport-Security` (HSTS) | ❌ **안 함** | 외부 인터넷 공격 방어용. 폐쇄망에서 무의미 |

**핵심 정리**

1. **Spring Security 기본값(nosniff, X-Frame-Options, Cache-Control) → 아무것도 안 해도 이미 적용됨. 그냥 두면 됨**
2. **CSP → 해도 되고 안 해도 됨.** Stored XSS 걱정되면 Report-Only로 점검 후 선택
3. **HSTS, Permissions-Policy → 폐쇄망에서는 보통 안 함.** 과잉 방어

> **정정**: 본 보고서 초안에서 "P0 우선 적용"이라고 했던 것은 **외부 인터넷 서비스 기준의 과잉 평가**였습니다. 폐쇄망 내부 시스템 기준으로는 **Spring Security 기본값만으로 충분**하며, CSP는 선택적 보너스 방어로 강등합니다.


### 3.2 🟡 P1: 에러 응답 구조화 (AJAX 대상 한정)

`@Valid` 실패 시 `BindingResult.getFieldErrors()`를 응답 본문에 포함하지 않아, AJAX 프론트엔드(`SmsHistoryController`의 `/create`, `/update`)에서 필드별 에러 매핑이 어려움.

```json
{
  "status": 400,
  "message": "입력값 검증 실패",
  "errors": [ { "field": "empId", "message": "필수입니다" } ]
}
```

→ `GlobalExceptionHandler#handleMethodArgumentNotValidException`에서 `errors[]` 반환. (JSON 응답에 한정)

### 3.3 🟡 P1: 웹 접근성 (a11y)

- `aria-label`, `role` 속성 미흹
- 키보드 내비게이션, `:focus-visible` 스타일 부재
- 색상 대비 4.5:1(WCAG 2.1 AA) 점검

### 3.4 🟡 P2: 반응형 / 뷰포트

- `viewport` meta, 미디어 쿼리 점검
- 운영 환경 디바이스(데스크톱 위주)에 맞춰 필요 범위 결정

### 3.5 🟢 P3 (선택): Design Token CSS

- 인라인 스타일/중복 CSS 값을 CSS 변수화 → 유지보수성 향상
- 다크모드는 맥락상 불필요할 수 있으므로 토큰화만 우선 검토

---

## 4. 우선순위 매트릭스 (폐쇄망 기준 재조정)

| 우선순위 | 항목 | 난이도 | 영향도 | 비고 |
|----------|------|--------|--------|------|
| ✅ **충족** | 보안 헤더 (기본값) | — | — | Spring Security 6 기본값(nosniff, X-Frame-Options, Cache-Control) 이미 적용 중. **추가 작업 불필요** |
| 🟡 선택 | CSP (Stored XSS 대비) | 중간 | 중간 | Report-Only로 점검 후 선택. 굳이 안 해도 됨 |
| 🟡 선택 | Referrer-Policy | 낮음 | 낮음 | 보너스. 설정 1줄 |
| ⚪ 안 함 | HSTS / Permissions-Policy | — | — | 폐쇄망에서 과잉 방어 |
| **P1** | 에러 응답 구조화(errors[]) | 낮음 | 중간 | AJAX 대상 한정 |
| **P1** | 웹 접근성(a11y) | 중간 | 중간 | 점진적 개선 |
| **P2** | 반응형/뷰포트 | 낮음 | 중간 | 운영 디바이스 맞춤 |
| **P3** | Design Token CSS | 중간 | 낮음 | 선택 |
| ~~철회~~ | ~~Controller/API 분리~~ | — | — | 의도적 설계, 철회 |
| ~~철회~~ | ~~RESTful URL~~ | — | — | 과잉 요구, 철회 |
| ~~철회~~ | ~~API 버전 관리~~ | — | — | 외부 API 없음, 철회 |
| ~~철회~~ | ~~프론트엔드 빌드 시스템~~ | — | — | 폐쇄망 SSR에서 바닐라 JS가 합리적, 철회 |
| ~~철회~~ | ~~OpenAPI 문서화~~ | — | — | 공통코드 API 1개뿐, 과잉 (외부 연동 추가 시 재검토) |

---

## 5. 제약사항 (AGENTS.md 기준)

- 폐쇄망 환경이므로 npm/Maven 외부 registry 접근 제한 가능
- 운영 DB/서버 작업은 사전 승인 필수
- `EMP.PERM_*` 컬럼은 권한 로직에 사용 금지 (legacy)
- 실패를 fallback으로 우회하지 않음
- 매직넘버/매직스트링은 상수화

---

## 6. 조치 이력 (Audit Log)

| 일시 | 조치 | 비고 |
|------|------|------|
| 2026-06-24 | 분석 보고서 최초 작성 | 코드 수정 없음 (사용자 지시 "수정금지") |
| 2026-06-24 | 코드 원복 | 보안 헤더/에러 응답 개선을 잠시 적용했다가 사용자 "수정금지" 지시에 따라 `git checkout`으로 원복. 변경 파일: `SecurityConfig.java`, `ApiResponse.java`, `GlobalExceptionHandler.java` |
| 2026-06-24 | **전면 정정** | 사용자 피드백 반영: v3는 폐쇄망 SSR 내부 시스템, 공통코드 API만 의도적 분리. 2.1~2.3(RESTful/버전/Controller 분리), 빌드 시스템, OpenAPI 항목 철회. 코드 수정 없음. |
| 2026-06-24 | **보안 헤더 평가 강등** | 사용자 피드백("폐쇄망인데 다 적용해야 하나?") 반영. 폐쇄망 위협 모델 재검토: 외부 공격자·MITM 불가능, 외부 CDN 주입 불가능. 결론: Spring Security 기본값만으로 충족, CSP는 선택적 보너스, HSTS/Permissions-Policy는 과잉 방어. 3.1절을 P0에서 🟡 선택으로 강등. |
