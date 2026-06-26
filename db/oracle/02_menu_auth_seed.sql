SET DEFINE OFF

DECLARE
    PROCEDURE upsert_role(
        p_role_cd   IN SMS.TB_ROLE.ROLE_CD%TYPE,
        p_role_nm   IN SMS.TB_ROLE.ROLE_NM%TYPE,
        p_role_desc IN SMS.TB_ROLE.ROLE_DESC%TYPE,
        p_sort_ord  IN SMS.TB_ROLE.SORT_ORD%TYPE
    ) IS
    BEGIN
        MERGE INTO SMS.TB_ROLE T
        USING (
            SELECT p_role_cd ROLE_CD, p_role_nm ROLE_NM, p_role_desc ROLE_DESC, p_sort_ord SORT_ORD
            FROM DUAL
        ) S
        ON (T.ROLE_CD = S.ROLE_CD)
        WHEN MATCHED THEN UPDATE SET
            T.ROLE_NM = S.ROLE_NM,
            T.ROLE_DESC = S.ROLE_DESC,
            T.SORT_ORD = S.SORT_ORD,
            T.USE_YN = 'Y',
            T.UPD_ID = 'SYSTEM',
            T.UPD_DTTM = SYSTIMESTAMP
        WHEN NOT MATCHED THEN INSERT (
            ROLE_CD, ROLE_NM, ROLE_DESC, SORT_ORD, USE_YN, REG_ID
        ) VALUES (
            S.ROLE_CD, S.ROLE_NM, S.ROLE_DESC, S.SORT_ORD, 'Y', 'SYSTEM'
        );
    END;

    PROCEDURE upsert_menu(
        p_menu_id        IN SMS.TB_MENU.MENU_ID%TYPE,
        p_parent_menu_id IN SMS.TB_MENU.PARENT_MENU_ID%TYPE,
        p_menu_nm        IN SMS.TB_MENU.MENU_NM%TYPE,
        p_menu_url       IN SMS.TB_MENU.MENU_URL%TYPE,
        p_menu_level     IN SMS.TB_MENU.MENU_LEVEL%TYPE,
        p_sort_ord       IN SMS.TB_MENU.SORT_ORD%TYPE,
        p_menu_type      IN SMS.TB_MENU.MENU_TYPE%TYPE,
        p_remark         IN SMS.TB_MENU.REMARK%TYPE DEFAULT NULL
    ) IS
    BEGIN
        MERGE INTO SMS.TB_MENU T
        USING (
            SELECT p_menu_id MENU_ID,
                   p_parent_menu_id PARENT_MENU_ID,
                   p_menu_nm MENU_NM,
                   p_menu_url MENU_URL,
                   p_menu_level MENU_LEVEL,
                   p_sort_ord SORT_ORD,
                   p_menu_type MENU_TYPE,
                   p_remark REMARK
            FROM DUAL
        ) S
        ON (T.MENU_ID = S.MENU_ID)
        WHEN MATCHED THEN UPDATE SET
            T.PARENT_MENU_ID = S.PARENT_MENU_ID,
            T.MENU_NM = S.MENU_NM,
            T.MENU_URL = S.MENU_URL,
            T.MENU_LEVEL = S.MENU_LEVEL,
            T.SORT_ORD = S.SORT_ORD,
            T.MENU_TYPE = S.MENU_TYPE,
            T.DISPLAY_YN = 'Y',
            T.USE_YN = 'Y',
            T.SYSTEM_YN = 'N',
            T.REMARK = S.REMARK,
            T.UPD_ID = 'SYSTEM',
            T.UPD_DTTM = SYSTIMESTAMP
        WHEN NOT MATCHED THEN INSERT (
            MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_URL, MENU_LEVEL, SORT_ORD,
            MENU_TYPE, ICON_NM, DISPLAY_YN, USE_YN, SYSTEM_YN, REMARK, REG_ID
        ) VALUES (
            S.MENU_ID, S.PARENT_MENU_ID, S.MENU_NM, S.MENU_URL, S.MENU_LEVEL, S.SORT_ORD,
            S.MENU_TYPE, NULL, 'Y', 'Y', 'N', S.REMARK, 'SYSTEM'
        );
    END;

    PROCEDURE upsert_emp_role(
        p_emp_id  IN SMS.TB_EMP_ROLE.EMP_ID%TYPE,
        p_dep_id  IN SMS.TB_EMP_ROLE.DEP_ID%TYPE,
        p_role_cd IN SMS.TB_EMP_ROLE.ROLE_CD%TYPE
    ) IS
    BEGIN
        MERGE INTO SMS.TB_EMP_ROLE T
        USING (
            SELECT p_emp_id EMP_ID, p_dep_id DEP_ID, p_role_cd ROLE_CD
            FROM DUAL
        ) S
        ON (T.EMP_ID = S.EMP_ID AND T.DEP_ID = S.DEP_ID AND T.ROLE_CD = S.ROLE_CD)
        WHEN MATCHED THEN UPDATE SET
            T.USE_YN = 'Y',
            T.UPD_ID = 'SYSTEM',
            T.UPD_DTTM = SYSTIMESTAMP
        WHEN NOT MATCHED THEN INSERT (
            EMP_ID, DEP_ID, ROLE_CD, USE_YN, REG_ID
        ) VALUES (
            S.EMP_ID, S.DEP_ID, S.ROLE_CD, 'Y', 'SYSTEM'
        );
    END;

    PROCEDURE upsert_menu_auth(
        p_menu_id IN SMS.TB_MENU_AUTH.MENU_ID%TYPE,
        p_role_cd IN SMS.TB_MENU_AUTH.ROLE_CD%TYPE
    ) IS
    BEGIN
        MERGE INTO SMS.TB_MENU_AUTH T
        USING (
            SELECT p_menu_id MENU_ID, p_role_cd ROLE_CD
            FROM DUAL
        ) S
        ON (T.MENU_ID = S.MENU_ID AND T.ROLE_CD = S.ROLE_CD)
        WHEN MATCHED THEN UPDATE SET
            T.CAN_READ = 'Y',
            T.CAN_CREATE = 'Y',
            T.CAN_UPDATE = 'Y',
            T.CAN_DELETE = 'Y',
            T.CAN_APPROVE = 'Y',
            T.CAN_CANCEL = 'Y',
            T.CAN_DOWNLOAD = 'Y',
            T.CAN_MASK_VIEW = 'Y',
            T.USE_YN = 'Y',
            T.UPD_ID = 'SYSTEM',
            T.UPD_DTTM = SYSTIMESTAMP
        WHEN NOT MATCHED THEN INSERT (
            MENU_ID, ROLE_CD,
            CAN_READ, CAN_CREATE, CAN_UPDATE, CAN_DELETE,
            CAN_APPROVE, CAN_CANCEL, CAN_DOWNLOAD, CAN_MASK_VIEW,
            USE_YN, REG_ID
        ) VALUES (
            S.MENU_ID, S.ROLE_CD,
            'Y', 'Y', 'Y', 'Y',
            'Y', 'Y', 'Y', 'Y',
            'Y', 'SYSTEM'
        );
    END;
BEGIN
    upsert_role('ROLE_ADMIN', '시스템 관리자', '메뉴/권한/사용자/시스템 관리 권한', 10);
    upsert_role('ROLE_MANAGER', '업무 관리자', '업무 조회/등록/수정/승인 권한', 20);
    upsert_role('ROLE_USER', '일반 사용자', '일반 업무 조회 및 제한된 등록 권한', 30);
    upsert_role('ROLE_VIEWER', '조회 사용자', '조회 전용 권한', 40);

    upsert_menu('G_BASIC', NULL, '기본메뉴', NULL, 1, 10, 'G');
    upsert_menu('BASIC_INTRO', 'G_BASIC', 'SMS관리시스템 안내', '/basic/intro', 2, 10, 'M');
    upsert_menu('BASIC_NOTICE', 'G_BASIC', '공지사항', '/basic/notice', 2, 20, 'M');
    upsert_menu('BASIC_MESSAGE', 'G_BASIC', '메시지조회', '/basic/message', 2, 30, 'M');
    upsert_menu('BASIC_USER_SEARCH', 'G_BASIC', '사용자조회', '/basic/user-search', 2, 40, 'M');
    upsert_menu('BASIC_MFA', 'G_BASIC', 'MFA사용자관리', '/basic/mfa', 2, 50, 'M');

    upsert_menu('G_SMS_SEARCH', NULL, 'SMS발송조회', NULL, 1, 20, 'G');
    upsert_menu('SMS_HISTORY', 'G_SMS_SEARCH', '발송이력조회', '/sms/history', 2, 10, 'M');
    upsert_menu('SMS_CUSTOMER_SEARCH', 'G_SMS_SEARCH', '고객별 조회', '/sms/customer-search', 2, 20, 'M');
    upsert_menu('SMS_SSN_SEARCH', 'G_SMS_SEARCH', '주민번호 조회', '/sms/ssn-search', 2, 30, 'M');

    upsert_menu('G_CAMPAIGN', NULL, '캠페인SMS', NULL, 1, 30, 'G');
    upsert_menu('CAMPAIGN_TARGET', 'G_CAMPAIGN', '발송대상관리', '/campaign/target-manage', 2, 10, 'M');
    upsert_menu('CAMPAIGN_TARGET_APPROVAL', 'G_CAMPAIGN', '발송대상승인', '/approval', 2, 20, 'M');
    upsert_menu('CAMPAIGN_SMS_REGISTER', 'G_CAMPAIGN', 'SMS등록', '/campaign/sms/register', 2, 30, 'M');
    upsert_menu('CAMPAIGN_LMS_REGISTER', 'G_CAMPAIGN', 'LMS등록', '/campaign/lms/register', 2, 40, 'M');
    upsert_menu('CAMPAIGN_ALIMTALK_REGISTER', 'G_CAMPAIGN', '알림톡등록', '/campaign/alimtalk/register', 2, 50, 'M');
    upsert_menu('CAMPAIGN_SMS_APPROVE', 'G_CAMPAIGN', 'SMS승인', '/campaign/sms/approve', 2, 60, 'M');
    upsert_menu('CAMPAIGN_LMS_APPROVE', 'G_CAMPAIGN', 'LMS승인', '/campaign/lms/approve', 2, 70, 'M');
    upsert_menu('CAMPAIGN_ALIMTALK_APPROVE', 'G_CAMPAIGN', '알림톡승인', '/campaign/alimtalk/approve', 2, 80, 'M');
    upsert_menu('CAMPAIGN_SMS_HISTORY', 'G_CAMPAIGN', '발송이력조회', '/sms/campaign', 2, 90, 'M', 'SMS 캠페인 발송이력');
    upsert_menu('CAMPAIGN_LMS_HISTORY', 'G_CAMPAIGN', 'LMS발송이력조회', '/sms/campaign-lms', 2, 100, 'M');
    upsert_menu('CAMPAIGN_ALIMTALK_HISTORY', 'G_CAMPAIGN', '알림톡 발송이력조회', '/sms/campaign-alimtalk', 2, 110, 'M');

    upsert_menu('G_SYSTEM', NULL, '시스템관리', NULL, 1, 40, 'G');
    upsert_menu('SYSTEM_DEP', 'G_SYSTEM', '부서관리', '/system/dept-manage', 2, 10, 'M', 'DEP 기준');
    upsert_menu('SYSTEM_MESSAGE', 'G_SYSTEM', '메시지 관리', '/system/message', 2, 20, 'M');
    upsert_menu('SYSTEM_KAKAO_TEMPLATE', 'G_SYSTEM', '카카오템플릿관리', '/system/kakao-template', 2, 30, 'M');
    upsert_menu('SYSTEM_AD_MESSAGE', 'G_SYSTEM', '광고성 메시지관리', '/system/ad-message', 2, 40, 'M');
    upsert_menu('SYSTEM_HOURLY_STATS', 'G_SYSTEM', '시간대별조회', '/sms/dept-stat', 2, 50, 'M');

    upsert_menu('G_ACCOUNT', NULL, '시스템관리 계정관리', NULL, 1, 50, 'G');
    upsert_menu('ACCOUNT_USER', 'G_ACCOUNT', '사용자관리', '/account/user-manage', 2, 10, 'M', 'EMP 기준');

    upsert_menu('G_STATISTICS', NULL, '통계 관리', NULL, 1, 60, 'G');
    upsert_menu('STAT_MARKETING_OPTOUT', 'G_STATISTICS', '마케팅 철회 통계', '/statistics/marketing-optout', 2, 10, 'M');

    upsert_emp_role('admin', 'D001', 'ROLE_ADMIN');

    FOR R IN (SELECT MENU_ID FROM SMS.TB_MENU WHERE USE_YN = 'Y') LOOP
        upsert_menu_auth(R.MENU_ID, 'ROLE_ADMIN');
    END LOOP;
END;
/

COMMIT;