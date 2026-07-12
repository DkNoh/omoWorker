package com.scbk.sms.service.system.scaffold;

import java.util.Map;

/**
 * Controller 생성. two-track 구조, /create와 /update 분리 (/save 금지), 개인정보 포함 시 @PrivacyLog 부착.
 * controller.java.tpl 리소스를 치환한다.
 */
public final class ControllerTemplate {

  private static final String TEMPLATE = "scaffold-templates/controller.java.tpl";

  private ControllerTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    return ResourceTemplateRenderer.render(
        TEMPLATE,
        Map.of(
            "MODULE_NAME", model.moduleName(),
            "DOMAIN_CLASS", cls,
            "DOMAIN_ID", model.domainId(),
            "DOMAIN_NAME", model.domainName(),
            "SCREEN_URL", model.screenUrl(),
            "IMPORTS", imports(model, cls),
            "DATA_PRIVACY_LOG", dataPrivacyLog(model),
            "CRUD_SECTION", crudSection(model, cls),
            "EXCEL_SECTION", excelSection(model, cls),
            "PRIVACY_SECTION", privacySection(model, cls)));
  }

  private static String imports(ScaffoldModel model, String cls) {
    String module = model.moduleName();
    StringBuilder sb = new StringBuilder();
    if (model.includePrivacy()) {
      sb.append("import com.scbk.sms.annotation.PrivacyLog;\n");
    }
    sb.append("import com.scbk.sms.dto.common.ApiResponse;\n")
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
          .append("UpdateRequestDTO;\n");
    }
    sb.append("import com.scbk.sms.service.")
        .append(module)
        .append(".")
        .append(cls)
        .append("Service;\n")
        .append("import com.scbk.sms.vo.")
        .append(module)
        .append(".")
        .append(cls)
        .append("VO;\n");
    if (model.includeExcel()) {
      sb.append("import jakarta.servlet.http.HttpServletResponse;\n");
    }
    if (model.includeCreateUpdate()) {
      sb.append("import jakarta.validation.Valid;\n");
      for (String imp : model.pkParamImports()) {
        sb.append(imp).append("\n");
      }
    }
    sb.append("import lombok.RequiredArgsConstructor;\n")
        .append("import org.springframework.http.ResponseEntity;\n")
        .append("import org.springframework.stereotype.Controller;\n")
        .append("import org.springframework.web.bind.annotation.GetMapping;\n")
        .append("import org.springframework.web.bind.annotation.ModelAttribute;\n");
    if (model.includeCreateUpdate()) {
      sb.append("import org.springframework.web.bind.annotation.PostMapping;\n")
          .append("import org.springframework.web.bind.annotation.RequestBody;\n")
          .append("import org.springframework.web.bind.annotation.RequestMapping;\n")
          .append("import org.springframework.web.bind.annotation.RequestParam;\n");
    } else {
      sb.append("import org.springframework.web.bind.annotation.RequestMapping;\n");
    }
    sb.append("import org.springframework.web.bind.annotation.ResponseBody;\n");
    return sb.toString();
  }

  private static String dataPrivacyLog(ScaffoldModel model) {
    if (!model.includePrivacy()) {
      return "";
    }
    return "    @PrivacyLog(action = \"" + model.domainName() + " 목록 조회\")\n";
  }

  private static String crudSection(ScaffoldModel model, String cls) {
    if (!model.includeCreateUpdate()) {
      return "";
    }
    return "\n    @ResponseBody\n"
        + "    @PostMapping(\"/create\")\n"
        + "    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody "
        + cls
        + "UpdateRequestDTO request) {\n"
        + "        service.create(request);\n"
        + "        return ResponseEntity.ok(ApiResponse.success(\"등록되었습니다.\", null));\n"
        + "    }\n\n"
        + "    @ResponseBody\n"
        + "    @PostMapping(\"/update\")\n"
        + "    public ResponseEntity<ApiResponse<String>> update(@Valid @RequestBody "
        + cls
        + "UpdateRequestDTO request) {\n"
        + "        service.update(request);\n"
        + "        return ResponseEntity.ok(ApiResponse.success(\"수정되었습니다.\", null));\n"
        + "    }\n\n"
        + "    @ResponseBody\n"
        + "    @PostMapping(\"/delete\")\n"
        + "    public ResponseEntity<ApiResponse<String>> delete("
        + deleteRequestParams(model)
        + ") {\n"
        + "        service.delete("
        + String.join(", ", model.pkFieldNames())
        + ");\n"
        + "        return ResponseEntity.ok(ApiResponse.success(\"삭제되었습니다.\", null));\n"
        + "    }\n";
  }

  private static String excelSection(ScaffoldModel model, String cls) {
    if (!model.includeExcel()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    if (model.includePrivacy()) {
      sb.append("    @PrivacyLog(action = \"").append(model.domainName()).append(" 엑셀 다운로드\")\n");
    }
    sb.append("    @GetMapping(\"/excel\")\n")
        .append("    public void downloadExcel(@ModelAttribute ")
        .append(cls)
        .append("SearchRequestDTO request,\n")
        .append("                              HttpServletResponse response) {\n")
        .append("        service.downloadExcel(request, response);\n")
        .append("    }\n");
    return sb.toString();
  }

  private static String privacySection(ScaffoldModel model, String cls) {
    if (!model.includePrivacy()) {
      return "";
    }
    return "\n    @PrivacyLog(action = \""
        + model.domainName()
        + " 원문 상세 조회\")\n"
        + "    @ResponseBody\n"
        + "    @GetMapping(\"/unmask\")\n"
        + "    public ResponseEntity<ApiResponse<"
        + cls
        + "VO>> getUnmaskedDetail(@RequestParam "
        + model.pkJavaType()
        + " "
        + model.pkFieldName()
        + ") {\n"
        + "        return ResponseEntity.ok(ApiResponse.success(service.getUnmaskedDetail("
        + model.pkFieldName()
        + ")));\n"
        + "    }\n";
  }

  private static String deleteRequestParams(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < model.pkColumns().size(); i++) {
      String pkColumn = model.pkColumns().get(i);
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("@RequestParam ")
          .append(model.pkJavaType(pkColumn))
          .append(" ")
          .append(QueryColumnExtractor.toCamelCase(pkColumn));
    }
    return sb.toString();
  }
}
