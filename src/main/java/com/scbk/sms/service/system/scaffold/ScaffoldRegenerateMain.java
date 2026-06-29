package com.scbk.sms.service.system.scaffold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.dto.system.ScaffoldApplyFileResultDTO;
import com.scbk.sms.dto.system.ScaffoldCaseRecord;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * scaffold-cases/*.json 메타 파일을 읽어 모든 산출물을 일괄 재생성한다. Scaffold UI로 한 번 이상 apply한 화면은 메타 파일이 저장되어
 * 있으므로, 템플릿 코드가 바뀌어도 이 명령 한 번으로 모든 화면의 산출물이 자동 갱신된다.
 *
 * <p>실행: {@code mvn -q compile exec:java
 * -Dexec.mainClass=com.scbk.sms.service.system.scaffold.ScaffoldRegenerateMain}
 *
 * <p>Spring 컨테이너 없이 동작한다. typeMap이 메타 파일에 저장되어 있어 DB 접속이 불필요하다.
 */
public class ScaffoldRegenerateMain {

  public static void main(String[] args) {
    ScaffoldCaseStore caseStore = new ScaffoldCaseStore(new ObjectMapper());
    ScaffoldFileApplier applier = new ScaffoldFileApplier(Path.of(""));

    List<ScaffoldCaseRecord> records = caseStore.loadAll();
    if (records.isEmpty()) {
      System.out.println("scaffold-cases/*.json 이 없습니다. Scaffold UI에서 먼저 apply 하세요.");
      return;
    }

    System.out.println("scaffold-cases " + records.size() + "건 재생성 시작");
    for (ScaffoldCaseRecord record : records) {
      ScaffoldRequestDTO request = record.getRequest();
      ScaffoldDialect dialect = ScaffoldDialect.from(record.getDialect());
      ScaffoldModel model =
          new ScaffoldModel(
              request, record.getColumns(), record.getSearchVars(), record.getTypeMap(), dialect);

      Map<String, String> generated = generateAll(model);
      List<ScaffoldApplyFileResultDTO> results = applier.apply(request, generated);

      String caseName = request.getModuleName() + "/" + request.getDomainId();
      long overwritten = results.stream().filter(r -> "OVERWRITE".equals(r.getStatus())).count();
      long unchanged = results.stream().filter(r -> "UNCHANGED".equals(r.getStatus())).count();
      long newFiles = results.stream().filter(r -> "NEW".equals(r.getStatus())).count();
      System.out.printf(
          "  %s: NEW=%d, OVERWRITE=%d, UNCHANGED=%d%n", caseName, newFiles, overwritten, unchanged);
    }
    System.out.println("재생성 완료. mvn test 로 검증하세요.");
  }

  private static Map<String, String> generateAll(ScaffoldModel model) {
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
}
