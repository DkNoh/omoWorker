package com.scbk.sms.service.system.scaffold;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 산출물 종류, 파일명, 템플릿 경로와 생성 조건을 한곳에서 관리하는 렌더링 오케스트레이터.
 *
 * <p>호출부가 파일별 분기 로직을 중복하지 않도록 {@link Artifact}가 산출물 계약을 소유한다. 템플릿에는 기본적으로 {@code model}을
 * 제공하고, Mapper XML·메뉴 SQL·페이지 JavaScript처럼 별도 계산값이 필요한 산출물만 전용 변수를 추가한다.
 */
public final class ScaffoldArtifactRenderer {

  private static final Map<String, String> PAGE_TEMPLATE_DIRECTORIES =
      Map.of(
          "LIST", "list",
          "EXCEL", "excel",
          "CRUD", "crud");

  private ScaffoldArtifactRenderer() {}

  /** 현재 화면 모드에서 활성화된 모든 산출물을 선언 순서대로 렌더링한다. */
  public static Map<String, String> renderAll(ScaffoldModel model) {
    Map<String, String> files = new LinkedHashMap<>();
    for (Artifact artifact : Artifact.values()) {
      if (artifact.enabled(model)) {
        files.put(artifact.outputName(model), render(artifact, model));
      }
    }
    return files;
  }

  /** 테스트나 부분 미리보기에서 지정한 산출물만 렌더링하되 모드별 생성 조건은 동일하게 적용한다. */
  public static Map<String, String> renderSelected(ScaffoldModel model, Artifact... artifacts) {
    Map<String, String> files = new LinkedHashMap<>();
    for (Artifact artifact : artifacts) {
      if (artifact.enabled(model)) {
        files.put(artifact.outputName(model), render(artifact, model));
      }
    }
    return files;
  }

  /** 산출물 하나의 템플릿 경로와 컨텍스트 변수를 확정해 TEXT 템플릿을 렌더링한다. */
  public static String render(Artifact artifact, ScaffoldModel model) {
    return ResourceTemplateRenderer.render(
        artifact.templatePath(model), variables(artifact, model));
  }

  private static Map<String, ?> variables(Artifact artifact, ScaffoldModel model) {
    if (artifact == Artifact.MAPPER_XML) {
      return Map.of("model", model, "xml", MapperXmlViewFactory.create(model));
    }
    if (artifact == Artifact.MENU_SQL) {
      String mode = model.screenMode();
      boolean crudMode = "CRUD".equals(mode);
      return Map.of(
          "model", model,
          "canCreate", crudMode ? "Y" : "N",
          "canUpdate", crudMode ? "Y" : "N",
          "canDelete", crudMode ? "Y" : "N",
          "canDownload", "EXCEL".equals(mode) ? "Y" : "N",
          "canMaskView", model.includePrivacy() ? "Y" : "N");
    }
    if (artifact == Artifact.PAGE_JS) {
      String rowHeaders = model.getRequest().isShowRowNumber() ? "['rowNum']" : "[]";
      return Map.of("model", model, "rowHeaders", rowHeaders);
    }
    return Map.of("model", model);
  }

  private static String pageTemplateDirectory(ScaffoldModel model) {
    String directory = PAGE_TEMPLATE_DIRECTORIES.get(model.screenMode());
    if (directory == null) {
      throw new IllegalArgumentException(
          "지원하지 않는 screenMode입니다: " + model.screenMode() + " (지원: LIST, EXCEL, CRUD)");
    }
    return directory;
  }

  /** 생성 가능한 파일 종류. enum 선언 순서가 미리보기와 적용 결과의 표시 순서가 된다. */
  public enum Artifact {
    SEARCH_DTO,
    UPDATE_DTO,
    VO,
    MAPPER_INTERFACE,
    MAPPER_XML,
    SERVICE,
    CONTROLLER,
    SERVICE_TEST,
    CONTROLLER_TEST,
    PAGE_HTML,
    PAGE_JS,
    MENU_SQL;

    boolean enabled(ScaffoldModel model) {
      return this != UPDATE_DTO || model.includeCreateUpdate();
    }

    String outputName(ScaffoldModel model) {
      return switch (this) {
        case SEARCH_DTO -> model.domainClass() + "SearchRequestDTO.java";
        case UPDATE_DTO -> model.domainClass() + "UpdateRequestDTO.java";
        case VO -> model.domainClass() + "VO.java";
        case MAPPER_INTERFACE -> model.domainClass() + "Mapper.java";
        case MAPPER_XML -> model.domainClass() + "Mapper.xml";
        case SERVICE -> model.domainClass() + "Service.java";
        case CONTROLLER -> model.domainClass() + "Controller.java";
        case SERVICE_TEST -> model.domainClass() + "ServiceTest.java";
        case CONTROLLER_TEST -> model.domainClass() + "ControllerTest.java";
        case PAGE_HTML -> model.domainId() + ".html";
        case PAGE_JS -> model.domainId() + ".js";
        case MENU_SQL -> "메뉴등록.sql";
      };
    }

    String templatePath(ScaffoldModel model) {
      return switch (this) {
        case SEARCH_DTO -> "scaffold-templates/dto.java.tpl";
        case UPDATE_DTO -> "scaffold-templates/update-request-dto.java.tpl";
        case VO -> "scaffold-templates/vo.java.tpl";
        case MAPPER_INTERFACE -> "scaffold-templates/mapper-interface.java.tpl";
        case MAPPER_XML -> "scaffold-templates/mapper-xml.xml.tpl";
        case SERVICE -> "scaffold-templates/service.java.tpl";
        case CONTROLLER -> "scaffold-templates/controller.java.tpl";
        case SERVICE_TEST -> "scaffold-templates/service-test.java.tpl";
        case CONTROLLER_TEST -> "scaffold-templates/controller-test.java.tpl";
        case PAGE_HTML -> "scaffold-templates/" + pageTemplateDirectory(model) + "/page.html.tpl";
        case PAGE_JS -> "scaffold-templates/" + pageTemplateDirectory(model) + "/page.js.tpl";
        case MENU_SQL -> "scaffold-templates/menu-sql.sql.tpl";
      };
    }
  }
}
