package com.scbk.sms.service.system.scaffold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scbk.sms.dto.system.ScaffoldApplyFileResultDTO;
import com.scbk.sms.dto.system.ScaffoldCaseRecord;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link ScaffoldRegenerateMain#run}의 dry-run 분기와 파일 미쓰기 계약을 검증한다. dry-run은
 * {@link ScaffoldFileApplier#preview}만 호출해야 하고, 기본 모드는 {@link ScaffoldFileApplier#apply}를 호출해야 한다.
 */
class ScaffoldRegenerateMainTest {

  @Test
  void dryRun은_preview만_호출하고_apply는_호출하지_않는다() {
    // given
    ScaffoldFileApplier applier = mock(ScaffoldFileApplier.class);
    when(applier.preview(any(), any()))
        .thenReturn(List.of());

    // when
    boolean result =
        ScaffoldRegenerateMain.run(new String[] {"--dry-run"}, List.of(newRecord()), applier);

    // then
    assertThat(result).isTrue();
    verify(applier).preview(any(), any());
    verify(applier, never()).apply(any(), any());
  }

  @Test
  void 기본모드는_apply를_호출한다() {
    // given
    ScaffoldFileApplier applier = mock(ScaffoldFileApplier.class);
    when(applier.apply(any(), any())).thenReturn(List.of());

    // when
    boolean result = ScaffoldRegenerateMain.run(new String[0], List.of(newRecord()), applier);

    // then
    assertThat(result).isTrue();
    verify(applier).apply(any(), any());
    verify(applier, never()).preview(any(), any());
  }

  @Test
  void dryRun에서_NEW가_감지되면_false를_반환한다() {
    // given
    ScaffoldFileApplier applier = mock(ScaffoldFileApplier.class);
    when(applier.preview(any(), any()))
        .thenReturn(
            List.of(new ScaffoldApplyFileResultDTO("X.java", "x/X.java", "NEW", "신규 파일")));

    // when
    boolean result =
        ScaffoldRegenerateMain.run(new String[] {"--dry-run"}, List.of(newRecord()), applier);

    // then
    assertThat(result).isFalse();
    verify(applier, never()).apply(any(), any());
  }

  @Test
  void dryRun에서_OVERWRITE가_감지되면_false를_반환한다() {
    // given
    ScaffoldFileApplier applier = mock(ScaffoldFileApplier.class);
    when(applier.preview(any(), any()))
        .thenReturn(
            List.of(
                new ScaffoldApplyFileResultDTO(
                    "X.java", "x/X.java", "OVERWRITE", "기존 파일 덮어쓰기")));

    // when
    boolean result =
        ScaffoldRegenerateMain.run(new String[] {"--dry-run"}, List.of(newRecord()), applier);

    // then
    assertThat(result).isFalse();
  }

  @Test
  void dryRun은_실제_applier로_실행해도_파일을_전혀_생성하지_않는다(@TempDir Path tempDir) throws IOException {
    // given — outputRoot가 빈 tempDir인 real applier. 빈 디렉토리이므로 모든 산출물이 NEW로 감지된다.
    ScaffoldFileApplier realApplier = new ScaffoldFileApplier(tempDir);

    // when
    boolean result =
        ScaffoldRegenerateMain.run(
            new String[] {"--dry-run"}, List.of(newRecord()), realApplier);

    // then — 빈 tempDir이므로 NEW 감지로 result=false가 정상 동작. 핵심 계약은 NEW를 감지하더라도 디스크에 안 쓴다는 것.
    assertThat(result)
        .as("빈 디렉토리에서는 모든 산출물이 NEW로 감지되어 false가 정상")
        .isFalse();
    try (Stream<Path> walk = Files.walk(tempDir)) {
      long fileCount = walk.filter(Files::isRegularFile).count();
      assertThat(fileCount)
          .as("dry-run은 NEW를 감지하더라도 디스크에 어떤 파일도 써서는 안 된다")
          .isZero();
    }
  }

  @Test
  void 기본모드는_실제_applier로_실행하면_산출물을_생성한다(@TempDir Path tempDir) throws IOException {
    // given
    ScaffoldFileApplier realApplier = new ScaffoldFileApplier(tempDir);

    // when
    boolean result =
        ScaffoldRegenerateMain.run(new String[0], List.of(newRecord()), realApplier);

    // then — 기본 모드는 apply를 타서 NEW 파일들이 tempDir 아래에 생성되어야 한다 (dry-run이 아님을 증명)
    assertThat(result).isTrue();
    try (Stream<Path> walk = Files.walk(tempDir)) {
      long fileCount = walk.filter(Files::isRegularFile).count();
      assertThat(fileCount)
          .as("기본 모드는 apply로 산출물을 디스크에 써야 한다")
          .isPositive();
    }
  }

  @Test
  void records가_비어있으면_true를_반환한다() {
    // given
    ScaffoldFileApplier applier = mock(ScaffoldFileApplier.class);

    // when
    boolean result = ScaffoldRegenerateMain.run(new String[] {"--dry-run"}, List.of(), applier);

    // then
    assertThat(result).isTrue();
    verify(applier, never()).preview(any(), any());
    verify(applier, never()).apply(any(), any());
  }

  /** ScaffoldTemplateTest의 model() 헬퍼와 동일한 유효한 최소 입력. 모든 템플릿 generate가 정상 동작하는 입력이다. */
  private static ScaffoldCaseRecord newRecord() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");

    ScaffoldCaseRecord record = new ScaffoldCaseRecord();
    record.setRequest(request);
    record.setColumns(List.of("SEND_DT", "RECEIVER_NO"));
    record.setSearchVars(List.of("startDt"));
    record.setTypeMap(Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));
    record.setDialect("ORACLE");
    return record;
  }
}
