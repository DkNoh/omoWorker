package com.scbk.sms.service.system.scaffold;

import com.scbk.sms.dto.system.ScaffoldApplyFileResultDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 렌더링된 산출물의 프로젝트 상대 경로를 결정하고 미리보기 또는 실제 쓰기를 수행한다.
 *
 * <p>파일명은 알려진 산출물 목록으로만 라우팅하며 module/domain 입력 형식, 프로젝트 루트 이탈, 대소문자만 다른 기존 파일 충돌을 쓰기 전에
 * 차단한다. {@link #preview(ScaffoldRequestDTO, Map)}와 {@link #apply(ScaffoldRequestDTO, Map)}는 같은 경로 판정 로직을 사용해
 * 확인창에서 본 대상과 실제 적용 대상을 일치시킨다.
 */
@Component
@Profile("local")
public class ScaffoldFileApplier {

  private static final Pattern MODULE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*$");
  // ScaffoldRequestDTO와 동일 규약: v2 baseline 3단계 URL 재현을 위해 내부 슬래시 1개까지 허용한다.
  private static final Pattern DOMAIN_ID_PATTERN =
      Pattern.compile("^[a-z][a-z0-9-]*(/[a-z][a-z0-9-]*)?$");
  private static final Pattern DOMAIN_CLASS_PATTERN = Pattern.compile("^[A-Z][A-Za-z0-9]*$");
  private static final char WINDOWS_PATH_SEPARATOR = '\\';
  private static final char DISPLAY_PATH_SEPARATOR = '/';
  private final Path outputRoot;

  @Autowired
  public ScaffoldFileApplier(@Value("${sms.scaffold.output-root:}") String outputRoot) {
    this(resolveOutputRoot(outputRoot));
  }

  ScaffoldFileApplier(Path outputRoot) {
    this.outputRoot = outputRoot.toAbsolutePath().normalize();
  }

  /** 파일을 쓰지 않고 각 산출물의 대상 경로와 변경 상태를 계산한다. */
  public List<ScaffoldApplyFileResultDTO> preview(
      ScaffoldRequestDTO request, Map<String, String> generatedFiles) {
    validateRequest(request);

    List<ScaffoldApplyFileResultDTO> results = new ArrayList<>();
    for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
      Path targetPath = resolveTargetPath(request, entry.getKey());
      validateNoCaseOnlyCollision(targetPath);
      results.add(result(entry.getKey(), targetPath, entry.getValue()));
    }
    return results;
  }

  /** 미리보기 검증을 먼저 통과한 뒤 변경된 내용을 UTF-8로 기록한다. */
  public List<ScaffoldApplyFileResultDTO> apply(
      ScaffoldRequestDTO request, Map<String, String> generatedFiles) {
    List<ScaffoldApplyFileResultDTO> preview = preview(request, generatedFiles);
    for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
      Path targetPath = resolveTargetPath(request, entry.getKey());
      writeFile(targetPath, entry.getValue());
    }
    return preview;
  }

  /** 산출물 이름을 허용된 소스·리소스·테스트·SQL 경로 중 하나로 매핑한다. */
  Path resolveTargetPath(ScaffoldRequestDTO request, String generatedName) {
    String moduleName = request.getModuleName();
    String domainId = request.getDomainId();
    String domainClass = request.getDomainClass();

    Path relativePath;
    if ((domainClass + "SearchRequestDTO.java").equals(generatedName)
        || (domainClass + "UpdateRequestDTO.java").equals(generatedName)) {
      relativePath = Paths.get("src/main/java/com/scbk/sms/dto", moduleName, generatedName);
    } else if ((domainClass + "VO.java").equals(generatedName)) {
      relativePath = Paths.get("src/main/java/com/scbk/sms/vo", moduleName, generatedName);
    } else if ((domainClass + "Mapper.java").equals(generatedName)) {
      relativePath = Paths.get("src/main/java/com/scbk/sms/mapper", moduleName, generatedName);
    } else if ((domainClass + "Mapper.xml").equals(generatedName)) {
      relativePath = Paths.get("src/main/resources/mapper", moduleName, generatedName);
    } else if ((domainClass + "Service.java").equals(generatedName)) {
      relativePath = Paths.get("src/main/java/com/scbk/sms/service", moduleName, generatedName);
    } else if ((domainClass + "Controller.java").equals(generatedName)) {
      relativePath = Paths.get("src/main/java/com/scbk/sms/controller", moduleName, generatedName);
    } else if ((domainClass + "ServiceTest.java").equals(generatedName)) {
      relativePath = Paths.get("src/test/java/com/scbk/sms/service", moduleName, generatedName);
    } else if ((domainClass + "ControllerTest.java").equals(generatedName)) {
      relativePath = Paths.get("src/test/java/com/scbk/sms/controller", moduleName, generatedName);
    } else if ((domainId + ".html").equals(generatedName)) {
      relativePath = Paths.get("src/main/resources/templates", moduleName, generatedName);
    } else if ((domainId + ".js").equals(generatedName)) {
      relativePath = Paths.get("src/main/resources/static/js", moduleName, generatedName);
    } else if ("메뉴등록.sql".equals(generatedName)) {
      relativePath = Paths.get("db/oracle", moduleName + "_" + domainId + "_menu_seed.sql");
    } else {
      throw new IllegalArgumentException("알 수 없는 scaffold 산출물입니다: " + generatedName);
    }

    Path targetPath = outputRoot.resolve(relativePath).normalize();
    if (!targetPath.startsWith(outputRoot)) {
      throw new IllegalArgumentException("scaffold 적용 경로가 프로젝트 루트를 벗어났습니다.");
    }
    return targetPath;
  }

  private void writeFile(Path targetPath, String content) {
    try {
      if (Files.exists(targetPath)) {
        String current = Files.readString(targetPath, StandardCharsets.UTF_8);
        if (content.equals(current)) {
          return; // 변경 없음
        }
      }
      Files.createDirectories(targetPath.getParent());
      Files.writeString(targetPath, content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("scaffold 파일 적용에 실패했습니다: " + targetPath, e);
    }
  }

  private void validateNoCaseOnlyCollision(Path targetPath) {
    Path parent = targetPath.getParent();
    if (!Files.isDirectory(parent)) {
      return;
    }

    String expectedName = targetPath.getFileName().toString();
    try (Stream<Path> siblings = Files.list(parent)) {
      Path collision =
          siblings
              .filter(
                  sibling -> {
                    String actualName = sibling.getFileName().toString();
                    return actualName.equalsIgnoreCase(expectedName)
                        && !actualName.equals(expectedName);
                  })
              .findFirst()
              .orElse(null);
      if (collision == null) {
        return;
      }

      throw new IllegalArgumentException(
          "scaffold 적용 경로 대소문자 충돌: "
              + toDisplayPath(outputRoot.relativize(targetPath))
              + "은(는) 기존 "
              + toDisplayPath(outputRoot.relativize(collision))
              + "과 충돌합니다. domainClass 표기를 기존 파일과 정확히 맞추세요.");
    } catch (IOException e) {
      throw new UncheckedIOException("scaffold 적용 경로 확인에 실패했습니다: " + targetPath, e);
    }
  }

  private ScaffoldApplyFileResultDTO result(String fileName, Path targetPath, String content) {
    String relativePath = toDisplayPath(outputRoot.relativize(targetPath));
    try {
      if (!Files.exists(targetPath)) {
        return new ScaffoldApplyFileResultDTO(fileName, relativePath, "NEW", "신규 파일");
      }
      String current = Files.readString(targetPath, StandardCharsets.UTF_8);
      if (current.equals(content)) {
        return new ScaffoldApplyFileResultDTO(fileName, relativePath, "UNCHANGED", "변경 없음");
      }
      return new ScaffoldApplyFileResultDTO(fileName, relativePath, "OVERWRITE", "기존 파일 덮어쓰기");
    } catch (IOException e) {
      throw new UncheckedIOException("scaffold 파일 미리보기에 실패했습니다: " + targetPath, e);
    }
  }

  private static String toDisplayPath(Path path) {
    return path.toString().replace(WINDOWS_PATH_SEPARATOR, DISPLAY_PATH_SEPARATOR);
  }

  private static Path resolveOutputRoot(String configuredOutputRoot) {
    if (StringUtils.hasText(configuredOutputRoot)) {
      return Paths.get(configuredOutputRoot);
    }
    return Paths.get("");
  }

  private static void validateRequest(ScaffoldRequestDTO request) {
    validate("moduleName", request.getModuleName(), MODULE_PATTERN);
    validate("domainId", request.getDomainId(), DOMAIN_ID_PATTERN);
    validate("domainClass", request.getDomainClass(), DOMAIN_CLASS_PATTERN);
  }

  private static void validate(String fieldName, String value, Pattern pattern) {
    if (!StringUtils.hasText(value) || !pattern.matcher(value).matches()) {
      throw new IllegalArgumentException("scaffold 적용 입력값이 올바르지 않습니다: " + fieldName);
    }
  }
}
