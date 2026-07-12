package com.scbk.sms.service.system.scaffold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mapper XML 생성. $변수 라인은 동적 <if> 조건으로 변환된다. mapper-xml.xml.tpl 리소스를 치환한다. */
public final class MapperXmlTemplate {

  private static final String TEMPLATE = "scaffold-templates/mapper-xml.xml.tpl";
  private static final Pattern SEARCH_VAR_PATTERN = Pattern.compile("\\$([a-zA-Z0-9_]+)");
  private static final Pattern COLUMN_COMPARE_PATTERN =
      Pattern.compile("([A-Za-z0-9_\\.]+)\\s*(<>|>=|<=|=|>|<)\\s*\\$([a-zA-Z0-9_]+)");
  // BETWEEN $a AND $b 패턴. 비교 연산자 패턴이 BETWEEN을 잡지 못하므로 별도 처리한다.
  // 하한($a)은 000000, 상한($b)은 235959 suffix로 변환해 당일 inclusive 범위를 만든다.
  private static final Pattern COLUMN_BETWEEN_PATTERN =
      Pattern.compile(
          "([A-Za-z0-9_\\.]+)\\s+BETWEEN\\s+\\$([a-zA-Z0-9_]+)\\s+AND\\s+\\$([a-zA-Z0-9_]+)",
          Pattern.CASE_INSENSITIVE);

  private MapperXmlTemplate() {}

  public static String generate(ScaffoldModel model) {
    String cls = model.domainClass();
    String module = model.moduleName();
    String targetTable = model.targetTable();
    if (model.includeCreateUpdate() && targetTable.isEmpty()) {
      throw new IllegalStateException(
          "CRUD 모드는 targetTable이 필요합니다. 조회 SQL의 FROM 테이블을 추론할 수 없으면 수정 대상 테이블을 입력하세요.");
    }
    validateCrudModel(model);

    return ResourceTemplateRenderer.render(
        TEMPLATE,
        Map.of(
            "MODULE_NAME", module,
            "DOMAIN_CLASS", cls,
            "ORDER_BY", model.orderBy(),
            "PAGE_CLAUSE", model.dialect().pageClause(),
            "SEARCH_CONDITIONS", buildSearchConditionIfs(model, "            "),
            "BASE_QUERY", buildBaseQuerySql(model, "            "),
            "CRUD_SECTION", crudSection(model, cls, targetTable),
            "EXCEL_SECTION", excelSection(model, cls),
            "PRIVACY_SECTION", privacySection(model, cls, module)));
  }

  private static String crudSection(ScaffoldModel model, String cls, String targetTable) {
    if (!model.includeCreateUpdate()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("\n    <insert id=\"insert\">\n")
        .append(signature(cls, "insert"))
        .append(insertSql(model, targetTable))
        .append("    </insert>\n\n")
        .append("    <!-- update 기준: 수정 허용 컬럼만 SET하고, 잠금 컬럼을 지정한 경우 WHERE에 함께 둔다. -->\n")
        .append("    <update id=\"update\">\n")
        .append(signature(cls, "update"))
        .append("        UPDATE ")
        .append(targetTable)
        .append("\n")
        .append(updateSetClause(model))
        .append(pkWhereClause(model, "         WHERE "))
        .append(lockWhereClause(model))
        .append("    </update>\n\n")
        .append("    <delete id=\"delete\">\n")
        .append(signature(cls, "delete"))
        .append("        DELETE FROM ")
        .append(targetTable)
        .append(pkWhereClause(model, " WHERE "))
        .append("    </delete>\n");
    return sb.toString();
  }

  private static String excelSection(ScaffoldModel model, String cls) {
    if (!model.includeExcel()) {
      return "";
    }
    return "\n    <select id=\"selectListForExcel\" resultType=\"java.util.HashMap\">\n"
        + signature(cls, "selectListForExcel")
        + "        SELECT A.*\n"
        + "        FROM (\n"
        + "        <include refid=\"baseQuery\"/>\n"
        + "        ) A\n"
        + "        <include refid=\"searchConditions\"/>\n"
        + "        ORDER BY "
        + model.orderBy()
        + "\n"
        + "    </select>\n";
  }

  private static String privacySection(ScaffoldModel model, String cls, String module) {
    if (!model.includePrivacy()) {
      return "";
    }
    return "\n    <select id=\"selectDetail\" resultType=\"com.scbk.sms.vo."
        + module
        + "."
        + cls
        + "VO\">\n"
        + signature(cls, "selectDetail")
        + "        SELECT A.*\n"
        + "        FROM (\n"
        + "        <include refid=\"baseQuery\"/>\n"
        + "        ) A\n"
        + "        WHERE A."
        + model.pkColumn()
        + " = #{"
        + model.pkFieldName()
        + "}\n"
        + "    </select>\n";
  }

  // rawQuery를 SELECT/FROM 부분(baseQuery용)과 WHERE 이하 부분(searchConditions용)으로 분리한다.
  // 분리 기준은 top-level WHERE (괄호 깊이 0에서 첫 등장). 보통 "WHERE 1=1" 형태.
  private static String[] splitRawQuery(String rawQuery) {
    String lower = rawQuery.toLowerCase();
    int depth = 0;
    boolean inSingle = false;
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if (c == '\'' && !inSingle) {
        inSingle = true;
      } else if (c == '\'' && inSingle) {
        inSingle = false;
      } else if (!inSingle) {
        if (c == '(') depth++;
        else if (c == ')') depth--;
        else if (depth == 0
            && i + 5 <= lower.length()
            && lower.substring(i, i + 5).equals("where")
            && (i == 0 || !Character.isLetterOrDigit(lower.charAt(i - 1)))
            && (i + 5 == lower.length() || !Character.isLetterOrDigit(lower.charAt(i + 5)))) {
          return new String[] {rawQuery.substring(0, i), rawQuery.substring(i + 5)};
        }
      }
    }
    return new String[] {rawQuery, ""};
  }

  // baseQuery: SELECT/FROM 부분만. WHERE/조건은 searchConditions로 이동했다.
  private static String buildBaseQuerySql(ScaffoldModel model, String indent) {
    String[] parts = splitRawQuery(model.rawQuery());
    StringBuilder sb = new StringBuilder();
    for (String line : parts[0].split("\n")) {
      if (!line.trim().isEmpty()) {
        sb.append(indent).append(escapeSqlText(line)).append("\n");
      }
    }
    return sb.toString();
  }

  // searchConditions 내용: WHERE 이하 줄들을 <if> 래핑. $변수 없는 줄은 평문 AND로.
  // "1=1" no-op 줄은 스킵. mybatis <where>가 자동으로 첫 AND를 WHERE로 변환한다.
  private static String buildSearchConditionIfs(ScaffoldModel model, String indent) {
    Map<String, ScaffoldModel.SearchParam> paramMap = new HashMap<>();
    for (ScaffoldModel.SearchParam param : model.searchParams()) {
      paramMap.put(param.name(), param);
    }

    String[] parts = splitRawQuery(model.rawQuery());
    StringBuilder sb = new StringBuilder();
    if (parts[1].trim().isEmpty()
        && model.getSearchVars().isEmpty()
        && !model.getColumns().isEmpty()) {
      String firstColumn = model.getColumns().get(0).trim().toUpperCase();
      sb.append(indent)
          .append("<if test=\"searchKeyword != null and searchKeyword != ''\">\n")
          .append(indent)
          .append("    AND A.")
          .append(firstColumn)
          .append(" LIKE '%' || #{searchKeyword} || '%'\n")
          .append(indent)
          .append("</if>\n");
      return sb.toString();
    }

    for (String line : parts[1].split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) continue;
      if (trimmed.replace(" ", "").equalsIgnoreCase("1=1")) continue;

      if (!line.contains("$")) {
        String normalized = trimmed.toUpperCase().startsWith("AND ") ? trimmed : "AND " + trimmed;
        sb.append(indent).append(escapeSqlText(normalized)).append("\n");
        continue;
      }

      java.util.List<String> lineVars = new java.util.ArrayList<>();
      Matcher matcher = SEARCH_VAR_PATTERN.matcher(line);
      while (matcher.find()) {
        lineVars.add(QueryColumnExtractor.toCamelCase(matcher.group(1)));
      }

      sb.append(indent).append("<if test=\"");
      for (int i = 0; i < lineVars.size(); i++) {
        if (i > 0) sb.append(" and ");
        sb.append(lineVars.get(i)).append(" != null and ").append(lineVars.get(i)).append(" != ''");
      }
      sb.append("\">\n");

      String replacedLine = replaceBindVariables(model, paramMap, line);
      String normalized = replacedLine.trim();
      if (!normalized.toUpperCase().startsWith("AND ")) {
        normalized = "AND " + normalized;
      }
      sb.append(indent).append("    ").append(normalized).append("\n");
      sb.append(indent).append("</if>\n");
    }
    return sb.toString();
  }

  private static String replaceBindVariables(
      ScaffoldModel model, Map<String, ScaffoldModel.SearchParam> paramMap, String line) {
    String betweenHandled = replaceBetweenClauses(model, paramMap, line);

    Matcher compareMatcher = COLUMN_COMPARE_PATTERN.matcher(betweenHandled);
    StringBuffer buffer = new StringBuffer();
    while (compareMatcher.find()) {
      String columnRef = compareMatcher.group(1);
      String operator = compareMatcher.group(2);
      String rawVar = compareMatcher.group(3);
      String fieldName = QueryColumnExtractor.toCamelCase(rawVar);
      String replacement =
          compareReplacement(model, paramMap.get(fieldName), columnRef, operator, fieldName);
      compareMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
    }
    compareMatcher.appendTail(buffer);

    return SEARCH_VAR_PATTERN
        .matcher(buffer.toString())
        .replaceAll(match -> "#{" + QueryColumnExtractor.toCamelCase(match.group(1)) + "}");
  }

  private static String replaceBetweenClauses(
      ScaffoldModel model, Map<String, ScaffoldModel.SearchParam> paramMap, String line) {
    Matcher betweenMatcher = COLUMN_BETWEEN_PATTERN.matcher(line);
    StringBuffer buffer = new StringBuffer();
    while (betweenMatcher.find()) {
      String columnRef = betweenMatcher.group(1);
      String fromField = QueryColumnExtractor.toCamelCase(betweenMatcher.group(2));
      String toField = QueryColumnExtractor.toCamelCase(betweenMatcher.group(3));
      String lower = bindExpression(model, paramMap.get(fromField), columnRef, ">=", fromField);
      String upper = bindExpression(model, paramMap.get(toField), columnRef, "<=", toField);
      String replacement = columnRef + " BETWEEN " + lower + " AND " + upper;
      betweenMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
    }
    betweenMatcher.appendTail(buffer);
    return buffer.toString();
  }

  private static String compareReplacement(
      ScaffoldModel model,
      ScaffoldModel.SearchParam param,
      String columnRef,
      String operator,
      String fieldName) {
    if (param == null || !param.isDate() || !"=".equals(operator)) {
      return columnRef
          + " "
          + xmlOperator(operator)
          + " "
          + bindExpression(model, param, columnRef, operator, fieldName);
    }

    String javaType = model.getTypeMap().getOrDefault(columnName(columnRef).toUpperCase(), "");
    if ("LocalDateTime".equals(javaType)) {
      String start = model.dialect().timestampExpression(fieldName, "000000");
      return columnRef
          + " "
          + xmlOperator(">=")
          + " "
          + start
          + " AND "
          + columnRef
          + " "
          + xmlOperator("<")
          + " "
          + model.dialect().plusOneDay(start);
    }
    if ("LocalDate".equals(javaType)) {
      String start = model.dialect().dateExpression(fieldName);
      return columnRef
          + " "
          + xmlOperator(">=")
          + " "
          + start
          + " AND "
          + columnRef
          + " "
          + xmlOperator("<")
          + " "
          + model.dialect().plusOneDay(start);
    }
    return columnRef + " " + xmlOperator(operator) + " #{" + fieldName + "}";
  }

  private static String bindExpression(
      ScaffoldModel model,
      ScaffoldModel.SearchParam param,
      String columnRef,
      String operator,
      String fieldName) {
    if (param == null || !param.isDate()) {
      return "#{" + fieldName + "}";
    }
    String columnName = columnName(columnRef);
    String javaType = model.getTypeMap().getOrDefault(columnName.toUpperCase(), "");
    if ("LocalDateTime".equals(javaType)) {
      String suffix = isUpperBound(operator, fieldName) ? "235959" : "000000";
      return model.dialect().timestampExpression(fieldName, suffix);
    }
    if ("LocalDate".equals(javaType)) {
      return model.dialect().dateExpression(fieldName);
    }
    return "#{" + fieldName + "}";
  }

  private static String columnName(String columnRef) {
    return columnRef.contains(".")
        ? columnRef.substring(columnRef.lastIndexOf('.') + 1)
        : columnRef;
  }

  private static String escapeSqlText(String sqlLine) {
    return sqlLine.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String xmlOperator(String operator) {
    if (operator.contains("<") || operator.contains(">")) {
      return "<![CDATA[ " + operator + " ]]>";
    }
    return operator;
  }

  private static boolean isUpperBound(String operator, String fieldName) {
    return "<=".equals(operator)
        || "<".equals(operator)
        || fieldName.endsWith("To")
        || fieldName.startsWith("end")
        || fieldName.startsWith("to");
  }

  private static String insertSql(ScaffoldModel model, String targetTable) {
    List<String> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();
    for (ScaffoldModel.ColumnConfig column : editableColumns(model)) {
      columns.add(column.columnName());
      values.add(bindParameter(column.fieldName(), column.javaType()));
    }

    if (hasColumn(model, "REG_DTTM")) {
      columns.add("REG_DTTM");
      values.add(currentValueExpression(model, "REG_DTTM"));
    }
    if (!model.lockColumn().isEmpty()) {
      columns.add(model.lockColumn());
      values.add(currentValueExpression(model, model.lockColumn()));
    } else if (hasColumn(model, "UPD_DTTM")) {
      columns.add("UPD_DTTM");
      values.add(currentValueExpression(model, "UPD_DTTM"));
    }

    if (columns.isEmpty()) {
      throw new IllegalStateException("CRUD INSERT를 생성할 수정 가능 컬럼이 없습니다. 컬럼 옵션에서 등록/수정할 컬럼을 선택하세요.");
    }

    return "        INSERT INTO "
        + targetTable
        + " (\n"
        + "            "
        + String.join(",\n            ", columns)
        + "\n"
        + "        ) VALUES (\n"
        + "            "
        + String.join(",\n            ", values)
        + "\n"
        + "        )\n";
  }

  private static String updateSetClause(ScaffoldModel model) {
    List<String> assignments = new ArrayList<>();
    for (ScaffoldModel.ColumnConfig column : editableColumns(model)) {
      assignments.add(
          column.columnName() + " = " + bindParameter(column.fieldName(), column.javaType()));
    }
    if (!model.lockColumn().isEmpty()) {
      assignments.add(
          model.lockColumn() + " = " + currentValueExpression(model, model.lockColumn()));
    }
    if (assignments.isEmpty()) {
      String firstPk = model.pkColumn().isEmpty() ? "ID" : model.pkColumn();
      assignments.add(firstPk + " = " + firstPk);
    }

    StringBuilder sb = new StringBuilder("           SET ");
    for (int i = 0; i < assignments.size(); i++) {
      if (i > 0) {
        sb.append("               ");
      }
      sb.append(assignments.get(i));
      if (i < assignments.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private static List<ScaffoldModel.ColumnConfig> editableColumns(ScaffoldModel model) {
    return model.columnConfigs().stream().filter(ScaffoldModel.ColumnConfig::editable).toList();
  }

  private static boolean hasColumn(ScaffoldModel model, String columnName) {
    return model.getColumns().stream()
        .map(String::trim)
        .map(String::toUpperCase)
        .anyMatch(columnName::equals);
  }

  private static String currentValueExpression(ScaffoldModel model, String columnName) {
    String normalized = columnName.toUpperCase();
    String javaType = model.getTypeMap().getOrDefault(normalized, "");
    if ("LocalDate".equals(javaType)) {
      return model.dialect().currentDate();
    }
    if ("String".equals(javaType) && (normalized.endsWith("DTTM") || normalized.endsWith("_DTM"))) {
      return model.dialect().currentTimestampString();
    }
    return model.dialect().currentTimestamp();
  }

  private static String pkWhereClause(ScaffoldModel model, String prefix) {
    List<String> pkColumns = model.pkColumns();
    if (pkColumns.isEmpty()) {
      throw new IllegalStateException("CRUD mode requires at least one PK column.");
    }
    StringBuilder sb = new StringBuilder(prefix);
    for (int i = 0; i < pkColumns.size(); i++) {
      String column = pkColumns.get(i);
      if (i > 0) {
        sb.append("           AND ");
      }
      sb.append(column)
          .append(" = ")
          .append(bindParameter(QueryColumnExtractor.toCamelCase(column), model.pkJavaType(column)))
          .append("\n");
    }
    return sb.toString();
  }

  private static String lockWhereClause(ScaffoldModel model) {
    if (model.lockColumn().isEmpty()) {
      return "";
    }
    String beforeValue = bindParameter(model.beforeLockFieldName(), model.lockJavaType());
    return "           AND ("
        + model.lockColumn()
        + " = "
        + beforeValue
        + "\n"
        + "                OR ("
        + model.lockColumn()
        + " IS NULL AND "
        + beforeValue
        + " IS NULL))\n";
  }

  private static void validateCrudModel(ScaffoldModel model) {
    if (!model.includeCreateUpdate() || model.lockColumn().isEmpty()) {
      return;
    }
    if (model.pkColumns().contains(model.lockColumn())) {
      throw new IllegalStateException(
          "Optimistic lock column must not be a PK column: " + model.lockColumn());
    }
  }

  private static String bindParameter(String fieldName, String javaType) {
    String jdbcType =
        switch (javaType) {
          case "LocalDateTime" -> "TIMESTAMP";
          case "LocalDate" -> "DATE";
          case "BigDecimal" -> "DECIMAL";
          case "Long", "Integer" -> "NUMERIC";
          case "byte[]" -> "BINARY";
          case "String" -> "VARCHAR";
          default -> "";
        };
    if (jdbcType.isEmpty()) {
      return "#{" + fieldName + "}";
    }
    return "#{" + fieldName + ",jdbcType=" + jdbcType + "}";
  }

  private static String signature(String mapperShort, String method) {
    return "        /* " + mapperShort + "Mapper." + method + " */\n";
  }
}
