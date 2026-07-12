package com.scbk.sms.service.system.scaffold;

import java.util.Map;

/** VO 생성. 타입 추론 결과 반영, Lombok 기반. vo.java.tpl 리소스를 치환한다. */
public final class VoTemplate {

  private static final String TEMPLATE = "scaffold-templates/vo.java.tpl";

  private VoTemplate() {}

  public static String generate(ScaffoldModel model) {
    return ResourceTemplateRenderer.render(
        TEMPLATE,
        Map.of(
            "MODULE_NAME", model.moduleName(),
            "DOMAIN_CLASS", model.domainClass(),
            "IMPORTS", typeImports(model),
            "PRIVACY_COMMENT", privacyComment(model),
            "FIELDS", fields(model)));
  }

  private static String typeImports(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    if (model.getTypeMap().containsValue("BigDecimal")) {
      sb.append("import java.math.BigDecimal;\n");
    }
    if (model.getTypeMap().containsValue("LocalDate")) {
      sb.append("import java.time.LocalDate;\n");
    }
    if (model.getTypeMap().containsValue("LocalDateTime")) {
      sb.append("import java.time.LocalDateTime;\n");
    }
    return sb.toString();
  }

  private static String privacyComment(ScaffoldModel model) {
    return model.includePrivacy() ? "// 개인정보 컬럼은 Service에서 MaskingUtil로 마스킹한 값을 담는다.\n" : "";
  }

  private static String fields(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (String column : model.getColumns()) {
      if (column.trim().isEmpty()) {
        continue;
      }
      String javaType = model.getTypeMap().getOrDefault(column, "String");
      sb.append("    private ")
          .append(javaType)
          .append(" ")
          .append(QueryColumnExtractor.toCamelCase(column.trim()))
          .append(";\n");
    }
    return sb.toString();
  }
}
