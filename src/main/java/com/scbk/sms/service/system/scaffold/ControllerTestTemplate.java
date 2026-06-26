package com.scbk.sms.service.system.scaffold;

/**
 * Controller 테스트 생성. standalone MockMvc로 ApiResponse 포맷을 검증한다. CRUD 화면(includeCreateUpdate)이면 /data
 * 조회 외에 /create, /update, /delete의 성공 메시지(ApiResponse.message 필드)와 Service 위임까지 함께 검증한다.
 */
public final class ControllerTestTemplate {

  private ControllerTestTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    String module = model.moduleName();
    boolean crud = model.includeCreateUpdate();

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.controller.")
        .append(module)
        .append(";\n\n")
        .append("import static org.mockito.ArgumentMatchers.any;\n")
        .append("import static org.mockito.BDDMockito.given;\n");
    if (crud) {
      sb.append("import static org.mockito.BDDMockito.then;\n");
    }
    sb.append(
        "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n");
    if (crud) {
      sb.append(
          "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;\n");
    }
    sb.append(
            "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;\n")
        .append(
            "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;\n\n")
        .append("import com.scbk.sms.dto.common.PageResponseDTO;\n")
        .append("import com.scbk.sms.dto.")
        .append(module)
        .append(".")
        .append(cls)
        .append("SearchRequestDTO;\n")
        .append("import com.scbk.sms.service.")
        .append(module)
        .append(".")
        .append(cls)
        .append("Service;\n")
        .append("import java.util.List;\n")
        .append("import org.junit.jupiter.api.BeforeEach;\n")
        .append("import org.junit.jupiter.api.Test;\n")
        .append("import org.junit.jupiter.api.extension.ExtendWith;\n")
        .append("import org.mockito.Mock;\n")
        .append("import org.mockito.junit.jupiter.MockitoExtension;\n");
    if (crud) {
      sb.append("import org.springframework.http.MediaType;\n");
    }
    sb.append("import org.springframework.test.web.servlet.MockMvc;\n")
        .append("import org.springframework.test.web.servlet.setup.MockMvcBuilders;\n\n")
        .append("@ExtendWith(MockitoExtension.class)\n")
        .append("class ")
        .append(cls)
        .append("ControllerTest {\n\n")
        .append("    @Mock\n")
        .append("    private ")
        .append(cls)
        .append("Service service;\n\n")
        .append("    private MockMvc mockMvc;\n\n")
        .append("    @BeforeEach\n")
        .append("    void setUp() {\n")
        .append("        mockMvc = MockMvcBuilders.standaloneSetup(new ")
        .append(cls)
        .append("Controller(service)).build();\n")
        .append("    }\n\n")
        .append("    @Test\n")
        .append("    void data는_ApiResponse_포맷으로_응답한다() throws Exception {\n")
        .append("        // given\n")
        .append("        given(service.search(any())).willReturn(\n")
        .append("            PageResponseDTO.of(List.of(), new ")
        .append(cls)
        .append("SearchRequestDTO(), 0));\n\n")
        .append("        // when / then\n")
        .append("        mockMvc.perform(get(\"")
        .append(model.screenUrl())
        .append("/data\"))\n")
        .append("            .andExpect(status().isOk())\n")
        .append("            .andExpect(jsonPath(\"$.code\").value(200))\n")
        .append("            .andExpect(jsonPath(\"$.data.totalCount\").value(0));\n")
        .append("    }\n");

    if (crud) {
      sb.append("\n    @Test\n")
          .append("    void create는_등록_성공_메시지를_반환한다() throws Exception {\n")
          .append("        // when / then\n")
          .append("        mockMvc.perform(post(\"")
          .append(model.screenUrl())
          .append("/create\")\n")
          .append("                .contentType(MediaType.APPLICATION_JSON)\n")
          .append("                .content(\"{}\"))\n")
          .append("            .andExpect(status().isOk())\n")
          .append("            .andExpect(jsonPath(\"$.message\").value(\"등록되었습니다.\"));\n\n")
          .append("        then(service).should().create(any());\n")
          .append("    }\n\n")
          .append("    @Test\n")
          .append("    void update는_수정_성공_메시지를_반환한다() throws Exception {\n")
          .append("        // when / then\n")
          .append("        mockMvc.perform(post(\"")
          .append(model.screenUrl())
          .append("/update\")\n")
          .append("                .contentType(MediaType.APPLICATION_JSON)\n")
          .append("                .content(\"{}\"))\n")
          .append("            .andExpect(status().isOk())\n")
          .append("            .andExpect(jsonPath(\"$.message\").value(\"수정되었습니다.\"));\n\n")
          .append("        then(service).should().update(any());\n")
          .append("    }\n\n")
          .append("    @Test\n")
          .append("    void delete는_삭제_성공_메시지를_반환한다() throws Exception {\n")
          .append("        // when / then\n")
          .append("        mockMvc.perform(post(\"")
          .append(model.screenUrl())
          .append("/delete\")\n");

      java.util.List<String> pkFieldNames = model.pkFieldNames();
      java.util.List<String> pkColumns = model.pkColumns();
      for (int i = 0; i < pkFieldNames.size(); i++) {
        String field = pkFieldNames.get(i);
        String paramValue =
            ServiceTestTemplate.sampleParamValue(model.pkJavaType(pkColumns.get(i)));
        boolean last = i == pkFieldNames.size() - 1;
        sb.append("                .param(\"")
            .append(field)
            .append("\", \"")
            .append(paramValue)
            .append("\")")
            .append(last ? ")\n" : "\n");
      }

      sb.append("            .andExpect(status().isOk())\n")
          .append("            .andExpect(jsonPath(\"$.message\").value(\"삭제되었습니다.\"));\n\n")
          .append("        then(service).should().delete(")
          .append(ServiceTestTemplate.samplePkArgs(model))
          .append(");\n")
          .append("    }\n");
    }

    sb.append("}\n");
    return sb.toString();
  }
}
