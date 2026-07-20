package com.scbk.sms.service.system.scaffold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mapper XML 템플릿에 전달할 구조화된 SQL 데이터를 계산한다. XML 렌더링은 공통 artifact renderer가 담당한다. */
public final class MapperXmlViewFactory {

  private static final Pattern SEARCH_VAR_PATTERN = Pattern.compile("\\$([a-zA-Z0-9_]+)");
  private static final Pattern COLUMN_COMPARE_PATTERN =
      Pattern.compile("([A-Za-z0-9_\\.]+)\\s*(<>|>=|<=|=|>|<)\\s*\\$([a-zA-Z0-9_]+)");
  private static final Pattern COLUMN_BETWEEN_PATTERN =
      Pattern.compile(
          "([A-Za-z0-9_\\.]+)\\s+BETWEEN\\s+\\$([a-zA-Z0-9_]+)\\s+AND\\s+\\$([a-zA-Z0-9_]+)",
          Pattern.CASE_INSENSITIVE);

  private MapperXmlViewFactory() {}

  public static MapperXmlView create(ScaffoldModel model) {
    if (model.includeCreateUpdate() && model.targetTable().isEmpty()) {
      throw new IllegalStateException(
          "CRUD 모드는 targetTable이 필요합니다. 조회 SQL에서 FROM 테이블을 추론할 수 없으면 수정 대상 테이블을 입력하세요.");
    }
    validateCrudModel(model);

    return new MapperXmlView(
        buildBaseQueryLines(model),
        buildSearchConditions(model),
        model.includeCreateUpdate() ? buildInsertValues(model) : List.of(),
        model.includeCreateUpdate() ? buildUpdateAssignments(model) : List.of(),
        model.includeCreateUpdate() ? buildPkBindings(model) : List.of(),
        model.hasLockColumn()
            ? bindParameter(model.beforeLockFieldName(), model.lockJavaType())
            : "");
  }

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
        if (c == '(') {
          depth++;
        } else if (c == ')') {
          depth--;
        } else if (depth == 0
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

  private static List<String> buildBaseQueryLines(ScaffoldModel model) {
    String[] parts = splitRawQuery(model.rawQuery());
    Map<String, ScaffoldModel.SearchParam> paramMap = buildParamMap(model);
    return parts[0]
        .lines()
        .filter(line -> !line.trim().isEmpty())
        .map(line -> buildBaseQueryLine(model, paramMap, line))
        .toList();
  }

  // baseQuery 라인 중 $variable이 포함된 라인(LEFT JOIN 서브쿼리 내부 등 depth>=1)은
  // searchConditions로 빼지 않고 baseQuery 안에서 #{var}로 파라미터화 + <if> 가드로 제자리 감싼다.
  // 집계 의미론을 보존하기 위해 서브쿼리를 분해하지 않고 그대로 둔다.
  // 서브쿼리 안의 WHERE 1=1 앵커가 선행 AND를 안전하게 만든다.
  private static String buildBaseQueryLine(
      ScaffoldModel model, Map<String, ScaffoldModel.SearchParam> paramMap, String line) {
    if (!line.contains("$")) {
      return escapeSqlText(line);
    }
    String trimmed = line.trim();
    if (!trimmed.regionMatches(true, 0, "AND ", 0, 4)) {
      throw new IllegalArgumentException(
          "서브쿼리 검색조건의 $variable은 WHERE 1=1 뒤의 AND 조건에서만 사용할 수 있습니다: " + trimmed);
    }
    List<String> lineVars = new ArrayList<>();
    Matcher matcher = SEARCH_VAR_PATTERN.matcher(line);
    while (matcher.find()) {
      lineVars.add(QueryColumnExtractor.toCamelCase(matcher.group(1)));
    }
    String test =
        lineVars.stream()
            .map(field -> field + " != null and " + field + " != ''")
            .reduce((left, right) -> left + " and " + right)
            .orElse("");
    String parameterized = replaceBindVariables(model, paramMap, line).trim();
    return "<if test=\"" + test + "\">" + escapeSqlTextPreservingCdata(parameterized) + "</if>";
  }

  private static Map<String, ScaffoldModel.SearchParam> buildParamMap(ScaffoldModel model) {
    Map<String, ScaffoldModel.SearchParam> paramMap = new HashMap<>();
    for (ScaffoldModel.SearchParam param : model.searchParams()) {
      paramMap.put(param.name(), param);
    }
    return paramMap;
  }

  private static List<SearchCondition> buildSearchConditions(ScaffoldModel model) {
    Map<String, ScaffoldModel.SearchParam> paramMap = buildParamMap(model);

    String[] parts = splitRawQuery(model.rawQuery());
    if (parts[1].trim().isEmpty()
        && model.getSearchVars().isEmpty()
        && !model.getColumns().isEmpty()) {
      String firstColumn = model.getColumns().get(0).trim().toUpperCase();
      return List.of(
          new SearchCondition(
              "searchKeyword != null and searchKeyword != ''",
              "AND A." + firstColumn + " LIKE '%' || #{searchKeyword} || '%'"));
    }

    List<SearchCondition> conditions = new ArrayList<>();
    for (String line : parts[1].split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.replace(" ", "").equalsIgnoreCase("1=1")) {
        continue;
      }

      if (!line.contains("$")) {
        String normalized = trimmed.toUpperCase().startsWith("AND ") ? trimmed : "AND " + trimmed;
        conditions.add(new SearchCondition("", escapeSqlText(normalized)));
        continue;
      }

      List<String> lineVars = new ArrayList<>();
      Matcher matcher = SEARCH_VAR_PATTERN.matcher(line);
      while (matcher.find()) {
        lineVars.add(QueryColumnExtractor.toCamelCase(matcher.group(1)));
      }
      String test =
          lineVars.stream()
              .map(field -> field + " != null and " + field + " != ''")
              .reduce((left, right) -> left + " and " + right)
              .orElse("");

      String normalized = replaceBindVariables(model, paramMap, line).trim();
      if (!normalized.toUpperCase().startsWith("AND ")) {
        normalized = "AND " + normalized;
      }
      conditions.add(new SearchCondition(test, normalized));
    }
    return List.copyOf(conditions);
  }

  private static String replaceBindVariables(
      ScaffoldModel model, Map<String, ScaffoldModel.SearchParam> paramMap, String line) {
    String betweenHandled = replaceBetweenClauses(model, paramMap, line);
    String comparisons =
        COLUMN_COMPARE_PATTERN
            .matcher(betweenHandled)
            .replaceAll(
                match -> {
                  String columnRef = match.group(1);
                  String operator = match.group(2);
                  String fieldName = QueryColumnExtractor.toCamelCase(match.group(3));
                  String replacement =
                      compareReplacement(
                          model, paramMap.get(fieldName), columnRef, operator, fieldName);
                  return Matcher.quoteReplacement(replacement);
                });

    return SEARCH_VAR_PATTERN
        .matcher(comparisons)
        .replaceAll(match -> "#{" + QueryColumnExtractor.toCamelCase(match.group(1)) + "}");
  }

  private static String replaceBetweenClauses(
      ScaffoldModel model, Map<String, ScaffoldModel.SearchParam> paramMap, String line) {
    return COLUMN_BETWEEN_PATTERN
        .matcher(line)
        .replaceAll(
            match -> {
              String columnRef = match.group(1);
              String fromField = QueryColumnExtractor.toCamelCase(match.group(2));
              String toField = QueryColumnExtractor.toCamelCase(match.group(3));
              String lower =
                  bindExpression(model, paramMap.get(fromField), columnRef, ">=", fromField);
              String upper = bindExpression(model, paramMap.get(toField), columnRef, "<=", toField);
              return Matcher.quoteReplacement(columnRef + " BETWEEN " + lower + " AND " + upper);
            });
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
    String javaType = model.getTypeMap().getOrDefault(columnName(columnRef).toUpperCase(), "");
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

  private static String escapeSqlTextPreservingCdata(String sqlLine) {
    return escapeSqlText(sqlLine)
        .replace("&lt;![CDATA[ &gt;= ]]&gt;", "<![CDATA[ >= ]]>")
        .replace("&lt;![CDATA[ &lt;= ]]&gt;", "<![CDATA[ <= ]]>")
        .replace("&lt;![CDATA[ &gt; ]]&gt;", "<![CDATA[ > ]]>")
        .replace("&lt;![CDATA[ &lt; ]]&gt;", "<![CDATA[ < ]]>");
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

  private static List<SqlBinding> buildInsertValues(ScaffoldModel model) {
    List<SqlBinding> values = new ArrayList<>();
    for (ScaffoldModel.ColumnConfig column : model.editableColumns()) {
      values.add(
          new SqlBinding(
              column.columnName(), bindParameter(column.fieldName(), column.javaType())));
    }
    if (hasColumn(model, "REG_DTTM")) {
      values.add(new SqlBinding("REG_DTTM", currentValueExpression(model, "REG_DTTM")));
    }
    if (model.hasLockColumn()) {
      values.add(
          new SqlBinding(model.lockColumn(), currentValueExpression(model, model.lockColumn())));
    } else if (hasColumn(model, "UPD_DTTM")) {
      values.add(new SqlBinding("UPD_DTTM", currentValueExpression(model, "UPD_DTTM")));
    }
    if (values.isEmpty()) {
      throw new IllegalStateException("CRUD INSERT를 생성할 수정 가능 컬럼이 없습니다. 컬럼 옵션에서 등록/수정할 컬럼을 선택하세요.");
    }
    return List.copyOf(values);
  }

  private static List<SqlBinding> buildUpdateAssignments(ScaffoldModel model) {
    List<SqlBinding> assignments = new ArrayList<>();
    for (ScaffoldModel.ColumnConfig column : model.editableColumns()) {
      assignments.add(
          new SqlBinding(
              column.columnName(), bindParameter(column.fieldName(), column.javaType())));
    }
    if (model.hasLockColumn()) {
      assignments.add(
          new SqlBinding(model.lockColumn(), currentValueExpression(model, model.lockColumn())));
    }
    if (assignments.isEmpty()) {
      String firstPk = model.pkColumn().isEmpty() ? "ID" : model.pkColumn();
      assignments.add(new SqlBinding(firstPk, firstPk));
    }
    return List.copyOf(assignments);
  }

  private static List<SqlBinding> buildPkBindings(ScaffoldModel model) {
    if (model.pkColumns().isEmpty()) {
      throw new IllegalStateException("CRUD mode requires at least one PK column.");
    }
    return model.pkFields().stream()
        .map(pk -> new SqlBinding(pk.columnName(), bindParameter(pk.fieldName(), pk.javaType())))
        .toList();
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

  private static void validateCrudModel(ScaffoldModel model) {
    if (model.includeCreateUpdate()
        && model.hasLockColumn()
        && model.pkColumns().contains(model.lockColumn())) {
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
    return jdbcType.isEmpty()
        ? "#{" + fieldName + "}"
        : "#{" + fieldName + ",jdbcType=" + jdbcType + "}";
  }

  public record MapperXmlView(
      List<String> baseQueryLines,
      List<SearchCondition> searchConditions,
      List<SqlBinding> insertValues,
      List<SqlBinding> updateAssignments,
      List<SqlBinding> pkBindings,
      String beforeLockBinding) {}

  public record SearchCondition(String test, String sql) {
    public boolean conditional() {
      return !test.isEmpty();
    }
  }

  public record SqlBinding(String column, String expression) {}
}
