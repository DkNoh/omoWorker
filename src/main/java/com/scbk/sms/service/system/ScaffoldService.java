package com.scbk.sms.service.system;

import com.scbk.sms.config.ScaffoldProperties;
import com.scbk.sms.dto.system.ScaffoldApplyFileResultDTO;
import com.scbk.sms.dto.system.ScaffoldCaseRecord;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.service.system.scaffold.ColumnTypeInferrer;
import com.scbk.sms.service.system.scaffold.QueryColumnExtractor;
import com.scbk.sms.service.system.scaffold.QueryColumnExtractor.SearchParameterSource;
import com.scbk.sms.service.system.scaffold.QueryColumnExtractor.SearchRangePosition;
import com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer;
import com.scbk.sms.service.system.scaffold.ScaffoldCaseStore;
import com.scbk.sms.service.system.scaffold.ScaffoldDialect;
import com.scbk.sms.service.system.scaffold.ScaffoldFileApplier;
import com.scbk.sms.service.system.scaffold.ScaffoldMetadataReader;
import com.scbk.sms.service.system.scaffold.ScaffoldModel;
import com.scbk.sms.service.system.scaffold.ScaffoldTableMetadata;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * QuerySpec({@code rawQuery}와 {@code $검색변수})를 완성된 화면 산출물로 변환하는 애플리케이션 서비스.
 *
 * <p>처리 순서는 SQL 구조 분석 → JDBC 타입/테이블 메타데이터 조회 → CRUD 무결성 검증 → {@link ScaffoldModel} 구성 → 템플릿
 * 렌더링이다. {@link #preview(ScaffoldRequestDTO)}는 파일 상태만 계산하고, {@link #apply(ScaffoldRequestDTO)}만 재생성용 case와 실제
 * 파일을 기록한다. 이 차이는 확인창 이전 요청이 프로젝트 파일을 바꾸지 않는다는 UI 계약을 보장한다.
 */
@Service
@Profile("local")
public class ScaffoldService {

  private final ColumnTypeInferrer columnTypeInferrer;
  private final ScaffoldFileApplier scaffoldFileApplier;
  private final ScaffoldMetadataReader metadataReader;
  private final ScaffoldProperties scaffoldProperties;
  private final ScaffoldCaseStore caseStore;

  public ScaffoldService(
      ColumnTypeInferrer columnTypeInferrer,
      ScaffoldFileApplier scaffoldFileApplier,
      ScaffoldMetadataReader metadataReader,
      ScaffoldProperties scaffoldProperties,
      ScaffoldCaseStore caseStore) {
    this.columnTypeInferrer = columnTypeInferrer;
    this.scaffoldFileApplier = scaffoldFileApplier;
    this.metadataReader = metadataReader;
    this.scaffoldProperties = scaffoldProperties;
    this.caseStore = caseStore;
  }

  /** 요청을 분석·렌더링하고 파일명별 소스 문자열을 반환한다. 디스크에는 쓰지 않는다. */
  public Map<String, String> generate(ScaffoldRequestDTO request) {
    return generateFiles(request);
  }

  /**
   * 화면 옵션의 초기값을 만들기 위해 SELECT 컬럼, 검색변수, 대상 테이블 메타데이터를 분석한다.
   *
   * <p>DB comment는 직접 컬럼의 화면명 기본값으로만 사용한다. 계산 컬럼이나 comment가 없는 컬럼은 출력 alias/변수명으로
   * fallback하며, 최종 화면명은 사용자가 UI에서 수정할 수 있다.
   */
  public Map<String, Object> analyze(String rawQuery, String targetTable) {
    String resolvedTargetTable =
        hasText(targetTable) ? targetTable : QueryColumnExtractor.extractPrimaryTable(rawQuery);
    ScaffoldTableMetadata metadata = metadataReader.read(resolvedTargetTable);
    List<String> columns = QueryColumnExtractor.extractColumns(rawQuery);
    List<String> searchVars = QueryColumnExtractor.extractSearchVars(rawQuery);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("columns", columns);
    result.put("searchVars", searchVars);
    result.put("targetTable", resolvedTargetTable);
    result.put("pkColumns", metadata.pkColumns());
    result.put("nullableColumns", metadata.nullableByColumn());
    result.put("columnComments", resolveColumnComments(rawQuery, columns, metadata));
    result.put("searchParamLabels", resolveSearchParamLabels(rawQuery, searchVars, metadata));
    result.put("dbPlatform", scaffoldProperties.getDbPlatform());
    return result;
  }

  private Map<String, String> resolveSearchParamLabels(
      String rawQuery, List<String> searchVars, ScaffoldTableMetadata metadata) {
    Map<String, SearchParameterSource> sources =
        QueryColumnExtractor.extractSearchParameterSources(rawQuery);
    Map<String, String> directSources = QueryColumnExtractor.extractDirectColumnSources(rawQuery);
    Map<String, String> result = new LinkedHashMap<>();
    for (String searchVar : searchVars) {
      SearchParameterSource source = sources.get(searchVar);
      String label = "";
      if (source != null && source.rangePosition() == SearchRangePosition.START) {
        label = "시작일자";
      } else if (source != null && source.rangePosition() == SearchRangePosition.END) {
        label = "종료일자";
      } else if (source != null) {
        label = metadata.comment(source.columnName());
        if (!hasText(label)) {
          label = findOutputAlias(directSources, source.columnName());
        }
      }
      result.put(searchVar, hasText(label) ? label.trim() : searchVar);
    }
    return result;
  }

  private String findOutputAlias(Map<String, String> directSources, String sourceColumn) {
    return directSources.entrySet().stream()
        .filter(entry -> entry.getValue().equals(sourceColumn))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse("");
  }

  private Map<String, String> resolveColumnComments(
      String rawQuery, List<String> columns, ScaffoldTableMetadata metadata) {
    Map<String, String> directSources = QueryColumnExtractor.extractDirectColumnSources(rawQuery);
    Map<String, String> result = new LinkedHashMap<>();
    for (String column : columns) {
      String outputColumn = normalizeColumn(column);
      String sourceColumn = directSources.getOrDefault(outputColumn, outputColumn);
      String comment = metadata.comment(sourceColumn);
      if (!hasText(comment) && !sourceColumn.equals(outputColumn)) {
        comment = metadata.comment(outputColumn);
      }
      if (hasText(comment)) {
        result.put(outputColumn, comment.trim());
      }
    }
    return result;
  }

  /** 생성 파일의 대상 경로와 덮어쓰기 여부를 계산하되 파일과 case 메타데이터는 저장하지 않는다. */
  public List<ScaffoldApplyFileResultDTO> preview(ScaffoldRequestDTO request) {
    Map<String, String> generatedFiles = generateFiles(request);
    return scaffoldFileApplier.preview(request, generatedFiles);
  }

  /**
   * 확정된 요청을 재생성 case로 보관한 뒤 프로젝트 경로에 적용한다.
   *
   * <p>case에는 DB 재접속 없이 동일 결과를 재생성할 수 있도록 추출 컬럼, 검색변수, 타입, 방언까지 함께 저장한다.
   */
  public List<ScaffoldApplyFileResultDTO> apply(ScaffoldRequestDTO request) {
    List<String> columns = QueryColumnExtractor.extractColumns(request.getRawQuery());
    List<String> searchVars = QueryColumnExtractor.extractSearchVars(request.getRawQuery());
    Map<String, String> typeMap = columnTypeInferrer.inferTypes(request.getRawQuery(), columns);
    ScaffoldCaseRecord record = new ScaffoldCaseRecord();
    record.setRequest(request);
    record.setColumns(columns);
    record.setSearchVars(searchVars);
    record.setTypeMap(typeMap);
    record.setDialect(scaffoldProperties.dialect().name());
    caseStore.save(record);

    Map<String, String> generatedFiles = generateFiles(request);
    return scaffoldFileApplier.apply(request, generatedFiles);
  }

  /** 분석 결과를 하나의 불변 렌더링 모델로 묶어 모드별 전체 산출물을 생성한다. */
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
    return ScaffoldArtifactRenderer.renderAll(model);
  }

  /**
   * CRUD 요청을 실제 테이블 메타데이터로 보강하고 PK·낙관적 잠금 컬럼의 안전 조건을 검증한다.
   *
   * <p>클라이언트가 보낸 PK나 잠금 컬럼을 그대로 신뢰하지 않는다. 대상 테이블의 실제 PK를 기본값으로 사용하고, 모든 PK와 잠금 컬럼이
   * SELECT 결과 및 테이블 메타데이터에 존재할 때만 쓰기 SQL 생성을 허용한다.
   */
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
