package com.scbk.sms.service.system.scaffold;

import com.scbk.sms.dto.system.ScaffoldColumnOptionDTO;
import com.scbk.sms.dto.system.ScaffoldMenuOptionDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.dto.system.ScaffoldSearchParamOptionDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 템플릿이 사용하는 요청값과 SQL/DB 분석 결과를 정규화한 읽기 전용 생성 모델.
 *
 * <p>템플릿은 원본 DTO를 직접 해석하지 않고 이 모델의 accessor만 사용한다. 여기서 화면 모드, PK, 컬럼 표시/수정 화이트리스트, 검색 입력,
 * 마스킹, 날짜 형식, 메뉴 기본값을 한 번 계산함으로써 Java·XML·HTML·JavaScript 산출물이 같은 계약을 공유한다.
 */
public class ScaffoldModel {

  private static final Pattern BETWEEN_SEARCH_RANGE_PATTERN =
      Pattern.compile(
          "\\bBETWEEN\\s+\\$([a-zA-Z0-9_]+)\\s+AND\\s+\\$([a-zA-Z0-9_]+)",
          Pattern.CASE_INSENSITIVE);

  private final ScaffoldRequestDTO request;
  private final List<String> columns;
  private final List<String> searchVars;
  private final Map<String, String> typeMap;
  private final ScaffoldDialect dialect;

  // 생성 입력과 분석 결과는 방어적 복사해 렌더링 도중 변경되지 않게 한다.
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

  /**
   * Thymeleaf 템플릿용 accessor. {@link #getTypeMap()}와 동일한 값을 반환한다. SpEL은 JavaBean property 규칙을 따르므로
   * {@code ${model.typeMap()}}(메서드 호출)이 {@code getTypeMap()}을 찾지 못한다. 템플릿에서 {@code
   * ${model.voFields()}}, {@code ${model.pkFields()}} 등과 일관된 {@code xxx()} 호출 문법을 쓰기 위해 제공한다.
   */
  public Map<String, String> typeMap() {
    return typeMap;
  }

  public ScaffoldDialect dialect() {
    return dialect;
  }

  // ---- 화면 식별자와 생성 모드 -------------------------------------------------------------

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

  /**
   * CRUD(create/update) 산출물 생성 여부. 명시적 {@code screenMode}가 권위를 가진다: {@code CRUD}일 때만 {@code true}이며
   * legacy {@code includeCreateUpdate} boolean은 모순되어도 무시된다. {@code screenMode}가 비어있을 때만 legacy
   * boolean fallback으로 동작한다(하위호환).
   */
  public boolean includeCreateUpdate() {
    if (StringUtils.hasText(request.getScreenMode())) {
      return "CRUD".equals(screenMode());
    }
    return request.isIncludeCreateUpdate();
  }

  /**
   * 엑셀 산출물 생성 여부. 명시적 {@code screenMode}가 권위를 가진다: {@code EXCEL}일 때만 {@code true}이며 legacy {@code
   * includeExcel} boolean은 모순되어도 무시된다. {@code screenMode}가 비어있을 때만 legacy boolean fallback으로
   * 동작한다(하위호환).
   */
  public boolean includeExcel() {
    if (StringUtils.hasText(request.getScreenMode())) {
      return "EXCEL".equals(screenMode());
    }
    return request.isIncludeExcel();
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

  // ---- CRUD 대상, PK와 낙관적 잠금 ---------------------------------------------------------

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

  public boolean hasLockColumn() {
    return !lockColumn().isEmpty();
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

  // ---- 검색조건과 그리드/모달 컬럼 ---------------------------------------------------------

  public List<SearchParam> searchParams() {
    List<String> vars = getSearchVars().isEmpty() ? List.of("searchKeyword") : getSearchVars();
    Set<String> betweenRangeEndVars = betweenRangeEndVars();
    Map<String, ScaffoldSearchParamOptionDTO> optionMap = new HashMap<>();
    for (ScaffoldSearchParamOptionDTO option : request.getSearchParamOptions()) {
      if (option != null && StringUtils.hasText(option.getName())) {
        optionMap.put(option.getName(), option);
      }
    }

    List<SearchParam> result = new ArrayList<>();
    for (String var : vars) {
      ScaffoldSearchParamOptionDTO option = optionMap.get(var);
      String label =
          option != null && StringUtils.hasText(option.getLabel()) ? option.getLabel() : var;
      String inputType =
          option != null && StringUtils.hasText(option.getInputType())
              ? option.getInputType().trim().toUpperCase()
              : (isDateVar(var) ? "DATE" : "TEXT");
      String defaultValue =
          option != null && StringUtils.hasText(option.getDefaultValue())
              ? option.getDefaultValue().trim().toUpperCase()
              : "NONE";
      String optionsText = option != null ? option.getOptionsText() : null;
      result.add(
          new SearchParam(
              var, label, inputType, defaultValue, optionsText, betweenRangeEndVars.contains(var)));
    }
    return result;
  }

  private Set<String> betweenRangeEndVars() {
    Set<String> result = new TreeSet<>();
    if (!StringUtils.hasText(request.getRawQuery())) {
      return result;
    }
    Matcher matcher = BETWEEN_SEARCH_RANGE_PATTERN.matcher(request.getRawQuery());
    while (matcher.find()) {
      result.add(QueryColumnExtractor.toCamelCase(matcher.group(2)));
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

  /** 그리드의 남는 너비를 채울 마지막 표시 컬럼인지 판별한다. */
  public boolean isLastVisibleColumn(ColumnConfig candidate) {
    if (candidate == null || !candidate.visible()) {
      return false;
    }
    List<ColumnConfig> configs = columnConfigs();
    for (int index = configs.size() - 1; index >= 0; index--) {
      ColumnConfig config = configs.get(index);
      if (config.visible()) {
        return config.fieldName().equals(candidate.fieldName());
      }
    }
    return false;
  }

  public List<VoField> voFields() {
    List<VoField> result = new ArrayList<>();
    for (String column : columns) {
      if (column == null || column.trim().isEmpty()) {
        continue;
      }
      String trimmed = column.trim();
      String javaType = typeMap.getOrDefault(column, "String");
      String fieldName = QueryColumnExtractor.toCamelCase(trimmed);
      result.add(new VoField(trimmed, fieldName, javaType));
    }
    return result;
  }

  public List<PkField> pkFields() {
    List<PkField> result = new ArrayList<>();
    for (String pkColumn : pkColumns()) {
      result.add(
          new PkField(pkColumn, QueryColumnExtractor.toCamelCase(pkColumn), pkJavaType(pkColumn)));
    }
    return result;
  }

  /** editable 컬럼만 필터링한다. 템플릿 반복문에서 사용한다. */
  public List<ColumnConfig> editableColumns() {
    return columnConfigs().stream().filter(ColumnConfig::editable).toList();
  }

  public List<ColumnConfig> modalColumns() {
    return columnConfigs().stream().filter(ColumnConfig::modalVisible).toList();
  }

  public List<List<ColumnConfig>> modalRows() {
    List<ColumnConfig> columns = modalColumns();
    List<List<ColumnConfig>> rows = new ArrayList<>();
    for (int index = 0; index < columns.size(); index += 2) {
      rows.add(columns.subList(index, Math.min(index + 2, columns.size())));
    }
    return rows;
  }

  /** 마스킹이 필요한 컬럼만 필터링한다. Service/Excel 템플릿에서 사용한다. */
  public List<ColumnConfig> maskedColumns() {
    return columnConfigs().stream().filter(ColumnConfig::hasMask).toList();
  }

  /** editable + required + 특정 Java 타입인 필드가 있는지 (import용). */
  public boolean hasEditableRequiredOfType(String javaType) {
    return columnConfigs().stream()
        .filter(ColumnConfig::editable)
        .filter(ColumnConfig::hasValidate)
        .anyMatch(
            c -> c.validate().toLowerCase().contains("required") && javaType.equals(c.javaType()));
  }

  /** editable + required + String 컬럼이 있는지 (UpdateRequestDTO import NotBlank용). */
  public boolean hasEditableRequiredNotBlank() {
    return hasEditableRequiredOfType("String");
  }

  /** editable + required + 비-String 컬럼이 있는지 (UpdateRequestDTO import NotNull용). */
  public boolean hasEditableRequiredNotNull() {
    return columnConfigs().stream()
        .filter(ColumnConfig::editable)
        .filter(ColumnConfig::hasValidate)
        .anyMatch(
            c -> c.validate().toLowerCase().contains("required") && !"String".equals(c.javaType()));
  }

  /** typeMap에 특정 Java 타입이 포함되어 있는지 (import용). */
  public boolean hasType(String javaType) {
    return typeMap.containsValue(javaType);
  }

  public List<SearchParam> searchParamsWithDefaults() {
    return searchParams().stream().filter(param -> !"NONE".equals(param.defaultValue())).toList();
  }

  public List<String> defaultFormFieldNames() {
    List<String> fields = new ArrayList<>(pkFieldNames());
    fields.addAll(editableColumns().stream().map(ColumnConfig::fieldName).toList());
    if (!lockColumn().isEmpty()) {
      fields.add(beforeLockFieldName());
    }
    return fields;
  }

  // ---- 생성 코드에서 사용하는 표시·escape·테스트 샘플 유틸리티 -----------------------------

  public String maskingMethodName(String maskType) {
    if (maskType == null) {
      return "maskPhone";
    }
    return switch (maskType.trim().toLowerCase()) {
      case "name", "nm" -> "maskName";
      case "email" -> "maskEmail";
      case "birth", "birthdate", "birth_date" -> "maskBirthDate";
      case "rrn", "ssn" -> "maskRrn";
      case "card", "bizno" -> "maskCard";
      default -> "maskPhone";
    };
  }

  public String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  public String htmlEscape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  public String jsEscape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("'", "\\'");
  }

  public String sampleValue(String javaType) {
    return switch (javaType) {
      case "Integer" -> "1";
      case "Long" -> "1L";
      case "LocalDate" -> "java.time.LocalDate.of(2020, 1, 1)";
      case "LocalDateTime" -> "java.time.LocalDateTime.of(2020, 1, 1, 0, 0)";
      case "BigDecimal" -> "java.math.BigDecimal.ONE";
      case "byte[]" -> "new byte[] {1}";
      default -> "\"1\"";
    };
  }

  public String sampleParamValue(String javaType) {
    return switch (javaType) {
      case "LocalDate" -> "2020-01-01";
      case "LocalDateTime" -> "2020-01-01T00:00:00";
      case "byte[]" -> "AQ==";
      default -> "1";
    };
  }

  // ---- 메뉴 seed 기본값 -------------------------------------------------------------------

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

  private static List<SelectOption> parseOptions(String optionsText) {
    if (!StringUtils.hasText(optionsText)) {
      return List.of();
    }
    return Arrays.stream(optionsText.split(","))
        .map(String::trim)
        .filter(token -> !token.isEmpty())
        .map(ScaffoldModel::parseOption)
        .toList();
  }

  private static SelectOption parseOption(String token) {
    String[] parts = token.split("[:=]", 2);
    return parts.length == 2
        ? new SelectOption(parts[0].trim(), parts[1].trim())
        : new SelectOption(token, token);
  }

  // 템플릿 반복문이 문자열 규칙을 다시 해석하지 않도록 필요한 파생값과 판별 메서드를 함께 제공한다.
  public record SearchParam(
      String name,
      String label,
      String inputType,
      String defaultValue,
      String optionsText,
      boolean betweenRangeEnd) {
    public boolean isDate() {
      return "DATE".equals(inputType);
    }

    public boolean isSelect() {
      return "SELECT".equals(inputType);
    }

    public boolean isRadio() {
      return "RADIO".equals(inputType);
    }

    public boolean isBetweenRangeEnd() {
      return isDate() && betweenRangeEnd;
    }

    public List<SelectOption> options() {
      return parseOptions(optionsText);
    }
  }

  public record SelectOption(String value, String label) {}

  public record VoField(String columnName, String fieldName, String javaType) {}

  public record PkField(String columnName, String fieldName, String javaType) {}

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

    public boolean isRequired() {
      return hasValidate() && validate.toLowerCase().contains("required");
    }

    /** required 검증 + String → @NotBlank 어노테이션 대상 (UpdateRequestDTO 필드용). */
    public boolean requiresNotBlank() {
      return isRequired() && "String".equals(javaType);
    }

    /** required 검증 + 비-String → @NotNull 어노테이션 대상 (UpdateRequestDTO 필드용). */
    public boolean requiresNotNull() {
      return isRequired() && !"String".equals(javaType);
    }

    public boolean hasOptions() {
      return optionsText != null && !optionsText.isBlank();
    }

    public List<SelectOption> options() {
      return parseOptions(optionsText);
    }

    public boolean isNumeric() {
      return "Integer".equals(javaType) || "Long".equals(javaType) || "BigDecimal".equals(javaType);
    }
  }
}
