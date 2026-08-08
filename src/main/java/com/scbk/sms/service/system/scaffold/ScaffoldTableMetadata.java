package com.scbk.sms.service.system.scaffold;

import java.util.List;
import java.util.Map;

/**
 * JDBC 메타데이터에서 읽은 대상 테이블의 생성 판단 근거.
 *
 * @param pkColumns DB가 보고한 키 순서대로 정렬된 실제 PK 컬럼
 * @param nullableByColumn 컬럼별 NULL 허용 여부
 * @param commentsByColumn 컬럼별 DB comment. 화면명 기본값에만 사용한다.
 */
public record ScaffoldTableMetadata(
    List<String> pkColumns,
    Map<String, Boolean> nullableByColumn,
    Map<String, String> commentsByColumn) {

  public ScaffoldTableMetadata(
      List<String> pkColumns, Map<String, Boolean> nullableByColumn) {
    this(pkColumns, nullableByColumn, Map.of());
  }

  /** 컬럼을 찾지 못한 경우 안전하게 nullable로 간주해 과도한 필수 검증 생성을 피한다. */
  public boolean isNullable(String columnName) {
    if (columnName == null) {
      return true;
    }
    return nullableByColumn.getOrDefault(columnName.trim().toUpperCase(), true);
  }

  /** comment가 없거나 컬럼명이 비어 있으면 빈 문자열을 반환한다. */
  public String comment(String columnName) {
    if (columnName == null) {
      return "";
    }
    return commentsByColumn.getOrDefault(columnName.trim().toUpperCase(), "");
  }
}
