# 07-mybatis-sql-inventory.md

## 분석 범위

- 대상: `src/main/resources/mapper/*.xml`
- 목적: Mapper XML에 정의된 SQL, 조회/삽입 대상 테이블, 결과 VO, 파라미터, 핵심 WHERE/JOIN 조건을 카탈로그화
- 제외: Mapper Java 인터페이스의 메서드명은 연결선으로만 기록하고, SQL 본문은 XML 기준

## Mapper XML 목록

| Mapper | 경로:줄 | 주요 용도 |
|---|---:|---|
| `LoginEmployeeMapper` | `src/main/resources/mapper/auth/LoginEmployeeMapper.xml:6` | 로그인/ID 조회용 `EMP` 조회 |
| `EmployeeRoleMapper` | `src/main/resources/mapper/menu/EmployeeRoleMapper.xml:6` | `EMP_ID + DEP_ID` 기준 역할 코드 조회 |
| `MenuMapper` | `src/main/resources/mapper/menu/MenuMapper.xml:6` | 메뉴 트리용 `TB_MENU`/`TB_MENU_AUTH` 조회 |
| `MenuAuthMapper` | `src/main/resources/mapper/menu/MenuAuthMapper.xml:6` | URL 기준 메뉴 권한 플래그 조회 |
| `CommonCodeMapper` | `src/main/resources/mapper/system/CommonCodeMapper.xml:6` | 부서/역할 콤보 조회 |
| `PrivacyAuditLogMapper` | `src/main/resources/mapper/system/PrivacyAuditLogMapper.xml:6` | 개인정보 감사 로그 삽입 |
| `SmsHistoryMapper` | `src/main/resources/mapper/sms/SmsHistoryMapper.xml:6` | 발송이력 목록/상세/수정 관련 SQL |

## Mapper별 SQL 카탈로그

### `LoginEmployeeMapper`

| SQL | 경로:줄 | 대상 테이블 | 결과 VO | 핵심 조건 |
|---|---:|---|---|---|
| `selectActiveEmployeesByEmpId` | `src/main/resources/mapper/auth/LoginEmployeeMapper.xml:8` | `SMS.EMP` | `LoginEmployeeVO` | `EMP_ID = #{empId}`, `ACT_YN = 'Y'` |

### `EmployeeRoleMapper`

| SQL | 경로:줄 | 대상 테이블 | 결과 VO | 핵심 조건 |
|---|---:|---|---|---|
| `selectRoleCodes` | `src/main/resources/mapper/menu/EmployeeRoleMapper.xml:8` | `SMS.TB_EMP_ROLE`, `SMS.TB_ROLE` | `List<String>` | `ER.EMP_ID = #{empId}`, `ER.DEP_ID = #{depId}`, `ER.USE_YN = 'Y'`, `R.USE_YN = 'Y'` |

역할 조회는 `TB_ROLE.USE_YN = 'Y'`와 `TB_EMP_ROLE.USE_YN = 'Y'`를 모두 걸고, `SORT_ORD, ROLE_CD` 순으로 정렬한다.

### `MenuMapper`

| SQL | 경로:줄 | 대상 테이블 | 결과 VO | 핵심 조건 |
|---|---:|---|---|---|
| `selectReadableMenus` | `src/main/resources/mapper/menu/MenuMapper.xml:8` | `SMS.TB_MENU`, `SMS.TB_MENU_AUTH` | `MenuItemVO` | `M.USE_YN='Y'`, `M.DISPLAY_YN='Y'`, `MA.USE_YN='Y'`, `MA.CAN_READ='Y'`, `MA.ROLE_CD IN (...)` |

메뉴 목록은 `TB_MENU_AUTH.CAN_READ = 'Y'`만 만족하는 행을 `DISTINCT`로 가져온다. `TB_MENU_AUTH`가 여러 역할로 중복될 수 있으므로 `DISTINCT`는 동일 메뉴가 여러 역할에 걸쳐 읽기 권한을 가진 경우 중복 제거용이다.

### `MenuAuthMapper`

| SQL | 경로:줄 | 대상 테이블 | 결과 VO | 핵심 조건 |
|---|---:|---|---|---|
| `selectMenuPermissions` | `src/main/resources/mapper/menu/MenuAuthMapper.xml:9` | `SMS.TB_MENU`, `SMS.TB_MENU_AUTH` | `MenuAuthVO` | `M.USE_YN='Y'`, `MA.USE_YN='Y'`, `M.MENU_URL = #{menuUrl}`, `MA.ROLE_CD IN (...)` |

권한 플래그는 여러 역할의 `TB_MENU_AUTH` 행을 `MAX(CAN_...)`로 집계한다. `Y`가 하나라도 있으면 해당 권한이 허용되는 구조다.

### `CommonCodeMapper`

| SQL | 경로:줄 | 대상 테이블 | 결과 VO | 핵심 조건 |
|---|---:|---|---|---|
| `selectDepartments` | `src/main/resources/mapper/system/CommonCodeMapper.xml:9` | `SMS.DEP` | `CommonCodeVO` | `D.ACT_YN = 'Y'`, 선택적 `keyword` |
| `selectRoles` | `src/main/resources/mapper/system/CommonCodeMapper.xml:23` | `SMS.TB_ROLE` | `CommonCodeVO` | `R.USE_YN = 'Y'`, 선택적 `keyword` |

`dept` 콤보는 `DEP_ID, DEP_NM`, `role` 콤보는 `ROLE_CD, ROLE_NM`을 반환한다.

### `PrivacyAuditLogMapper`

| SQL | 경로:줄 | 대상 테이블 | 입력 VO | 핵심 필드 |
|---|---:|---|---|---|
| `insertAuditLog` | `src/main/resources/mapper/system/PrivacyAuditLogMapper.xml:8` | `SMS.TB_PRIVACY_AUDIT_LOG` | `PrivacyAuditLogVO` | `EMP_ID, DEP_ID, EXECUTOR_IP, REQUEST_URL, ACTION_TYPE, TARGET_DATA` |

감사 로그 삽입은 `TARGET_DATA`를 직접 저장한다. 실제 개인정보 원문은 `PrivacyLogAspect` 단계에서 `MaskingUtil`로 마스킹되어 들어간다.

### `SmsHistoryMapper`

| SQL | 경로:줄 | 대상 테이블 | 결과/입력 | 핵심 조건 |
|---|---:|---|---|---|
| `selectCount` | `src/main/resources/mapper/sms/SmsHistoryMapper.xml:8` | `SMS.SMS_HISTORY` | `int` | 검색 조건, 선택적 `SEND_TYPE` |
| `selectList` | `src/main/resources/mapper/sms/SmsHistoryMapper.xml:32` | `SMS.SMS_HISTORY` | `SmsHistoryVO` | 검색 조건, 페이징, `SENT_AT` 정렬 |
| `selectDetail` | `src/main/resources/mapper/sms/SmsHistoryMapper.xml:92` | `SMS.SMS_HISTORY` | `SmsHistoryVO` | `SMS_HISTORY_ID` 또는 `REQUEST_ID` |
| `updateStatus` | `src/main/resources/mapper/sms/SmsHistoryMapper.xml:124` | `SMS.SMS_HISTORY` | `int` | `SMS_HISTORY_ID, REQUEST_ID`와 수정 가능 필드 화이트리스트 |

`SmsHistoryMapper`는 발송이력 도메인의 핵심 SQL이다. 목록 조회는 검색 조건을 `PageRequestDTO` 계열 DTO에서 받아 `LIMIT/OFFSET` 처리하고, 상세 조회는 `SMS_HISTORY_ID` 또는 `REQUEST_ID`를 조건으로 한다. 수정 SQL은 `REG_ID, REG_DTTM` 같은 시스템 필드를 직접 수정하지 않도록 화이트리스트 필드만 포함한다.

## SQL 패턴 정리

| 패턴 | Mapper | 설명 |
|---|---|---|
| `IN <foreach>` | `EmployeeRoleMapper`, `MenuMapper`, `MenuAuthMapper` | 여러 ROLE_CODE를 동적 SQL로 전달 |
| `MAX(CAN_...)` | `MenuAuthMapper` | 역할별 권한 플래그를 Y 우선으로 집계 |
| `LIKE '%' || #{keyword} || '%'` | `CommonCodeMapper` | Oracle 문자열 LIKE 문법 사용 |
| `LIMIT/OFFSET` | `SmsHistoryMapper` | 목록 페이징 |
| `parameterType=VO` | `PrivacyAuditLogMapper` | VO 필드를 컬럼명과 매핑해 삽입 |

## 검증 포인트

- `MenuMapper`는 `TB_MENU_AUTH.CAN_READ='Y'`만 만족하면 메뉴 트리에 포함되므로, `TB_MENU`가 `USE_YN='Y'`여도 권한이 없으면 UI에 보이지 않는다.
- `MenuAuthMapper`는 `MAX(CAN_...)`를 사용하므로 `Y/N` 문자열 기준으로 집계된다. 현재 DB가 `Y/N` 문자열이면 동작하지만, Boolean 타입이면 다른 집계 방식이 필요하다.
- `CommonCodeMapper`는 Oracle `LIKE` 문법을 그대로 사용하므로 MyBatis와 Oracle 환경에 맞는다.
- `PrivacyAuditLogMapper`는 감사 로그 삽입 실패를 Mapper에서 삼키지 않으므로, 서비스/AOP에서 실패가 전파된다.
- `SmsHistoryMapper`의 수정 SQL은 화이트리스트 필드만 포함하므로 시스템 필드 직접 수정은 어렵다.
