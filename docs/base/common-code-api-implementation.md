# Common Code API Implementation

화면 콤보(`CommonUtils.initCombos`)와 자동완성(`initAutocomplete`)이 사용하는 공통코드 API 구현 기록이다.

## Endpoint

```text
GET /api/common-code/{codeType}            전체 목록 (콤보용)
GET /api/common-code/{codeType}?keyword=x  부분 일치 검색 (자동완성용)
```

응답은 `ApiResponse<List<CommonCodeVO>>`이며, 화면에서는 전역 인터셉터가 언래핑하므로
`res.data`가 곧 `[{code, name}, ...]` 배열이다.

## 지원 코드 타입

| codeType | 기준 테이블 | 조건 | 정렬 |
|---|---|---|---|
| `dept` | `SMS.DEP` | `ACT_YN = 'Y'` | `DEP_NM` |
| `role` | `SMS.TB_ROLE` | `USE_YN = 'Y'` | `SORT_ORD` |

- 모든 타입이 `keyword` 부분 일치(코드/이름)를 지원한다.
- 지원하지 않는 타입은 빈 목록으로 숨기지 않고 400(`UNSUPPORTED_CODE_TYPE`, C003)으로 보고한다. (v2는 빈 배열 반환 — v3 확정 변경)
- v2의 `bank`, `messages` 타입은 해당 도메인 화면을 만들 때 폐쇄망에서 추가한다.

## 새 코드 타입 추가 절차

1. `CommonCodeMapper` + `CommonCodeMapper.xml`에 조회 추가 (실제 테이블 확인 후)
2. `CommonCodeService.getCommonCodes`에 case 추가
3. 이 문서와 `screen-convention.md`의 지원 타입을 갱신
4. `CommonCodeServiceTest`에 테스트 추가

## 인증/인가 방식 (확정)

- Spring Security 인증(로그인) 필수 — 미인증은 접근 불가.
- 메뉴 권한 Interceptor 검증은 제외 — 로그인한 모든 사용자가 쓰는 화면 보조 API이므로
  `WebMvcConfig` 공통 제외 경로(`/api/common-code/**`)에 코드 레벨로 고정했다.
- 외부 연계용 `/api/**`의 인증 방식은 여전히 미확정이다. (`rest-api-standard.md`)

## 생성 파일

| 파일 | 역할 |
|---|---|
| `controller/system/CommonCodeApiController.java` | `@RestController`, `/api/common-code/{codeType}` |
| `service/system/CommonCodeService.java` | 타입 분기. 미지원 타입 오류 보고 |
| `mapper/system/CommonCodeMapper.java` / `.xml` | DEP, TB_ROLE 조회 |
| `vo/common/CommonCodeVO.java` | `{code, name}` |
| `exception/ErrorCode.java` | `UNSUPPORTED_CODE_TYPE(C003)` 추가 |
| `config/WebMvcConfig.java` | `/api/common-code/**` 공통 제외 경로 추가 |

## 화면 사용 예

```html
<select id="depId" class="common-combo form-select" data-code-type="dept" style="width:180px;">
    <option value="">전체</option>
</select>
```

```javascript
CommonUtils.initAutocomplete({
    inputEl: '#depText',
    balloonEl: '#depBalloon',
    apiUrl: '/api/common-code/dept'
});
```

## 검증 결과

2026-06-12 기준:

```text
mvn test                  PASS (47 tests, 신규 3: CommonCodeService)
mvn -DskipTests package   PASS
```

화면 확인(사용자): 서버 기동 후 로그인 상태에서 브라우저로
`http://localhost:8081/api/common-code/dept` 접근 시 부서 JSON이 와야 하고,
`/api/common-code/bank`는 400(C003)이 와야 한다.
