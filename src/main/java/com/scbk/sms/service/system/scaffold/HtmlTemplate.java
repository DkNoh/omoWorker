package com.scbk.sms.service.system.scaffold;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 화면 HTML 생성. screen-convention.md의 목록 화면 표준 골격을 따른다. */
public final class HtmlTemplate {

  private HtmlTemplate() {}

  public static String generate(ScaffoldModel model) {
    List<ScaffoldModel.SearchParam> params = model.searchParams();

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html>\n")
        .append("<!-- Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. -->\n")
        .append("<html xmlns:th=\"http://www.thymeleaf.org\"\n")
        .append("      xmlns:layout=\"http://www.ultraq.net.nz/thymeleaf/layout\"\n")
        .append("      layout:decorate=\"~{defaultLayout}\">\n")
        .append("<head>\n")
        .append("    <title>")
        .append(model.domainName())
        .append("</title>\n")
        .append("</head>\n")
        .append("<body>\n")
        .append("<main layout:fragment=\"content\">\n\n")
        .append("    <div class=\"content-header\">\n")
        .append("        <h2>")
        .append(model.domainName())
        .append("</h2>\n")
        .append("    </div>\n\n")
        .append("    <!-- 검색조건 카드 -->\n")
        .append("    <div class=\"card mb-4 shadow-sm border-0 scaffold-search-card\">\n")
        .append("        <div class=\"card-body\">\n")
        .append("            <div class=\"row align-items-center g-3 flex-wrap\">\n");

    Set<String> rendered = new HashSet<>();
    for (ScaffoldModel.SearchParam param : params) {
      if (rendered.contains(param.name())) {
        continue;
      }
      ScaffoldModel.SearchParam toParam = findRangeTo(param, params);
      if (toParam != null) {
        rendered.add(param.name());
        rendered.add(toParam.name());
        sb.append("                <div class=\"col-auto\"><label for=\"")
            .append(param.name())
            .append("\" class=\"col-form-label fw-bold\">")
            .append(rangeLabel(param.name(), toParam.name()))
            .append("</label></div>\n")
            .append("                <div class=\"col-auto d-flex align-items-center gap-2\">\n")
            .append("                    ");
        appendDatePickerInput(sb, param.name(), 150);
        sb.append("\n")
            .append("                    <span>~</span>\n")
            .append("                    ");
        appendDatePickerInput(sb, toParam.name(), 150);
        sb.append("\n").append("                </div>\n");
        continue;
      }

      rendered.add(param.name());
      sb.append("                <div class=\"col-auto\"><label for=\"")
          .append(param.name())
          .append("\" class=\"col-form-label fw-bold\">")
          .append(param.name())
          .append("</label></div>\n")
          .append("                <div class=\"col-auto\">");
      appendInput(sb, param);
      sb.append("</div>\n");
    }

    sb.append("                <div class=\"col-auto ms-auto d-flex gap-2\">\n")
        .append(createButton(model))
        .append(
            "                    <button type=\"button\" id=\"btn-search\" class=\"btn btn-primary px-4\" aria-label=\"조회\"><i data-lucide=\"search\" aria-hidden=\"true\"></i><span>조회</span></button>\n")
        .append(
            "                    <button type=\"button\" id=\"btn-reset\" class=\"btn btn-secondary px-3\" aria-label=\"검색 조건 초기화\"><i data-lucide=\"rotate-ccw\" aria-hidden=\"true\"></i><span>초기화</span></button>\n");
    if (model.includeExcel()) {
      sb.append(
          "                    <button type=\"button\" id=\"btn-excel\" th:if=\"${pageAuth.download}\" class=\"btn btn-success ms-1\" aria-label=\"엑셀 다운로드\"><i data-lucide=\"download\" aria-hidden=\"true\"></i><span>엑셀</span></button>\n");
    }
    sb.append("                </div>\n")
        .append("            </div>\n")
        .append("        </div>\n")
        .append("    </div>\n\n")
        .append("    <!-- Toast UI Grid -->\n")
        .append("    <div th:replace=\"~{fragments/toast-grid :: gridCard}\"></div>\n\n")
        .append("</main>\n")
        .append("<th:block layout:fragment=\"script\">\n")
        .append("    <script th:src=\"@{/js/")
        .append(model.moduleName())
        .append("/")
        .append(model.domainId())
        .append(".js}\"></script>\n")
        .append("</th:block>\n")
        .append("</body>\n")
        .append("</html>\n");
    return sb.toString();
  }

  private static void appendInput(StringBuilder sb, ScaffoldModel.SearchParam param) {
    if (param.isSelect()) {
      sb.append("<select id=\"")
          .append(param.name())
          .append("\" class=\"form-select scaffold-search-control\" aria-label=\"")
          .append(escape(param.name()))
          .append("\">\n")
          .append("                        <option value=\"\">전체</option>\n");
      for (Option option : parseOptions(param.optionsText())) {
        sb.append("                        <option value=\"")
            .append(escape(option.value()))
            .append("\">")
            .append(escape(option.label()))
            .append("</option>\n");
      }
      sb.append("                    </select>");
      return;
    }
    if (param.isRadio()) {
      sb.append("<div id=\"")
          .append(param.name())
          .append(
              "\" class=\"d-flex align-items-center gap-2 scaffold-radio-group\" role=\"radiogroup\" aria-label=\"")
          .append(escape(param.name()))
          .append("\">\n")
          .append(
              "                        <label class=\"form-check-label\"><input class=\"form-check-input me-1\" type=\"radio\" name=\"")
          .append(param.name())
          .append("\" value=\"\" checked>전체</label>\n");
      for (Option option : parseOptions(param.optionsText())) {
        sb.append(
                "                        <label class=\"form-check-label\"><input class=\"form-check-input me-1\" type=\"radio\" name=\"")
            .append(param.name())
            .append("\" value=\"")
            .append(escape(option.value()))
            .append("\">")
            .append(escape(option.label()))
            .append("</label>\n");
      }
      sb.append("                    </div>");
      return;
    }
    if (param.isDate()) {
      appendDatePickerInput(sb, param.name(), 160);
      return;
    }
    sb.append("<input type=\"text\" id=\"")
        .append(param.name())
        .append("\" class=\"form-control scaffold-search-control\" aria-label=\"")
        .append(escape(param.name()))
        .append("\">");
  }

  private static String createButton(ScaffoldModel model) {
    if (!model.includeCreateUpdate()) {
      return "";
    }
    return "                    <button type=\"button\" id=\"btn-create\" th:if=\"${pageAuth.create}\" class=\"btn btn-outline-primary px-3\" aria-label=\"신규 등록\"><i data-lucide=\"plus\" aria-hidden=\"true\"></i><span>등록</span></button>\n";
  }

  private static void appendDatePickerInput(StringBuilder sb, String inputId, int width) {
    sb.append("<div class=\"scaffold-date-field ")
        .append(dateFieldSizeClass(width))
        .append("\">")
        .append("<div class=\"tui-datepicker-input tui-datetime-input scaffold-datepicker-input\">")
        .append("<input type=\"text\" id=\"")
        .append(inputId)
        .append("\" data-search-type=\"date\" autocomplete=\"off\" aria-label=\"")
        .append(inputId)
        .append("\">")
        .append("<span class=\"tui-ico-date\" aria-hidden=\"true\"></span>")
        .append("</div>")
        .append("<div id=\"")
        .append(datePickerLayerId(inputId))
        .append("\" class=\"scaffold-date-picker-layer\"></div>")
        .append("</div>");
  }

  private static String dateFieldSizeClass(int width) {
    return width <= 150 ? "scaffold-date-field-sm" : "scaffold-date-field-md";
  }

  private static String datePickerLayerId(String inputId) {
    return inputId + "PickerLayer";
  }

  private static ScaffoldModel.SearchParam findRangeTo(
      ScaffoldModel.SearchParam fromParam, List<ScaffoldModel.SearchParam> params) {
    if (!fromParam.isDate()) {
      return null;
    }
    String target = rangeToName(fromParam.name());
    if (target == null) {
      return null;
    }
    for (ScaffoldModel.SearchParam param : params) {
      if (target.equals(param.name()) && param.isDate()) {
        return param;
      }
    }
    return null;
  }

  private static String rangeToName(String name) {
    if (name.endsWith("From")) {
      return name.substring(0, name.length() - 4) + "To";
    }
    if (name.startsWith("start") && name.length() > 5) {
      return "end" + name.substring(5);
    }
    if (name.startsWith("from") && name.length() > 4) {
      return "to" + name.substring(4);
    }
    return null;
  }

  private static String rangeLabel(String fromName, String toName) {
    if (fromName.endsWith("From")) {
      return fromName.substring(0, fromName.length() - 4);
    }
    if (fromName.startsWith("start") && toName.startsWith("end")) {
      return fromName;
    }
    if (fromName.startsWith("from") && toName.startsWith("to")) {
      return fromName;
    }
    return fromName;
  }

  private static List<Option> parseOptions(String optionsText) {
    if (optionsText == null || optionsText.trim().isEmpty()) {
      return List.of();
    }
    return java.util.Arrays.stream(optionsText.split(","))
        .map(String::trim)
        .filter(token -> !token.isEmpty())
        .map(HtmlTemplate::parseOption)
        .toList();
  }

  private static Option parseOption(String token) {
    String[] parts = token.split("[:=]", 2);
    if (parts.length == 2) {
      return new Option(parts[0].trim(), parts[1].trim());
    }
    return new Option(token, token);
  }

  private static String escape(String value) {
    return value == null
        ? ""
        : value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
  }

  private record Option(String value, String label) {}
}
