package com.scbk.sms.service.system.scaffold;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

final class ExcelPageRenderer implements ScaffoldPageRenderer {

  private static final String HTML_TEMPLATE = "scaffold-templates/excel/page.html.tpl";
  private static final String JS_TEMPLATE = "scaffold-templates/excel/page.js.tpl";

  @Override
  public Map<String, String> render(ScaffoldModel model) {
    Map<String, String> files = new LinkedHashMap<>();
    files.put(model.domainId() + ".html", renderHtml(model));
    files.put(model.domainId() + ".js", renderJs(model));
    return files;
  }

  private String renderHtml(ScaffoldModel model) {
    return ResourceTemplateRenderer.render(
        HTML_TEMPLATE,
        Map.of(
            "DOMAIN_NAME", html(model.domainName()),
            "MODULE_NAME", model.moduleName(),
            "DOMAIN_ID", model.domainId(),
            "SEARCH_CARD", searchCard(model)));
  }

  private String renderJs(ScaffoldModel model) {
    return ResourceTemplateRenderer.render(
        JS_TEMPLATE,
        Map.of(
            "SCREEN_URL", js(model.screenUrl()),
            "SEARCH_INPUTS",
                quotedNames(
                    model.searchParams().stream().map(ScaffoldModel.SearchParam::name).toList()),
            "SEARCH_DEFAULTS", searchDefaults(model.searchParams()),
            "GRID_COLUMNS", gridColumns(model.columnConfigs())));
  }

  private static String searchCard(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    sb.append("    <div class=\"card mb-4 shadow-sm border-0 scaffold-search-card\">\n")
        .append("        <div class=\"card-body\">\n")
        .append("            <div class=\"row align-items-center g-3 flex-wrap\">\n");
    for (ScaffoldModel.SearchParam param : model.searchParams()) {
      sb.append("                <div class=\"col-auto\"><label for=\"")
          .append(param.name())
          .append("\" class=\"col-form-label fw-bold\">")
          .append(html(param.name()))
          .append("</label></div>\n")
          .append("                <div class=\"col-auto\">")
          .append(CrudPanelControls.searchInput(param))
          .append("</div>\n");
    }
    sb.append("                <div class=\"col-auto ms-auto d-flex gap-2\">\n")
        .append(
            "                    <button type=\"button\" id=\"btn-search\" class=\"btn btn-primary px-4\" aria-label=\"조회\"><i data-lucide=\"search\" aria-hidden=\"true\"></i><span>조회</span></button>\n")
        .append(
            "                    <button type=\"button\" id=\"btn-reset\" class=\"btn btn-secondary px-3\" aria-label=\"검색 조건 초기화\"><i data-lucide=\"rotate-ccw\" aria-hidden=\"true\"></i><span>초기화</span></button>\n")
        .append(
            "                    <button type=\"button\" id=\"btn-excel\" th:if=\"${pageAuth.download}\" class=\"btn btn-success px-3\" aria-label=\"엑셀 다운로드\"><i data-lucide=\"download\" aria-hidden=\"true\"></i><span>엑셀</span></button>\n")
        .append("                </div>\n")
        .append("            </div>\n")
        .append("        </div>\n")
        .append("    </div>");
    return sb.toString();
  }

  private static String gridColumns(List<ScaffoldModel.ColumnConfig> columns) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < columns.size(); i++) {
      ScaffoldModel.ColumnConfig column = columns.get(i);
      sb.append("            { header: '")
          .append(js(column.headerName()))
          .append("', name: '")
          .append(column.fieldName())
          .append("', align: '")
          .append(column.align())
          .append("', width: ")
          .append(column.width());
      if (!column.visible()) {
        sb.append(", hidden: true");
      }
      appendFormatter(sb, column);
      sb.append(" }");
      if (i < columns.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private static void appendFormatter(StringBuilder sb, ScaffoldModel.ColumnConfig column) {
    if (column.hasMask()) {
      sb.append(", formatter: ({ value }) => TuiCommon.maskValue(value, '")
          .append(column.maskType())
          .append("')");
    } else if (column.hasOptions()) {
      sb.append(", formatter: TuiCommon.badgeByValue({ labels: ")
          .append(column.optionsJsObject())
          .append(" })");
    } else if ("DATE".equals(column.dateFormat())) {
      sb.append(", formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD')");
    } else if ("DATETIME".equals(column.dateFormat())) {
      sb.append(", formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')");
    } else if ("AUTO".equals(column.dateFormat()) && column.isDateColumn()) {
      sb.append(", formatter: TuiCommon.fmt.date");
    }
  }

  private static String quotedNames(List<String> names) {
    return names.stream()
        .map(name -> "'" + js(name) + "'")
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }

  private static String searchDefaults(List<ScaffoldModel.SearchParam> params) {
    StringJoiner joiner = new StringJoiner(", ");
    for (ScaffoldModel.SearchParam param : params) {
      if (!"NONE".equals(param.defaultValue())) {
        joiner.add(param.name() + ": '" + js(param.defaultValue()) + "'");
      }
    }
    return joiner.toString();
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

  private static String js(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
  }
}
