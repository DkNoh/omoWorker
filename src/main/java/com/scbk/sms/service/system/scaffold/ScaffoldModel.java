package com.scbk.sms.service.system.scaffold;

import com.scbk.sms.dto.system.ScaffoldColumnOptionDTO;
import com.scbk.sms.dto.system.ScaffoldMenuOptionDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.dto.system.ScaffoldSearchParamOptionDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.util.StringUtils;

/** 템플릿 생성에 필요한 입력 + 분석 결과 묶음. */
public class ScaffoldModel {

  private final ScaffoldRequestDTO request;
  private final List<String> columns;
  private final List<String> searchVars;
  private final Map<String, String> typeMap;
  private final ScaffoldDialect dialect;

  public ScaffoldModel(
      ScaffoldRequestDTO request,
      List<String> columns,
      List<String> searchVars,
      Map<String, String> typeMap) {
    this(request, columns, searchVars, typeMap, ScaffoldDialect.ORACLE);
  }

  public ScaffoldModel(
      ScaffoldRequestDTO request,
      List<String> columns,
      List<String> searchVars,
      Map<String, String> typeMap,
      ScaffoldDialect dialect) {
    this.request = request;
    this.columns = List.copyOf(columns);
    this.searchVars = List.copyOf(searchVars);
    this.typeMap = Map.copyOf(typeMap);
    this.dialect = dialect == null ? ScaffoldDialect.ORACLE : dialect;
  }

  public ScaffoldRequestDTO getRequest() {
    return request;
  }

  public List<String> getColumns() {
    return columns;
  }

  public List<String> getSearchVars() {
    return searchVars;
  }

  public Map<String, String> getTypeMap() {
    return typeMap;
  }

  public ScaffoldDialect dialect() {
    return dialect;
  }

  public String moduleName() {
    return request.getModuleName();
  }

  public String domainId() {
    return request.getDomainId();
  }

  public String domainClass() {
    return request.getDomainClass();
  }

  public String domainName() {
    return request.getDomainName();
  }

  public String rawQuery() {
    return request.getRawQuery();
  }

  public String orderBy() {
    return request.getOrderBy();
  }

  public boolean includeCreateUpdate() {
    return "CRUD".equals(screenMode()) || request.isIncludeCreateUpdate();
  }

  public boolean includeExcel() {
    return "EXCEL".equals(screenMode()) || request.isIncludeExcel();
  }

  public boolean includeDetailModal() {
    return "DETAIL".equals(screenMode()) || "CRUD".equals(screenMode()) || request.isIncludeModal();
  }

  public boolean includePrivacy() {
    return request.isIncludePrivacy();
  }

  public String targetTable() {
    if (StringUtils.hasText(request.getTargetTable())) {
      return normalizeTable(request.getTargetTable());
    }
    return normalizeTable(QueryColumnExtractor.extractPrimaryTable(rawQuery()));
  }

  public String screenMode() {
    if (StringUtils.hasText(request.getScreenMode())) {
      return request.getScreenMode().trim().toUpperCase();
    }
    if (request.isIncludeCreateUpdate()) {
      return "CRUD";
    }
    if (request.isIncludeExcel()) {
      return "EXCEL";
    }
    return "LIST";
  }

  public String pkColumn() {
    return pkColumns().stream().findFirst().orElse("");
  }

  public List<String> pkColumns() {
    List<String> configured =
        request.getPkColumns().stream()
            .map(ScaffoldModel::normalizeColumn)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    if (!configured.isEmpty()) {
      return configured;
    }
    String legacy = normalizeColumn(request.getPkColumn());
    if (StringUtils.hasText(legacy)) {
      return List.of(legacy);
    }
    return List.of();
  }

  public String pkFieldName() {
    String pkColumn = pkColumn();
    return StringUtils.hasText(pkColumn) ? QueryColumnExtractor.toCamelCase(pkColumn) : "id";
  }

  public List<String> pkFieldNames() {
    return pkColumns().stream().map(QueryColumnExtractor::toCamelCase).toList();
  }

  /** delete 파라미터(PK)에 필요한 java import 문 목록. (LocalDate/LocalDateTime/BigDecimal) */
  public List<String> pkParamImports() {
    Set<String> imports = new TreeSet<>();
    for (String pkColumn : pkColumns()) {
      switch (pkJavaType(pkColumn)) {
        case "LocalDate" -> imports.add("import java.time.LocalDate;");
        case "LocalDateTime" -> imports.add("import java.time.LocalDateTime;");
        case "BigDecimal" -> imports.add("import java.math.BigDecimal;");
        default -> {}
      }
    }
    return new ArrayList<>(imports);
  }

  public String pkJavaType() {
    String pkColumn = pkColumn();
    return StringUtils.hasText(pkColumn) ? typeMap.getOrDefault(pkColumn, "String") : "String";
  }

  public String pkJavaType(String pkColumn) {
    String normalized = normalizeColumn(pkColumn);
    return StringUtils.hasText(normalized) ? typeMap.getOrDefault(normalized, "String") : "String";
  }

  public String lockJavaType() {
    String lockColumn = lockColumn();
    return StringUtils.hasText(lockColumn) ? typeMap.getOrDefault(lockColumn, "String") : "String";
  }

  public String lockColumn() {
    return normalizeColumn(request.getLockColumn());
  }

  public String beforeLockFieldName() {
    String lockColumn = lockColumn();
    if (!StringUtils.hasText(lockColumn)) {
      return "beforeUpdateDttm";
    }
    String field = QueryColumnExtractor.toCamelCase(lockColumn);
    return "before" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
  }

  public String lockFieldName() {
    String lockColumn = lockColumn();
    if (!StringUtils.hasText(lockColumn)) {
      return "updateDttm";
    }
    return QueryColumnExtractor.toCamelCase(lockColumn);
  }

  public String screenUrl() {
    return "/" + moduleName() + "/" + domainId();
  }

  public List<SearchParam> searchParams() {
    List<String> vars = getSearchVars().isEmpty() ? List.of("searchKeyword") : getSearchVars();
    Map<String, ScaffoldSearchParamOptionDTO> optionMap = new HashMap<>();
    for (ScaffoldSearchParamOptionDTO option : request.getSearchParamOptions()) {
      if (option != null && StringUtils.hasText(option.getName())) {
        optionMap.put(option.getName(), option);
      }
    }

    List<SearchParam> result = new ArrayList<>();
    for (String var : vars) {
      ScaffoldSearchParamOptionDTO option = optionMap.get(var);
      String inputType =
          option != null && StringUtils.hasText(option.getInputType())
              ? option.getInputType().trim().toUpperCase()
              : (isDateVar(var) ? "DATE" : "TEXT");
      String defaultValue =
          option != null && StringUtils.hasText(option.getDefaultValue())
              ? option.getDefaultValue().trim().toUpperCase()
              : "NONE";
      String optionsText = option != null ? option.getOptionsText() : null;
      result.add(new SearchParam(var, inputType, defaultValue, optionsText));
    }
    return result;
  }

  public List<ColumnConfig> columnConfigs() {
    Map<String, ScaffoldColumnOptionDTO> optionMap = new HashMap<>();
    for (ScaffoldColumnOptionDTO option : request.getColumnOptions()) {
      if (option != null && StringUtils.hasText(option.getColumnName())) {
        optionMap.put(normalizeColumn(option.getColumnName()), option);
      }
    }

    List<ColumnConfig> result = new ArrayList<>();
    for (String column : columns) {
      String columnName = normalizeColumn(column);
      ScaffoldColumnOptionDTO option = optionMap.get(columnName);
      String javaType = typeMap.getOrDefault(columnName, "String");
      boolean dateColumn = "LocalDate".equals(javaType) || "LocalDateTime".equals(javaType);
      String headerName =
          option != null && StringUtils.hasText(option.getHeaderName())
              ? option.getHeaderName()
              : columnName;
      int width =
          option != null && option.getWidth() != null && option.getWidth() > 0
              ? option.getWidth()
              : 150;
      String align =
          option != null && StringUtils.hasText(option.getAlign())
              ? option.getAlign().trim().toLowerCase()
              : "center";
      String dateFormat = resolveDateFormat(option, dateColumn);
      String maskType =
          option != null && StringUtils.hasText(option.getMaskType())
              ? option.getMaskType().trim().toUpperCase()
              : "NONE";
      String inputMask =
          option != null && StringUtils.hasText(option.getInputMask())
              ? option.getInputMask().trim().toLowerCase()
              : "";
      String validate =
          option != null && StringUtils.hasText(option.getValidate())
              ? option.getValidate().trim()
              : "";
      String optionsText = option != null ? option.getOptionsText() : null;
      boolean visible = option == null || option.isVisible();
      boolean modalVisible = option == null || option.isModalVisible();
      boolean editable =
          (option != null ? option.isEditable() : isDefaultEditableCandidate(columnName))
              && !isProtectedUpdateColumn(columnName);
      result.add(
          new ColumnConfig(
              columnName,
              QueryColumnExtractor.toCamelCase(columnName),
              javaType,
              headerName,
              width,
              align,
              dateFormat,
              maskType,
              visible,
              modalVisible,
              editable,
              inputMask,
              validate,
              optionsText));
    }
    return result;
  }

  public String menuId() {
    ScaffoldMenuOptionDTO menuOption = request.getMenuOption();
    if (menuOption != null && StringUtils.hasText(menuOption.getMenuId())) {
      return menuOption.getMenuId().trim().toUpperCase();
    }
    return (moduleName() + "_" + domainId()).toUpperCase().replace("-", "_").replace("/", "_");
  }

  public String parentMenuId() {
    ScaffoldMenuOptionDTO menuOption = request.getMenuOption();
    if (menuOption != null && StringUtils.hasText(menuOption.getParentMenuId())) {
      return menuOption.getParentMenuId().trim().toUpperCase();
    }
    return "/* TODO: 상위 메뉴 ID */";
  }

  public String roleCode() {
    ScaffoldMenuOptionDTO menuOption = request.getMenuOption();
    if (menuOption != null && StringUtils.hasText(menuOption.getRoleCode())) {
      return menuOption.getRoleCode().trim().toUpperCase();
    }
    return "ROLE_ADMIN";
  }

  public int menuSortOrd() {
    ScaffoldMenuOptionDTO menuOption = request.getMenuOption();
    if (menuOption != null && menuOption.getSortOrd() != null && menuOption.getSortOrd() > 0) {
      return menuOption.getSortOrd();
    }
    return 99;
  }

  public static boolean isDateVar(String var) {
    String lower = var.toLowerCase();
    return lower.endsWith("date") || lower.endsWith("dt") || lower.endsWith("at");
  }

  private static String normalizeColumn(String column) {
    return StringUtils.hasText(column) ? column.trim().toUpperCase() : "";
  }

  private static String resolveDateFormat(ScaffoldColumnOptionDTO option, boolean dateColumn) {
    if (option == null || !StringUtils.hasText(option.getDateFormat())) {
      return dateColumn ? "AUTO" : "NONE";
    }
    String raw = option.getDateFormat().trim();
    String upper = raw.toUpperCase();
    if (upper.equals("AUTO")
        || upper.equals("DATE")
        || upper.equals("DATETIME")
        || upper.equals("NONE")) {
      return upper;
    }
    return raw;
  }

  private static String normalizeTable(String table) {
    return StringUtils.hasText(table) ? table.trim().replace("\"", "").toUpperCase() : "";
  }

  private boolean isProtectedUpdateColumn(String columnName) {
    if (!StringUtils.hasText(columnName)) {
      return true;
    }
    String normalized = normalizeColumn(columnName);
    return pkColumns().contains(normalized)
        || normalized.equals(lockColumn())
        || normalized.equals("REG_ID")
        || normalized.equals("REG_DTTM")
        || normalized.equals("UPD_ID")
        || normalized.equals("UPD_DTTM")
        || normalized.equals("REQUEST_ID");
  }

  private boolean isDefaultEditableCandidate(String columnName) {
    if (!StringUtils.hasText(columnName)) {
      return false;
    }
    String normalized = normalizeColumn(columnName);
    return !normalized.endsWith("_ID")
        && !isProtectedUpdateColumn(normalized)
        && !normalized.equals("CREATED_AT")
        && !normalized.equals("UPDATED_AT");
  }

  public record SearchParam(
      String name, String inputType, String defaultValue, String optionsText) {
    public boolean isDate() {
      return "DATE".equals(inputType);
    }

    public boolean isSelect() {
      return "SELECT".equals(inputType);
    }

    public boolean isRadio() {
      return "RADIO".equals(inputType);
    }
  }

  public record ColumnConfig(
      String columnName,
      String fieldName,
      String javaType,
      String headerName,
      int width,
      String align,
      String dateFormat,
      String maskType,
      boolean visible,
      boolean modalVisible,
      boolean editable,
      String inputMask,
      String validate,
      String optionsText) {
    public boolean isDateColumn() {
      return "LocalDate".equals(javaType) || "LocalDateTime".equals(javaType);
    }

    public boolean hasMask() {
      return !"NONE".equals(maskType);
    }

    public boolean hasInputMask() {
      return inputMask != null && !inputMask.isEmpty();
    }

    public boolean hasValidate() {
      return validate != null && !validate.isEmpty();
    }

    public boolean hasOptions() {
      return optionsText != null && !optionsText.isBlank();
    }

    public String optionsJsObject() {
      if (!hasOptions()) {
        return "{}";
      }
      StringBuilder sb = new StringBuilder("{ ");
      String[] tokens = optionsText.split(",");
      boolean first = true;
      for (String raw : tokens) {
        String token = raw.trim();
        if (token.isEmpty()) {
          continue;
        }
        int sep = indexOfOptionSeparator(token);
        String value = sep < 0 ? token : token.substring(0, sep).trim();
        String label = sep < 0 ? token : token.substring(sep + 1).trim();
        if (value.isEmpty()) {
          continue;
        }
        if (!first) {
          sb.append(", ");
        }
        sb.append(value).append(": '").append(escapeJsString(label)).append("'");
        first = false;
      }
      return sb.append(" }").toString();
    }

    private static int indexOfOptionSeparator(String token) {
      for (int i = 0; i < token.length(); i++) {
        char c = token.charAt(i);
        if (c == ':' || c == '=') {
          return i;
        }
      }
      return -1;
    }

    private static String escapeJsString(String s) {
      if (s == null) {
        return "";
      }
      return s.replace("\\", "\\\\").replace("'", "\\'");
    }
  }
}
