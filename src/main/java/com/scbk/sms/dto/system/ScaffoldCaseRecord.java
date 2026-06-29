package com.scbk.sms.dto.system;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scaffold 산출물 1세트의 원본 입력을 그대로 보관하는 메타 레코드. ScaffoldRegenerateMain이 이 파일을 읽어 템플릿 변경 없이도 모든 산출물을 동일한
 * 입력으로 다시 생성할 수 있게 한다. ScaffoldService.apply() 호출 시 자동으로
 * src/main/resources/scaffold-cases/{module}_{domainId}.json 파일로 저장된다.
 */
public class ScaffoldCaseRecord {

  private ScaffoldRequestDTO request = new ScaffoldRequestDTO();
  private List<String> columns = new ArrayList<>();
  private List<String> searchVars = new ArrayList<>();
  private Map<String, String> typeMap = new LinkedHashMap<>();
  private String dialect = "ORACLE";

  public ScaffoldRequestDTO getRequest() {
    return request;
  }

  public void setRequest(ScaffoldRequestDTO request) {
    this.request = request != null ? request : new ScaffoldRequestDTO();
  }

  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns != null ? columns : new ArrayList<>();
  }

  public List<String> getSearchVars() {
    return searchVars;
  }

  public void setSearchVars(List<String> searchVars) {
    this.searchVars = searchVars != null ? searchVars : new ArrayList<>();
  }

  public Map<String, String> getTypeMap() {
    return typeMap;
  }

  public void setTypeMap(Map<String, String> typeMap) {
    this.typeMap = typeMap != null ? typeMap : new LinkedHashMap<>();
  }

  public String getDialect() {
    return dialect;
  }

  public void setDialect(String dialect) {
    this.dialect = dialect != null ? dialect : "ORACLE";
  }
}
