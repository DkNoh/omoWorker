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
 * <p>실행:
 *
 * <pre>{@code
 * // 일괄 재생성 (파일 쓰기)
 * mvn -q compile exec:java \
 *   -Dexec.mainClass=com.scbk.sms.service.system.scaffold.ScaffoldRegenerateMain
 *
 * // dry-run (파일 미쓰기, 변경 감지만)
 * mvn -q compile exec:java \
 *   -Dexec.mainClass=com.scbk.sms.service.system.scaffold.ScaffoldRegenerateMain \
 *   -Dexec.args=--dry-run
 * }</pre>
 *
 * <p>Spring 컨테이너 없이 동작한다. typeMap이 메타 파일에 저장되어 있어 DB 접속이 불필요하다. {@code --dry-run}은 {@link
 * ScaffoldFileApplier#preview}만 호출하고 파일을 전혀 쓰지 않는다. dry-run에서 NEW/OVERWRITE가 감지되면 {@link
 * #run(String[], List, ScaffoldFileApplier)}이 {@code false}를 반환하고 main은 exit code 1로 종료한다.
 */
public class ScaffoldRegenerateMain {

  /** dry-run 활성화 플래그. */
  static final String DRY_RUN_FLAG = "--dry-run";

  public static void main(String[] args) {
    ScaffoldCaseStore caseStore = new ScaffoldCaseStore(new ObjectMapper());
    ScaffoldFileApplier applier = new ScaffoldFileApplier(Path.of(""));

    boolean unchanged = run(args, caseStore.loadAll(), applier);
    if (!unchanged) {
      System.exit(1);
    }
  }

  /**
   * 모든 scaffold-cases를 재생성한다.
   *
   * <p>{@code args}에 {@link #DRY_RUN_FLAG}가 있으면 {@link ScaffoldFileApplier#preview}만 호출해 디스크 쓰기를 전혀
   * 하지 않는다. dry-run 모드에서 모든 케이스가 UNCHANGED(NEW=0, OVERWRITE=0)이면 {@code true}를 반환하고, 하나라도
   * NEW/OVERWRITE가 있으면 {@code false}를 반환한다. 기본(쓰기) 모드는 재생성 후 항상 {@code true}를 반환한다.
   *
   * <p>이 메서드는 package-private이며 실제 의존성(main이 구성한 caseStore/applier)을 주입받아 {@link
   * ScaffoldRegenerateMainTest}에서 검증 가능하다.
   *
   * @param args CLI 인자. {@code null}이면 dry-run 미적용으로 간주한다.
   * @param records 재생성 대상 scaffold-cases. 비어 있으면 안내만 출력하고 {@code true}를 반환한다.
   * @param applier preview/apply를 수행할 applier. dry-run 여부에 따라 분기한다.
   * @return dry-run이면 모든 케이스 UNCHANGED 여부, 기본 모드면 항상 {@code true}
   */
  static boolean run(String[] args, List<ScaffoldCaseRecord> records, ScaffoldFileApplier applier) {
    boolean dryRun = isDryRun(args);

    if (records.isEmpty()) {
      System.out.println("scaffold-cases/*.json 이 없습니다. Scaffold UI에서 먼저 apply 하세요.");
      return true;
    }

    System.out.println(
        "scaffold-cases " + records.size() + "건 재생성 시작" + (dryRun ? " (dry-run)" : ""));
    long totalNew = 0;
    long totalOverwrite = 0;
    for (ScaffoldCaseRecord record : records) {
      ScaffoldRequestDTO request = record.getRequest();
      ScaffoldDialect dialect = ScaffoldDialect.from(record.getDialect());
      ScaffoldModel model =
          new ScaffoldModel(
              request, record.getColumns(), record.getSearchVars(), record.getTypeMap(), dialect);

      Map<String, String> generated = generateAll(model);
      List<ScaffoldApplyFileResultDTO> results =
          dryRun ? applier.preview(request, generated) : applier.apply(request, generated);

      String caseName = request.getModuleName() + "/" + request.getDomainId();
      long overwritten = results.stream().filter(r -> "OVERWRITE".equals(r.getStatus())).count();
      long unchanged = results.stream().filter(r -> "UNCHANGED".equals(r.getStatus())).count();
      long newFiles = results.stream().filter(r -> "NEW".equals(r.getStatus())).count();
      totalNew += newFiles;
      totalOverwrite += overwritten;
      System.out.printf(
          "  %s: NEW=%d, OVERWRITE=%d, UNCHANGED=%d%n", caseName, newFiles, overwritten, unchanged);
    }

    if (dryRun) {
      if (totalNew == 0 && totalOverwrite == 0) {
        System.out.println("dry-run 완료: 모든 산출물 UNCHANGED. 변경 사항 없음.");
        return true;
      }
      System.out.printf(
          "dry-run 경고: 변경 감지 (NEW=%d, OVERWRITE=%d). 템플릿/규약 변경이 의도면 apply로 반영하세요.%n",
          totalNew, totalOverwrite);
      return false;
    }
    System.out.println("재생성 완료. mvn test 로 검증하세요.");
    return true;
  }

  private static boolean isDryRun(String[] args) {
    if (args == null) {
      return false;
    }
    for (String arg : args) {
      if (DRY_RUN_FLAG.equals(arg)) {
        return true;
      }
    }
    return false;
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
    results.putAll(ScaffoldPageRenderers.render(model));
    results.put("메뉴등록.sql", MenuSqlTemplate.generate(model));
    return results;
  }
}
