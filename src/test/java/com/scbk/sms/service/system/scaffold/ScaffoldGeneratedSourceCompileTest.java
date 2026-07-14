package com.scbk.sms.service.system.scaffold;

import static com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer.Artifact.*;
import static com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer.render;
import static org.assertj.core.api.Assertions.assertThat;

import com.scbk.sms.dto.system.ScaffoldColumnOptionDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * full 모델이 생성한 모든 Java 소스를 JDK 21 compiler로 실제 컴파일한다.
 *
 * <p>리팩터링 후에도 생성 코드가 컴파일 가능한지 검증한다. {@link ToolProvider#getSystemJavaCompiler()}를 사용하여 {@code
 * --release 21} 옵션으로 컴파일한다.
 *
 * <p>classpath는 {@code surefire.test.class.path} 시스템 프로퍼티를 사용하고, 없으면 {@code java.class.path}로 폴백한다.
 * Lombok annotation processor를 processor path에 포함한다.
 *
 * <p>Self-contained: ScaffoldTemplateTest/ScaffoldOutputGoldenTest의 private helper를 호출하지 않는다.
 */
class ScaffoldGeneratedSourceCompileTest {

  @TempDir Path tempDir;

  // ============================================================================================
  // Self-contained full model (same topology as ScaffoldOutputGoldenTest.fullModel)
  // ============================================================================================

  private ScaffoldModel fullModel() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        """
            SELECT A.SMS_HISTORY_ID, A.SEND_DT, A.SEND_TYPE, A.SEND_STATUS, A.RECEIVER_NO, A.UPD_DTTM
            FROM SMS_HISTORY A
            WHERE 1=1
            AND A.SEND_DT >= $send_dt_from
            AND A.SEND_DT <= $send_dt_to
            AND A.SEND_TYPE = $send_type
            AND A.SEND_STATUS = $send_status
            """);
    request.setOrderBy("A.SEND_DT DESC, A.SMS_HISTORY_ID DESC");

    ScaffoldColumnOptionDTO receiverNo = new ScaffoldColumnOptionDTO();
    receiverNo.setColumnName("RECEIVER_NO");
    receiverNo.setVisible(true);
    receiverNo.setModalVisible(true);
    receiverNo.setEditable(true);
    receiverNo.setHeaderName("수신번호");
    receiverNo.setWidth(160);
    receiverNo.setAlign("left");
    receiverNo.setDateFormat("NONE");
    receiverNo.setMaskType("PHONE");

    ScaffoldColumnOptionDTO sendType = new ScaffoldColumnOptionDTO();
    sendType.setColumnName("SEND_TYPE");
    sendType.setVisible(true);
    sendType.setModalVisible(true);
    sendType.setEditable(true);
    sendType.setHeaderName("발송유형");
    sendType.setWidth(120);
    sendType.setAlign("center");
    sendType.setDateFormat("NONE");
    sendType.setMaskType("NONE");
    sendType.setValidate("required");

    request.setColumnOptions(List.of(receiverNo, sendType));
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    request.setIncludePrivacy(true);
    request.setIncludeExcel(true);

    return new ScaffoldModel(
        request,
        List.of("SMS_HISTORY_ID", "SEND_DT", "SEND_TYPE", "SEND_STATUS", "RECEIVER_NO", "UPD_DTTM"),
        List.of("sendDtFrom", "sendDtTo", "sendType", "sendStatus"),
        Map.of(
            "SMS_HISTORY_ID", "Long",
            "SEND_DT", "LocalDateTime",
            "SEND_TYPE", "String",
            "SEND_STATUS", "String",
            "RECEIVER_NO", "String",
            "UPD_DTTM", "LocalDateTime"));
  }

  private ScaffoldModel privacyOnlyModel() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("privacy-history");
    request.setDomainClass("PrivacyHistory");
    request.setDomainName("개인정보이력조회");
    request.setRawQuery("SELECT A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1");
    request.setOrderBy("A.RECEIVER_NO");
    request.setScreenMode("LIST");
    request.setPkColumn("RECEIVER_NO");
    request.setIncludePrivacy(true);
    return new ScaffoldModel(
        request, List.of("RECEIVER_NO"), List.of(), Map.of("RECEIVER_NO", "String"));
  }

  // ============================================================================================
  // Java source extraction helpers
  // ============================================================================================

  private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
  private static final Pattern TYPE_PATTERN =
      Pattern.compile(
          "(?:public\\s+)?(?:final\\s+)?(?:abstract\\s+)?(?:class|interface|enum|record)\\s+(\\w+)");

  /** 생성된 Java 소스에서 package와 type name을 추출하여 파일 경로를 결정한다. */
  private Path sourceFilePath(Path srcRoot, String source) {
    Matcher pkgMatcher = PACKAGE_PATTERN.matcher(source);
    assertThat(pkgMatcher.find()).as("생성된 소스에 package 선언이 없습니다:\n" + source).isTrue();
    String pkg = pkgMatcher.group(1);

    Matcher typeMatcher = TYPE_PATTERN.matcher(source);
    assertThat(typeMatcher.find()).as("생성된 소스에 class/interface 선언이 없습니다:\n" + source).isTrue();
    String typeName = typeMatcher.group(1);

    return srcRoot.resolve(pkg.replace('.', '/')).resolve(typeName + ".java");
  }

  /** full 모델의 모든 Java 템플릿 출력을 생성하여 소스 디렉토리에 기록한다. */
  private Path writeGeneratedSources(ScaffoldModel model) throws IOException {
    Path srcRoot = tempDir.resolve("src");
    Files.createDirectories(srcRoot);

    Map<String, String> generated = new LinkedHashMap<>();
    generated.put("DTO", render(SEARCH_DTO, model));
    if (model.includeCreateUpdate()) {
      generated.put("UPDATE_REQUEST_DTO", render(UPDATE_DTO, model));
    }
    generated.put("VO", render(VO, model));
    generated.put("MAPPER", render(MAPPER_INTERFACE, model));
    generated.put("SERVICE", render(SERVICE, model));
    generated.put("CONTROLLER", render(CONTROLLER, model));
    generated.put("SERVICE_TEST", render(SERVICE_TEST, model));
    generated.put("CONTROLLER_TEST", render(CONTROLLER_TEST, model));

    List<Path> sourceFiles = new ArrayList<>();
    for (Map.Entry<String, String> entry : generated.entrySet()) {
      String name = entry.getKey();
      String source = entry.getValue();
      Path file = sourceFilePath(srcRoot, source);
      Files.createDirectories(file.getParent());
      Files.writeString(file, source, StandardCharsets.UTF_8);
      sourceFiles.add(file);
    }
    return srcRoot;
  }

  // ============================================================================================
  // Compilation
  // ============================================================================================

  private String resolveClasspath() {
    String cp = System.getProperty("surefire.test.class.path");
    if (cp == null || cp.isBlank()) {
      cp = System.getProperty("java.class.path");
    }
    assertThat(cp)
        .as("classpath를 결정할 수 없습니다 (surefire.test.class.path / java.class.path)")
        .isNotNull();
    return cp;
  }

  @Test
  void full_모델_생성_Java_소스가_JDK21으로_컴파일된다() throws IOException {
    // given
    ScaffoldModel model = fullModel();
    Path srcRoot = writeGeneratedSources(model);
    Path classesDir = tempDir.resolve("classes");
    Files.createDirectories(classesDir);

    // JDK compiler 확인
    var compiler = ToolProvider.getSystemJavaCompiler();
    assertThat(compiler)
        .as("ToolProvider.getSystemJavaCompiler()가 null입니다. JDK로 실행해야 합니다 (JRE 불가).")
        .isNotNull();

    String classpath = resolveClasspath();

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    // when: 컴파일 실행
    try (var fileManager =
        compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
      fileManager.setLocation(StandardLocation.CLASS_PATH, List.of());
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classesDir.toFile()));

      // 소스 파일 수집
      Iterable<? extends JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjectsFromFiles(
              Files.walk(srcRoot)
                  .filter(p -> p.toString().endsWith(".java"))
                  .map(Path::toFile)
                  .toList());

      List<String> options =
          List.of(
              "--release",
              "21",
              "-classpath",
              classpath,
              "-processorpath",
              classpath,
              "-d",
              classesDir.toString());

      javax.tools.JavaCompiler.CompilationTask task =
          compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

      boolean success = task.call();

      // then: 컴파일 성공
      if (!success) {
        StringBuilder sb = new StringBuilder("컴파일 실패 — diagnostics:\n");
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
          if (d.getKind() == Diagnostic.Kind.ERROR) {
            sb.append("  ERROR ");
            JavaFileObject source = d.getSource();
            if (source != null) {
              sb.append(source.getName()).append(":").append(d.getLineNumber()).append(":");
            }
            sb.append(d.getMessage(null)).append("\n");
          }
        }
        assertThat(success).as(sb.toString()).isTrue();
      }

      // 에러 diagnostic이 0개여야 한다
      long errorCount =
          diagnostics.getDiagnostics().stream()
              .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
              .count();
      assertThat(errorCount).as("컴파일은 성공했지만 ERROR diagnostic이 있습니다").isZero();

      // 컴파일된 .class 파일이 존재해야 한다
      assertThat(classesDir.resolve("com/scbk/sms/dto/sms/SmsHistorySearchRequestDTO.class"))
          .exists();
      assertThat(classesDir.resolve("com/scbk/sms/dto/sms/SmsHistoryUpdateRequestDTO.class"))
          .exists();
      assertThat(classesDir.resolve("com/scbk/sms/vo/sms/SmsHistoryVO.class")).exists();
      assertThat(classesDir.resolve("com/scbk/sms/mapper/sms/SmsHistoryMapper.class")).exists();
      assertThat(classesDir.resolve("com/scbk/sms/service/sms/SmsHistoryService.class")).exists();
      assertThat(classesDir.resolve("com/scbk/sms/controller/sms/SmsHistoryController.class"))
          .exists();
    }
  }

  @Test
  void privacy_only_모델_생성_Java_소스가_JDK21으로_컴파일된다() throws IOException {
    compileGeneratedSources(privacyOnlyModel(), "privacy-only");
  }

  private void compileGeneratedSources(ScaffoldModel model, String outputName) throws IOException {
    Path srcRoot = writeGeneratedSources(model);
    Path classesDir = tempDir.resolve(outputName + "-classes");
    Files.createDirectories(classesDir);
    var compiler = ToolProvider.getSystemJavaCompiler();
    assertThat(compiler).isNotNull();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    try (var fileManager =
        compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
      Iterable<? extends JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjectsFromFiles(
              Files.walk(srcRoot)
                  .filter(path -> path.toString().endsWith(".java"))
                  .map(Path::toFile)
                  .toList());
      List<String> options =
          List.of(
              "--release",
              "21",
              "-classpath",
              resolveClasspath(),
              "-processorpath",
              resolveClasspath(),
              "-d",
              classesDir.toString());

      // when
      boolean success =
          compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

      // then
      assertThat(success)
          .as(outputName + " generated sources failed to compile:\n" + diagnostics.getDiagnostics())
          .isTrue();
    }
  }
}
