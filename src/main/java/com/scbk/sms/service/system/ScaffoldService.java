package com.scbk.sms.service.system;

import com.scbk.sms.config.ScaffoldProperties;
import com.scbk.sms.dto.system.ScaffoldApplyFileResultDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.service.system.scaffold.ColumnTypeInferrer;
import com.scbk.sms.service.system.scaffold.ControllerTemplate;
import com.scbk.sms.service.system.scaffold.ControllerTestTemplate;
import com.scbk.sms.service.system.scaffold.DtoTemplate;
import com.scbk.sms.service.system.scaffold.HtmlTemplate;
import com.scbk.sms.service.system.scaffold.JsTemplate;
import com.scbk.sms.service.system.scaffold.MapperInterfaceTemplate;
import com.scbk.sms.service.system.scaffold.MapperXmlTemplate;
import com.scbk.sms.service.system.scaffold.MenuSqlTemplate;
import com.scbk.sms.service.system.scaffold.QueryColumnExtractor;
import com.scbk.sms.service.system.scaffold.ScaffoldDialect;
import com.scbk.sms.service.system.scaffold.ScaffoldFileApplier;
import com.scbk.sms.service.system.scaffold.ScaffoldMetadataReader;
import com.scbk.sms.service.system.scaffold.ScaffoldModel;
import com.scbk.sms.service.system.scaffold.ScaffoldTableMetadata;
import com.scbk.sms.service.system.scaffold.ServiceTemplate;
import com.scbk.sms.service.system.scaffold.ServiceTestTemplate;
import com.scbk.sms.service.system.scaffold.UpdateRequestDtoTemplate;
import com.scbk.sms.service.system.scaffold.VoTemplate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** QuerySpec(rawQuery + $변수)에서 화면 1세트의 코드를 생성하고, local 전용 apply 요청에서는 정해진 프로젝트 경로로 저장한다. */
@Service
@Profile("local")
public class ScaffoldService {

  private final ColumnTypeInferrer columnTypeInferrer;
  private final ScaffoldFileApplier scaffoldFileApplier;
  private final ScaffoldMetadataReader metadataReader;
  private final ScaffoldProperties scaffoldProperties;

  public ScaffoldService(
      ColumnTypeInferrer columnTypeInferrer,
      ScaffoldFileApplier scaffoldFileApplier,
      ScaffoldMetadataReader metadataReader,
      ScaffoldProperties scaffoldProperties) {
    this.columnTypeInferrer = columnTypeInferrer;
    this.scaffoldFileApplier = scaffoldFileApplier;
    this.metadataReader = metadataReader;
    this.scaffoldProperties = scaffoldProperties;
  }

  public Map<String, String> generate(ScaffoldRequestDTO request) {
    return generateFiles(request);
  }

  public Map<String, Object> analyze(String rawQuery, String targetTable) {
    String resolvedTargetTable =
        hasText(targetTable) ? targetTable : QueryColumnExtractor.extractPrimaryTable(rawQuery);
    ScaffoldTableMetadata metadata = metadataReader.read(resolvedTargetTable);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("columns", QueryColumnExtractor.extractColumns(rawQuery));
    result.put("searchVars", QueryColumnExtractor.extractSearchVars(rawQuery));
    result.put("targetTable", resolvedTargetTable);
    result.put("pkColumns", metadata.pkColumns());
    result.put("nullableColumns", metadata.nullableByColumn());
    result.put("dbPlatform", scaffoldProperties.getDbPlatform());
    return result;
  }

  public List<ScaffoldApplyFileResultDTO> preview(ScaffoldRequestDTO request) {
    Map<String, String> generatedFiles = generateFiles(request);
    return scaffoldFileApplier.preview(request, generatedFiles);
  }

  public List<ScaffoldApplyFileResultDTO> apply(ScaffoldRequestDTO request) {
    Map<String, String> generatedFiles = generateFiles(request);
    return scaffoldFileApplier.apply(request, generatedFiles);
  }

  private Map<String, String> generateFiles(ScaffoldRequestDTO request) {
    List<String> columns = QueryColumnExtractor.extractColumns(request.getRawQuery());
    if (columns.isEmpty()) {
      throw new IllegalStateException("rawQuery에서 SELECT 컬럼을 추출하지 못했습니다. 쿼리를 확인하세요.");
    }
    List<String> searchVars = QueryColumnExtractor.extractSearchVars(request.getRawQuery());
    Map<String, String> typeMap = columnTypeInferrer.inferTypes(request.getRawQuery(), columns);
    ScaffoldDialect dialect = scaffoldProperties.dialect();
    enrichAndValidateCrudRequest(request, columns);

    ScaffoldModel model = new ScaffoldModel(request, columns, searchVars, typeMap, dialect);
    String cls = model.domainClass();

    Map<String, String> results = new LinkedHashMap<>();
    results.put(cls + "SearchRequestDTO.java", DtoTemplate.generate(model));
    if (model.includeCreateUpdate()) {
      results.put(cls + "UpdateRequestDTO.java", UpdateRequestDtoTemplate.generate(model));
    }
    results.put(cls + "VO.java", VoTemplate.generate(model));
    results.put(cls + "Mapper.java", MapperInterfaceTemplate.generate(model));
    results.put(cls + "Mapper.xml", MapperXmlTemplate.generate(model));
    results.put(cls + "Service.java", ServiceTemplate.generate(model));
    results.put(cls + "Controller.java", ControllerTemplate.generate(model));
    results.put(cls + "ServiceTest.java", ServiceTestTemplate.generate(model));
    results.put(cls + "ControllerTest.java", ControllerTestTemplate.generate(model));
    results.put(model.domainId() + ".html", HtmlTemplate.generate(model));
    results.put(model.domainId() + ".js", JsTemplate.generate(model));
    results.put("메뉴등록.sql", MenuSqlTemplate.generate(model));
    return results;
  }

  private void enrichAndValidateCrudRequest(ScaffoldRequestDTO request, List<String> columns) {
    ScaffoldModel baseModel =
        new ScaffoldModel(request, columns, List.of(), Map.of(), scaffoldProperties.dialect());
    if (!baseModel.includeCreateUpdate()) {
      return;
    }

    String targetTable = baseModel.targetTable();
    if (!hasText(targetTable)) {
      throw new IllegalStateException("CRUD mode requires targetTable.");
    }

    ScaffoldTableMetadata metadata = metadataReader.read(targetTable);
    if (metadata.pkColumns().isEmpty()) {
      throw new IllegalStateException(
          "CRUD mode requires a real primary key. Table has no PK: " + targetTable);
    }

    List<String> pkColumns =
        request.getPkColumns().isEmpty()
            ? metadata.pkColumns()
            : normalizeColumns(request.getPkColumns());
    request.setPkColumns(pkColumns);
    request.setPkColumn(pkColumns.get(0));

    List<String> queryColumns = normalizeColumns(columns);
    for (String pkColumn : pkColumns) {
      if (!queryColumns.contains(pkColumn)) {
        throw new IllegalStateException("CRUD query must include PK column: " + pkColumn);
      }
    }

    String lockColumn = normalizeColumn(request.getLockColumn());
    if (hasText(lockColumn)) {
      if (!queryColumns.contains(lockColumn)) {
        throw new IllegalStateException("CRUD query must include lock column: " + lockColumn);
      }
      if (!metadata.nullableByColumn().containsKey(lockColumn)) {
        throw new IllegalStateException(
            "Lock column does not exist in target table: " + lockColumn);
      }
      if (pkColumns.contains(lockColumn)) {
        throw new IllegalStateException(
            "Optimistic lock column must not be a PK column: " + lockColumn);
      }
    }
  }

  private static List<String> normalizeColumns(List<String> columns) {
    return columns.stream()
        .map(ScaffoldService::normalizeColumn)
        .filter(ScaffoldService::hasText)
        .distinct()
        .toList();
  }

  private static String normalizeColumn(String column) {
    return column == null ? "" : column.trim().toUpperCase();
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
