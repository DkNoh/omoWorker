package com.scbk.sms.service.system.scaffold;

/** VO 생성. 타입 추론 결과 반영, Lombok 기반. */
public final class VoTemplate {

  private VoTemplate() {}

  public static String generate(ScaffoldModel model) {
    boolean hasLocalDate = model.getTypeMap().containsValue("LocalDate");
    boolean hasLocalDateTime = model.getTypeMap().containsValue("LocalDateTime");
    boolean hasBigDecimal = model.getTypeMap().containsValue("BigDecimal");

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.vo.").append(model.moduleName()).append(";\n\n");
    if (hasBigDecimal) {
      sb.append("import java.math.BigDecimal;\n");
    }
    if (hasLocalDate) {
      sb.append("import java.time.LocalDate;\n");
    }
    if (hasLocalDateTime) {
      sb.append("import java.time.LocalDateTime;\n");
    }
    sb.append("import lombok.Data;\n\n");
    if (model.includePrivacy()) {
      sb.append("// 개인정보 컬럼은 Service에서 MaskingUtil로 마스킹한 값을 담는다.\n");
    }
    sb.append("/** Scaffold 생성(v1) — scaffold 소유. 골격은 재생성 시 덮어쓴다. */\n")
        .append("@Data\n")
        .append("public class ")
        .append(model.domainClass())
        .append("VO {\n\n");

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
    sb.append("}\n");
    return sb.toString();
  }
}
