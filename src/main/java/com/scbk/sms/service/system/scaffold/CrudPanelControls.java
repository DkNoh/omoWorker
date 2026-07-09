package com.scbk.sms.service.system.scaffold;

import java.util.Arrays;
import java.util.List;

final class CrudPanelControls {

  private CrudPanelControls() {}

  static String searchInput(ScaffoldModel.SearchParam param) {
    if (param.isSelect()) {
      return select(
          param.name(), param.optionsText(), "form-select scaffold-search-control", "전체", "");
    }
    if (param.isRadio()) {
      return radioGroup(param);
    }
    if (param.isDate()) {
      return "<div class=\"scaffold-date-field scaffold-date-field-md\"><div class=\"tui-datepicker-input tui-datetime-input scaffold-datepicker-input\"><input type=\"text\" id=\""
          + param.name()
          + "\" data-search-type=\"date\" autocomplete=\"off\" aria-label=\""
          + html(param.name())
          + "\"><span class=\"tui-ico-date\" aria-hidden=\"true\"></span></div><div id=\""
          + param.name()
          + "PickerLayer\" class=\"scaffold-date-picker-layer\"></div></div>";
    }
    return "<input type=\"text\" id=\""
        + param.name()
        + "\" class=\"form-control scaffold-search-control\" aria-label=\""
        + html(param.name())
        + "\">";
  }

  static String formControl(ScaffoldModel.ColumnConfig column) {
    String id = "f-" + column.fieldName();
    if (column.hasOptions()) {
      return select(
              column.fieldName(),
              column.optionsText(),
              "form-select",
              "선택",
              formatAttributes(column))
          .replace("id=\"" + column.fieldName() + "\"", "id=\"" + id + "\"");
    }
    String type =
        switch (column.javaType()) {
          case "Integer", "Long", "BigDecimal" -> "number";
          default -> "text";
        };
    return "<input type=\""
        + type
        + "\" class=\"form-control\" id=\""
        + id
        + "\" name=\""
        + column.fieldName()
        + "\""
        + formatAttributes(column)
        + ">";
  }

  private static String select(
      String name, String optionsText, String cssClass, String emptyLabel, String attrs) {
    StringBuilder sb = new StringBuilder("<select id=\"");
    sb.append(name)
        .append("\" class=\"")
        .append(cssClass)
        .append("\" name=\"")
        .append(name)
        .append("\" aria-label=\"")
        .append(html(name))
        .append("\"")
        .append(attrs)
        .append("><option value=\"\">")
        .append(emptyLabel)
        .append("</option>");
    for (Option option : parseOptions(optionsText)) {
      sb.append("<option value=\"")
          .append(html(option.value()))
          .append("\">")
          .append(html(option.label()))
          .append("</option>");
    }
    return sb.append("</select>").toString();
  }

  private static String radioGroup(ScaffoldModel.SearchParam param) {
    StringBuilder sb = new StringBuilder("<div id=\"");
    sb.append(param.name())
        .append(
            "\" class=\"d-flex align-items-center gap-2 scaffold-radio-group\" role=\"radiogroup\" aria-label=\"")
        .append(html(param.name()))
        .append(
            "\"><label class=\"form-check-label\"><input class=\"form-check-input me-1\" type=\"radio\" name=\"")
        .append(param.name())
        .append("\" value=\"\" checked>전체</label>");
    for (Option option : parseOptions(param.optionsText())) {
      sb.append(
              "<label class=\"form-check-label\"><input class=\"form-check-input me-1\" type=\"radio\" name=\"")
          .append(param.name())
          .append("\" value=\"")
          .append(html(option.value()))
          .append("\">")
          .append(html(option.label()))
          .append("</label>");
    }
    return sb.append("</div>").toString();
  }

  private static String formatAttributes(ScaffoldModel.ColumnConfig column) {
    StringBuilder attrs = new StringBuilder();
    if (column.hasInputMask()) {
      attrs.append(" data-mask=\"").append(html(column.inputMask())).append("\"");
    }
    if (column.hasValidate()) {
      attrs.append(" data-validate=\"").append(html(column.validate())).append("\"");
    }
    return attrs.toString();
  }

  private static List<Option> parseOptions(String optionsText) {
    if (optionsText == null || optionsText.trim().isEmpty()) {
      return List.of();
    }
    return Arrays.stream(optionsText.split(","))
        .map(String::trim)
        .filter(token -> !token.isEmpty())
        .map(CrudPanelControls::parseOption)
        .toList();
  }

  private static Option parseOption(String token) {
    String[] parts = token.split("[:=]", 2);
    if (parts.length == 2) {
      return new Option(parts[0].trim(), parts[1].trim());
    }
    return new Option(token, token);
  }

  private static String html(String value) {
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
