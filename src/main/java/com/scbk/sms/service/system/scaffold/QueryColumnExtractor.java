package com.scbk.sms.service.system.scaffold;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

/**
 * QuerySpec 원문에서 출력 컬럼, 대상 테이블, {@code $검색변수}와 원본 컬럼 연결을 추출한다.
 *
 * <p>구조 판단은 JSQLParser 결과를 우선한다. 컬럼 목록처럼 안전한 축약 결과를 만들 수 있는 경우에만 제한적인 정규식 fallback을 사용하며,
 * 계산식의 원본 컬럼처럼 확정할 수 없는 정보는 추측하지 않고 비워 둔다.
 */
public final class QueryColumnExtractor {

  private static final Pattern SEARCH_VAR_PATTERN = Pattern.compile("\\$([a-zA-Z0-9_]+)");
  private static final Pattern SELECT_PART_PATTERN =
      Pattern.compile("SELECT(.*?)\\s+FROM\\s", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern FROM_TABLE_PATTERN =
      Pattern.compile(
          "\\bFROM\\s+((?:\"?[A-Za-z][A-Za-z0-9_]*\"?\\.)?\"?[A-Za-z][A-Za-z0-9_]*\"?)\\b",
          Pattern.CASE_INSENSITIVE);
  private static final String SQL_IDENTIFIER = "\\\"?[A-Za-z][A-Za-z0-9_]*\\\"?";
  private static final Pattern BETWEEN_SEARCH_SOURCE_PATTERN =
      Pattern.compile(
          "(?:"
              + SQL_IDENTIFIER
              + "\\.)?("
              + SQL_IDENTIFIER
              + ")\\s+BETWEEN\\s+\\$([a-zA-Z0-9_]+)\\s+AND\\s+\\$([a-zA-Z0-9_]+)",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern DIRECT_SEARCH_SOURCE_PATTERN =
      Pattern.compile(
          "(?:"
              + SQL_IDENTIFIER
              + "\\.)?("
              + SQL_IDENTIFIER
              + ")\\s*(?:>=|<=|<>|!=|=|>|<|LIKE\\b|IN\\s*\\()[^\\r\\n;]*?\\$([a-zA-Z0-9_]+)",
          Pattern.CASE_INSENSITIVE);

  private QueryColumnExtractor() {}

  /** SELECT 출력 컬럼명을 순서대로 추출한다. 명시적 alias가 있으면 원본 컬럼명보다 우선한다. */
  public static List<String> extractColumns(String query) {
    if (query == null || query.trim().isEmpty()) {
      return new ArrayList<>();
    }

    String safeQuery = safeParseQuery(query);

    try {
      net.sf.jsqlparser.statement.Statement stmt = CCJSqlParserUtil.parse(safeQuery);
      if (!(stmt instanceof Select select) || select.getPlainSelect() == null) {
        throw new IllegalStateException("PlainSelect 구조가 아닙니다.");
      }
      PlainSelect plainSelect = select.getPlainSelect();

      List<String> columns = new ArrayList<>();
      for (SelectItem<?> item : plainSelect.getSelectItems()) {
        String alias = item.getAlias() != null ? item.getAlias().getName() : null;
        if (alias != null) {
          columns.add(alias.replaceAll("^\"|\"$", ""));
        } else if (item.getExpression() instanceof Column column) {
          columns.add(column.getColumnName());
        } else {
          String expr = item.getExpression().toString();
          String last = lastIdentifier(expr);
          if (!last.isEmpty()) {
            columns.add(last);
          }
        }
      }
      return columns;
    } catch (Exception e) {
      return extractColumnsFallback(query);
    }
  }

  /** SELECT 결과 컬럼(alias 우선)을 원본 테이블 컬럼에 연결한다. 계산식은 원본 컬럼을 확정할 수 없어 제외한다. */
  public static Map<String, String> extractDirectColumnSources(String query) {
    Map<String, String> sources = new LinkedHashMap<>();
    if (query == null || query.trim().isEmpty()) {
      return sources;
    }

    try {
      net.sf.jsqlparser.statement.Statement stmt = CCJSqlParserUtil.parse(safeParseQuery(query));
      if (!(stmt instanceof Select select) || select.getPlainSelect() == null) {
        return sources;
      }
      for (SelectItem<?> item : select.getPlainSelect().getSelectItems()) {
        if (!(item.getExpression() instanceof Column column)) {
          continue;
        }
        String outputName =
            item.getAlias() != null ? item.getAlias().getName() : column.getColumnName();
        sources.put(normalizeColumn(outputName), normalizeColumn(column.getColumnName()));
      }
    } catch (Exception ignored) {
      // 파싱이 불가능하면 출력 컬럼명과 DB 컬럼명이 같은 경우만 호출부의 fallback으로 처리한다.
    }
    return sources;
  }

  /** CRUD 기준 테이블을 추출한다. 조인/서브쿼리 화면은 화면에서 targetTable로 명시한다. */
  public static String extractPrimaryTable(String query) {
    if (query == null || query.trim().isEmpty()) {
      return "";
    }

    try {
      net.sf.jsqlparser.statement.Statement stmt = CCJSqlParserUtil.parse(safeParseQuery(query));
      if (stmt instanceof Select select && select.getPlainSelect() != null) {
        PlainSelect plainSelect = select.getPlainSelect();
        if (plainSelect.getFromItem() instanceof Table table) {
          return normalizeIdentifier(table.getFullyQualifiedName());
        }
      }
    } catch (Exception ignored) {
      // fallback below
    }

    Matcher matcher = FROM_TABLE_PATTERN.matcher(query);
    if (matcher.find()) {
      return normalizeIdentifier(matcher.group(1));
    }
    return "";
  }

  /** {@code $변수}를 중복 없이 등장 순서대로 추출하고 camelCase로 변환한다. */
  public static List<String> extractSearchVars(String query) {
    List<String> vars = new ArrayList<>();
    if (query == null || query.trim().isEmpty()) {
      return vars;
    }
    Matcher matcher = SEARCH_VAR_PATTERN.matcher(query);
    while (matcher.find()) {
      String varName = toCamelCase(matcher.group(1));
      if (!vars.contains(varName)) {
        vars.add(varName);
      }
    }
    return vars;
  }

  /** 직접 검색조건의 $변수를 원본 컬럼에 연결하고 BETWEEN 시작/종료 역할을 함께 반환한다. */
  public static Map<String, SearchParameterSource> extractSearchParameterSources(String query) {
    Map<String, SearchParameterSource> sources = new LinkedHashMap<>();
    if (query == null || query.trim().isEmpty()) {
      return sources;
    }

    Matcher betweenMatcher = BETWEEN_SEARCH_SOURCE_PATTERN.matcher(query);
    while (betweenMatcher.find()) {
      String columnName = normalizeColumn(betweenMatcher.group(1));
      sources.put(
          toCamelCase(betweenMatcher.group(2)),
          new SearchParameterSource(columnName, SearchRangePosition.START));
      sources.put(
          toCamelCase(betweenMatcher.group(3)),
          new SearchParameterSource(columnName, SearchRangePosition.END));
    }

    Matcher directMatcher = DIRECT_SEARCH_SOURCE_PATTERN.matcher(query);
    while (directMatcher.find()) {
      String varName = toCamelCase(directMatcher.group(2));
      sources.putIfAbsent(
          varName,
          new SearchParameterSource(
              normalizeColumn(directMatcher.group(1)), SearchRangePosition.NONE));
    }
    return sources;
  }

  /** snake_case를 MyBatis의 map-underscore-to-camel-case 규칙과 같은 camelCase로 변환한다. */
  public static String toCamelCase(String value) {
    String[] parts = value.toLowerCase().split("_");
    StringBuilder sb = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      if (parts[i].isEmpty()) {
        continue;
      }
      sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
    }
    return sb.toString();
  }

  private static List<String> extractColumnsFallback(String query) {
    List<String> columns = new ArrayList<>();
    Matcher matcher = SELECT_PART_PATTERN.matcher(query);
    if (!matcher.find()) {
      return columns;
    }
    for (String part : splitTopLevel(matcher.group(1))) {
      part = part.trim();
      if (part.isEmpty()) {
        continue;
      }
      String[] tokens = part.split("\\s+");
      String colName = tokens[tokens.length - 1];
      if (colName.contains(".")) {
        colName = colName.substring(colName.lastIndexOf(".") + 1);
      }
      columns.add(colName);
    }
    return columns;
  }

  private static String safeParseQuery(String query) {
    return query
        .replaceAll("#\\{[^}]+\\}", "NULL")
        .replaceAll("\\$\\{[^}]+\\}", "NULL")
        .replaceAll("\\$([a-zA-Z0-9_]+)", "NULL");
  }

  private static String normalizeIdentifier(String value) {
    return value == null ? "" : value.replace("\"", "").trim().toUpperCase();
  }

  private static String normalizeColumn(String value) {
    return value == null
        ? ""
        : value.replaceAll("^\"|\"$", "").trim().toUpperCase(Locale.ROOT);
  }

  private static List<String> splitTopLevel(String text) {
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int depth = 0;
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '\'' && !inDoubleQuote) {
        inSingleQuote = !inSingleQuote;
      } else if (ch == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
      } else if (!inSingleQuote && !inDoubleQuote) {
        if (ch == '(') {
          depth++;
        } else if (ch == ')' && depth > 0) {
          depth--;
        } else if (ch == ',' && depth == 0) {
          parts.add(current.toString());
          current.setLength(0);
          continue;
        }
      }
      current.append(ch);
    }
    parts.add(current.toString());
    return parts;
  }

  private static String lastIdentifier(String expression) {
    String last = "";
    for (String token : expression.split("[^a-zA-Z0-9_]")) {
      if (!token.trim().isEmpty()) {
        last = token.trim();
      }
    }
    return last;
  }

  public enum SearchRangePosition {
    NONE,
    START,
    END
  }

  public record SearchParameterSource(
      String columnName, SearchRangePosition rangePosition) {}
}
