-- ============================================================
-- 메뉴 등록 SQL ( 발송이력조회 )
-- 폐쇄망 반입 전에는 parentMenuId/menuId/roleCode 값을 수동 확인한다.
-- ============================================================

INSERT INTO SMS.TB_MENU (
    MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_URL,
    MENU_LEVEL, SORT_ORD, MENU_TYPE, DISPLAY_YN, USE_YN, SYSTEM_YN, REG_ID
) VALUES (
    'SMS_HISTORY', '/* TODO: 상위 메뉴 ID */', '발송이력조회', '/sms/history',
    2, 99, 'M', 'Y', 'Y', 'N', 'SYSTEM'
);

INSERT INTO SMS.TB_MENU_AUTH (
    MENU_ID, ROLE_CD,
    CAN_READ, CAN_CREATE, CAN_UPDATE, CAN_DELETE,
    CAN_APPROVE, CAN_CANCEL, CAN_DOWNLOAD, CAN_MASK_VIEW,
    USE_YN, REG_ID
) VALUES (
    'SMS_HISTORY', 'ROLE_ADMIN',
    'Y', 'Y', 'Y', 'Y',
    'Y', 'Y', 'Y', 'Y',
    'Y', 'SYSTEM'
);

COMMIT;

-- 파일 배치 경로
-- src/main/java/com/scbk/sms/dto/sms/SmsHistorySearchRequestDTO.java
-- src/main/java/com/scbk/sms/vo/sms/SmsHistoryVO.java
-- src/main/java/com/scbk/sms/mapper/sms/SmsHistoryMapper.java
-- src/main/java/com/scbk/sms/service/sms/SmsHistoryService.java
-- src/main/java/com/scbk/sms/controller/sms/SmsHistoryController.java
-- src/main/resources/mapper/sms/SmsHistoryMapper.xml
-- src/main/resources/templates/sms/history.html
-- src/main/resources/static/js/sms/history.js
-- src/test/java/com/scbk/sms/service/sms/SmsHistoryServiceTest.java
-- src/test/java/com/scbk/sms/controller/sms/SmsHistoryControllerTest.java

-- 생성 후 docs/base/screen-generation-guide.md의 8~10단계(권한 확인, 검증, 문서 갱신)를 수행한다.
