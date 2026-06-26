# EMP / DEP 사용자 식별 정책

v3는 실제 운영 기준 테이블인 `EMP`, `DEP`를 사용자와 부서의 기준 테이블로 사용한다.

## DEP

```sql
CREATE TABLE SMS.DEP (
    DEP_ID   VARCHAR2(12),
    DEP_NM   VARCHAR2(20),
    WRT_DTTM CHAR(14),
    DEL_DTTM CHAR(14),
    ACT_YN   CHAR(1),
    PRIMARY KEY (DEP_ID)
);
```

## EMP

```sql
CREATE TABLE SMS.EMP (
    EMP_ID             VARCHAR2(12) NOT NULL,
    DEP_ID             VARCHAR2(12) NOT NULL,
    EMP_PASS           VARCHAR2(24),
    EMP_NM             VARCHAR2(20),
    EMP_LEV            CHAR(1),
    REG_DTTM           VARCHAR2(14),
    MAX_SEND_CNT       NUMBER,
    PERM_CPN           CHAR(1),
    PERM_SYS           CHAR(1),
    PERM_STA           CHAR(1),
    NOW_SEND_CNT       NUMBER,
    LAST_SEND_DT       CHAR(8),
    LOGIN_FAIL_CNT     NUMBER,
    EMP_PHONE          VARCHAR2(15),
    PERM_PSN           CHAR(1),
    PERM_AUT           CHAR(1),
    PERM_CPN_AGREE     CHAR(1),
    DEL_DTTM           VARCHAR2(14),
    ACT_YN             CHAR(1),
    LAST_LOGIN_DTTM    VARCHAR2(14),
    PASS_UPDATE_DTTM   CHAR(14),
    PASS_UPDATE_YN     CHAR(1) DEFAULT 'N' NOT NULL,
    PERM_SND_CANCEL    CHAR(1),
    UPDATE_DTTM        CHAR(14),
    PERM_NOM           CHAR(1),
    PERM_NOM_AGREE     CHAR(1),
    PERM_MMS           CHAR(1),
    PERM_MMS_AGREE     CHAR(1),
    OLD_PWD1           VARCHAR2(24),
    OLD_PWD2           VARCHAR2(24),
    OLD_PWD3           VARCHAR2(24),
    OLD_PWD4           VARCHAR2(24),
    OLD_PWD5           VARCHAR2(24),
    OLD_PWD6           VARCHAR2(24),
    OLD_PWD7           VARCHAR2(24),
    OLD_PWD8           VARCHAR2(24),
    OLD_PWD9           VARCHAR2(24),
    OLD_PWD10          VARCHAR2(24),
    MFA_YN             CHAR(1),
    CONSTRAINT EMP_PK PRIMARY KEY (EMP_ID, DEP_ID)
);
```

## 사용자 식별 기준

`EMP_ID` 단독으로 사용자를 식별하지 않는다.

v3에서 사용자를 식별할 때는 항상 아래 두 값을 함께 사용한다.

```text
EMP_ID + DEP_ID
```

이 기준은 다음 영역에 모두 적용한다.

- 로그인 세션 principal
- 역할 매핑
- 메뉴 권한 판단
- 감사 로그
- 개인정보 조회 이력
- 등록자/수정자 추적

## ACT_YN 정책

- `EMP.ACT_YN = 'Y'`인 사용자만 로그인할 수 있다.
- `DEP.ACT_YN = 'Y'`인 부서만 활성 부서로 판단한다.
- `DEL_DTTM`이 존재하면 삭제 또는 비활성 후보로 본다.
- 활성 여부 판단은 `ACT_YN`을 우선 기준으로 둔다.

## EMP.PERM_* 처리 정책

v3는 `EMP.PERM_*` 컬럼을 권한 판단 기준으로 사용하지 않는다.

다음 컬럼들은 AS-IS legacy 권한 필드로만 간주한다.

```text
PERM_CPN
PERM_SYS
PERM_STA
PERM_PSN
PERM_AUT
PERM_CPN_AGREE
PERM_SND_CANCEL
PERM_NOM
PERM_NOM_AGREE
PERM_MMS
PERM_MMS_AGREE
```

v3 신규 권한 판단은 반드시 아래 테이블만 사용한다.

```text
TB_ROLE
TB_EMP_ROLE
TB_MENU
TB_MENU_AUTH
```

단, 초기 데이터 이관 시 `EMP.PERM_*` 값은 참고자료로 사용할 수 있다. 이 경우에도 런타임 권한 판단에 직접 사용하지 않는다.

## local 로그인 주의점

local은 `EMP_ID`만 입력받는다. 하지만 `EMP`의 PK가 `(EMP_ID, DEP_ID)`이므로 동일 `EMP_ID`에 활성 부서가 여러 건이면 자동 선택하지 않는다.

활성 판단은 `EMP.ACT_YN = 'Y'`와 소속 `DEP.ACT_YN = 'Y'`를 모두 만족하는 행 기준이다. 아래 조회 결과 건수도 이 기준으로 센다.

처리 기준:

| 조회 결과 | 처리 |
|---|---|
| 0건 | 로그인 실패 |
| 1건 | 로그인 성공 |
| 2건 이상 | 로그인 실패, 중복 활성 사용자 데이터 정리 필요 |

자동으로 첫 번째 부서를 선택하지 않는다. 이는 권한 오작동을 막기 위한 규칙이다.
