package com.scbk.sms.service.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.config.ScaffoldProperties;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.service.system.scaffold.ColumnTypeInferrer;
import com.scbk.sms.service.system.scaffold.ScaffoldCaseStore;
import com.scbk.sms.service.system.scaffold.ScaffoldFileApplier;
import com.scbk.sms.service.system.scaffold.ScaffoldMetadataReader;
import com.scbk.sms.service.system.scaffold.ScaffoldTableMetadata;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScaffoldServiceTest {

  @Mock private ColumnTypeInferrer columnTypeInferrer;

  @Mock private ScaffoldFileApplier scaffoldFileApplier;

  @Mock private ScaffoldMetadataReader metadataReader;

  private ScaffoldService service;

  @BeforeEach
  void setUp() {
    service =
        new ScaffoldService(
            columnTypeInferrer,
            scaffoldFileApplier,
            metadataReader,
            new ScaffoldProperties(),
            new ScaffoldCaseStore(new ObjectMapper()));
    given(columnTypeInferrer.inferTypes(anyString(), anyList()))
        .willReturn(
            Map.of(
                "SMS_HISTORY_ID", "Long",
                "SEND_TYPE", "String",
                "UPD_DTTM", "LocalDateTime"));
  }

  @Test
  void CRUD는_PK가_없는_테이블이면_생성을_막는다() {
    // given
    given(metadataReader.read("SMS.SMS_HISTORY"))
        .willReturn(new ScaffoldTableMetadata(List.of(), nullableMap(false)));

    // when / then
    assertThatThrownBy(() -> service.generate(request()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("requires a real primary key");
  }

  @Test
  void nullable_낙관적_잠금_컬럼도_null_safe_WHERE로_생성할_수_있다() {
    // given
    given(metadataReader.read("SMS.SMS_HISTORY"))
        .willReturn(new ScaffoldTableMetadata(List.of("SMS_HISTORY_ID"), nullableMap(true)));

    // when / then
    assertThatCode(() -> service.generate(request())).doesNotThrowAnyException();
  }

  @Test
  void 낙관적_잠금_컬럼은_PK로_선택할_수_없다() {
    // given
    given(metadataReader.read("SMS.SMS_HISTORY"))
        .willReturn(new ScaffoldTableMetadata(List.of("SMS_HISTORY_ID"), nullableMap(false)));
    ScaffoldRequestDTO request = request();
    request.setLockColumn("SMS_HISTORY_ID");

    // when / then
    assertThatThrownBy(() -> service.generate(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must not be a PK column");
  }

  private ScaffoldRequestDTO request() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("Sms History");
    request.setScreenMode("CRUD");
    request.setTargetTable("SMS.SMS_HISTORY");
    request.setPkColumns(List.of());
    request.setLockColumn("UPD_DTTM");
    request.setRawQuery(
        """
            SELECT A.SMS_HISTORY_ID, A.SEND_TYPE, A.UPD_DTTM
            FROM SMS.SMS_HISTORY A
            WHERE 1 = 1
            """);
    request.setOrderBy("A.SMS_HISTORY_ID DESC");
    return request;
  }

  // === Task 7: 명시적 screenMode 가 legacy feature flag 보다 우선한다 (generate 경계) ===

  @Test
  void 명시적_LIST와_모순된_includeCreateUpdate는_CRUD_PK_검증을_수행하지_않는다() {
    // given : screenMode=LIST 임에도 legacy includeCreateUpdate=true 가 충돌.
    // metadataReader 는 호출되지 않아야 하므로 stub 하지 않는다.
    ScaffoldRequestDTO request = baseRequest();
    request.setScreenMode("LIST");
    request.setIncludeCreateUpdate(true);

    // when / then : 순수 LIST 이므로 CRUD PK 검증/요구 없이 생성된다
    assertThatCode(() -> service.generate(request)).doesNotThrowAnyException();
  }

  @Test
  void 명시적_CRUD와_모순된_includeExcel은_엑셀_엔드포인트를_생성하지_않는다() {
    // given : screenMode=CRUD + legacy includeExcel=true (모순). PK/lock 은 유효.
    given(metadataReader.read("SMS.SMS_HISTORY"))
        .willReturn(new ScaffoldTableMetadata(List.of("SMS_HISTORY_ID"), nullableMap(false)));
    ScaffoldRequestDTO request = baseRequest();
    request.setScreenMode("CRUD");
    request.setIncludeExcel(true);
    request.setLockColumn("UPD_DTTM");

    // when
    Map<String, String> files = service.generate(request);

    // then : CRUD 산출물은 있되 엑셀 엔드포인트/의존성은 없다
    String controller = files.get("SmsHistoryController.java");
    assertThat(controller).isNotNull();
    assertThat(controller).contains("@PostMapping(\"/create\")");
    assertThat(controller).doesNotContain("@GetMapping(\"/excel\")");
    assertThat(controller).doesNotContain("HttpServletResponse");
    // 메뉴 권한 : CRUD → DOWNLOAD=N
    assertThat(files.get("메뉴등록.sql")).contains("'N', 'N', 'N', 'N',");
  }

  @Test
  void 빈_screenMode와_legacy_includeCreateUpdate_true는_CRUD로_동작한다() {
    // given : screenMode 미지정 + legacy includeCreateUpdate=true (하위호환)
    given(metadataReader.read("SMS.SMS_HISTORY"))
        .willReturn(new ScaffoldTableMetadata(List.of("SMS_HISTORY_ID"), nullableMap(false)));
    ScaffoldRequestDTO request = baseRequest();
    request.setScreenMode(" ");
    request.setIncludeCreateUpdate(true);
    request.setLockColumn("UPD_DTTM");

    // when
    Map<String, String> files = service.generate(request);

    // then : legacy fallback 이 그대로 CRUD 로 동작한다
    assertThat(files.get("SmsHistoryController.java")).contains("@PostMapping(\"/create\")");
    assertThat(files).containsKey("SmsHistoryUpdateRequestDTO.java");
  }

  @Test
  void 명시적_CRUD_PANEL_화면모드는_generate_단계에서_거부된다() {
    // given : generate/preview/apply 공통 렌더링 경로
    ScaffoldRequestDTO request = baseRequest();
    request.setScreenMode("CRUD_PANEL");

    // when / then : 명시적으로 거부된다 (지원 목록 LIST, EXCEL, CRUD)
    assertThatThrownBy(() -> service.generate(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("지원하지 않는 screenMode입니다: CRUD_PANEL")
        .hasMessageContaining("(지원: LIST, EXCEL, CRUD)");
  }

  private ScaffoldRequestDTO baseRequest() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("Sms History");
    request.setTargetTable("SMS.SMS_HISTORY");
    request.setPkColumns(List.of());
    request.setRawQuery(
        """
            SELECT A.SMS_HISTORY_ID, A.SEND_TYPE, A.UPD_DTTM
            FROM SMS.SMS_HISTORY A
            WHERE 1 = 1
            """);
    request.setOrderBy("A.SMS_HISTORY_ID DESC");
    return request;
  }

  private Map<String, Boolean> nullableMap(boolean lockNullable) {
    Map<String, Boolean> nullableByColumn = new LinkedHashMap<>();
    nullableByColumn.put("SMS_HISTORY_ID", false);
    nullableByColumn.put("SEND_TYPE", false);
    nullableByColumn.put("UPD_DTTM", lockNullable);
    return nullableByColumn;
  }
}
