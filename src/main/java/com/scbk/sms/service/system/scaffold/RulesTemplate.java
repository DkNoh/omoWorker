package com.scbk.sms.service.system.scaffold;

/** 업무 규칙 확장점(Rules) 생성. scaffold-contract.md §4-B. */
public final class RulesTemplate {

  private RulesTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    String module = model.moduleName();

    StringBuilder sb = new StringBuilder();
    sb.append("package com.scbk.sms.service.")
        .append(module)
        .append(";\n\n")
        .append("import com.scbk.sms.dto.")
        .append(module)
        .append(".")
        .append(cls)
        .append("UpdateRequestDTO;\n")
        .append("import org.springframework.stereotype.Component;\n\n")
        .append("/**\n")
        .append(" * ")
        .append(model.domainName())
        .append(" 업무 규칙 확장점 (scaffold-contract.md §4-B).\n")
        .append(" * Scaffold가 최초 1회 생성한다. 재생성 시 이 파일은 보존된다.\n")
        .append(" * validateOnCreate/validateOnUpdate에 업무 검증/파생 로직을 자유롭게 구현한다.\n")
        .append(" */\n")
        .append("@Component\n")
        .append("public class ")
        .append(cls)
        .append("Rules {\n\n")
        .append("    public void validateOnCreate(")
        .append(cls)
        .append("UpdateRequestDTO request) {\n")
        .append("        // TODO: 등록 시 업무 규칙 검증을 구현한다.\n")
        .append("    }\n\n")
        .append("    public void validateOnUpdate(")
        .append(cls)
        .append("UpdateRequestDTO request) {\n")
        .append("        // TODO: 수정 시 업무 규칙 검증을 구현한다.\n")
        .append("    }\n")
        .append("}\n");
    return sb.toString();
  }
}
