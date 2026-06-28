package com.scbk.sms.service.system.scaffold;

import java.util.List;

/** 화면 JS 생성. TuiPageBuilder로만 그리드를 초기화한다. */
public final class JsTemplate {

  private JsTemplate() {}

  public static String generate(ScaffoldModel model) {
    List<ScaffoldModel.SearchParam> params = model.searchParams();
    List<ScaffoldModel.ColumnConfig> columns = model.columnConfigs();

    StringBuilder sb = new StringBuilder();
    sb.append("// Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.\n");
    sb.append("document.addEventListener('DOMContentLoaded', function () {\n");
    sb.append("    const pageBuilder = new TuiPageBuilder({\n")
        .append("        el: 'grid',\n")
        .append("        apiUrl: '")
        .append(model.screenUrl())
        .append("/data',\n")
        .append("        searchInputs: [");
    for (int i = 0; i < params.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("'").append(params.get(i).name()).append("'");
    }
    sb.append("],\n").append("        searchDefaults: {");
    boolean firstDefault = true;
    for (ScaffoldModel.SearchParam param : params) {
      if ("NONE".equals(param.defaultValue())) {
        continue;
      }
      if (!firstDefault) {
        sb.append(", ");
      }
      sb.append(param.name()).append(": '").append(param.defaultValue()).append("'");
      firstDefault = false;
    }
    sb.append("},\n").append("        rowHeaders: ['rowNum'],\n").append("        columns: [\n");

    for (int i = 0; i < columns.size(); i++) {
      ScaffoldModel.ColumnConfig column = columns.get(i);
      sb.append("            { header: '")
          .append(escapeJs(column.headerName()))
          .append("', name: '")
          .append(column.fieldName())
          .append("', align: '")
          .append(column.align())
          .append("', width: ")
          .append(column.width());
      if (!column.visible()) {
        sb.append(", hidden: true");
      }
      if (!column.modalVisible()) {
        sb.append(", modalVisible: false");
      }
      if (column.editable()) {
        sb.append(", editable: true");
      }
      if (column.hasInputMask()) {
        sb.append(", inputMask: '").append(escapeJs(column.inputMask())).append("'");
      }
      if (column.hasValidate()) {
        sb.append(", validate: '").append(escapeJs(column.validate())).append("'");
      }
      appendFormatter(sb, column);
      sb.append(" }");
      if (i < columns.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append("        ]");
    if (model.includeDetailModal()) {
      sb.append(",\n        autoModal: true,\n")
          .append("        autoModalTitle: '")
          .append(escapeJs(model.domainName()))
          .append(" 상세'");
      if (model.includeCreateUpdate()) {
        sb.append(",\n")
            .append("        modalActions: {\n")
            .append("            createUrl: '")
            .append(model.screenUrl())
            .append("/create',\n")
            .append("            updateUrl: '")
            .append(model.screenUrl())
            .append("/update',\n")
            .append("            deleteUrl: '")
            .append(model.screenUrl())
            .append("/delete',\n")
            .append("            pkFields: [")
            .append(pkFields(model))
            .append("],\n");
        if (!model.lockColumn().isEmpty()) {
          sb.append("            lockField: '")
              .append(model.lockFieldName())
              .append("',\n")
              .append("            beforeLockField: '")
              .append(model.beforeLockFieldName())
              .append("',\n");
        }
        sb.append("            editable: true\n").append("        }");
      }
    }
    sb.append("\n").append("    });\n");

    if (model.includeExcel()) {
      sb.append("\n    document.querySelector('#btn-excel')?.addEventListener('click', () => {\n")
          .append("        if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) return;\n")
          .append("        const qs = pageBuilder.getSearchParams().toString();\n")
          .append("        window.location.href = '")
          .append(model.screenUrl())
          .append("/excel' + (qs ? '?' + qs : '');\n")
          .append("    });\n");
    }

    sb.append("});\n");
    return sb.toString();
  }

  private static void appendFormatter(StringBuilder sb, ScaffoldModel.ColumnConfig column) {
    if (column.hasMask()) {
      sb.append(", formatter: ({ value }) => TuiCommon.maskValue(value, '")
          .append(column.maskType())
          .append("')");
      return;
    }
    if (column.hasOptions()) {
      sb.append(", formatter: TuiCommon.badgeByValue({ labels: ")
          .append(column.optionsJsObject())
          .append(" })");
      return;
    }
    String dateFormat = column.dateFormat();
    if ("NONE".equals(dateFormat)) {
      return;
    }
    if ("DATE".equals(dateFormat)) {
      sb.append(", formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD')");
      return;
    }
    if ("DATETIME".equals(dateFormat)) {
      sb.append(", formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')");
      return;
    }
    if ("AUTO".equals(dateFormat)) {
      if (column.isDateColumn()) {
        sb.append(", formatter: TuiCommon.fmt.date");
      }
      return;
    }
    sb.append(", formatter: ({ value }) => TuiCommon.formatDate(value, '")
        .append(escapeJs(dateFormat))
        .append("')");
  }

  private static String escapeJs(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
  }

  private static String pkFields(ScaffoldModel model) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < model.pkFieldNames().size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("'").append(model.pkFieldNames().get(i)).append("'");
    }
    return sb.toString();
  }
}
