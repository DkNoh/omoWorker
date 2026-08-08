package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 개발자 매뉴얼의 인덱스·링크와 핵심 코드 계약을 정적 파일 대조로 검증한다. */
class ManualDocumentationContractTest {

  private static final Path MANUAL_DIR = Path.of("docs", "menual");
  private static final Path DOCS_README = Path.of("docs", "base", "README.md");
  private static final Path SAMPLE_INDEX =
      Path.of("src", "main", "resources", "static", "samples", "index.html");
  private static final Path SAMPLE_MANUAL = MANUAL_DIR.resolve("sample-to-screen_manual.md");

  private static final Pattern INDEXED_MANUAL = Pattern.compile("`(menual/[^`]+\\.md)`");
  private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^]]+]\\(([^)]+)\\)");
  private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.+?)\\s*#*\\s*$");
  private static final Pattern LOCAL_ANCHOR = Pattern.compile("\\[[^]]+]\\(#([^)]+)\\)");
  private static final Pattern SAMPLE_LINK = Pattern.compile("href=\"\\./([^\"]+\\.html)\"");

  @Test
  void menual의_모든_MD는_문서_인덱스에_등록되고_등록_경로가_존재한다() throws IOException {
    String index = read(DOCS_README);
    List<String> manualFiles;
    try (var files = Files.list(MANUAL_DIR)) {
      manualFiles =
          files
              .filter(Files::isRegularFile)
              .map(path -> path.getFileName().toString())
              .filter(name -> name.endsWith(".md"))
              .sorted()
              .toList();
    }

    for (String fileName : manualFiles) {
      assertThat(index)
          .as("docs/menual 문서는 docs/base/README.md에 등록되어야 한다: " + fileName)
          .contains("`menual/" + fileName + "`");
    }

    Matcher matcher = INDEXED_MANUAL.matcher(index);
    List<String> indexedFiles = new ArrayList<>();
    while (matcher.find()) {
      String relative = matcher.group(1);
      indexedFiles.add(relative);
      assertThat(Path.of("docs").resolve(relative))
          .as("문서 인덱스가 존재하지 않는 파일을 가리킨다: " + relative)
          .exists();
    }

    assertThat(indexedFiles).doesNotHaveDuplicates();
    assertThat(indexedFiles).hasSameSizeAs(manualFiles);
  }

  @Test
  void 매뉴얼의_상대_Markdown_링크는_존재하는_파일을_가리킨다() throws IOException {
    for (Path manual : manualDocuments()) {
      String markdown = read(manual);
      Matcher matcher = MARKDOWN_LINK.matcher(markdown);
      while (matcher.find()) {
        String target = matcher.group(1).trim();
        if (target.startsWith("#")
            || target.startsWith("http://")
            || target.startsWith("https://")
            || target.startsWith("mailto:")) {
          continue;
        }
        String pathOnly = target.contains("#") ? target.substring(0, target.indexOf('#')) : target;
        Path resolved = manual.getParent().resolve(pathOnly).normalize();
        assertThat(resolved)
            .as(manual + "의 상대 링크 대상이 존재해야 한다: " + target)
            .exists();
      }
    }
  }

  @Test
  void 매뉴얼의_목차_anchor는_실제_제목과_일치한다() throws IOException {
    for (Path manual : manualDocuments()) {
      String markdown = read(manual);
      Set<String> headingSlugs = headingSlugs(markdown);
      Matcher matcher = LOCAL_ANCHOR.matcher(markdown);
      while (matcher.find()) {
        String anchor = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        assertThat(headingSlugs)
            .as(manual + "의 목차 anchor가 실제 제목과 일치해야 한다: #" + anchor)
            .contains(anchor);
      }
    }
  }

  @Test
  void 샘플_인덱스의_화면은_존재하고_샘플_전환_매뉴얼에_등록된다() throws IOException {
    String index = read(SAMPLE_INDEX);
    String manual = read(SAMPLE_MANUAL);
    Matcher matcher = SAMPLE_LINK.matcher(index);
    Set<String> samples = new HashSet<>();

    while (matcher.find()) {
      String fileName = matcher.group(1);
      samples.add(fileName);
      assertThat(SAMPLE_INDEX.getParent().resolve(fileName))
          .as("samples/index.html 링크 대상이 존재해야 한다: " + fileName)
          .exists();
      assertThat(manual)
          .as("sample-to-screen_manual.md에 인덱스 샘플이 등록되어야 한다: " + fileName)
          .contains("`" + fileName + "`");
    }

    assertThat(samples).as("현재 정적 샘플 인덱스의 화면 수").hasSize(12);
  }

  @Test
  void 공통_JS_매뉴얼은_defaultLayout의_SweetAlert_경로와_로드_순서를_따른다()
      throws IOException {
    String layout = read(Path.of("src", "main", "resources", "templates", "defaultLayout.html"));
    String manual = read(MANUAL_DIR.resolve("common-js_manual.md"));
    String swalJs = "/lib/sweetalert2/11.26.25/sweetalert2.all.min.js";
    String swalCss = "/lib/sweetalert2/11.26.25/sweetalert2.min.css";

    assertThat(layout).contains(swalJs, swalCss);
    assertThat(manual)
        .contains(swalJs, swalCss)
        .doesNotContain("src=\"/lib/sweetalert2.all.min.js\"")
        .doesNotContain("@{/lib/sweetalert2.min.css}");

    assertThat(layout.indexOf("sweetalert2.all.min.js"))
        .isLessThan(layout.indexOf("/js/common/notify.js"));
    assertThat(layout.indexOf("/js/common/form-binder.js"))
        .isLessThan(layout.indexOf("/js/common/field-format.js"));
  }

  @Test
  void 권한_매뉴얼은_revision_기반_세션_캐시_갱신을_설명한다() throws IOException {
    String manual = read(MANUAL_DIR.resolve("auth-page_manual.md"));

    assertThat(manual)
        .contains("MenuCacheRevision.invalidateAfterCommit()")
        .contains("다음 MVC 화면 요청")
        .contains("AtomicLong")
        .doesNotContain("권한이 바뀌면 재로그인(새 세션)이 필요하다")
        .doesNotContain("권한 변경 검증은 **재로그인(새 세션)** 후 수행한다");
  }

  @Test
  void 매뉴얼은_현재_테스트화면의_삭제된_Excel_API를_표준_예제로_사용하지_않는다()
      throws IOException {
    for (Path manual : manualDocuments()) {
      assertThat(read(manual))
          .as(manual + "은 현재 메뉴 테스트 화면의 과거 Excel API에 의존하면 안 된다")
          .doesNotContain("/sms/history/excel")
          .doesNotContain("SmsHistoryController.downloadExcel")
          .doesNotContain("SmsHistoryService.downloadExcel");
    }
  }

  private List<Path> manualDocuments() throws IOException {
    try (var files = Files.list(MANUAL_DIR)) {
      return files
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".md"))
          .sorted()
          .toList();
    }
  }

  private Set<String> headingSlugs(String markdown) {
    Set<String> slugs = new HashSet<>();
    Map<String, Integer> duplicates = new HashMap<>();
    for (String line : markdown.split("\\R")) {
      Matcher matcher = HEADING.matcher(line);
      if (!matcher.matches()) {
        continue;
      }
      String base = slug(matcher.group(1));
      int duplicate = duplicates.getOrDefault(base, 0);
      duplicates.put(base, duplicate + 1);
      slugs.add(duplicate == 0 ? base : base + "-" + duplicate);
    }
    return slugs;
  }

  private String slug(String heading) {
    return heading
        .replaceAll("<[^>]*>", "")
        .replace("`", "")
        .replace("*", "")
        .replace("~", "")
        .toLowerCase(Locale.ROOT)
        .trim()
        .replaceAll("[^\\p{L}\\p{N}\\s_-]", "")
        .replaceAll("\\s", "-");
  }

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }
}
