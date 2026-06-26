package com.scbk.sms.service.system.scaffold;

import java.util.List;
import java.util.Map;

public record ScaffoldTableMetadata(List<String> pkColumns, Map<String, Boolean> nullableByColumn) {
  public boolean isNullable(String columnName) {
    if (columnName == null) {
      return true;
    }
    return nullableByColumn.getOrDefault(columnName.trim().toUpperCase(), true);
  }
}
