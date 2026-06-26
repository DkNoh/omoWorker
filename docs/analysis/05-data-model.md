# 05-data-model.md

## 분석 범위

- 대상: `db/oracle` 아래 `.sql` 파일, `docker/local-emp-dep-seed.sql`, 주요 VO 클래스.
- 목적: v3 운영 기준의 EMP/DEP, v3 권한 모델, 발송이력, 개인정보 감사 로그 구조를 실제 SQL 기준으로 정리한다.
- 원칙: `EMP_ID` 단독이 아니라 `EMP_ID + DEP_ID`를 사용자 식별 기준으로 사용한다. `EMP.PERM_*` 컬럼은 신규 권한 판단 기준으로 사용하지 않는다.
- 확인 범위: 파일이 너무 크거나 핵심만 필요한 경우 실제 확인한 필드만 기록하고, 불확실한 관계는 별도로 표시한다.

## db/oracle SQL/DDL 목록

| 순서 | 경로 |
|---:|---|
| 1 | `db/oracle/01_menu_auth_schema.sql` |
| 2 | `db/oracle/02_menu_auth_seed.sql` |
| 3 | `db/oracle/03_privacy_audit_log_schema.sql` |
| 4 | `db/oracle/04_sms_history_schema.sql` |
| 5 | `db/oracle/05_sms_history_seed.sql` |
| 6 | `db/oracle/sms_history_menu_seed.sql` |

## 운영 EMP/DEP 기준

| 테이블 | 경로:줄 | PK | 주요 컬럼 | 관계 |
|---|---|---|---|---|
| `SMS.DEP` | `docker/local-emp-dep-seed.sql:5` | `DEP_ID` | `DEP_ID`, `DEP_NM`, `ACT_YN` | `EMP.DEP_ID`가 참조하는 부서 단위 |
| `SMS.EMP` | `docker/local-emp-dep-seed.sql:12` | `(EMP_ID, DEP_ID)` | `EMP_ID`, `DEP_ID`, `EMP_NM`, `ACT_YN` | `(EMP_ID, DEP_ID)`가 `SMS.DEP(DEP_ID)`를 참조 |

- `docker/local-emp-dep-seed.sql`은 로컬 docker 전용 테스트 픽스처이다. 실제 운영 EMP/DEP DDL은 `db/oracle` 하위 SQL에 포함되지 않는다.
- `SMS.EMP`의 기본키는 복합키 `(EMP_ID, DEP_ID)`이며, v3 권한/감사/발송이력 관계도 이 복합키 기준으로 연결된다.

## v3 권한 모델 관계

| 테이블 | 경로:줄 | PK | 주요 컬럼 | 관계 |
|---|---|---|---|---|
| `SMS.TB_ROLE` | `db/oracle/01_menu_auth_schema.sql:1` | `ROLE_CD` | `ROLE_CD`, `ROLE_NM`, `ROLE_DESC`, `SORT_ORD`, `USE_YN`, `REG_ID`, `REG_DTTM`, `UPD_ID`, `UPD_DTTM` | 권한 코드 자체를 정의 |
| `SMS.TB_EMP_ROLE` | `db/oracle/01_menu_auth_schema.sql:15` | `(EMP_ID, DEP_ID, ROLE_CD)` | `EMP_ID`, `DEP_ID`, `ROLE_CD`, `USE_YN`, `REG_ID`, `REG_DTTM`, `UPD_ID`, `UPD_DTTM` | `(EMP_ID, DEP_ID) -> SMS.EMP`, `ROLE_CD -> SMS.TB_ROLE` |
| `SMS.TB_MENU` | `db/oracle/01_menu_auth_schema.sql:35` | `MENU_ID` | `MENU_ID`, `PARENT_MENU_ID`, `MENU_NM`, `MENU_URL`, `MENU_LEVEL`, `SORT_ORD`, `MENU_TYPE`, `ICON_NM`, `DISPLAY_YN`, `USE_YN`, `SYSTEM_YN`, `REMARK`, `REG_ID`, `REG_DTTM`, `UPD_ID`, `UPD_DTTM` | 자기 참조 `PARENT_MENU_ID -> TB_MENU(MENU_ID)` |
| `SMS.TB_MENU_AUTH` | `db/oracle/01_menu_auth_schema.sql:69` | `(MENU_ID, ROLE_CD)` | `MENU_ID`, `ROLE_CD`, `CAN_READ`, `CAN_CREATE`, `CAN_UPDATE`, `CAN_DELETE`, `CAN_APPROVE`, `CAN_CANCEL`, `CAN_DOWNLOAD`, `CAN_MASK_VIEW`, `USE_YN`, `REG_ID`, `REG_DTTM`, `UPD_ID`, `UPD_DTTM` | `MENU_ID -> TB_MENU`, `ROLE_CD -> TB_ROLE` |

관계 요약:

| 출발 | 도착 | 조건 |
|---|---|---|
| `SMS.TB_EMP_ROLE` | `SMS.EMP` | `(EMP_ID, DEP_ID)`가 `(EMP_ID, DEP_ID)`를 참조 |
| `SMS.TB_EMP_ROLE` | `SMS.TB_ROLE` | `ROLE_CD`가 `ROLE_CD`를 참조 |
| `SMS.TB_MENU_AUTH` | `SMS.TB_MENU` | `MENU_ID`가 `MENU_ID`를 참조 |
| `SMS.TB_MENU_AUTH` | `SMS.TB_ROLE` | `ROLE_CD`가 `ROLE_CD`를 참조 |
| `SMS.TB_MENU` | `SMS.TB_MENU` | `PARENT_MENU_ID`가 자기 테이블 `MENU_ID`를 참조 |

## db/oracle/01_menu_auth_schema.sql 분석

- 경로: `db/oracle/01_menu_auth_schema.sql`
- 정의 대상: `TB_ROLE`, `TB_EMP_ROLE`, `TB_MENU`, `TB_MENU_AUTH`

### `SMS.TB_ROLE`

| 컬럼 | 타입 | NOT NULL | 기본값 | 제약/의미 |
|---|---|---:|---|---|
| `ROLE_CD` | `VARCHAR2(30)` | Y | - | PK, 권한 코드 |
| `ROLE_NM` | `VARCHAR2(100)` | Y | - | 권한명 |
| `ROLE_DESC` | `VARCHAR2(500)` | N | - | 설명 |
| `SORT_ORD` | `NUMBER(5)` | Y | `0` | 순서 |
| `USE_YN` | `CHAR(1)` | Y | `'Y'` | 사용 여부, `Y/N` 체크 |
| `REG_ID` | `VARCHAR2(12)` | Y | - | 등록자 |
| `REG_DTTM` | `TIMESTAMP(6)` | Y | `SYSTIMESTAMP` | 등록일시 |
| `UPD_ID` | `VARCHAR2(12)` | N | - | 수정자 |
| `UPD_DTTM` | `TIMESTAMP(6)` | N | - | 수정일시 |

- PK: `CONSTRAINT PK_TB_ROLE PRIMARY KEY (ROLE_CD)`(`db/oracle/01_menu_auth_schema.sql:11`)
- 체크: `USE_YN IN ('Y', 'N')`(`db/oracle/01_menu_auth_schema.sql:12`)

### `SMS.TB_EMP_ROLE`

| 컬럼 | 타입 | NOT NULL | 기본값 | 제약/의미 |
|---|---|---:|---|---|
| `EMP_ID` | `VARCHAR2(12)` | Y | - | EMP ID |
| `DEP_ID` | `VARCHAR2(12)` | Y | - | 부서 ID |
| `ROLE_CD` | `VARCHAR2(30)` | Y | - | 권한 코드 |
| `USE_YN` | `CHAR(1)` | Y | `'Y'` | 사용 여부 |
| `REG_ID` | `VARCHAR2(12)` | Y | - | 등록자 |
| `REG_DTTM` | `TIMESTAMP(6)` | Y | `SYSTIMESTAMP` | 등록일시 |
| `UPD_ID` | `VARCHAR2(12)` | N | - | 수정자 |
| `UPD_DTTM` | `TIMESTAMP(6)` | N | - | 수정일시 |

- PK: `(EMP_ID, DEP_ID, ROLE_CD)`(`db/oracle/01_menu_auth_schema.sql:24`)
- FK: `(EMP_ID, DEP_ID) -> SMS.EMP(EMP_ID, DEP_ID)`(`db/oracle/01_menu_auth_schema.sql:25`)
- FK: `ROLE_CD -> SMS.TB_ROLE(ROLE_CD)`(`db/oracle/01_menu_auth_schema.sql:27`)
- 체크: `USE_YN IN ('Y', 'N')`(`db/oracle/01_menu_auth_schema.sql:29`)

### `SMS.TB_MENU`

| 컬럼 | 타입 | NOT NULL | 기본값 | 제약/의미 |
|---|---|---:|---|---|
| `MENU_ID` | `VARCHAR2(50)` | Y | - | 메뉴 ID |
| `PARENT_MENU_ID` | `VARCHAR2(50)` | N | - | 자기 참조 부모 메뉴 |
| `MENU_NM` | `VARCHAR2(100)` | Y | - | 메뉴명 |
| `MENU_URL` | `VARCHAR2(300)` | N | - | 메뉴 URL |
| `MENU_LEVEL` | `NUMBER(2)` | Y | - | 메뉴 레벨 |
| `SORT_ORD` | `NUMBER(5)` | Y | `0` | 순서 |
| `MENU_TYPE` | `CHAR(1)` | Y | `'M'` | `G/M` 체크 |
| `ICON_NM` | `VARCHAR2(50)` | N | - | 아이콘 |
| `DISPLAY_YN` | `CHAR(1)` | Y | `'Y'` | 표시 여부 |
| `USE_YN` | `CHAR(1)` | Y | `'Y'` | 사용 여부 |
| `SYSTEM_YN` | `CHAR(1)` | Y | `'N'` | 시스템 메뉴 여부 |
| `REMARK` | `VARCHAR2(500)` | N | - | 비고 |
| `REG_ID` | `VARCHAR2(12)` | Y | - | 등록자 |
| `REG_DTTM` | `TIMESTAMP(6)` | Y | `SYSTIMESTAMP` | 등록일시 |
| `UPD_ID` | `VARCHAR2(12)` | N | - | 수정자 |
| `UPD_DTTM` | `TIMESTAMP(6)` | N | - | 수정일시 |

- PK: `MENU_ID`(`db/oracle/01_menu_auth_schema.sql:52`)
- FK: `PARENT_MENU_ID -> TB_MENU(MENU_ID)`(`db/oracle/01_menu_auth_schema.sql:53`)
- 체크: `MENU_TYPE IN ('G', 'M')`(`db/oracle/01_menu_auth_schema.sql:55`)
- 체크: `DISPLAY_YN`, `USE_YN`, `SYSTEM_YN`은 각각 `Y/N` 체크(`db/oracle/01_menu_auth_schema.sql:56`~`:58`)
- 체크: `MENU_TYPE='G'`이면 `MENU_URL IS NULL`, `MENU_TYPE='M'`이면 `MENU_URL IS NOT NULL`(`db/oracle/01_menu_auth_schema.sql:59`)
- 인덱스: `UX_TB_MENU_URL`, `IX_TB_MENU_01`, `IX_TB_MENU_02`(`db/oracle/01_menu_auth_schema.sql:65`~`:67`)

### `SMS.TB_MENU_AUTH`

| 컬럼 | 타입 | NOT NULL | 기본값 | 제약/의미 |
|---|---|---:|---|---|
| `MENU_ID` | `VARCHAR2(50)` | Y | - | 메뉴 ID |
| `ROLE_CD` | `VARCHAR2(30)` | Y | - | 권한 코드 |
| `CAN_READ` | `CHAR(1)` | Y | `'N'` | 읽기 권한 |
| `CAN_CREATE` | `CHAR(1)` | Y | `'N'` | 등록 권한 |
| `CAN_UPDATE` | `CHAR(1)` | Y | `'N'` | 수정 권한 |
| `CAN_DELETE` | `CHAR(1)` | Y | `'N'` | 삭제 권한 |
| `CAN_APPROVE` | `CHAR(1)` | Y | `'N'` | 승인 권한 |
| `CAN_CANCEL` | `CHAR(1)` | Y | `'N'` | 취소 권한 |
| `CAN_DOWNLOAD` | `CHAR(1)` | Y | `'N'` | 다운로드 권한 |
| `CAN_MASK_VIEW` | `CHAR(1)` | Y | `'N'` | 마스킹 보기 권한 |
| `USE_YN` | `CHAR(1)` | Y | `'Y'` | 사용 여부 |
| `REG_ID` | `VARCHAR2(12)` | Y | - | 등록자 |
| `REG_DTTM` | `TIMESTAMP(6)` | Y | `SYSTIMESTAMP` | 등록일시 |
| `UPD_ID` | `VARCHAR2(12)` | N | - | 수정자 |
| `UPD_DTTM` | `TIMESTAMP(6)` | N | - | 수정일시 |

- PK: `(MENU_ID, ROLE_CD)`(`db/oracle/01_menu_auth_schema.sql:85`)
- FK: `MENU_ID -> TB_MENU(MENU_ID)`(`db/oracle/01_menu_auth_schema.sql:86`)
- FK: `ROLE_CD -> TB_ROLE(ROLE_CD)`(`db/oracle/01_menu_auth_schema.sql:88`)
- 체크: `CAN_READ`부터 `CAN_MASK_VIEW`까지 `Y/N` 체크(`db/oracle/01_menu_auth_schema.sql:90`~`:97`)
- 체크: `USE_YN IN ('Y', 'N')`(`db/oracle/01_menu_auth_schema.sql:98`)
- 인덱스: `IX_TB_MENU_AUTH_01`(`db/oracle/01_menu_auth_schema.sql:101`)

## db/oracle/02_menu_auth_seed.sql 분석

- 경로: `db/oracle/02_menu_auth_seed.sql`
- 정의 대상: `TB_ROLE`, `TB_MENU`, `TB_EMP_ROLE`, `TB_MENU_AUTH` seed/upsert 절차

### 시드 절차

| 절차 | 경로:줄 | 대상 | 핵심 동작 |
|---|---|---|---|
| `upsert_role` | `db/oracle/02_menu_auth_seed.sql:4` | `SMS.TB_ROLE` | `MERGE`로 역할 코드/명칭/순서를 삽입/업데이트 |
| `upsert_menu` | `db/oracle/02_menu_auth_seed.sql:31` | `SMS.TB_MENU` | `MERGE`로 메뉴 트리를 삽입/업데이트 |
| `upsert_emp_role` | `db/oracle/02_menu_auth_seed.sql:77` | `SMS.TB_EMP_ROLE` | `(EMP_ID, DEP_ID, ROLE_CD)` 기준 역할 부여 |
| `upsert_menu_auth` | `db/oracle/02_menu_auth_seed.sql:100` | `SMS.TB_MENU_AUTH` | 메뉴별 모든 권한 플래그를 `Y`로 부여 |

### 시드 데이터 요약

| 대상 | 경로:줄 | 내용 |
|---|---|---|
| `ROLE_ADMIN` | `db/oracle/02_menu_auth_seed.sql:136` | 시스템 관리자 |
| `ROLE_MANAGER` | `db/oracle/02_menu_auth_seed.sql:137` | 업무 관리자 |
| `ROLE_USER` | `db/oracle/02_menu_auth_seed.sql:138` | 일반 사용자 |
| `ROLE_VIEWER` | `db/oracle/02_menu_auth_seed.sql:139` | 조회 사용자 |
| `G_BASIC`, `G_SMS_SEARCH`, `G_CAMPAIGN`, `G_SYSTEM`, `G_ACCOUNT`, `G_STATISTICS` | `db/oracle/02_menu_auth_seed.sql:141`~`:177` | 그룹 메뉴 |
| `SMS_HISTORY` | `db/oracle/02_menu_auth_seed.sql:149` | 발송이력조회 메뉴 |
| `admin / D001 / ROLE_ADMIN` | `db/oracle/02_menu_auth_seed.sql:179` | 로컬/운영 EMP 역할 시드 |
| `ROLE_ADMIN` 전체 메뉴 권한 | `db/oracle/02_menu_auth_seed.sql:181`~`:183` | `USE_YN='Y'`인 모든 메뉴에 전권한 부여 |

## db/oracle/03_privacy_audit_log_schema.sql 분석

- 경로: `db/oracle/03_privacy_audit_log_schema.sql`
- 정의 대상: `SMS.TB_PRIVACY_AUDIT_LOG`

| 컬럼 | 타입 | NOT NULL | 기본값 | 제약/의미 |
|---|---|---:|---|---|
| `LOG_ID` | `NUMBER GENERATED ALWAYS AS IDENTITY` | Y | 자동 생성 | PK |
| `EMP_ID` | `VARCHAR2(12)` | Y | - | 행위자 EMP ID |
| `DEP_ID` | `VARCHAR2(12)` | Y | - | 행위자 DEP ID |
| `EXECUTOR_IP` | `VARCHAR2(45)` | N | - | 요청 IP |
| `REQUEST_URL` | `VARCHAR2(300)` | Y | - | 요청 URL |
| `ACTION_TYPE` | `VARCHAR2(100 CHAR)` | Y | - | 수행 업무명 |
| `TARGET_DATA` | `VARCHAR2(500 CHAR)` | N | - | 마스킹된 요청 파라미터 |
| `REG_DTTM` | `TIMESTAMP(6)` | Y | `SYSTIMESTAMP` | 등록일시 |

- PK: `LOG_ID`(`db/oracle/03_privacy_audit_log_schema.sql:16`)
- 인덱스: `(EMP_ID, DEP_ID, REG_DTTM)`, `(REG_DTTM)`(`db/oracle/03_privacy_audit_log_schema.sql:19`~`:20`)
- 관계: EMP/DEP를 참조한다는 주석은 있으나 실제 FK 제약은 선언되지 않았다(`db/oracle/03_privacy_audit_log_schema.sql:23`~`:24`).

## db/oracle/04_sms_history_schema.sql 분석

- 경로: `db/oracle/04_sms_history_schema.sql`
- 정의 대상: `SMS.SMS_HISTORY`

| 컬럼 | 타입 | NOT NULL | 기본값 | 제약/의미 |
|---|---|---:|---|---|
| `SMS_HISTORY_ID` | `NUMBER GENERATED ALWAYS AS IDENTITY` | Y | 자동 생성 | PK |
| `REQUEST_ID` | `VARCHAR2(50)` | Y | - | 요청 식별자 |
| `SENT_AT` | `TIMESTAMP(6)` | Y | - | 발송 일시 |
| `RECEIVER_NO` | `VARCHAR2(20)` | Y | - | 수신자 번호 |
| `SENDER_NO` | `VARCHAR2(20)` | N | - | 발신 번호 |
| `SEND_TYPE` | `VARCHAR2(20)` | Y | - | 발송 유형 |
| `SEND_STATUS` | `VARCHAR2(20)` | Y | `'SENT'` | 발송 상태 |
| `RESULT_CD` | `VARCHAR2(30)` | N | - | 결과 코드 |
| `RESULT_MSG` | `VARCHAR2(500 CHAR)` | N | - | 결과 메시지 |
| `MESSAGE` | `CLOB` | N | - | 메시지 본문 |
| `EMP_ID` | `VARCHAR2(12)` | N | - | 발송 사용자 EMP ID |
| `DEP_ID` | `VARCHAR2(12)` | N | - | 발송 사용자 DEP ID |
| `REG_ID` | `VARCHAR2(12)` | Y | - | 등록자 |
| `REG_DTTM` | `TIMESTAMP(6)` | Y | `SYSTIMESTAMP` | 등록일시 |
| `UPD_ID` | `VARCHAR2(12)` | N | - | 수정자 |
| `UPD_DTTM` | `TIMESTAMP(6)` | N | - | 수정일시 |

- PK: `SMS_HISTORY_ID`(`db/oracle/04_sms_history_schema.sql:24`)
- Unique: `REQUEST_ID`(`db/oracle/04_sms_history_schema.sql:25`)
- FK: `(EMP_ID, DEP_ID) -> SMS.EMP(EMP_ID, DEP_ID)`(`db/oracle/04_sms_history_schema.sql:26`)
- 체크: `SEND_TYPE IN ('SMS', 'LMS', 'MMS', 'ALIMTALK')`(`db/oracle/04_sms_history_schema.sql:28`)
- 체크: `SEND_STATUS IN ('READY', 'SENT', 'SUCCESS', 'FAIL', 'CANCEL')`(`db/oracle/04_sms_history_schema.sql:29`)
- 인덱스: `IX_SMS_HISTORY_01`부터 `IX_SMS_HISTORY_04`(`db/oracle/04_sms_history_schema.sql:32`~`:35`)

## db/oracle/05_sms_history_seed.sql 분석

- 경로: `db/oracle/05_sms_history_seed.sql`
- 정의 대상: `SMS.SMS_HISTORY` 샘플 데이터

| 항목 | 경로:줄 | 내용 |
|---|---|---|
| 샘플 1 | `db/oracle/05_sms_history_seed.sql:7` | `REQ202606180001`, `admin/D001`, `SMS`, `SUCCESS` |
| 샘플 2 | `db/oracle/05_sms_history_seed.sql:25` | `REQ202606180002`, `admin/D001`, `LMS`, `SUCCESS` |
| 샘플 3 | `db/oracle/05_sms_history_seed.sql:43` | `REQ202606180003`, `admin/D001`, `SMS`, `FAIL` |
| 샘플 4 | `db/oracle/05_sms_history_seed.sql:61` | `REQ202606180004`, `admin/D001`, `ALIMTALK`, `SUCCESS` |
| 샘플 5 | `db/oracle/05_sms_history_seed.sql:79` | `REQ202606180005`, `admin/D001`, `MMS`, `SENT` |
| 샘플 6 | `db/oracle/05_sms_history_seed.sql:97` | `REQ202606180006`, `admin/D001`, `SMS`, `CANCEL` |

- 모든 샘플은 `EMP_ID='admin'`, `DEP_ID='D001'`을 사용하므로, `SMS.SMS_HISTORY(EMP_ID, DEP_ID)`는 `SMS.EMP(EMP_ID, DEP_ID)`와 연결된다.
- `MESSAGE`는 `TO_CLOB(...)`로 저장된다.

## db/oracle/sms_history_menu_seed.sql 분석

- 경로: `db/oracle/sms_history_menu_seed.sql`
- 정의 대상: `SMS.TB_MENU`, `SMS.TB_MENU_AUTH`

| 항목 | 경로:줄 | 내용 |
|---|---|---|
| 메뉴 삽입 | `db/oracle/sms_history_menu_seed.sql:6` | `SMS_HISTORY` 메뉴 삽입 |
| 부모 메뉴 ID | `db/oracle/sms_history_menu_seed.sql:10` | `/* TODO: 상위 메뉴 ID */`로 미확정 |
| 권한 삽입 | `db/oracle/sms_history_menu_seed.sql:14` | `SMS_HISTORY`에 `ROLE_ADMIN` 전체 권한 부여 |

- 핵심 관계: `SMS.TB_MENU_AUTH(MENU_ID, ROLE_CD)`는 `SMS.TB_MENU(MENU_ID)`와 `SMS.TB_ROLE(ROLE_CD)`를 참조한다.
- 미확인: `PARENT_MENU_ID`가 실제 메뉴 ID가 아니라 주석 문자열이므로, 이 파일만으로는 메뉴 트리 자기 참조가 확정되지 않는다.

## 주요 VO 클래스 및 테이블 컬럼 매핑

| VO | 경로:줄 | 매핑 대상 | 매핑 컬럼 | 비고 |
|---|---|---|---|---|
| `SmsHistoryVO` | `src/main/java/com/scbk/sms/vo/sms/SmsHistoryVO.java:9` | `SMS.SMS_HISTORY` | `SMS_HISTORY_ID`, `REQUEST_ID`, `SENT_AT`, `RECEIVER_NO`, `SENDER_NO`, `SEND_TYPE`, `SEND_STATUS`, `RESULT_CD`, `RESULT_MSG` | `MESSAGE`, `EMP_ID`, `DEP_ID`, `REG_ID`, `REG_DTTM`, `UPD_ID`, `UPD_DTTM`은 포함하지 않음 |
| `LoginEmployeeVO` | `src/main/java/com/scbk/sms/vo/auth/LoginEmployeeVO.java:5` | `SMS.EMP`, `SMS.DEP` | `EMP_ID`, `DEP_ID`, `EMP_NM`, `DEP_NM`, `ACT_YN`, `DEP.ACT_YN` | 로그인/직원 조회용. `EMP_ID` 단독이 아니라 `(EMP_ID, DEP_ID)`를 함께 사용 |
| `MenuAuthVO` | `src/main/java/com/scbk/sms/vo/menu/MenuAuthVO.java:8` | `SMS.TB_MENU_AUTH` | `CAN_READ`, `CAN_CREATE`, `CAN_UPDATE`, `CAN_DELETE`, `CAN_APPROVE`, `CAN_CANCEL`, `CAN_DOWNLOAD`, `CAN_MASK_VIEW` | 여러 역할의 권한은 `MAX(CAN_...)`로 집계됨 |
| `MenuItemVO` | `src/main/java/com/scbk/sms/vo/menu/MenuItemVO.java:8` | `SMS.TB_MENU` | `MENU_ID`, `PARENT_MENU_ID`, `MENU_NM`, `MENU_URL`, `MENU_LEVEL`, `SORT_ORD`, `MENU_TYPE`, `ICON_NM`, `DISPLAY_YN`, `USE_YN`, `SYSTEM_YN` | `children` 필드는 `PARENT_MENU_ID -> MENU_ID` 자기 참조 트리 |
| `PrivacyAuditLogVO` | `src/main/java/com/scbk/sms/vo/system/PrivacyAuditLogVO.java:8` | `SMS.TB_PRIVACY_AUDIT_LOG` | `EMP_ID`, `DEP_ID`, `EXECUTOR_IP`, `REQUEST_URL`, `ACTION_TYPE`, `TARGET_DATA` | `LOG_ID`, `REG_DTTM`은 DB 생성/기록 컬럼이라 VO에 없음 |
| `CommonCodeVO` | `src/main/java/com/scbk/sms/vo/common/CommonCodeVO.java:8` | `SMS.DEP`, `SMS.TB_ROLE` | `code`, `name` | `DEP_ID/DEP_NM`, `ROLE_CD/ROLE_NM` 등을 콤보용으로 공통화 |

## 관계 정리

| 엔티티 | 관계 |
|---|---|
| `EMP` | `(EMP_ID, DEP_ID)`가 v3 권한/감사/발송이력의 사용자 식별 기준 |
| `DEP` | `EMP`의 부서 단위. `DEP_ID`만으로 EMP를 식별하지 않는다 |
| `TB_ROLE` | 권한 코드/명칭/순서/사용 여부 정의 |
| `TB_EMP_ROLE` | `EMP`와 `ROLE`의 다대다 연결 테이블 |
| `TB_MENU` | 메뉴 트리. 자기 참조 `PARENT_MENU_ID`로 계층 구조 |
| `TB_MENU_AUTH` | `TB_ROLE`과 `TB_MENU`를 연결하는 메뉴 권한 테이블 |
| `SMS.SMS_HISTORY` | 발송 이력. `EMP_ID+DEP_ID`로 EMP와 연결 |
| `TB_PRIVACY_AUDIT_LOG` | 개인정보 접근 로그. EMP/DEP는 필드로 저장되지만 FK는 없음 |

## 확인하지 못한 부분 / 주의사항

- `db/oracle` 하위 SQL에는 `SMS.EMP`, `SMS.DEP`의 실제 운영 DDL이 포함되어 있지 않다. 현재 확인한 EMP/DEP 구조는 `docker/local-emp-dep-seed.sql`의 로컬 fixture이다.
- `SMS.TB_PRIVACY_AUDIT_LOG`는 EMP/DEP를 필드로 저장하지만 FK가 선언되어 있지 않다.
- `SMS.SMS_HISTORY`의 `EMP_ID`, `DEP_ID`는 DDL상 NOT NULL이 아니지만, seed와 운영 기준에서는 `admin/D001`을 사용한다.
- `sms_history_menu_seed.sql`의 `PARENT_MENU_ID`가 주석 문자열이므로, 이 파일만으로는 `SMS_HISTORY`의 실제 상위 메뉴가 확정되지 않는다.
- `TB_MENU_AUTH` 권한은 문자열 `Y/N`을 `MAX(CAN_...)`로 집계한다. 현재 DB가 `Y/N` 문자열이면 동작하지만, Boolean 타입이면 집계 방식 변경이 필요하다.
