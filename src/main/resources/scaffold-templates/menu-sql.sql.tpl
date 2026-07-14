-- Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
-- ============================================================
-- 메뉴 등록 SQL ( [( ${model.domainName()} )] )
-- 폐쇄망 반입 전에는 parentMenuId/menuId/roleCode 값을 수동 확인한다.
-- ============================================================

INSERT INTO SMS.TB_MENU (
    MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_URL,
    MENU_LEVEL, SORT_ORD, MENU_TYPE, DISPLAY_YN, USE_YN, SYSTEM_YN, REG_ID
) VALUES (
    '[( ${model.menuId()} )]', '[( ${model.parentMenuId()} )]', '[( ${model.domainName()} )]', '[( ${model.screenUrl()} )]',
    2, [( ${model.menuSortOrd()} )], 'M', 'Y', 'Y', 'N', 'SYSTEM'
);

INSERT INTO SMS.TB_MENU_AUTH (
    MENU_ID, ROLE_CD,
    CAN_READ, CAN_CREATE, CAN_UPDATE, CAN_DELETE,
    CAN_APPROVE, CAN_CANCEL, CAN_DOWNLOAD, CAN_MASK_VIEW,
    USE_YN, REG_ID
) VALUES (
    '[( ${model.menuId()} )]', '[( ${model.roleCode()} )]',
    'Y', '[( ${canCreate} )]', '[( ${canUpdate} )]', '[( ${canDelete} )]',
    'N', 'N', '[( ${canDownload} )]', '[( ${canMaskView} )]',
    'Y', 'SYSTEM'
);

COMMIT;

-- 파일 배치 경로
-- src/main/java/com/scbk/sms/dto/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]SearchRequestDTO.java
-- src/main/java/com/scbk/sms/vo/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]VO.java
-- src/main/java/com/scbk/sms/mapper/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]Mapper.java
-- src/main/java/com/scbk/sms/service/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]Service.java
-- src/main/java/com/scbk/sms/controller/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]Controller.java
-- src/main/resources/mapper/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]Mapper.xml
-- src/main/resources/templates/[( ${model.moduleName()} )]/[( ${model.domainId()} )].html
-- src/main/resources/static/js/[( ${model.moduleName()} )]/[( ${model.domainId()} )].js
-- src/test/java/com/scbk/sms/service/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]ServiceTest.java
-- src/test/java/com/scbk/sms/controller/[( ${model.moduleName()} )]/[( ${model.domainClass()} )]ControllerTest.java

-- 생성 후 docs/base/screen-generation-guide.md의 8~10단계(권한 확인, 검증, 문서 갱신)를 수행한다.
