package com.scbk.sms.service.system.scaffold;

import java.util.List;
import java.util.Map;

/** Service 생성. PageResponseDTO.of 계약 적용, plain Java. service.java.tpl 리소스를 치환한다. */
public final class ServiceTemplate {

  private static final String TEMPLATE = "scaffold-templates/service.java.tpl";

  private ServiceTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    return ResourceTemplateRenderer.render(
        TEMPLATE,
        Map.of(
            "MODULE_NAME", model.moduleName(),
            "DOMAIN_CLASS", cls,
            "IMPORTS", imports(model, cls),
            "MASK_LIST_COLUMNS", maskListColumns(model),
            "CRUD_SECTION", crudSection(model, cls),
            "EXCEL_SECTION", excelSection(model, cls),
            "PRIVACY_SECTION", privacySection(model, cls)));
  }

  private static String imports(ScaffoldModel model, String cls) {
    String module = model.moduleName();
    StringBuilder sb = new StringBuilder();
    sb.append("import com.scbk.sms.dto.common.PageResponseDTO;\n")
        .append("import com.scbk.sms.dto.")
        .append(module)
        .append(".")
        .append(cls)
        .append("SearchRequestDTO;\n");
    if (model.includeCreateUpdate()) {
      sb.append("import com.scbk.sms.dto.")
          .append(module)
          .append(".")
          .append(cls)
          .append("UpdateRequestDTO;\n");
    }
    if (model.includeCreateUpdate() || model.includePrivacy()) {
      sb.append("import com.scbk.sms.exception.CustomException;\n")
          .append("import com.scbk.sms.exception.ErrorCode;\n");
    }
    sb.append("import com.scbk.sms.mapper.")
        .append(module)
        .append(".")
        .append(cls)
        .append("Mapper;\n")
        .append("import com.scbk.sms.vo.")
        .append(module)
        .append(".")
        .append(cls)
        .append("VO;\n");
    if (model.includeExcel()) {
      sb.append("import com.scbk.sms.util.ExcelUtil;\n")
          .append("import jakarta.servlet.http.HttpServletResponse;\n");
    }
    if (model.includePrivacy()) {
      sb.append("import com.scbk.sms.util.MaskingUtil;\n");
    }
    sb.append("import java.util.List;\n");
    if (model.includeExcel()) {
      sb.append("import java.util.Map;\n");
    }
    if (model.includeCreateUpdate()) {
      for (String imp : model.pkParamImports()) {
        sb.append(imp).append("\n");
      }
    }
    sb.append("import lombok.RequiredArgsConstructor;\n")
        .append("import org.springframework.stereotype.Service;\n")
        .append("import org.springframework.transaction.annotation.Transactional;\n");
    return sb.toString();
  }

  private static String crudSection(ScaffoldModel model, String cls) {
    if (!model.includeCreateUpdate()) {
      return "";
    }
    return "\n    @Transactional\n"
        + "    public void create("
        + cls
        + "UpdateRequestDTO request) {\n"
        + "        // TODO: 등록 전 업무 규칙 검증(중복 체크, 필수값 보정 등)을 여기에 추가한다.\n"
        + "        mapper.insert(request);\n"
        + "    }\n\n"
        + "    @Transactional\n"
        + "    public void update("
        + cls
        + "UpdateRequestDTO request) {\n"
        + "        // TODO: 수정 전 업무 규칙 검증(상태 전이, 권한 확인 등)을 여기에 추가한다.\n"
        + "        int updated = mapper.update(request);\n"
        + "        if (updated == 0) {\n"
        + "            // 다른 사용자가 먼저 수정했거나(낙관적 잠금) 대상이 없다\n"
        + "            throw new CustomException(ErrorCode.UPDATE_CONFLICT);\n"
        + "        }\n"
        + "    }\n\n"
        + "    @Transactional\n"
        + "    public void delete("
        + deleteMethodParams(model)
        + ") {\n"
        + "        int deleted = mapper.delete("
        + deleteCallArgs(model)
        + ");\n"
        + "        if (deleted == 0) {\n"
        + "            // 다른 사용자가 먼저 삭제했거나 대상이 없다\n"
        + "            throw new CustomException(ErrorCode.DELETE_CONFLICT);\n"
        + "        }\n"
        + "    }\n";
  }

  private static String excelSection(ScaffoldModel model, String cls) {
    if (!model.includeExcel()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("\n    @Transactional(readOnly = true)\n")
        .append("    public void downloadExcel(")
        .append(cls)
        .append("SearchRequestDTO request, HttpServletResponse response) {\n")
        .append("        String[] headers = {")
        .append(joinQuoted(model, false))
        .append("};\n")
        .append("        String[] keys = {")
        .append(joinQuoted(model, true))
        .append("};\n");
    sb.append("        List<Map<String, Object>> list = mapper.selectListForExcel(request);\n")
        .append(maskExcelRows(model))
        .append("        ExcelUtil.downloadExcel(response, \"")
        .append(cls)
        .append("_export\", headers, list, keys);\n")
        .append("    }\n");
    return sb.toString();
  }

  private static String privacySection(ScaffoldModel model, String cls) {
    if (!model.includePrivacy()) {
      return "";
    }
    return "\n    @Transactional(readOnly = true)\n"
        + "    public "
        + cls
        + "VO getUnmaskedDetail("
        + model.pkJavaType()
        + " "
        + model.pkFieldName()
        + ") {\n"
        + "        "
        + cls
        + "VO vo = mapper.selectDetail("
        + model.pkFieldName()
        + ");\n"
        + "        if (vo == null) {\n"
        + "            throw new CustomException(ErrorCode.DATA_NOT_FOUND);\n"
        + "        }\n"
        + "        return vo;\n"
        + "    }\n";
  }

  private static String joinQuoted(ScaffoldModel model, boolean upperCase) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < model.getColumns().size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      String value = model.getColumns().get(i).trim();
      sb.append("\"").append(upperCase ? value.toUpperCase() : value).append("\"");
    }
    return sb.toString();
  }

  private static String deleteMethodParams(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < model.pkColumns().size(); i++) {
      String pkColumn = model.pkColumns().get(i);
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(model.pkJavaType(pkColumn))
          .append(" ")
          .append(QueryColumnExtractor.toCamelCase(pkColumn));
    }
    return sb.toString();
  }

  private static String deleteCallArgs(ScaffoldModel model) {
    return String.join(", ", model.pkFieldNames());
  }

  private static String maskExcelRows(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (ScaffoldModel.ColumnConfig column : model.columnConfigs()) {
      if (!column.hasMask()) {
        continue;
      }
      sb.append("        for (Map<String, Object> row : list) {\n")
          .append("            Object value = row.get(\"")
          .append(column.columnName())
          .append("\");\n")
          .append("            if (value != null) {\n")
          .append("                row.put(\"")
          .append(column.columnName())
          .append("\", MaskingUtil.")
          .append(maskingMethod(column.maskType()))
          .append("(value.toString()));\n")
          .append("            }\n")
          .append("        }\n");
    }
    return sb.toString();
  }

  private static String maskListColumns(ScaffoldModel model) {
    List<ScaffoldModel.ColumnConfig> masked =
        model.columnConfigs().stream().filter(ScaffoldModel.ColumnConfig::hasMask).toList();
    if (masked.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("        list.forEach(vo -> {\n");
    for (ScaffoldModel.ColumnConfig column : masked) {
      String cap = capitalize(column.fieldName());
      sb.append("            vo.set")
          .append(cap)
          .append("(MaskingUtil.")
          .append(maskingMethod(column.maskType()))
          .append("(vo.get")
          .append(cap)
          .append("()));\n");
    }
    sb.append("        });\n");
    return sb.toString();
  }

  private static String maskingMethod(String maskType) {
    if (maskType == null) {
      return "maskPhone";
    }
    return switch (maskType.trim().toLowerCase()) {
      case "name", "nm" -> "maskName";
      case "rrn", "ssn" -> "maskRrn";
      case "card", "bizno" -> "maskCard";
      default -> "maskPhone";
    };
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
