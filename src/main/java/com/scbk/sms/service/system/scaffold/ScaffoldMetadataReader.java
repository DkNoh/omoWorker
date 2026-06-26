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

@Component
@Profile("local")
public class ScaffoldMetadataReader {

  private final DataSource dataSource;

  public ScaffoldMetadataReader(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public ScaffoldTableMetadata read(String targetTable) {
    TableName tableName = TableName.parse(targetTable);
    if (!StringUtils.hasText(tableName.table())) {
      return new ScaffoldTableMetadata(List.of(), Map.of());
    }

    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      TableName resolved = resolveTableName(metaData, tableName);
      return new ScaffoldTableMetadata(
          readPrimaryKeys(metaData, resolved), readNullability(metaData, resolved));
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

  private Map<String, Boolean> readNullability(DatabaseMetaData metaData, TableName tableName)
      throws SQLException {
    Map<String, Boolean> nullableByColumn = new LinkedHashMap<>();
    try (ResultSet rs =
        metaData.getColumns(tableName.catalog(), tableName.schema(), tableName.table(), null)) {
      while (rs.next()) {
        String columnName = normalizeColumn(rs.getString("COLUMN_NAME"));
        boolean nullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
        nullableByColumn.put(columnName, nullable);
      }
    }
    return nullableByColumn;
  }

  private static String normalizeColumn(String columnName) {
    return columnName == null ? "" : columnName.trim().toUpperCase(Locale.ROOT);
  }

  private record PkColumn(short seq, String columnName) {}

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
