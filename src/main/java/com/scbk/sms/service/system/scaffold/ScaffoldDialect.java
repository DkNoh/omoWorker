package com.scbk.sms.service.system.scaffold;

import java.sql.Types;
import java.util.Locale;

/** SQL fragments used by scaffold generation. */
public enum ScaffoldDialect {
  ORACLE {
    @Override
    public String emptyResultQuery(String sql) {
      return "SELECT * FROM (" + sql + ") WHERE ROWNUM = 0";
    }

    @Override
    public String pageClause() {
      return "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY";
    }

    @Override
    public String currentTimestamp() {
      return "SYSTIMESTAMP";
    }

    @Override
    public String currentDate() {
      return "TRUNC(SYSDATE)";
    }

    @Override
    public String currentTimestampString() {
      return "TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
    }

    @Override
    public String dateExpression(String fieldName) {
      return "TO_DATE(#{" + fieldName + "}, 'YYYYMMDD')";
    }

    @Override
    public String timestampExpression(String fieldName, String suffix) {
      return "TO_TIMESTAMP(#{" + fieldName + "} || '" + suffix + "', 'YYYYMMDDHH24MISS')";
    }

    @Override
    public String plusOneDay(String expression) {
      return expression + " + INTERVAL '1' DAY";
    }
  },
  POSTGRES {
    @Override
    public String emptyResultQuery(String sql) {
      return "SELECT * FROM (" + sql + ") scaffold_src WHERE 1 = 0";
    }

    @Override
    public String pageClause() {
      return "OFFSET #{offset} LIMIT #{size}";
    }

    @Override
    public String currentTimestamp() {
      return "CURRENT_TIMESTAMP";
    }

    @Override
    public String currentDate() {
      return "CURRENT_DATE";
    }

    @Override
    public String currentTimestampString() {
      return "TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDDHH24MISS')";
    }

    @Override
    public String dateExpression(String fieldName) {
      return "TO_DATE(#{" + fieldName + "}, 'YYYYMMDD')";
    }

    @Override
    public String timestampExpression(String fieldName, String suffix) {
      return "TO_TIMESTAMP(#{" + fieldName + "} || '" + suffix + "', 'YYYYMMDDHH24MISS')";
    }

    @Override
    public String plusOneDay(String expression) {
      return expression + " + INTERVAL '1 day'";
    }
  },
  DB2 {
    @Override
    public String emptyResultQuery(String sql) {
      return "SELECT * FROM (" + sql + ") scaffold_src FETCH FIRST 0 ROWS ONLY";
    }

    @Override
    public String pageClause() {
      return "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY";
    }

    @Override
    public String currentTimestamp() {
      return "CURRENT TIMESTAMP";
    }

    @Override
    public String currentDate() {
      return "CURRENT DATE";
    }

    @Override
    public String currentTimestampString() {
      return "VARCHAR_FORMAT(CURRENT TIMESTAMP, 'YYYYMMDDHH24MISS')";
    }

    @Override
    public String dateExpression(String fieldName) {
      return "DATE(TIMESTAMP_FORMAT(#{" + fieldName + "}, 'YYYYMMDD'))";
    }

    @Override
    public String timestampExpression(String fieldName, String suffix) {
      return "TIMESTAMP_FORMAT(#{" + fieldName + "} || '" + suffix + "', 'YYYYMMDDHH24MISS')";
    }

    @Override
    public String plusOneDay(String expression) {
      return expression + " + 1 DAY";
    }
  };

  public abstract String emptyResultQuery(String sql);

  public abstract String pageClause();

  public abstract String currentTimestamp();

  public abstract String currentDate();

  public abstract String currentTimestampString();

  public abstract String dateExpression(String fieldName);

  public abstract String timestampExpression(String fieldName, String suffix);

  public abstract String plusOneDay(String expression);

  public String javaType(String typeName, int jdbcType, int precision, int scale) {
    return switch (jdbcType) {
      case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> "Integer";
      case Types.BIGINT -> "Long";
      case Types.NUMERIC, Types.DECIMAL -> scale > 0 ? "BigDecimal" : integerType(precision);
      case Types.DATE -> "LocalDate";
      case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE, -101, -102 -> "LocalDateTime";
      case Types.CLOB, Types.NCLOB, Types.LONGVARCHAR, Types.LONGNVARCHAR -> "String";
      case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> "byte[]";
      default -> typeNameFallback(typeName, precision, scale);
    };
  }

  public static ScaffoldDialect from(String value) {
    if (value == null || value.trim().isEmpty()) {
      return ORACLE;
    }
    String normalized = value.trim().replace("-", "_").toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "ORACLE" -> ORACLE;
      case "POSTGRES", "POSTGRESQL" -> POSTGRES;
      case "DB2" -> DB2;
      default ->
          throw new IllegalArgumentException(
              "Unsupported sms.scaffold.db-platform: "
                  + value
                  + " (allowed: oracle, postgres, db2)");
    };
  }

  private static String integerType(int precision) {
    return precision > 9 ? "Long" : "Integer";
  }

  private static String typeNameFallback(String typeName, int precision, int scale) {
    String normalized =
        typeName == null ? "" : typeName.toUpperCase(Locale.ROOT).split("\\(")[0].trim();
    return switch (normalized) {
      case "NUMBER", "NUMERIC", "DECIMAL" -> scale > 0 ? "BigDecimal" : integerType(precision);
      case "DATE" -> "LocalDate";
      case "TIMESTAMP",
              "TIMESTAMP WITH TIME ZONE",
              "TIMESTAMP WITH LOCAL TIME ZONE",
              "TIMESTAMPTZ",
              "DATETIME" ->
          "LocalDateTime";
      case "CLOB", "NCLOB", "TEXT" -> "String";
      case "BLOB", "BYTEA" -> "byte[]";
      default -> "String";
    };
  }
}
