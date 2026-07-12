package com.scbk.sms.service.system.scaffold;

import java.util.List;
import java.util.Map;

/** SearchRequestDTO 생성. PageRequestDTO 상속, Lombok 기반. dto.java.tpl 리소스를 치환한다. */
public final class DtoTemplate {

  private static final String TEMPLATE = "scaffold-templates/dto.java.tpl";

  private DtoTemplate() {}

  public static String generate(ScaffoldModel model) {
    return ResourceTemplateRenderer.render(
        TEMPLATE,
        Map.of(
            "MODULE_NAME", model.moduleName(),
            "DOMAIN_CLASS", model.domainClass(),
            "FIELDS", fields(model.searchParams())));
  }

  private static String fields(List<ScaffoldModel.SearchParam> params) {
    StringBuilder sb = new StringBuilder();
    for (ScaffoldModel.SearchParam param : params) {
      sb.append("    private String ").append(param.name()).append(";\n");
    }
    return sb.toString();
  }
}
