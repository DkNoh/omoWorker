package com.scbk.sms.service.system.scaffold;

import com.scbk.sms.config.ScaffoldProperties;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 원쿼리를 ROWNUM = 0으로 감싸 실행해 ResultSetMetaData로 컬럼 타입을 추론한다. 실제 DB 근거 우선 원칙에 따라, 추론 실패 시 String으로
 * 강행하지 않고 오류를 보고한다.
 */
@Component
@Profile("local")
public class ColumnTypeInferrer {

  private final JdbcTemplate jdbcTemplate;
  private final ScaffoldProperties scaffoldProperties;

  public ColumnTypeInferrer(JdbcTemplate jdbcTemplate, ScaffoldProperties scaffoldProperties) {
    this.jdbcTemplate = jdbcTemplate;
    this.scaffoldProperties = scaffoldProperties;
  }

  public Map<String, String> inferTypes(String rawQuery, List<String> columns) {
    Map<String, String> typeMap = new LinkedHashMap<>();
    columns.forEach(column -> typeMap.put(column, "String"));

    if (rawQuery == null || rawQuery.trim().isEmpty() || columns.isEmpty()) {
      return typeMap;
    }

    String safeQuery =
        rawQuery
            .replaceAll("(?i)<[^>]+>", " ")
            .replaceAll("#\\{[^}]+\\}", "NULL")
            .replaceAll("\\$\\{[^}]+\\}", "NULL")
            .replaceAll("\\$([a-zA-Z0-9_]+)", "NULL")
            .replaceAll("(?m)^\\s*AND\\s*$", "")
            .replaceAll("(?m)^\\s*OR\\s*$", "")
            .replaceAll("(?i)WHERE\\s+(AND|OR)\\b", "WHERE 1=1")
            .trim();
    ScaffoldDialect dialect = scaffoldProperties.dialect();
    String wrappedQuery = dialect.emptyResultQuery(safeQuery);

    try {
      jdbcTemplate.execute(
          (java.sql.Connection conn) -> {
            try (PreparedStatement ps = conn.prepareStatement(wrappedQuery);
                ResultSet rs = ps.executeQuery()) {
              ResultSetMetaData meta = rs.getMetaData();
              for (int i = 1; i <= meta.getColumnCount(); i++) {
                String label = meta.getColumnLabel(i);
                String javaType =
                    dialect.javaType(
                        meta.getColumnTypeName(i),
                        meta.getColumnType(i),
                        meta.getPrecision(i),
                        meta.getScale(i));
                String matched = findMatchingColumn(columns, label);
                if (matched != null) {
                  typeMap.put(matched, javaType);
                }
              }
            }
            return null;
          });
    } catch (Exception e) {
      // 타입 추론 실패를 String으로 강행하지 않는다. 쿼리를 수정해서 다시 시도한다.
      // ORA-XXXXX 원문은 cause 체인 안쪽에 있으므로 루트 원인을 화면 메시지로 노출한다.
      throw new IllegalStateException("컬럼 타입 추론에 실패했습니다. 쿼리를 확인하세요: " + rootCauseMessage(e), e);
    }
    return typeMap;
  }

  private String rootCauseMessage(Throwable e) {
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root.getMessage();
  }

  private String findMatchingColumn(List<String> columns, String label) {
    for (String column : columns) {
      if (column.equalsIgnoreCase(label)) {
        return column;
      }
    }
    return null;
  }
}
