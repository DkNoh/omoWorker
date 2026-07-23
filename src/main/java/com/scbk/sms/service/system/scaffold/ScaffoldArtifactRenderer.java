package com.scbk.sms.service.system.scaffold;

import java.util.LinkedHashMap;
import java.util.Map;

/** Scaffold 산출물의 파일명, tpl 경로, 생성 조건을 한곳에서 관리하고 공통 렌더러로 생성한다. */
public final class ScaffoldArtifactRenderer {

  private static final Map<String, String> PAGE_TEMPLATE_DIRECTORIES =
      Map.of(
          "LIST", "list",
          "EXCEL", "excel",
          "CRUD", "crud");

  private ScaffoldArtifactRenderer() {}

  public static Map<String, String> renderAll(ScaffoldModel model) {
    Map<String, String> files = new LinkedHashMap<>();
    for (Artifact artifact : Artifact.values()) {
      if (artifact.enabled(model)) {
        files.put(artifact.outputName(model), render(artifact, model));
      }
    }
    return files;
  }

  public static Map<String, String> renderSelected(ScaffoldModel model, Artifact... artifacts) {
    Map<String, String> files = new LinkedHashMap<>();
    for (Artifact artifact : artifacts) {
      if (artifact.enabled(model)) {
        files.put(artifact.outputName(model), render(artifact, model));
      }
    }
    return files;
  }

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
