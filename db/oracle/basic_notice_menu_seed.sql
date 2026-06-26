-- ============================================================
-- 메뉴 등록 SQL ( 공지사항관리 )
-- 폐쇄망 반입 전에는 parentMenuId/menuId/roleCode 값을 수동 확인한다.
-- ============================================================

INSERT INTO SMS.TB_MENU (
    MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_URL,
    MENU_LEVEL, SORT_ORD, MENU_TYPE, DISPLAY_YN, USE_YN, SYSTEM_YN, REG_ID
) VALUES (
    'BASIC_NOTICE', '/* TODO: 상위 메뉴 ID */', '공지사항관리', '/basic/notice',
    2, 99, 'M', 'Y', 'Y', 'N', 'SYSTEM'
);

INSERT INTO SMS.TB_MENU_AUTH (
    MENU_ID, ROLE_CD,
    CAN_READ, CAN_CREATE, CAN_UPDATE, CAN_DELETE,
    CAN_APPROVE, CAN_CANCEL, CAN_DOWNLOAD, CAN_MASK_VIEW,
    USE_YN, REG_ID
) VALUES (
    'BASIC_NOTICE', 'ROLE_ADMIN',
    'Y', 'Y', 'Y', 'Y',
    'Y', 'Y', 'Y', 'Y',
    'Y', 'SYSTEM'
);

COMMIT;

-- 파일 배치 경로
-- src/main/java/com/scbk/sms/dto/basic/NoticeSearchRequestDTO.java
-- src/main/java/com/scbk/sms/vo/basic/NoticeVO.java
-- src/main/java/com/scbk/sms/mapper/basic/NoticeMapper.java
-- src/main/java/com/scbk/sms/service/basic/NoticeService.java
-- src/main/java/com/scbk/sms/controller/basic/NoticeController.java
-- src/main/resources/mapper/basic/NoticeMapper.xml
-- src/main/resources/templates/basic/notice.html
-- src/main/resources/static/js/basic/notice.js
-- src/test/java/com/scbk/sms/service/basic/NoticeServiceTest.java
-- src/test/java/com/scbk/sms/controller/basic/NoticeControllerTest.java

-- 생성 후 docs/base/screen-generation-guide.md의 8~10단계(권한 확인, 검증, 문서 갱신)를 수행한다.
