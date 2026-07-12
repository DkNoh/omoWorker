package com.scbk.sms.service.system.scaffold;

import java.util.Map;

/** Mapper interface 생성. mapper-interface.java.tpl 리소스를 치환한다. */
public final class MapperInterfaceTemplate {

  private static final String TEMPLATE = "scaffold-templates/mapper-interface.java.tpl";

  private MapperInterfaceTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    return ResourceTemplateRenderer.render(
        TEMPLATE,
        Map.of(
            "MODULE_NAME", model.moduleName(),
            "DOMAIN_CLASS", cls,
            "IMPORTS", imports(model, cls),
            "DETAIL_METHOD", detailMethod(model, cls),
            "CRUD_METHODS", crudMethods(model, cls),
            "EXCEL_METHOD", excelMethod(model, cls)));
  }

  private static String imports(ScaffoldModel model, String cls) {
    String module = model.moduleName();
    StringBuilder sb = new StringBuilder();
    sb.append("import com.scbk.sms.dto.")
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
    sb.append("import com.scbk.sms.vo.")
        .append(module)
        .append(".")
        .append(cls)
        .append("VO;\n")
        .append("import java.util.List;\n");
    if (model.includeExcel()) {
      sb.append("import java.util.Map;\n");
    }
    if (model.includeCreateUpdate()) {
      for (String imp : model.pkParamImports()) {
        sb.append(imp).append("\n");
      }
    }
    sb.append("import org.apache.ibatis.annotations.Mapper;\n");
    if (model.includeCreateUpdate()) {
      sb.append("import org.apache.ibatis.annotations.Param;\n");
    }
    return sb.toString();
  }

  private static String detailMethod(ScaffoldModel model, String cls) {
    if (!model.includePrivacy()) {
      return "";
    }
    return "\n    "
        + cls
        + "VO selectDetail("
        + model.pkJavaType()
        + " "
        + model.pkFieldName()
        + ");\n";
  }

  private static String crudMethods(ScaffoldModel model, String cls) {
    if (!model.includeCreateUpdate()) {
      return "";
    }
    return "\n    int insert("
        + cls
        + "UpdateRequestDTO request);\n\n"
        + "    int update("
        + cls
        + "UpdateRequestDTO request);\n\n"
        + "    int delete("
        + deleteParams(model)
        + ");\n";
  }

  private static String excelMethod(ScaffoldModel model, String cls) {
    if (!model.includeExcel()) {
      return "";
    }
    return "\n    // ExcelUtil 계약상 Map을 사용한다 (동적 컬럼 예외)\n"
        + "    List<Map<String, Object>> selectListForExcel("
        + cls
        + "SearchRequestDTO request);\n";
  }

  private static String deleteParams(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < model.pkColumns().size(); i++) {
      String pkColumn = model.pkColumns().get(i);
      String fieldName = QueryColumnExtractor.toCamelCase(pkColumn);
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("@Param(\"")
          .append(fieldName)
          .append("\") ")
          .append(model.pkJavaType(pkColumn))
          .append(" ")
          .append(fieldName);
    }
    return sb.toString();
  }
}
