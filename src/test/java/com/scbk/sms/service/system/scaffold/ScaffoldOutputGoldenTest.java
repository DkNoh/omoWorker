package com.scbk.sms.service.system.scaffold;

import static com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer.render;
import static org.assertj.core.api.Assertions.assertThat;

import com.scbk.sms.dto.system.ScaffoldColumnOptionDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer.Artifact;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Scaffold 템플릿(byte-level) golden 회귀 테스트.
 *
 * <p>현재 리소스 {@code .tpl} 기반 템플릿의 출력을 byte-level로 고정한다. Java 모델과 템플릿의 책임을 변경해도 의도하지 않은 출력 변화가 없는지
 * 검증한다.
 *
 * <p>모드:
 *
 * <ul>
 *   <li>기본 (compare): golden 파일과 {@code assertThat(actual).isEqualTo(expected)} 비교. 파일이 없으면 실패.
 *   <li>{@code -Dgolden.record=true}: 모든 모델의 golden 파일을 (re)write.
 *   <li>{@code -Dgolden.record=true -Dgolden.record.models=KEY[,KEY...]}: 지정 모델만 write; 나머지는 반드시
 *       compare.
 * </ul>
 *
 * <p>Self-contained: ScaffoldTemplateTest의 private helper를 호출하지 않고 이 클래스 내부에 동등한 helper를 재정의한다.
 */
class ScaffoldOutputGoldenTest {

  private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "scaffold-golden");

  private static final boolean RECORD =
      "true".equalsIgnoreCase(System.getProperty("golden.record"));
  private static final String SELECTOR = System.getProperty("golden.record.models");

  private static final Set<String> KNOWN_MODELS =
      Set.of("base", "crud", "full", "postgres", "db2", "keyword");

  @BeforeAll
  static void validateSelector() {
    if (SELECTOR != null) {
      String[] keys = SELECTOR.split(",");
      for (String raw : keys) {
        String key = raw.trim().toLowerCase();
        if (key.isEmpty()) {
          throw new AssertionError("golden.record.models에 빈 key가 있습니다. 알려진 key: " + KNOWN_MODELS);
        }
        if (!KNOWN_MODELS.contains(key)) {
          throw new AssertionError(
              "golden.record.models에 알 수 없는 model key '"
                  + key
                  + "' 가 있습니다. 알려진 key: "
                  + KNOWN_MODELS);
        }
      }
    }
    if (RECORD) {
      try {
        Files.createDirectories(GOLDEN_DIR);
      } catch (IOException e) {
        throw new IllegalStateException("golden 디렉토리 생성 실패: " + GOLDEN_DIR, e);
      }
    }
  }

  // ============================================================================================
  // Models
  // ============================================================================================

  // ---- model() helper (self-contained copy of ScaffoldTemplateTest.model) ----

  private ScaffoldModel model(boolean createUpdate, boolean excel, boolean privacy) {
    ScaffoldRequestDTO request = baseRequest();
    request.setIncludeCreateUpdate(createUpdate);
    request.setIncludeExcel(excel);
    request.setIncludePrivacy(privacy);
    if (createUpdate) {
      request.setPkColumn("RECEIVER_NO");
    }
    return new ScaffoldModel(
        request,
        List.of("SEND_DT", "RECEIVER_NO"),
        List.of("startDt"),
        Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));
  }

  private ScaffoldRequestDTO baseRequest() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");
    return request;
  }

  // ---- requestWithOptions() helper (self-contained copy) ----

  private ScaffoldRequestDTO requestWithOptions() {
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
    return request;
  }

  private static final List<String> SIX_COLUMNS =
      List.of("SMS_HISTORY_ID", "SEND_DT", "SEND_TYPE", "SEND_STATUS", "RECEIVER_NO", "UPD_DTTM");
  private static final Map<String, String> SIX_TYPE_MAP =
      Map.of(
          "SMS_HISTORY_ID", "Long",
          "SEND_DT", "LocalDateTime",
          "SEND_TYPE", "String",
          "SEND_STATUS", "String",
          "RECEIVER_NO", "String",
          "UPD_DTTM", "LocalDateTime");

  private ScaffoldModel sixColumnModel(ScaffoldRequestDTO request) {
    return new ScaffoldModel(
        request,
        SIX_COLUMNS,
        List.of("sendDtFrom", "sendDtTo", "sendType", "sendStatus"),
        SIX_TYPE_MAP);
  }

  private ScaffoldModel sixColumnModel(ScaffoldRequestDTO request, ScaffoldDialect dialect) {
    return new ScaffoldModel(
        request,
        SIX_COLUMNS,
        List.of("sendDtFrom", "sendDtTo", "sendType", "sendStatus"),
        SIX_TYPE_MAP,
        dialect);
  }

  // ---- columnOption() helper (self-contained copy) ----

  private ScaffoldColumnOptionDTO columnOption(
      String columnName,
      boolean visible,
      boolean modalVisible,
      boolean editable,
      String headerName,
      int width,
      String align,
      String dateFormat,
      String maskType) {
    ScaffoldColumnOptionDTO option = new ScaffoldColumnOptionDTO();
    option.setColumnName(columnName);
    option.setVisible(visible);
    option.setModalVisible(modalVisible);
    option.setEditable(editable);
    option.setHeaderName(headerName);
    option.setWidth(width);
    option.setAlign(align);
    option.setDateFormat(dateFormat);
    option.setMaskType(maskType);
    return option;
  }

  // ---- Individual model factory methods ----

  /** base: LIST 전용. searchVars=["startDt"], WHERE 포함 rawQuery. */
  private ScaffoldModel baseModel() {
    return model(false, false, false);
  }

  /** crud: CRUD. pkColumn="RECEIVER_NO". */
  private ScaffoldModel crudModel() {
    return model(true, false, false);
  }

  /**
   * full: 6컬럼 topology + CRUD + PK=SMS_HISTORY_ID + lock=UPD_DTTM + Excel + Privacy + masking +
   * validate.
   *
   * <p>screenMode를 명시하지 않고 legacy {@code includeCreateUpdate=true} + {@code includeExcel=true}로 설정해
   * 빈-mode legacy 결합(CRUD+Excel) fallback을 의도적으로 검증한다. 명시적 screenMode가 feature flag보다 우선하는 규약(Task
   * 7) 아래에서도 동일한 산출물(CRUD 모드 + 엑셀)을 내도록 한다.
   */
  private ScaffoldModel fullModel() {
    ScaffoldRequestDTO request = requestWithOptions();
    ScaffoldColumnOptionDTO receiverNo =
        columnOption("RECEIVER_NO", true, true, true, "수신번호", 160, "left", "NONE", "PHONE");
    ScaffoldColumnOptionDTO sendType =
        columnOption("SEND_TYPE", true, true, true, "발송유형", 120, "center", "NONE", "NONE");
    sendType.setValidate("required");
    request.setColumnOptions(List.of(receiverNo, sendType));
    request.setIncludeCreateUpdate(true);
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    request.setIncludePrivacy(true);
    request.setIncludeExcel(true);
    return sixColumnModel(request);
  }

  /** postgres: equality-date + lock topology, dialect POSTGRES. */
  private ScaffoldModel postgresModel() {
    return equalityDateLockModel(ScaffoldDialect.POSTGRES);
  }

  /** db2: equality-date + lock topology, dialect DB2. */
  private ScaffoldModel db2Model() {
    return equalityDateLockModel(ScaffoldDialect.DB2);
  }

  private ScaffoldModel equalityDateLockModel(ScaffoldDialect dialect) {
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    request.setRawQuery(
        """
            SELECT A.SMS_HISTORY_ID, A.SEND_DT, A.SEND_TYPE, A.SEND_STATUS, A.RECEIVER_NO, A.UPD_DTTM
            FROM SMS_HISTORY A
            WHERE 1=1
            AND A.SEND_DT = $send_dt
            """);
    return new ScaffoldModel(request, SIX_COLUMNS, List.of("sendDt"), SIX_TYPE_MAP, dialect);
  }

  /** keyword: empty searchVars + no-WHERE rawQuery — Mapper XML의 LIKE '%' 분기 실행. */
  private ScaffoldModel keywordModel() {
    ScaffoldRequestDTO request = baseRequest();
    request.setRawQuery("SELECT A.NOTICE_ID, A.TITLE FROM SMS.NOTICE A");
    return new ScaffoldModel(
        request,
        List.of("NOTICE_ID", "TITLE"),
        List.of(),
        Map.of("NOTICE_ID", "Long", "TITLE", "String"));
  }

  // ============================================================================================
  // Template enum
  // ============================================================================================

  enum Template {
    DTO {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.SEARCH_DTO, m);
      }
    },
    UPDATE_REQUEST_DTO {
      @Override
      public boolean applies(ScaffoldModel m) {
        return m.includeCreateUpdate();
      }

      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.UPDATE_DTO, m);
      }
    },
    VO {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.VO, m);
      }
    },
    MAPPER_INTERFACE {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.MAPPER_INTERFACE, m);
      }
    },
    MAPPER_XML {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.MAPPER_XML, m);
      }
    },
    SERVICE {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.SERVICE, m);
      }
    },
    CONTROLLER {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.CONTROLLER, m);
      }
    },
    SERVICE_TEST {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.SERVICE_TEST, m);
      }
    },
    CONTROLLER_TEST {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.CONTROLLER_TEST, m);
      }
    },
    HTML {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.PAGE_HTML, m);
      }
    },
    JS {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.PAGE_JS, m);
      }
    },
    MENU_SQL {
      @Override
      public String generate(ScaffoldModel m) {
        return render(Artifact.MENU_SQL, m);
      }
    };

    public abstract String generate(ScaffoldModel m);

    public boolean applies(ScaffoldModel m) {
      return true;
    }
  }

  // ============================================================================================
  // Record / Compare logic
  // ============================================================================================

  private boolean shouldRecord(String modelKey) {
    if (!RECORD) {
      return false;
    }
    if (SELECTOR == null) {
      return true;
    }
    Set<String> selected =
        Arrays.stream(SELECTOR.split(","))
            .map(s -> s.trim().toLowerCase())
            .collect(java.util.stream.Collectors.toSet());
    return selected.contains(modelKey);
  }

  private void verify(String modelKey, ScaffoldModel model) {
    for (Template template : Template.values()) {
      if (!template.applies(model)) {
        continue;
      }
      String actual = template.generate(model);
      assertThat(actual)
          .as(modelKey + "/" + template.name() + " — generate() must not return null")
          .isNotNull();

      // LF 정규화: 템플릿 .tpl 파일이 CRLF를 포함할 수 있으므로 LF로 정규화한다.
      // text-block 리팩터링(T3-T9) 이후에는 모든 출력이 자연스럽게 LF-only가 된다.
      actual = actual.replace("\r\n", "\n").replace("\r", "\n");

      Path goldenFile = GOLDEN_DIR.resolve(modelKey + "_" + template.name() + ".txt");

      assertThat(actual)
          .as(modelKey + "/" + template.name() + " — generated output must not contain CR (\\r)")
          .doesNotContain("\r");

      if (shouldRecord(modelKey)) {
        writeGolden(goldenFile, actual);
        // record 직후에도 자체 검증: LF 속성이 .gitattributes에 의해 lf로 설정되어 있어야 한다
        assertGitEolLf(goldenFile, modelKey + "/" + template.name());
      } else {
        assertThat(Files.exists(goldenFile))
            .as(
                modelKey
                    + "/"
                    + template.name()
                    + " — golden 파일이 없습니다. record 모드로 생성하세요: "
                    + "-Dgolden.record=true"
                    + (SELECTOR != null ? " -Dgolden.record.models=" + modelKey : ""))
            .isTrue();
        String expected = readGolden(goldenFile);
        assertThat(actual)
            .as(modelKey + "/" + template.name() + " — golden 불일치 (" + goldenFile + ")")
            .isEqualTo(expected);
        assertGitEolLf(goldenFile, modelKey + "/" + template.name());
      }
    }
  }

  private void writeGolden(Path goldenFile, String content) {
    try {
      Files.createDirectories(goldenFile.getParent());
      Files.writeString(goldenFile, content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("golden 파일 쓰기 실패: " + goldenFile, e);
    }
  }

  private String readGolden(Path goldenFile) {
    try {
      return Files.readString(goldenFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("golden 파일 읽기 실패: " + goldenFile, e);
    }
  }

  /** {@code git check-attr eol -- <file>} 결과가 {@code eol: lf}인지 검증한다. */
  private void assertGitEolLf(Path goldenFile, String label) {
    Path repoRoot = findGitRepoRoot();
    if (repoRoot == null) {
      return; // git repo가 아니면 스킵
    }
    Path relative = repoRoot.relativize(goldenFile.toAbsolutePath());
    String output = runGitCheckAttr(repoRoot, relative.toString().replace('\\', '/'));
    assertThat(output)
        .as(label + " — git check-attr eol must return 'eol: lf' for " + relative)
        .contains("eol: lf");
  }

  private Path findGitRepoRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      if (Files.isDirectory(current.resolve(".git"))) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  private String runGitCheckAttr(Path repoRoot, String relativePath) {
    try {
      ProcessBuilder pb = new ProcessBuilder("git", "check-attr", "eol", "--", relativePath);
      pb.directory(repoRoot.toFile());
      pb.redirectErrorStream(true);
      Process process = pb.start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      process.waitFor();
      return output;
    } catch (Exception e) {
      return ""; // git 실행 불가 시 빈 문자열 (assertion에서 실패)
    }
  }

  // ============================================================================================
  // Tests — one per model
  // ============================================================================================

  @Test
  void base() {
    verify("base", baseModel());
  }

  @Test
  void crud() {
    verify("crud", crudModel());
  }

  @Test
  void full() {
    verify("full", fullModel());
  }

  @Test
  void postgres() {
    verify("postgres", postgresModel());
  }

  @Test
  void db2() {
    verify("db2", db2Model());
  }

  @Test
  void keyword() {
    verify("keyword", keywordModel());
  }
}
