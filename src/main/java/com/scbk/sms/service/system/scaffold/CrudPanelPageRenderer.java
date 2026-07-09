package com.scbk.sms.service.system.scaffold;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

final class CrudPanelPageRenderer implements ScaffoldPageRenderer {

  private static final String HTML_TEMPLATE = "scaffold-templates/crud-panel/page.html.tpl";
  private static final String JS_TEMPLATE = "scaffold-templates/crud-panel/page.js.tpl";

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
            "SEARCH_CARD", searchCard(model),
            "DETAIL_PANEL", detailPanel(model)));
  }

  private String renderJs(ScaffoldModel model) {
    return ResourceTemplateRenderer.render(
        JS_TEMPLATE,
        Map.of(
            "SCREEN_URL", js(model.screenUrl()),
            "DOMAIN_NAME", js(model.domainName()),
            "SEARCH_INPUTS",
                quotedNames(
                    model.searchParams().stream().map(ScaffoldModel.SearchParam::name).toList()),
            "SEARCH_DEFAULTS", searchDefaults(model.searchParams()),
            "GRID_COLUMNS", gridColumns(model.columnConfigs()),
            "DEFAULT_FORM", defaultForm(model),
            "PK_FIELDS", quotedNames(model.pkFieldNames()),
            "LOCK_CONFIG", lockConfig(model)));
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
            "                    <button type=\"button\" id=\"btn-create\" th:if=\"${pageAuth.create}\" class=\"btn btn-outline-primary px-3\" aria-label=\"신규 등록\"><i data-lucide=\"plus\" aria-hidden=\"true\"></i><span>등록</span></button>\n")
        .append(
            "                    <button type=\"button\" id=\"btn-search\" class=\"btn btn-primary px-4\" aria-label=\"조회\"><i data-lucide=\"search\" aria-hidden=\"true\"></i><span>조회</span></button>\n")
        .append(
            "                    <button type=\"button\" id=\"btn-reset\" class=\"btn btn-secondary px-3\" aria-label=\"검색 조건 초기화\"><i data-lucide=\"rotate-ccw\" aria-hidden=\"true\"></i><span>초기화</span></button>\n")
        .append("                </div>\n")
        .append("            </div>\n")
        .append("        </div>\n")
        .append("    </div>");
    return sb.toString();
  }

  private static String detailPanel(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    sb.append("    <section class=\"card surface-card mb-4\" id=\"detail-panel\" hidden>\n")
        .append(
            "        <header class=\"card-header bg-transparent d-flex justify-content-between align-items-center flex-wrap gap-2\">\n")
        .append(
            "            <div class=\"d-flex align-items-center gap-2\"><i data-lucide=\"panel-right\" aria-hidden=\"true\"></i><span class=\"fw-semibold\" id=\"detail-panel-title\">")
        .append(html(model.domainName()))
        .append(" 상세</span><code id=\"detail-selected-key\"></code></div>\n")
        .append("            <div class=\"d-flex gap-2 flex-wrap\">\n")
        .append(
            "                <button type=\"button\" id=\"btn-save\" th:if=\"${pageAuth.create or pageAuth.update}\" class=\"btn btn-primary\"><i data-lucide=\"save\" aria-hidden=\"true\"></i><span>저장</span></button>\n")
        .append(
            "                <button type=\"button\" id=\"btn-delete\" th:if=\"${pageAuth.delete}\" class=\"btn btn-danger\"><i data-lucide=\"trash-2\" aria-hidden=\"true\"></i><span>삭제</span></button>\n")
        .append("            </div>\n")
        .append("        </header>\n")
        .append(
            "        <div class=\"card-body\"><form id=\"detail-form\" autocomplete=\"off\" novalidate>\n")
        .append(hiddenFields(model))
        .append("            <div class=\"row g-3\">\n")
        .append(formFields(model.columnConfigs()))
        .append("            </div>\n")
        .append("        </form></div>\n")
        .append("    </section>\n")
        .append("    <section class=\"card surface-card mb-4\" id=\"detail-empty-panel\">\n")
        .append(
            "        <div class=\"empty-state\"><i data-lucide=\"mouse-pointer-click\" class=\"empty-state-icon\" aria-hidden=\"true\"></i><p class=\"empty-state-title\">행을 선택하세요</p></div>\n")
        .append("    </section>");
    return sb.toString();
  }

  private static String hiddenFields(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (String pkField : model.pkFieldNames()) {
      sb.append("            <input type=\"hidden\" name=\"").append(pkField).append("\">\n");
    }
    if (!model.lockColumn().isEmpty()) {
      sb.append("            <input type=\"hidden\" name=\"")
          .append(model.beforeLockFieldName())
          .append("\">\n");
    }
    return sb.toString();
  }

  private static String formFields(java.util.List<ScaffoldModel.ColumnConfig> columns) {
    StringBuilder sb = new StringBuilder();
    for (ScaffoldModel.ColumnConfig column : columns) {
      if (!column.editable() && !column.modalVisible()) {
        continue;
      }
      if (!column.editable()) {
        sb.append(readonlyField(column));
        continue;
      }
      sb.append(
              "                <div class=\"col-12 col-md-6\"><label class=\"form-label\" for=\"f-")
          .append(column.fieldName())
          .append("\">")
          .append(html(column.headerName()))
          .append("</label>")
          .append(CrudPanelControls.formControl(column))
          .append("</div>\n");
    }
    return sb.toString();
  }

  private static String readonlyField(ScaffoldModel.ColumnConfig column) {
    return "                <div class=\"col-12 col-md-6\"><div class=\"form-label\">"
        + html(column.headerName())
        + "</div><div class=\"form-control-plaintext\" data-readonly-field=\""
        + column.fieldName()
        + "\"></div></div>\n";
  }

  private static String gridColumns(java.util.List<ScaffoldModel.ColumnConfig> columns) {
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

  private static String defaultForm(ScaffoldModel model) {
    StringJoiner joiner = new StringJoiner(", ", "{ ", " }");
    for (String pkField : model.pkFieldNames()) {
      joiner.add(pkField + ": ''");
    }
    for (ScaffoldModel.ColumnConfig column : model.columnConfigs()) {
      if (column.editable()) {
        joiner.add(column.fieldName() + ": ''");
      }
    }
    if (!model.lockColumn().isEmpty()) {
      joiner.add(model.beforeLockFieldName() + ": ''");
    }
    return joiner.toString();
  }

  private static String quotedNames(java.util.List<String> names) {
    return names.stream()
        .map(name -> "'" + js(name) + "'")
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }

  private static String searchDefaults(java.util.List<ScaffoldModel.SearchParam> params) {
    StringJoiner joiner = new StringJoiner(", ");
    for (ScaffoldModel.SearchParam param : params) {
      if (!"NONE".equals(param.defaultValue())) {
        joiner.add(param.name() + ": '" + js(param.defaultValue()) + "'");
      }
    }
    return joiner.toString();
  }

  private static String lockConfig(ScaffoldModel model) {
    if (model.lockColumn().isEmpty()) {
      return "null";
    }
    return "{ field: '"
        + js(model.lockFieldName())
        + "', beforeField: '"
        + js(model.beforeLockFieldName())
        + "' }";
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
