-- 로컬 docker 전용 테스트 픽스처 (운영 EMP/DEP 대용).
-- db/oracle(DBA 산출물)과 분리한다. 실제 EMP/DEP DDL/데이터를 확보하면 이 파일 대신 그것을 적재한다.
-- ActiveEmployeeResolver / LoginEmployeeMapper 가 읽는 최소 컬럼만 둔다.
-- 식별 기준은 (EMP_ID, DEP_ID). 로컬 로그인 테스트 사번 = admin.
CREATE TABLE SMS.DEP (
    DEP_ID  VARCHAR2(12)  NOT NULL,
    DEP_NM  VARCHAR2(100) NOT NULL,
    ACT_YN  CHAR(1) DEFAULT 'Y' NOT NULL,
    CONSTRAINT PK_DEP PRIMARY KEY (DEP_ID)
);

CREATE TABLE SMS.EMP (
    EMP_ID  VARCHAR2(12)  NOT NULL,
    DEP_ID  VARCHAR2(12)  NOT NULL,
    EMP_NM  VARCHAR2(100) NOT NULL,
    ACT_YN  CHAR(1) DEFAULT 'Y' NOT NULL,
    CONSTRAINT PK_EMP PRIMARY KEY (EMP_ID, DEP_ID)
);

INSERT INTO SMS.DEP (DEP_ID, DEP_NM, ACT_YN) VALUES ('D001', 'TEST_DEPT', 'Y');
INSERT INTO SMS.EMP (EMP_ID, DEP_ID, EMP_NM, ACT_YN) VALUES ('admin', 'D001', 'admin', 'Y');
COMMIT;
