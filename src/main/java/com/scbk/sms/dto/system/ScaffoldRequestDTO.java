package com.scbk.sms.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;

/** Query Scaffold 생성 요청 (QuerySpec). rawQuery 안의 $변수는 검색조건 규약이다. 예: AND A.SEND_DT >= $start_dt */
public class ScaffoldRequestDTO {

  @NotBlank(message = "moduleName은 필수입니다.")
  @Pattern(regexp = "^[a-z][a-z0-9]*$", message = "moduleName은 영문 소문자와 숫자만 사용할 수 있습니다.")
  private String moduleName;

  // v2 baseline 중 3단계 URL(예: /campaign/sms/register)을 그대로 재현하기 위해 내부 슬래시 1개까지 허용한다.
  @NotBlank(message = "domainId는 필수입니다.")
  @Pattern(
      regexp = "^[a-z][a-z0-9-]*(/[a-z][a-z0-9-]*)?$",
      message = "domainId는 영문 소문자, 숫자, 하이픈만 사용할 수 있고 내부 슬래시는 1개까지 허용합니다.")
  private String domainId;

  @NotBlank(message = "domainClass는 필수입니다.")
  @Pattern(regexp = "^[A-Z][A-Za-z0-9]*$", message = "domainClass는 영문 대문자로 시작하는 Java 클래스명이어야 합니다.")
  private String domainClass;

  @NotBlank(message = "domainName은 필수입니다.")
  private String domainName;

  @NotBlank(message = "rawQuery는 필수입니다.")
  private String rawQuery;

  @NotBlank(message = "orderBy는 필수입니다. 결정적 정렬 컬럼을 입력하세요.")
  private String orderBy;

  private boolean includeCreateUpdate;
  private boolean includeExcel;
  private boolean includeModal;
  private boolean includePrivacy;
  private String screenMode;
  private String targetTable;
  private String pkColumn;
  private List<String> pkColumns = new ArrayList<>();
  private String lockColumn;
  private List<ScaffoldSearchParamOptionDTO> searchParamOptions = new ArrayList<>();
  private List<ScaffoldColumnOptionDTO> columnOptions = new ArrayList<>();
  private ScaffoldMenuOptionDTO menuOption = new ScaffoldMenuOptionDTO();

  public String getModuleName() {
    return moduleName;
  }

  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }

  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  public String getDomainClass() {
    return domainClass;
  }

  public void setDomainClass(String domainClass) {
    this.domainClass = domainClass;
  }

  public String getDomainName() {
    return domainName;
  }

  public void setDomainName(String domainName) {
    this.domainName = domainName;
  }

  public String getRawQuery() {
    return rawQuery;
  }

  public void setRawQuery(String rawQuery) {
    this.rawQuery = rawQuery;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }

  public boolean isIncludeCreateUpdate() {
    return includeCreateUpdate;
  }

  public void setIncludeCreateUpdate(boolean includeCreateUpdate) {
    this.includeCreateUpdate = includeCreateUpdate;
  }

  public boolean isIncludeExcel() {
    return includeExcel;
  }

  public void setIncludeExcel(boolean includeExcel) {
    this.includeExcel = includeExcel;
  }

  public boolean isIncludeModal() {
    return includeModal;
  }

  public void setIncludeModal(boolean includeModal) {
    this.includeModal = includeModal;
  }

  public boolean isIncludePrivacy() {
    return includePrivacy;
  }

  public void setIncludePrivacy(boolean includePrivacy) {
    this.includePrivacy = includePrivacy;
  }

  public String getScreenMode() {
    return screenMode;
  }

  public void setScreenMode(String screenMode) {
    this.screenMode = screenMode;
  }

  public String getTargetTable() {
    return targetTable;
  }

  public void setTargetTable(String targetTable) {
    this.targetTable = targetTable;
  }

  public String getPkColumn() {
    return pkColumn;
  }

  public void setPkColumn(String pkColumn) {
    this.pkColumn = pkColumn;
  }

  public List<String> getPkColumns() {
    if ((pkColumns == null || pkColumns.isEmpty())
        && pkColumn != null
        && !pkColumn.trim().isEmpty()) {
      return List.of(pkColumn);
    }
    return pkColumns != null ? pkColumns : new ArrayList<>();
  }

  public void setPkColumns(List<String> pkColumns) {
    this.pkColumns = pkColumns != null ? pkColumns : new ArrayList<>();
  }

  public String getLockColumn() {
    return lockColumn;
  }

  public void setLockColumn(String lockColumn) {
    this.lockColumn = lockColumn;
  }

  public List<ScaffoldSearchParamOptionDTO> getSearchParamOptions() {
    return searchParamOptions;
  }

  public void setSearchParamOptions(List<ScaffoldSearchParamOptionDTO> searchParamOptions) {
    this.searchParamOptions = searchParamOptions != null ? searchParamOptions : new ArrayList<>();
  }

  public List<ScaffoldColumnOptionDTO> getColumnOptions() {
    return columnOptions;
  }

  public void setColumnOptions(List<ScaffoldColumnOptionDTO> columnOptions) {
    this.columnOptions = columnOptions != null ? columnOptions : new ArrayList<>();
  }

  public ScaffoldMenuOptionDTO getMenuOption() {
    return menuOption;
  }

  public void setMenuOption(ScaffoldMenuOptionDTO menuOption) {
    this.menuOption = menuOption != null ? menuOption : new ScaffoldMenuOptionDTO();
  }
}
