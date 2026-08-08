package com.scbk.sms.service.system.scaffold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

/**
 * QuerySpec을 Mapper XML 템플릿이 바로 반복 출력할 수 있는 구조화된 SQL 조각으로 변환한다.
 *
 * <p>단순 직접 SELECT만 검색조건을 원본 alias 안쪽에 배치하고 직접 COUNT로 최적화한다. JOIN 파생식, 집계, 서브쿼리, 집합 연산처럼
 * 의미가 달라질 수 있는 쿼리는 외부 래퍼 구조를 유지한다. CRUD SQL은 {@link ScaffoldModel}이 검증한 실제 PK와 수정 허용 컬럼만 사용한다.
 * 이 클래스는 SQL 구조를 계산할 뿐 XML 문자열 렌더링은 {@link ScaffoldArtifactRenderer}에 맡긴다.
 */
public final class MapperXmlViewFactory {

  private static final Pattern SEARCH_VAR_PATTERN = Pattern.compile("\\$([a-zA-Z0-9_]+)");
  private static final Pattern COLUMN_COMPARE_PATTERN =
      Pattern.compile("([A-Za-z0-9_\\.]+)\\s*(<>|>=|<=|=|>|<)\\s*\\$([a-zA-Z0-9_]+)");
  private static final Pattern COLUMN_BETWEEN_PATTERN =
      Pattern.compile(
          "([A-Za-z0-9_\\.]+)\\s+BETWEEN\\s+\\$([a-zA-Z0-9_]+)\\s+AND\\s+\\$([a-zA-Z0-9_]+)",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern SUBQUERY_PATTERN =
      Pattern.compile("\\(\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern AGGREGATE_PATTERN =
      Pattern.compile(
          "\\b(?:COUNT|SUM|AVG|MIN|MAX|LISTAGG|STRING_AGG|ARRAY_AGG|JSON_AGG|JSON_ARRAYAGG|XMLAGG|STDDEV|VARIANCE|GROUPING)\\s*\\(",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern WINDOW_PATTERN =
      Pattern.compile("\\bOVER\\s*\\(", Pattern.CASE_INSENSITIVE);

  private MapperXmlViewFactory() {}

  /**
   * 조회·검색·CRUD 바인딩을 하나의 Mapper XML view로 구성한다.
   *
   * @throws IllegalStateException CRUD 대상 테이블이나 PK/수정 컬럼 계약이 불완전한 경우
   */
  public static MapperXmlView create(ScaffoldModel model) {
    if (model.includeCreateUpdate() && model.targetTable().isEmpty()) {
      throw new IllegalStateException(
          "CRUD 모드는 targetTable이 필요합니다. 조회 SQL에서 FROM 테이블을 추론할 수 없으면 수정 대상 테이블을 입력하세요.");
    }
    validateCrudModel(model);
    String[] queryParts = splitRawQuery(model.rawQuery());
    QueryShape queryShape = analyzeQuery(model.rawQuery(), !queryParts[1].trim().isEmpty());

    return new MapperXmlView(
        buildBaseQueryLines(model, queryParts[0]),
        buildSearchConditions(model, queryParts[1], queryShape),
        queryShape.searchConditionsInsideBaseQuery(),
        queryShape.usesDirectCount(),
        queryShape.directCountFromClause(),
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

  private static QueryShape analyzeQuery(String rawQuery, boolean hasSeparatedWhere) {
    try {
      net.sf.jsqlparser.statement.Statement statement =
          CCJSqlParserUtil.parse(safeParseQuery(rawQuery));
      if (!(statement instanceof Select select) || select.getPlainSelect() == null) {
        return QueryShape.outerSafe();
      }
      PlainSelect plainSelect = select.getPlainSelect();
      // 의미론이 확실한 단순 행 조회에서만 내부 검색조건/직접 COUNT 최적화를 허용한다.
      if (!isSimpleRowQuery(select, plainSelect, rawQuery)) {
        return QueryShape.outerSafe();
      }

      String defaultSearchColumn = firstDirectColumn(plainSelect);
      if (!hasSeparatedWhere && defaultSearchColumn.isEmpty()) {
        return QueryShape.outerSafe();
      }
      String directCountFromClause =
          hasItems(plainSelect.getJoins()) ? "" : "FROM " + plainSelect.getFromItem();
      return new QueryShape(
          SearchConditionPlacement.INNER, defaultSearchColumn, directCountFromClause);
    } catch (Exception ignored) {
      return QueryShape.outerSafe();
    }
  }

  private static boolean isSimpleRowQuery(
      Select select, PlainSelect plainSelect, String rawQuery) {
    if (!(plainSelect.getFromItem() instanceof Table)
        || hasNonTableJoin(plainSelect.getJoins())
        || hasNonDirectSelectItem(plainSelect.getSelectItems())
        || hasItems(select.getWithItemsList())
        || plainSelect.getDistinct() != null
        || plainSelect.getGroupBy() != null
        || plainSelect.getHaving() != null
        || plainSelect.getQualify() != null
        || plainSelect.getOracleHierarchical() != null
        || hasItems(select.getOrderByElements())
        || select.getLimit() != null
        || select.getLimitBy() != null
        || select.getOffset() != null
        || select.getFetch() != null
        || select.getForClause() != null
        || plainSelect.getTop() != null
        || plainSelect.getSkip() != null
        || plainSelect.getFirst() != null
        || hasItems(plainSelect.getWindowDefinitions())) {
      return false;
    }

    String safeQuery = safeParseQuery(rawQuery);
    return !SUBQUERY_PATTERN.matcher(safeQuery).find()
        && !AGGREGATE_PATTERN.matcher(safeQuery).find()
        && !WINDOW_PATTERN.matcher(safeQuery).find();
  }

  private static boolean hasNonTableJoin(List<Join> joins) {
    return joins != null && joins.stream().anyMatch(join -> !(join.getRightItem() instanceof Table));
  }

  private static boolean hasNonDirectSelectItem(List<SelectItem<?>> selectItems) {
    return selectItems == null
        || selectItems.isEmpty()
        || selectItems.stream().anyMatch(item -> !(item.getExpression() instanceof Column));
  }

  private static boolean hasItems(List<?> items) {
    return items != null && !items.isEmpty();
  }

  private static String firstDirectColumn(PlainSelect plainSelect) {
    List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
    if (selectItems == null || selectItems.isEmpty()) {
      return "";
    }
    return selectItems.get(0).getExpression() instanceof Column column ? column.toString() : "";
  }

  private static String safeParseQuery(String query) {
    return query == null ? "" : query.replaceAll("\\$([a-zA-Z0-9_]+)", "NULL");
  }

  private static List<String> buildBaseQueryLines(ScaffoldModel model, String baseQuery) {
    Map<String, ScaffoldModel.SearchParam> paramMap = buildParamMap(model);
    return baseQuery
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
            .distinct()
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

  private static List<SearchCondition> buildSearchConditions(
      ScaffoldModel model,
      String rawConditions,
      QueryShape queryShape) {
    Map<String, ScaffoldModel.SearchParam> paramMap = buildParamMap(model);

    if (rawConditions.trim().isEmpty()
        && model.getSearchVars().isEmpty()
        && !model.getColumns().isEmpty()) {
      String firstColumn = model.getColumns().get(0).trim().toUpperCase();
      String searchColumn =
          queryShape.searchConditionsInsideBaseQuery()
              ? queryShape.defaultSearchColumn()
              : "A." + firstColumn;
      return List.of(
          new SearchCondition(
              "searchKeyword != null and searchKeyword != ''",
              "AND " + searchColumn + " LIKE '%' || #{searchKeyword} || '%'"));
    }

    List<SearchCondition> conditions = new ArrayList<>();
    for (String line : rawConditions.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.replace(" ", "").equalsIgnoreCase("1=1")) {
        continue;
      }

      if (!line.contains("$")) {
        String normalized = trimmed.toUpperCase().startsWith("AND ") ? trimmed : "AND " + trimmed;
        conditions.add(
            new SearchCondition(
                "", escapeSqlText(normalizeSearchAliases(model, normalized, queryShape))));
        continue;
      }

      List<String> lineVars = new ArrayList<>();
      Matcher matcher = SEARCH_VAR_PATTERN.matcher(line);
      while (matcher.find()) {
        lineVars.add(QueryColumnExtractor.toCamelCase(matcher.group(1)));
      }
      String test =
          lineVars.stream()
              .distinct()
              .map(field -> field + " != null and " + field + " != ''")
              .reduce((left, right) -> left + " and " + right)
              .orElse("");

      String normalized = replaceBindVariables(model, paramMap, line).trim();
      if (!normalized.toUpperCase().startsWith("AND ")) {
        normalized = "AND " + normalized;
      }
      conditions.add(
          new SearchCondition(test, normalizeSearchAliases(model, normalized, queryShape)));
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

  private static String normalizeOuterAliases(ScaffoldModel model, String condition) {
    String normalized = condition;
    for (String column : model.getColumns()) {
      String columnName = column.trim().toUpperCase();
      normalized =
          normalized.replaceAll(
              "(?i)\\b[A-Z][A-Z0-9_]*\\." + Pattern.quote(columnName) + "\\b", "A." + columnName);
    }
    return normalized;
  }

  private static String normalizeSearchAliases(
      ScaffoldModel model,
      String condition,
      QueryShape queryShape) {
    return queryShape.searchConditionsInsideBaseQuery()
        ? condition
        : normalizeOuterAliases(model, condition);
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
              ScaffoldModel.SearchParam upperParam = paramMap.get(toField);
              String javaType =
                  model.getTypeMap().getOrDefault(columnName(columnRef).toUpperCase(), "");
              if (upperParam != null && upperParam.isDate() && "LocalDateTime".equals(javaType)) {
                String upperStart = model.dialect().timestampExpression(toField, "000000");
                return Matcher.quoteReplacement(
                    columnRef
                        + " "
                        + xmlOperator(">=")
                        + " "
                        + lower
                        + " AND "
                        + columnRef
                        + " "
                        + xmlOperator("<")
                        + " "
                        + model.dialect().plusOneDay(upperStart));
              }
              if (upperParam != null && upperParam.isDate() && "LocalDate".equals(javaType)) {
                String upperStart = model.dialect().dateExpression(toField);
                return Matcher.quoteReplacement(
                    columnRef
                        + " "
                        + xmlOperator(">=")
                        + " "
                        + lower
                        + " AND "
                        + columnRef
                        + " "
                        + xmlOperator("<")
                        + " "
                        + model.dialect().plusOneDay(upperStart));
              }
              String upper = bindExpression(model, upperParam, columnRef, "<=", toField);
              return Matcher.quoteReplacement(columnRef + " BETWEEN " + lower + " AND " + upper);
            });
  }

  private static String compareReplacement(
      ScaffoldModel model,
      ScaffoldModel.SearchParam param,
      String columnRef,
      String operator,
      String fieldName) {
    if (param == null || !param.isDate()) {
      return columnRef
          + " "
          + xmlOperator(operator)
          + " "
          + bindExpression(model, param, columnRef, operator, fieldName);
    }

    String javaType = model.getTypeMap().getOrDefault(columnName(columnRef).toUpperCase(), "");
    if (isUpperBound(operator, fieldName) && "LocalDateTime".equals(javaType)) {
      String upperStart = model.dialect().timestampExpression(fieldName, "000000");
      return columnRef + " " + xmlOperator("<") + " " + model.dialect().plusOneDay(upperStart);
    }
    if (isUpperBound(operator, fieldName) && "LocalDate".equals(javaType)) {
      String upperStart = model.dialect().dateExpression(fieldName);
      return columnRef + " " + xmlOperator("<") + " " + model.dialect().plusOneDay(upperStart);
    }
    if (!"=".equals(operator)) {
      return columnRef
          + " "
          + xmlOperator(operator)
          + " "
          + bindExpression(model, param, columnRef, operator, fieldName);
    }
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
      return model.dialect().timestampExpression(fieldName, "000000");
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
      boolean searchConditionsInsideBaseQuery,
      boolean usesDirectCount,
      String directCountFromClause,
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

  private enum SearchConditionPlacement {
    INNER,
    OUTER_SAFE
  }

  private record QueryShape(
      SearchConditionPlacement searchConditionPlacement,
      String defaultSearchColumn,
      String directCountFromClause) {

    private static QueryShape outerSafe() {
      return new QueryShape(SearchConditionPlacement.OUTER_SAFE, "", "");
    }

    private boolean searchConditionsInsideBaseQuery() {
      return searchConditionPlacement == SearchConditionPlacement.INNER;
    }

    private boolean usesDirectCount() {
      return !directCountFromClause.isEmpty();
    }
  }
}
