# Common Utils Implementation (@PrivacyLog / MaskingUtil / ExcelUtil)

v2 공통 유틸을 v3로 이관한 구현 기록이다. 정책 기준은 `docs/base/audit-masking-policy.md`다.

## 생성 파일

| 파일 | 역할 |
|---|---|
| `annotation/PrivacyLog.java` | 감사 대상 메서드 표시. `action`에 "메뉴명 + 행위" 기재 |
| `aop/PrivacyLogAspect.java` | `@PrivacyLog` 메서드 실행 시 감사 로그 자동 기록 |
| `service/system/AuditLogService.java` | 감사 로그 적재 |
| `mapper/system/PrivacyAuditLogMapper.java` / `.xml` | `SMS.TB_PRIVACY_AUDIT_LOG` INSERT |
| `vo/system/PrivacyAuditLogVO.java` | 감사 로그 VO |
| `util/MaskingUtil.java` | 이름/전화번호/주민번호/카드번호 마스킹 + 텍스트 내 개인정보 마스킹 |
| `util/ExcelUtil.java` | SXSSF 기반 대용량 엑셀 다운로드 |
| `db/oracle/03_privacy_audit_log_schema.sql` | 감사 테이블 DDL (DBA 적용 필요) |

## pom 의존성 추가 (폐쇄망 반입 목록)

| 의존성 | 버전 | 용도 |
|---|---|---|
| `spring-boot-starter-aop` | Boot BOM 관리 | `@Aspect` AOP |
| `org.apache.poi:poi-ooxml` | 5.2.3 (v2와 동일) | 엑셀 생성 |

## 사용법

```java
@PrivacyLog(action = "주민번호 조회")
@ResponseBody
@GetMapping("/data")
public ResponseEntity<ApiResponse<PageResponseDTO<SsnSearchVO>>> getData(...) { ... }
```

```java
vo.setCustomerNm(MaskingUtil.maskName(raw));     // 홍*동
vo.setReceiverNo(MaskingUtil.maskPhone(raw));    // 010-****-5678
```

```java
ExcelUtil.downloadExcel(response, "발송이력_export", headers, dataList, keys);
```

## v2 대비 조정 사항

| 항목 | v2 | v3 |
|---|---|---|
| 행위자 기록 | `auth.getName()` (EMP_ID 단독) | principal의 `(EMP_ID, DEP_ID)` 복합 기록 |
| 감사 저장 실패 | try-catch로 삼키고 업무 계속 진행 | 전파한다. 기록되지 않은 개인정보 접근을 허용하지 않는다 |
| 파라미터 기록 | JSON 원문 490자 저장 (개인정보 원문 노출) | `MaskingUtil.maskPrivacyInText`로 주민번호/전화번호 후보 마스킹 후 저장 |
| 감사 테이블 | `TB_PRIVACY_AUDIT_LOG(EXECUTOR_ID)` | `SMS.TB_PRIVACY_AUDIT_LOG(EMP_ID, DEP_ID)` + 한글 컬럼 `CHAR` 단위 길이 |
| Lombok | 사용 | 이 공통 클래스들은 plain Java로 구현 (검증 완료된 코드라 유지). 신규 도메인/생성 코드는 Lombok 사용 — 2026-06-12 도입 |

파라미터 직렬화 실패(`HttpServletResponse` 인자 등)는 `"파라미터 직렬화 실패"`로 기록하고 업무는 진행한다. targetData는 보조 추적 정보이기 때문이며, 감사 로그 INSERT 실패와는 다르게 취급한다.

## 검증 결과

2026-06-11 기준:

```text
mvn test                  PASS (28 tests, 신규 10: Masking 5, AuditLog 2, Aspect 2, Excel 1)
mvn -DskipTests package   PASS
```

테스트로 고정한 규칙: (EMP_ID, DEP_ID) 기록, targetData 마스킹, 감사 저장 실패 시 업무 미실행, 마스킹 포맷 4종.

## 남은 고려사항

- `TB_PRIVACY_AUDIT_LOG` 적용 전에는 `@PrivacyLog` 호출이 실패한다. DBA 적용 후 사용한다.
- 감사 로그 조회 화면(관리자용)은 미구현이다. 화면 생성 단계의 대상이다.
- `maskPrivacyInText`는 숫자 패턴 기반 후보 마스킹이다. 계좌번호 등 추가 패턴이 필요하면 정책과 함께 확장한다.
