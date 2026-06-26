package com.scbk.sms.service.system.scaffold;

/** Service 단위 테스트 생성. Mapper는 Mockito mock, given/when/then 구조. */
public final class ServiceTestTemplate {

  private ServiceTestTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    String module = model.moduleName();

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.service.")
        .append(module)
        .append(";\n\n")
        .append("import static org.assertj.core.api.Assertions.assertThat;\n");
    if (model.includeCreateUpdate()) {
      sb.append("import static org.assertj.core.api.Assertions.assertThatThrownBy;\n")
          .append("import static org.mockito.ArgumentMatchers.any;\n");
    }
    sb.append("import static org.mockito.BDDMockito.given;\n");
    if (model.includeCreateUpdate()) {
      sb.append("import static org.mockito.BDDMockito.then;\n");
    }
    sb.append("\n")
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
          .append("import com.scbk.sms.exception.CustomException;\n");
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
        .append("VO;\n")
        .append("import java.util.List;\n")
        .append("import org.junit.jupiter.api.BeforeEach;\n")
        .append("import org.junit.jupiter.api.Test;\n")
        .append("import org.junit.jupiter.api.extension.ExtendWith;\n")
        .append("import org.mockito.Mock;\n")
        .append("import org.mockito.junit.jupiter.MockitoExtension;\n\n")
        .append("@ExtendWith(MockitoExtension.class)\n")
        .append("class ")
        .append(cls)
        .append("ServiceTest {\n\n")
        .append("    @Mock\n")
        .append("    private ")
        .append(cls)
        .append("Mapper mapper;\n\n")
        .append("    private ")
        .append(cls)
        .append("Service service;\n\n")
        .append("    @BeforeEach\n")
        .append("    void setUp() {\n")
        .append("        service = new ")
        .append(cls)
        .append("Service(mapper);\n")
        .append("    }\n\n")
        .append("    @Test\n")
        .append("    void 목록_조회는_페이지_응답으로_감싼다() {\n")
        .append("        // given\n")
        .append("        ")
        .append(cls)
        .append("SearchRequestDTO request = new ")
        .append(cls)
        .append("SearchRequestDTO();\n")
        .append("        request.setPage(1);\n")
        .append("        request.setSize(10);\n")
        .append("        given(mapper.count(request)).willReturn(1);\n")
        .append("        given(mapper.selectList(request)).willReturn(List.of(new ")
        .append(cls)
        .append("VO()));\n\n")
        .append("        // when\n")
        .append("        PageResponseDTO<")
        .append(cls)
        .append("VO> result = service.search(request);\n\n")
        .append("        // then\n")
        .append("        assertThat(result.getTotalCount()).isEqualTo(1);\n")
        .append("        assertThat(result.getContents()).hasSize(1);\n")
        .append("    }\n");

    if (model.includeCreateUpdate()) {
      sb.append("\n    @Test\n")
          .append("    void 수정_결과가_0건이면_충돌로_실패한다() {\n")
          .append("        // given : 낙관적 잠금 — 다른 사용자가 먼저 수정했거나 대상이 없는 상황\n")
          .append("        given(mapper.update(any())).willReturn(0);\n\n")
          .append("        // when / then\n")
          .append("        assertThatThrownBy(() -> service.update(new ")
          .append(cls)
          .append("UpdateRequestDTO()))\n")
          .append("            .isInstanceOf(CustomException.class);\n")
          .append("    }\n\n")
          .append("    @Test\n")
          .append("    void 삭제는_Mapper에_위임한다() {\n")
          .append("        // given\n")
          .append("        given(mapper.delete(")
          .append(samplePkArgs(model))
          .append(")).willReturn(1);\n\n")
          .append("        // when\n")
          .append("        service.delete(")
          .append(samplePkArgs(model))
          .append(");\n\n")
          .append("        // then\n")
          .append("        then(mapper).should().delete(")
          .append(samplePkArgs(model))
          .append(");\n")
          .append("    }\n\n")
          .append("    @Test\n")
          .append("    void 삭제_결과가_0건이면_충돌로_실패한다() {\n")
          .append("        // given : 다른 사용자가 먼저 삭제했거나 대상이 없는 상황\n")
          .append("        given(mapper.delete(")
          .append(samplePkArgs(model))
          .append(")).willReturn(0);\n\n")
          .append("        // when / then\n")
          .append("        assertThatThrownBy(() -> service.delete(")
          .append(samplePkArgs(model))
          .append("))\n")
          .append("            .isInstanceOf(CustomException.class);\n")
          .append("    }\n");
    }

    sb.append("\n    // TODO: 업무 규칙 테스트를 추가한다 (검증 조건, 상태 전이, 마스킹 등)\n").append("}\n");
    return sb.toString();
  }

  // ControllerTestTemplate도 동일한 PK 샘플값을 써야 해서 package-private로 공유한다.
  static String samplePkArgs(ScaffoldModel model) {
    return model.pkColumns().stream()
        .map(pkColumn -> sampleValue(model.pkJavaType(pkColumn)))
        .collect(java.util.stream.Collectors.joining(", "));
  }

  static String sampleValue(String javaType) {
    // Mockito 검증을 위해 두 번 평가해도 동일한(equals) 안정적 리터럴을 쓴다. now()는 금지.
    return switch (javaType) {
      case "Integer" -> "1";
      case "Long" -> "1L";
      case "LocalDate" -> "java.time.LocalDate.of(2020, 1, 1)";
      case "LocalDateTime" -> "java.time.LocalDateTime.of(2020, 1, 1, 0, 0)";
      case "BigDecimal" -> "java.math.BigDecimal.ONE";
      default -> "\"1\"";
    };
  }

  /** MockMvc .param() 등 HTTP 요청 파라미터 문자열 값. sampleValue()와 같은 값을 문자열 표현으로 맞춘다. */
  static String sampleParamValue(String javaType) {
    return switch (javaType) {
      case "LocalDate" -> "2020-01-01";
      case "LocalDateTime" -> "2020-01-01T00:00:00";
      default -> "1";
    };
  }
}
