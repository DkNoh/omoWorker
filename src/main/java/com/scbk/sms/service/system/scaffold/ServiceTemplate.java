package com.scbk.sms.service.system.scaffold;

/** Service 생성. PageResponseDTO.of 계약 적용, plain Java. */
public final class ServiceTemplate {

  private ServiceTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    String module = model.moduleName();

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.service.")
        .append(module)
        .append(";\n\n")
        .append("import com.scbk.sms.dto.common.PageResponseDTO;\n")
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
          .append("UpdateRequestDTO;\n")
          .append("import com.scbk.sms.exception.CustomException;\n")
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
        .append("import org.springframework.transaction.annotation.Transactional;\n\n")
        .append("@Service\n")
        .append("@RequiredArgsConstructor\n")
        .append("public class ")
        .append(cls)
        .append("Service {\n\n")
        .append("    private final ")
        .append(cls)
        .append("Mapper mapper;\n\n")
        .append("    @Transactional(readOnly = true)\n")
        .append("    public PageResponseDTO<")
        .append(cls)
        .append("VO> search(")
        .append(cls)
        .append("SearchRequestDTO request) {\n")
        .append("        request.validate();\n")
        .append("        int totalCount = mapper.count(request);\n")
        .append("        List<")
        .append(cls)
        .append("VO> list = mapper.selectList(request);\n");
    if (model.includePrivacy()) {
      sb.append("        // TODO: 개인정보 컬럼을 MaskingUtil로 마스킹한다. (audit-masking-policy.md)\n");
    }
    sb.append("        return PageResponseDTO.of(list, request, totalCount);\n").append("    }\n");

    if (model.includeCreateUpdate()) {
      sb.append("\n    @Transactional\n")
          .append("    public void create(")
          .append(cls)
          .append("UpdateRequestDTO request) {\n")
          .append("        mapper.insert(request);\n")
          .append("    }\n\n")
          .append("    @Transactional\n")
          .append("    public void update(")
          .append(cls)
          .append("UpdateRequestDTO request) {\n")
          .append("        int updated = mapper.update(request);\n")
          .append("        if (updated == 0) {\n")
          .append("            // 다른 사용자가 먼저 수정했거나(낙관적 잠금) 대상이 없다\n")
          .append("            throw new CustomException(ErrorCode.UPDATE_CONFLICT);\n")
          .append("        }\n")
          .append("    }\n\n")
          .append("    @Transactional\n")
          .append("    public void delete(")
          .append(deleteMethodParams(model))
          .append(") {\n")
          .append("        int deleted = mapper.delete(")
          .append(deleteCallArgs(model))
          .append(");\n")
          .append("        if (deleted == 0) {\n")
          .append("            // 다른 사용자가 먼저 삭제했거나 대상이 없다\n")
          .append("            throw new CustomException(ErrorCode.DELETE_CONFLICT);\n")
          .append("        }\n")
          .append("    }\n");
    }

    if (model.includeExcel()) {
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
    }

    sb.append("}\n");
    return sb.toString();
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
          .append("                // TODO: ")
          .append(column.maskType())
          .append(" 마스킹 정책에 맞게 MaskingUtil 적용\n")
          .append("                row.put(\"")
          .append(column.columnName())
          .append("\", value.toString());\n")
          .append("            }\n")
          .append("        }\n");
    }
    return sb.toString();
  }
}
