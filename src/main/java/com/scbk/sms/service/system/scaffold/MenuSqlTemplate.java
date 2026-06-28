package com.scbk.sms.service.system.scaffold;

/** 메뉴/권한 등록 SQL 생성. v3 스키마(TB_MENU MENU_ID, TB_MENU_AUTH ROLE_CD + CAN_* 8종) 기준. */
public final class MenuSqlTemplate {

  private MenuSqlTemplate() {}

  public static String generate(ScaffoldModel model) {
    String menuId = model.menuId();
    String module = model.moduleName();
    String cls = model.domainClass();
    String domainId = model.domainId();
    String mode = model.screenMode();
    String canRead = "Y";
    String canCreate = "CRUD".equals(mode) ? "Y" : "N";
    String canUpdate = "CRUD".equals(mode) ? "Y" : "N";
    String canDelete = "CRUD".equals(mode) ? "Y" : "N";
    String canApprove = "N";
    String canCancel = "N";
    String canDownload = "EXCEL".equals(mode) ? "Y" : "N";
    String canMaskView = model.includePrivacy() ? "Y" : "N";

    return "-- Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.\n"
        + "-- ============================================================\n"
        + "-- 메뉴 등록 SQL ( "
        + model.domainName()
        + " )\n"
        + "-- 폐쇄망 반입 전에는 parentMenuId/menuId/roleCode 값을 수동 확인한다.\n"
        + "-- ============================================================\n\n"
        + "INSERT INTO SMS.TB_MENU (\n"
        + "    MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_URL,\n"
        + "    MENU_LEVEL, SORT_ORD, MENU_TYPE, DISPLAY_YN, USE_YN, SYSTEM_YN, REG_ID\n"
        + ") VALUES (\n"
        + "    '"
        + menuId
        + "', '"
        + model.parentMenuId()
        + "', '"
        + model.domainName()
        + "', '"
        + model.screenUrl()
        + "',\n"
        + "    2, "
        + model.menuSortOrd()
        + ", 'M', 'Y', 'Y', 'N', 'SYSTEM'\n"
        + ");\n\n"
        + "INSERT INTO SMS.TB_MENU_AUTH (\n"
        + "    MENU_ID, ROLE_CD,\n"
        + "    CAN_READ, CAN_CREATE, CAN_UPDATE, CAN_DELETE,\n"
        + "    CAN_APPROVE, CAN_CANCEL, CAN_DOWNLOAD, CAN_MASK_VIEW,\n"
        + "    USE_YN, REG_ID\n"
        + ") VALUES (\n"
        + "    '"
        + menuId
        + "', '"
        + model.roleCode()
        + "',\n"
        + "    '"
        + canRead
        + "', '"
        + canCreate
        + "', '"
        + canUpdate
        + "', '"
        + canDelete
        + "',\n"
        + "    '"
        + canApprove
        + "', '"
        + canCancel
        + "', '"
        + canDownload
        + "', '"
        + canMaskView
        + "',\n"
        + "    'Y', 'SYSTEM'\n"
        + ");\n\n"
        + "COMMIT;\n\n"
        + "-- 파일 배치 경로\n"
        + "-- src/main/java/com/scbk/sms/dto/"
        + module
        + "/"
        + cls
        + "SearchRequestDTO.java\n"
        + "-- src/main/java/com/scbk/sms/vo/"
        + module
        + "/"
        + cls
        + "VO.java\n"
        + "-- src/main/java/com/scbk/sms/mapper/"
        + module
        + "/"
        + cls
        + "Mapper.java\n"
        + "-- src/main/java/com/scbk/sms/service/"
        + module
        + "/"
        + cls
        + "Service.java\n"
        + "-- src/main/java/com/scbk/sms/controller/"
        + module
        + "/"
        + cls
        + "Controller.java\n"
        + "-- src/main/resources/mapper/"
        + module
        + "/"
        + cls
        + "Mapper.xml\n"
        + "-- src/main/resources/templates/"
        + module
        + "/"
        + domainId
        + ".html\n"
        + "-- src/main/resources/static/js/"
        + module
        + "/"
        + domainId
        + ".js\n"
        + "-- src/test/java/com/scbk/sms/service/"
        + module
        + "/"
        + cls
        + "ServiceTest.java\n"
        + "-- src/test/java/com/scbk/sms/controller/"
        + module
        + "/"
        + cls
        + "ControllerTest.java\n\n"
        + "-- 생성 후 docs/base/screen-generation-guide.md의 8~10단계(권한 확인, 검증, 문서 갱신)를 수행한다.\n";
  }
}
