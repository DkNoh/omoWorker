package com.scbk.sms.service.system.scaffold;

/** 수정 요청 화이트리스트 DTO 생성. VO를 update 요청 객체로 직접 사용하지 않는다 (mass assignment 방지). */
public final class UpdateRequestDtoTemplate {

  private UpdateRequestDtoTemplate() {}

  public static String generate(ScaffoldModel model) {
    boolean hasLocalDate = model.getTypeMap().containsValue("LocalDate");
    boolean hasLocalDateTime = model.getTypeMap().containsValue("LocalDateTime");
    boolean hasBigDecimal = model.getTypeMap().containsValue("BigDecimal");
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

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.dto.").append(model.moduleName()).append(";\n\n");
    if (hasBigDecimal) {
      sb.append("import java.math.BigDecimal;\n");
    }
    if (hasLocalDate) {
      sb.append("import java.time.LocalDate;\n");
    }
    if (hasLocalDateTime) {
      sb.append("import java.time.LocalDateTime;\n");
    }
    sb.append("import lombok.Data;\n");
    if (hasRequiredNotBlank) {
      sb.append("import jakarta.validation.constraints.NotBlank;\n");
    }
    if (hasRequiredNotNull) {
      sb.append("import jakarta.validation.constraints.NotNull;\n");
    }
    sb.append("\n")
        .append("/**\n")
        .append(" * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.\n")
        .append(" * 수정 가능한 필드만 선언하는 화이트리스트 DTO.\n")
        .append(" * TODO: 실제 수정을 허용할 필드만 남기고 제거한다.\n")
        .append(" *       REG_ID/REG_DTTM, 시스템 필드, 권한 필드는 선언하지 않는다.\n")
        .append(" */\n")
        .append("@Data\n")
        .append("public class ")
        .append(model.domainClass())
        .append("UpdateRequestDTO {\n\n");

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

    if (!model.lockColumn().isEmpty()) {
      sb.append("\n    /** 낙관적 잠금용. 조회 시점의 ")
          .append(model.lockColumn())
          .append(" (hidden으로 받는다) */\n")
          .append("    private ")
          .append(model.lockJavaType())
          .append(" ")
          .append(model.beforeLockFieldName())
          .append(";\n");
    }
    sb.append("}\n");
    return sb.toString();
  }
}
