package com.scbk.sms.service.system.scaffold;

import java.util.Map;

public final class ScaffoldPageRenderers {

  private static final Map<String, ScaffoldPageRenderer> RENDERERS =
      Map.of(
          "LIST", new ListPageRenderer(),
          "EXCEL", new ExcelPageRenderer(),
          "DETAIL", new DetailPageRenderer(),
          "CRUD", new CrudPageRenderer(),
          "CRUD_PANEL", new CrudPanelPageRenderer());

  private ScaffoldPageRenderers() {}

  public static Map<String, String> render(ScaffoldModel model) {
    ScaffoldPageRenderer renderer = RENDERERS.get(model.screenMode());
    if (renderer == null) {
      throw new IllegalArgumentException(
          "지원하지 않는 screenMode입니다: "
              + model.screenMode()
              + " (지원: LIST, EXCEL, DETAIL, CRUD, CRUD_PANEL)");
    }
    return renderer.render(model);
  }
}
