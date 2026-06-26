package com.scbk.sms.service.system.scaffold;

/** Controller 생성. two-track 구조, /create와 /update 분리 (/save 금지), 개인정보 포함 시 @PrivacyLog 부착. */
public final class ControllerTemplate {

  private ControllerTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    String module = model.moduleName();

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.controller.").append(module).append(";\n\n");
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
    sb.append("import org.springframework.web.bind.annotation.ResponseBody;\n\n")
        .append("@Controller\n")
        .append("@RequiredArgsConstructor\n")
        .append("@RequestMapping(\"")
        .append(model.screenUrl())
        .append("\")\n")
        .append("public class ")
        .append(cls)
        .append("Controller {\n\n")
        .append("    private final ")
        .append(cls)
        .append("Service service;\n\n")
        .append("    @GetMapping\n")
        .append("    public String page() {\n")
        .append("        return \"")
        .append(module)
        .append("/")
        .append(model.domainId())
        .append("\";\n")
        .append("    }\n\n");

    if (model.includePrivacy()) {
      sb.append("    @PrivacyLog(action = \"").append(model.domainName()).append(" 목록 조회\")\n");
    }
    sb.append("    @ResponseBody\n")
        .append("    @GetMapping(\"/data\")\n")
        .append("    public ResponseEntity<ApiResponse<PageResponseDTO<")
        .append(cls)
        .append("VO>>> getData(\n")
        .append("            @ModelAttribute ")
        .append(cls)
        .append("SearchRequestDTO request) {\n")
        .append("        return ResponseEntity.ok(ApiResponse.success(service.search(request)));\n")
        .append("    }\n");

    if (model.includeCreateUpdate()) {
      sb.append("\n    @ResponseBody\n")
          .append("    @PostMapping(\"/create\")\n")
          .append("    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody ")
          .append(cls)
          .append("UpdateRequestDTO request) {\n")
          .append("        service.create(request);\n")
          .append("        return ResponseEntity.ok(ApiResponse.success(\"등록되었습니다.\", null));\n")
          .append("    }\n\n")
          .append("    @ResponseBody\n")
          .append("    @PostMapping(\"/update\")\n")
          .append("    public ResponseEntity<ApiResponse<String>> update(@Valid @RequestBody ")
          .append(cls)
          .append("UpdateRequestDTO request) {\n")
          .append("        service.update(request);\n")
          .append("        return ResponseEntity.ok(ApiResponse.success(\"수정되었습니다.\", null));\n")
          .append("    }\n\n")
          .append("    @ResponseBody\n")
          .append("    @PostMapping(\"/delete\")\n")
          .append("    public ResponseEntity<ApiResponse<String>> delete(")
          .append(deleteRequestParams(model))
          .append(") {\n")
          .append("        service.delete(")
          .append(String.join(", ", model.pkFieldNames()))
          .append(");\n")
          .append("        return ResponseEntity.ok(ApiResponse.success(\"삭제되었습니다.\", null));\n")
          .append("    }\n");
    }

    if (model.includeExcel()) {
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
    }

    sb.append("}\n");
    return sb.toString();
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
