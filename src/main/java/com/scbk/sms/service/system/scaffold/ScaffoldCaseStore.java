package com.scbk.sms.service.system.scaffold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.dto.system.ScaffoldCaseRecord;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * src/main/resources/scaffold-cases/*.json 메타 파일을 읽고 쓴다. ScaffoldRegenerateMain이 이 파일들을 읽어 일괄
 * 재생성한다.
 */
@Component
public class ScaffoldCaseStore {

  private static final Path CASES_DIR = Paths.get("src", "main", "resources", "scaffold-cases");

  private final ObjectMapper objectMapper;

  public ScaffoldCaseStore(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Path resolveCasePath(ScaffoldRequestDTO request) {
    String fileName =
        request.getModuleName() + "_" + request.getDomainId().replace("/", "_") + ".json";
    return CASES_DIR.resolve(fileName);
  }

  public void save(ScaffoldCaseRecord record) {
    try {
      Path path = resolveCasePath(record.getRequest());
      Files.createDirectories(path.getParent());
      String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
      Files.writeString(path, json, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "scaffold-case 저장 실패: " + record.getRequest().getDomainId(), e);
    }
  }

  public List<ScaffoldCaseRecord> loadAll() {
    List<ScaffoldCaseRecord> records = new ArrayList<>();
    if (!Files.exists(CASES_DIR)) {
      return records;
    }
    try (Stream<Path> files = Files.list(CASES_DIR)) {
      files.filter(p -> p.toString().endsWith(".json")).forEach(p -> records.add(load(p)));
    } catch (IOException e) {
      throw new UncheckedIOException("scaffold-cases 디렉토리 읽기 실패", e);
    }
    return records;
  }

  private ScaffoldCaseRecord load(Path path) {
    try {
      return objectMapper.readValue(
          Files.readString(path, StandardCharsets.UTF_8), ScaffoldCaseRecord.class);
    } catch (IOException e) {
      throw new UncheckedIOException("scaffold-case 읽기 실패: " + path, e);
    }
  }
}
