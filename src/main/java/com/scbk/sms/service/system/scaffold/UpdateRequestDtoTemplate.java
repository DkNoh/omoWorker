package com.scbk.sms.service.system.scaffold;

import java.util.Map;

/**
 * 수정 요청 화이트리스트 DTO 생성. VO를 update 요청 객체로 직접 사용하지 않는다 (mass assignment 방지).
 * update-request-dto.java.tpl 리소스를 치환한다.
 */
public final class UpdateRequestDtoTemplate {

  private static final String TEMPLATE = "scaffold-templates/update-request-dto.java.tpl";

  private UpdateRequestDtoTemplate() {}

  public static String generate(ScaffoldModel model) {
    boolean hasRequiredNotBlank = false;
    boolean hasRequiredNotNull = false;
    for (ScaffoldModel.ColumnConfig column : model.columnConfigs()) {
      if (column.editable()
          && column.hasValidate()
          && column.validate().toLowerCase().contains("required")) {
        if ("String".equals(column.javaType())) {
          hasRequiredNotBlank = true;
        } else {
          hasRequiredNotNull = true;
        }
      }
    }
    return ResourceTemplateRenderer.render(
        TEMPLATE,
        Map.of(
            "MODULE_NAME", model.moduleName(),
            "DOMAIN_CLASS", model.domainClass(),
            "IMPORTS", imports(model, hasRequiredNotBlank, hasRequiredNotNull),
            "PK_SECTION", pkSection(model),
            "EDITABLE_FIELDS", editableFields(model),
            "LOCK_FIELD", lockField(model)));
  }

  private static String imports(
      ScaffoldModel model, boolean hasRequiredNotBlank, boolean hasRequiredNotNull) {
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
    sb.append("import lombok.Data;\n");
    if (hasRequiredNotBlank) {
      sb.append("import jakarta.validation.constraints.NotBlank;\n");
    }
    if (hasRequiredNotNull) {
      sb.append("import jakarta.validation.constraints.NotNull;\n");
    }
    return sb.toString();
  }

  private static String pkSection(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    if (model.pkColumns().isEmpty()) {
      sb.append("    // TODO: PK 필드 (WHERE 조건). 실제 PK 컬럼명으로 교체한다\n")
          .append("    private String id;\n\n");
    } else {
      sb.append("    /** PK 필드 (WHERE 조건): ")
          .append(String.join(", ", model.pkColumns()))
          .append(" */\n");
      for (String pkColumn : model.pkColumns()) {
        sb.append("    private ")
            .append(model.pkJavaType(pkColumn))
            .append(" ")
            .append(QueryColumnExtractor.toCamelCase(pkColumn))
            .append(";\n");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private static String editableFields(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (ScaffoldModel.ColumnConfig column : model.columnConfigs()) {
      if (!column.editable()) {
        continue;
      }
      boolean required =
          column.hasValidate() && column.validate().toLowerCase().contains("required");
      if (required) {
        sb.append("String".equals(column.javaType()) ? "    @NotBlank\n" : "    @NotNull\n");
      }
      sb.append("    private ")
          .append(column.javaType())
          .append(" ")
          .append(column.fieldName())
          .append(";\n");
    }
    return sb.toString();
  }

  private static String lockField(ScaffoldModel model) {
    if (model.lockColumn().isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("\n    /** 낙관적 잠금용. 조회 시점의 ")
        .append(model.lockColumn())
        .append(" (hidden으로 받는다) */\n")
        .append("    private ")
        .append(model.lockJavaType())
        .append(" ")
        .append(model.beforeLockFieldName())
        .append(";\n");
    return sb.toString();
  }
}
