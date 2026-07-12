package com.scbk.sms.service.system.scaffold;

import java.util.HashMap;
import java.util.Map;

/**
 * 메뉴/권한 등록 SQL 생성. v3 스키마(TB_MENU MENU_ID, TB_MENU_AUTH ROLE_CD + CAN_* 8종) 기준. menu-sql.sql.tpl
 * 리소스를 치환한다.
 */
public final class MenuSqlTemplate {

  private static final String TEMPLATE = "scaffold-templates/menu-sql.sql.tpl";

  private MenuSqlTemplate() {}

  public static String generate(ScaffoldModel model) {
    String mode = model.screenMode();
    boolean crudMode = "CRUD".equals(mode) || "CRUD_PANEL".equals(mode);
    String canCreate = crudMode ? "Y" : "N";
    String canUpdate = crudMode ? "Y" : "N";
    String canDelete = crudMode ? "Y" : "N";
    String canDownload = "EXCEL".equals(mode) ? "Y" : "N";
    String canMaskView = model.includePrivacy() ? "Y" : "N";

    Map<String, String> tokens = new HashMap<>();
    tokens.put("DOMAIN_NAME", model.domainName());
    tokens.put("MENU_ID", model.menuId());
    tokens.put("PARENT_MENU_ID", model.parentMenuId());
    tokens.put("SCREEN_URL", model.screenUrl());
    tokens.put("MENU_SORT_ORD", Integer.toString(model.menuSortOrd()));
    tokens.put("ROLE_CODE", model.roleCode());
    tokens.put("CAN_CREATE", canCreate);
    tokens.put("CAN_UPDATE", canUpdate);
    tokens.put("CAN_DELETE", canDelete);
    tokens.put("CAN_DOWNLOAD", canDownload);
    tokens.put("CAN_MASK_VIEW", canMaskView);
    tokens.put("MODULE_NAME", model.moduleName());
    tokens.put("DOMAIN_CLASS", model.domainClass());
    tokens.put("DOMAIN_ID", model.domainId());
    return ResourceTemplateRenderer.render(TEMPLATE, tokens);
  }
}
