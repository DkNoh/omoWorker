package com.scbk.sms.service.system.scaffold;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 대상 테이블의 PK 순서, nullable 여부와 column comment를 JDBC {@link DatabaseMetaData}에서 읽는다.
 *
 * <p>DB 제품과 드라이버의 대소문자 보고 차이를 흡수하기 위해 입력 표기, 대문자, 소문자 후보를 순서대로 조회한다. 메타데이터 실패를 빈 값으로
 * 숨기면 잘못된 CRUD SQL이 생성될 수 있으므로 SQL 예외는 대상 테이블과 함께 즉시 보고한다.
 */
@Component
@Profile("local")
public class ScaffoldMetadataReader {

  private final DataSource dataSource;

  public ScaffoldMetadataReader(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** 스키마가 포함될 수 있는 테이블명을 해석하고 실제 DB 메타데이터를 하나의 값 객체로 반환한다. */
  public ScaffoldTableMetadata read(String targetTable) {
    TableName tableName = TableName.parse(targetTable);
    if (!StringUtils.hasText(tableName.table())) {
      return new ScaffoldTableMetadata(List.of(), Map.of(), Map.of());
    }

    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      TableName resolved = resolveTableName(metaData, tableName);
      ColumnMetadata columns = readColumns(metaData, resolved);
      return new ScaffoldTableMetadata(
          readPrimaryKeys(metaData, resolved),
          columns.nullableByColumn(),
          columns.commentsByColumn());
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Failed to read table metadata for " + targetTable + ": " + e.getMessage(), e);
    }
  }

  private TableName resolveTableName(DatabaseMetaData metaData, TableName tableName)
      throws SQLException {
    List<TableName> candidates = tableName.candidates();
    for (TableName candidate : candidates) {
      if (tableExists(metaData, candidate)) {
        return candidate;
      }
    }
    return tableName;
  }

  private boolean tableExists(DatabaseMetaData metaData, TableName tableName) throws SQLException {
    try (ResultSet rs =
        metaData.getTables(tableName.catalog(), tableName.schema(), tableName.table(), null)) {
      return rs.next();
    }
  }

  private List<String> readPrimaryKeys(DatabaseMetaData metaData, TableName tableName)
      throws SQLException {
    List<PkColumn> pkColumns = new ArrayList<>();
    try (ResultSet rs =
        metaData.getPrimaryKeys(tableName.catalog(), tableName.schema(), tableName.table())) {
      while (rs.next()) {
        pkColumns.add(
            new PkColumn(rs.getShort("KEY_SEQ"), normalizeColumn(rs.getString("COLUMN_NAME"))));
      }
    }
    return pkColumns.stream()
        .sorted(Comparator.comparingInt(PkColumn::seq))
        .map(PkColumn::columnName)
        .toList();
  }

  private ColumnMetadata readColumns(DatabaseMetaData metaData, TableName tableName)
      throws SQLException {
    Map<String, Boolean> nullableByColumn = new LinkedHashMap<>();
    Map<String, String> commentsByColumn = new LinkedHashMap<>();
    try (ResultSet rs =
        metaData.getColumns(tableName.catalog(), tableName.schema(), tableName.table(), null)) {
      while (rs.next()) {
        String columnName = normalizeColumn(rs.getString("COLUMN_NAME"));
        boolean nullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
        nullableByColumn.put(columnName, nullable);
        String remarks = rs.getString("REMARKS");
        if (StringUtils.hasText(remarks)) {
          commentsByColumn.put(columnName, remarks.trim());
        }
      }
    }
    return new ColumnMetadata(nullableByColumn, commentsByColumn);
  }

  private static String normalizeColumn(String columnName) {
    return columnName == null ? "" : columnName.trim().toUpperCase(Locale.ROOT);
  }

  private record PkColumn(short seq, String columnName) {}

  private record ColumnMetadata(
      Map<String, Boolean> nullableByColumn, Map<String, String> commentsByColumn) {}

  private record TableName(String catalog, String schema, String table) {
    static TableName parse(String raw) {
      if (!StringUtils.hasText(raw)) {
        return new TableName(null, null, "");
      }
      String cleaned = raw.trim().replace("\"", "");
      String[] parts = cleaned.split("\\.");
      if (parts.length == 3) {
        return new TableName(normalize(parts[0]), normalize(parts[1]), normalize(parts[2]));
      }
      if (parts.length == 2) {
        return new TableName(null, normalize(parts[0]), normalize(parts[1]));
      }
      return new TableName(null, null, normalize(cleaned));
    }

    List<TableName> candidates() {
      List<TableName> result = new ArrayList<>();
      result.add(this);
      result.add(new TableName(catalog, upper(schema), upper(table)));
      result.add(new TableName(catalog, lower(schema), lower(table)));
      return result.stream().distinct().toList();
    }

    private static String normalize(String value) {
      return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String upper(String value) {
      return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private static String lower(String value) {
      return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
  }
}
