package com.scbk.sms.service.system.scaffold;

import java.util.List;

/** SearchRequestDTO 생성. PageRequestDTO 상속, Lombok 기반. */
public final class DtoTemplate {

  private DtoTemplate() {}

  public static String generate(ScaffoldModel model) {
    List<ScaffoldModel.SearchParam> params = model.searchParams();

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.dto.")
        .append(model.moduleName())
        .append(";\n\n")
        .append("import com.scbk.sms.dto.common.PageRequestDTO;\n")
        .append("import lombok.Data;\n")
        .append("import lombok.EqualsAndHashCode;\n\n")
        .append("/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */\n")
        .append("@Data\n")
        .append("@EqualsAndHashCode(callSuper = true)\n")
        .append("public class ")
        .append(model.domainClass())
        .append("SearchRequestDTO extends PageRequestDTO {\n\n");

    for (ScaffoldModel.SearchParam param : params) {
      sb.append("    private String ").append(param.name()).append(";\n");
    }
    sb.append("}\n");
    return sb.toString();
  }
}
