package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 프로젝트 규약을 소스 파일 스캔으로 자동 검증한다. 화면(도메인 코드)이 늘어나도 이 테스트가 전부 검사하므로 별도 작업이 필요 없다. 규약을 바꾸면 이 테스트와 관련
 * 문서(screen-convention.md 등)를 함께 갱신한다.
 */
class ConventionTest {

  private static final Path JAVA_MAIN = Path.of("src", "main", "java");
  private static final Path MAPPER_DIR = Path.of("src", "main", "resources", "mapper");

  // Scaffold 산출물에만 붙는 헤더 마커. 마커가 있는 파일만 현재 scaffold 템플릿 규칙을 따라야 한다.
  // scaffold-contract.md §3의 산출물 품질 기준을 산출물 파일 자체로 검증하기 위한 필터.
  private static final String SCAFFOLD_MARKER = "Scaffold 생성(v1)";

  private static final Pattern SELECT_STAR = Pattern.compile("SELECT\\s+\\*");
  private static final Pattern REQUEST_BODY_VO = Pattern.compile("@RequestBody\\s+\\w*VO\\b");

  @Test
  void 검색_DTO는_PageRequestDTO를_상속한다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JAVA_MAIN)) {
      files
          .filter(p -> p.getFileName().toString().endsWith("SearchRequestDTO.java"))
          .forEach(
              p -> {
                if (!read(p).contains("extends PageRequestDTO")) {
                  violations.add(p.toString());
                }
              });
    }
    assertThat(violations).as("SearchRequestDTO는 PageRequestDTO를 상속해야 한다 (project.md)").isEmpty();
  }

  @Test
  void Controller는_save_endpoint를_만들지_않는다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JAVA_MAIN)) {
      files
          .filter(p -> p.toString().contains("controller") && p.toString().endsWith(".java"))
          .forEach(
              p -> {
                if (read(p).contains("\"/save\"")) {
                  violations.add(p.toString());
                }
              });
    }
    assertThat(violations)
        .as("등록/수정은 /create, /update로 분리한다. /save 금지 (menu-authority.md)")
        .isEmpty();
  }

  @Test
  void Controller는_VO를_요청_본문으로_받지_않는다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JAVA_MAIN)) {
      files
          .filter(p -> p.toString().contains("controller") && p.toString().endsWith(".java"))
          .forEach(
              p -> {
                if (REQUEST_BODY_VO.matcher(read(p)).find()) {
                  violations.add(p.toString());
                }
              });
    }
    assertThat(violations)
        .as("수정/등록 요청은 화이트리스트 UpdateRequestDTO로만 받는다. VO 직접 수신 금지 (screen-convention.md)")
        .isEmpty();
  }

  @Test
  void MapperXML은_SELECT_별표를_쓰지_않는다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(MAPPER_DIR)) {
      files
          .filter(p -> p.toString().endsWith(".xml"))
          .forEach(
              p -> {
                if (SELECT_STAR.matcher(read(p)).find()) {
                  violations.add(p.toString());
                }
              });
    }
    assertThat(violations)
        .as("SELECT *를 사용하지 않는다. 파생 테이블 별칭(SELECT A.*)은 허용 (mybatis-oracle.md)")
        .isEmpty();
  }

  @Test
  void 페이지_조회는_결정적_정렬을_가진다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(MAPPER_DIR)) {
      files
          .filter(p -> p.toString().endsWith(".xml"))
          .forEach(
              p -> {
                String content = read(p);
                if (content.contains("OFFSET") && !content.contains("ORDER BY")) {
                  violations.add(p.toString());
                }
              });
    }
    assertThat(violations).as("OFFSET/FETCH 페이지 조회에는 ORDER BY가 필수다 (mybatis-oracle.md)").isEmpty();
  }

  @Test
  void 신규_도메인_DTO와_VO는_Lombok_Data를_사용한다() throws IOException {
    // BASE 공통 코드(dto/common, dto/system, vo/common, vo/auth, vo/menu, vo/system)는 plain Java 유지
    // 대상이라 제외한다
    List<String> excludedDirs =
        List.of(
            "dto\\common",
            "dto\\system",
            "vo\\common",
            "vo\\auth",
            "vo\\menu",
            "vo\\system",
            "dto/common",
            "dto/system",
            "vo/common",
            "vo/auth",
            "vo/menu",
            "vo/system");
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JAVA_MAIN)) {
      files
          .filter(
              p -> {
                String path = p.toString();
                return path.endsWith(".java")
                    && (path.contains("dto") || path.contains("vo"))
                    && excludedDirs.stream().noneMatch(path::contains)
                    && (path.contains("\\dto\\")
                        || path.contains("\\vo\\")
                        || path.contains("/dto/")
                        || path.contains("/vo/"));
              })
          .forEach(
              p -> {
                if (!read(p).contains("@Data")) {
                  violations.add(p.toString());
                }
              });
    }
    assertThat(violations).as("신규 도메인 DTO/VO는 Lombok @Data를 사용한다 (project.md)").isEmpty();
  }

  // 아래 3개 테스트는 scaffold 템플릿 구조가 개선되어도 기존 산출물이 자동 갱신되지 않는
  // 드리프트를 감지한다. 템플릿이 구조를 바꾸면(예: baseQuery include 도입) 기존 산출물도
  // 같이 재생성 + apply해야 게이트를 통과한다. 마커가 없는 mapper xml(scaffold 이전 파일)은 제외.

  @Test
  void Scaffold_산출물_MapperXML은_baseQuery_include_패턴을_사용한다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(MAPPER_DIR)) {
      files
          .filter(p -> p.toString().endsWith(".xml"))
          .forEach(
              p -> {
                String content = read(p);
                if (!content.contains(SCAFFOLD_MARKER)) {
                  return;
                }
                boolean hasSql = content.contains("<sql id=\"baseQuery\">");
                boolean hasInclude = content.contains("<include refid=\"baseQuery\"/>");
                if (!hasSql || !hasInclude) {
                  violations.add(p + " (sql=" + hasSql + ", include=" + hasInclude + ")");
                }
              });
    }
    assertThat(violations)
        .as(
            "Scaffold 산출물 MapperXML은 baseQuery를 <sql>로 정의하고 <include>로 참조해야 한다 "
                + "(scaffold-contract.md §3). 템플릿 개선 시 기존 산출물도 재생성+apply 필요.")
        .isEmpty();
  }

  @Test
  void Scaffold_산출물_MapperXML은_searchConditions_include_패턴을_사용한다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(MAPPER_DIR)) {
      files
          .filter(p -> p.toString().endsWith(".xml"))
          .forEach(
              p -> {
                String content = read(p);
                if (!content.contains(SCAFFOLD_MARKER)) {
                  return;
                }
                boolean hasSql = content.contains("<sql id=\"searchConditions\">");
                boolean hasInclude = content.contains("<include refid=\"searchConditions\"/>");
                if (!hasSql || !hasInclude) {
                  violations.add(p + " (sql=" + hasSql + ", include=" + hasInclude + ")");
                }
              });
    }
    assertThat(violations)
        .as(
            "Scaffold 산출물 MapperXML은 searchConditions를 <sql>로 정의하고 <include>로 참조해야 한다 "
                + "(scaffold-contract.md §3)")
        .isEmpty();
  }

  @Test
  void Scaffold_산출물_MapperXML은_쿼리_시그니처를_가진다() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(MAPPER_DIR)) {
      files
          .filter(p -> p.toString().endsWith(".xml"))
          .forEach(
              p -> {
                String content = read(p);
                if (!content.contains(SCAFFOLD_MARKER)) {
                  return;
                }
                // 패턴: /* SomeMapper.method */
                boolean hasSignature =
                    content.matches("(?s).*?/\\*\\s*\\w+Mapper\\.\\w+\\s*\\*/.*");
                if (!hasSignature) {
                  violations.add(p.toString());
                }
              });
    }
    assertThat(violations)
        .as(
            "Scaffold 산출물 MapperXML은 각 쿼리에 /* Mapper.method */ 시그니처를 포함해야 한다 "
                + "(mybatis-oracle.md, scaffold-contract.md §3)")
        .isEmpty();
  }

  private String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new IllegalStateException("파일을 읽지 못했습니다: " + path, e);
    }
  }
}
