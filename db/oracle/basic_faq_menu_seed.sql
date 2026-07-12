-- Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
-- ============================================================
-- 메뉴 등록 SQL ( FAQ관리 )
-- 폐쇄망 반입 전에는 parentMenuId/menuId/roleCode 값을 수동 확인한다.
-- ============================================================

INSERT INTO SMS.TB_MENU (
    MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_URL,
    MENU_LEVEL, SORT_ORD, MENU_TYPE, DISPLAY_YN, USE_YN, SYSTEM_YN, REG_ID
) VALUES (
    'BASIC_FAQ', '/* TODO: 상위 메뉴 ID */', 'FAQ관리', '/basic/faq',
    2, 99, 'M', 'Y', 'Y', 'N', 'SYSTEM'
);

INSERT INTO SMS.TB_MENU_AUTH (
    MENU_ID, ROLE_CD,
    CAN_READ, CAN_CREATE, CAN_UPDATE, CAN_DELETE,
    CAN_APPROVE, CAN_CANCEL, CAN_DOWNLOAD, CAN_MASK_VIEW,
    USE_YN, REG_ID
) VALUES (
    'BASIC_FAQ', 'ROLE_ADMIN',
    'Y', 'Y', 'Y', 'Y',
    'N', 'N', 'N', 'N',
    'Y', 'SYSTEM'
);

COMMIT;

-- 파일 배치 경로
-- src/main/java/com/scbk/sms/dto/basic/FaqSearchRequestDTO.java
-- src/main/java/com/scbk/sms/vo/basic/FaqVO.java
-- src/main/java/com/scbk/sms/mapper/basic/FaqMapper.java
-- src/main/java/com/scbk/sms/service/basic/FaqService.java
-- src/main/java/com/scbk/sms/controller/basic/FaqController.java
-- src/main/resources/mapper/basic/FaqMapper.xml
-- src/main/resources/templates/basic/faq.html
-- src/main/resources/static/js/basic/faq.js
-- src/test/java/com/scbk/sms/service/basic/FaqServiceTest.java
-- src/test/java/com/scbk/sms/controller/basic/FaqControllerTest.java

-- 생성 후 docs/base/screen-generation-guide.md의 8~10단계(권한 확인, 검증, 문서 갱신)를 수행한다.
