# 도메인 규칙

v2 `.claude/rules/features/*.md`의 도메인 지식을 v3 메뉴 baseline 기준으로 이관한 문서다.

화면을 생성하기 전에 해당 도메인 섹션을 반드시 읽는다.

이 문서는 **도메인별 지식**(각 도메인의 테이블/주의사항)이다. "무엇을 도메인 1개로 묶을지" 판별하는 방법론은 `domain-boundary-guide.md`를 본다.

## 테이블 사용 방침 (2026-06-10 확정)

- v2 문서에 남아 있는 **legacy `TB_*` 업무 테이블(`TB_SMS_HISTORY`, `TB_DEPT`, `TB_CAMPAIGN`, `TB_CONTACT` 등)은 v3에서 사용하지 않는다.**
- 실제 운영 테이블(`EMP`, `DEP`, `SMS_HISTORY`, `CAMPAIGN`, `SMS_SEND_LOG` 등 비접두 테이블)을 기준으로 한다.
- 예외: v3 신규 권한 테이블 `TB_ROLE`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH`는 v3 확정 설계 테이블이다.
- 아래 표기된 테이블/컬럼명은 v2 코드 참고값이며, **폐쇄망 실제 DB 확인 전에는 미확정**이다. 확인 없이 컬럼명을 확정하지 않는다.

## 개인정보(PII) 공통 규칙

- PII 컬럼(수신번호/주민번호/고객명 등)은 목록·상세·엑셀에서 `MaskingUtil`로 마스킹해 표시한다.
- 원문 조회/다운로드는 `/unmask` 경로(`MASK_VIEW` 권한)로만 분리하고 감사 로그 대상으로 둔다.
- 스캐폴드는 어떤 컬럼이 PII인지 모르므로 자동 마스킹하지 못한다. **화면 생성 시 직접 적용한다.**
- 레퍼런스 예시: `SmsHistoryService.downloadExcel`(엑셀 수신번호 마스킹).

## SMS 발송 이력 (`/sms/history`)

- 기준 테이블: `SMS_HISTORY` (v2 코드 기준. `TB_SMS_HISTORY`는 사용하지 않는다)
- 대량 이력 조회는 기간 조건과 페이지네이션을 반드시 둔다.
- 수신 번호, 고객 식별 정보는 마스킹 상태로만 표시한다. (`audit-masking-policy.md`)
- 상태값(`WAIT`, `SUCCESS`, `FAIL` 등)은 DB, Service, 화면 표시가 일치해야 한다.
- 재발송/결과 업데이트는 원본 이력의 식별자와 상태 전이를 명확히 검증한다.
- `SMS_HISTORY.MESSAGE`는 CLOB 가능성이 있다. 조회/엑셀 시 길이와 타입을 고려한다.

## SMS 고객/주민번호 조회 (`/sms/customer-search`, `/sms/ssn-search`)

- v2 참조 테이블: `SMS_HISTORY`, `TB_CUSTOMER_SSN`(legacy 표기 — 실제 테이블명 폐쇄망 확인 필요)
- 주민번호/고객 식별 정보는 반드시 마스킹 상태로 표시한다. 원문 조회는 `CAN_MASK_VIEW` + 감사 로그 대상이다.
- 검색 조건은 기간, 전화번호, 고객명, 부서를 명확히 분리한다.
- 개인정보 조회 화면이므로 감사 로그 적용이 필수다.

## 캠페인 SMS (`/campaign/**`, `/sms/campaign*`)

- v2 코드가 직접 참조한 테이블: `CAMPAIGN`, `SMS_HISTORY`, `SMS_SEND_LOG` (`TB_CAMPAIGN`은 사용하지 않는다)
- 캠페인 상태값은 DB check constraint, Service, 화면 표시가 일치해야 한다.
- 발송 유형(`SMS`, `LMS`, `MMS`, 알림톡)은 허용값을 확인하고 하드코딩 확장을 피한다.
- 캠페인별 발송 건수는 `SMS_HISTORY` 기준인지 `SMS_SEND_LOG` 기준인지 명확히 구분한다.
- 승인 연계는 승인자/승인일 컬럼과 상태 전이를 함께 확인한다.

## 승인 (`/approval`)

- 승인 상태 변경은 이력 저장 여부를 함께 확인한다.
- 요청자, 승인자, 요청일, 승인일의 null 가능성을 명확히 처리한다.
- 상태값은 화면, Service, Mapper XML에서 같은 값을 사용한다.
- 승인/반려 처리는 Service 계층 트랜잭션으로 묶고 `CAN_APPROVE` 권한 대상이다.

## 부서 관리 (`/system/dept-manage`)

- v3 부서 기준 테이블은 `DEP` 하나다. v2의 `TB_DEPT`/`TB_DEP` 혼재를 끌어오지 않는다.
- 화면/DTO/VO에서 `DEPT` 표기를 쓰지 않는다. `DEP` 명칭으로 통일한다.
- 부서 비활성화는 `ACT_YN` 기준이며, 소속 사용자(`EMP`)와 역할(`TB_EMP_ROLE`) 영향도를 확인한다.

## 사용자 관리 (`/account/user-manage`)

- 기준 테이블: `EMP`. 식별은 항상 `(EMP_ID, DEP_ID)`다.
- `EMP.PERM_*`는 조회/수정 대상이 아니다. 권한은 `TB_EMP_ROLE`로만 관리한다.
- 사용자 등록/수정/삭제와 역할 변경은 감사 로그 대상이다.
- 비밀번호 관련 컬럼(`EMP_PASS`, `OLD_PWD*`)은 화면에 노출하지 않는다.

## 메뉴/권한 관리 (시스템관리)

- 기준 테이블: `TB_MENU`, `TB_MENU_AUTH`, `TB_ROLE`, `TB_EMP_ROLE` (v3 확정 설계)
- 메뉴 삭제 전 하위 메뉴와 권한 매핑 영향을 확인한다. `SYSTEM_YN = 'Y'` 메뉴는 보호 대상이다.
- 메뉴/권한 변경은 `MenuAuthInterceptor` 동작에 영향을 주므로 변경 후 반드시 검증한다.
- 신규 화면 추가 시 메뉴 등록 SQL과 권한 등록 SQL을 함께 준비한다.
- 역할 삭제 전 `TB_EMP_ROLE`, `TB_MENU_AUTH` 참조 영향을 확인한다.

## 기준정보/메시지 (`/basic/message`, `/system/message`, `/system/kakao-template`, `/system/ad-message`)

- v2 참조 테이블(`TB_MESSAGE`, `TB_BANK` 등)은 legacy 표기다. 실제 테이블명은 폐쇄망 확인 필요.
- 기준정보는 운영 중 바뀔 수 있는 값으로 본다. 사용 여부(`USE_YN` 류)와 정렬 순서를 고려한다.
- 메시지 코드는 유니크로 보고 중복 검증을 둔다. 메시지 내용은 CLOB 가능성을 고려한다.
- 코드값은 화면, Service, Mapper XML에서 같은 명칭을 사용한다.

## 통계 (`/statistics/marketing-optout`, `/sms/dept-stat`)

- 통계 기준 테이블과 기준일 컬럼을 먼저 확정한다.
- 기간 조건은 시작일/종료일 포함 범위를 명확히 한다.
- 발송 유형, 상태, 부서별 집계 기준을 화면 표시와 일치시킨다.
- count와 목록/차트 데이터의 조건이 어긋나지 않게 한다.
- 대량 테이블 집계는 인덱스와 WHERE 조건을 고려하고 전체 스캔을 피한다.
- 임시 샘플 데이터(`DUAL` 기반)와 실제 집계 쿼리를 혼동하지 않는다.

## 이관 제외

- 주소록(`TB_CONTACT` 계열): v3 메뉴 baseline에 없어 이관하지 않았다.
- 대시보드: v3 초기 화면은 메뉴 개요이므로 이관하지 않았다.
