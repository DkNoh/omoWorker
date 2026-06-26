package com.scbk.sms.service.system.scaffold;

/** Mapper interface 생성. */
public final class MapperInterfaceTemplate {

  private MapperInterfaceTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.mapper.")
        .append(model.moduleName())
        .append(";\n\n")
        .append("import com.scbk.sms.dto.")
        .append(model.moduleName())
        .append(".")
        .append(cls)
        .append("SearchRequestDTO;\n");
    if (model.includeCreateUpdate()) {
      sb.append("import com.scbk.sms.dto.")
          .append(model.moduleName())
          .append(".")
          .append(cls)
          .append("UpdateRequestDTO;\n");
    }
    sb.append("import com.scbk.sms.vo.")
        .append(model.moduleName())
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
    sb.append("\n")
        .append("@Mapper\n")
        .append("public interface ")
        .append(cls)
        .append("Mapper {\n\n")
        .append("    int count(")
        .append(cls)
        .append("SearchRequestDTO request);\n\n")
        .append("    List<")
        .append(cls)
        .append("VO> selectList(")
        .append(cls)
        .append("SearchRequestDTO request);\n");

    if (model.includeCreateUpdate()) {
      sb.append("\n    int insert(")
          .append(cls)
          .append("UpdateRequestDTO request);\n\n")
          .append("    int update(")
          .append(cls)
          .append("UpdateRequestDTO request);\n\n")
          .append("    int delete(")
          .append(deleteParams(model))
          .append(");\n");
    }
    if (model.includeExcel()) {
      sb.append("\n    // ExcelUtil 계약상 Map을 사용한다 (동적 컬럼 예외)\n")
          .append("    List<Map<String, Object>> selectListForExcel(")
          .append(cls)
          .append("SearchRequestDTO request);\n");
    }
    sb.append("}\n");
    return sb.toString();
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
