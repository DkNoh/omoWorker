package com.scbk.sms.service.system;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.config.ScaffoldProperties;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.service.system.scaffold.ColumnTypeInferrer;
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
            columnTypeInferrer, scaffoldFileApplier, metadataReader, new ScaffoldProperties());
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

  private Map<String, Boolean> nullableMap(boolean lockNullable) {
    Map<String, Boolean> nullableByColumn = new LinkedHashMap<>();
    nullableByColumn.put("SMS_HISTORY_ID", false);
    nullableByColumn.put("SEND_TYPE", false);
    nullableByColumn.put("UPD_DTTM", lockNullable);
    return nullableByColumn;
  }
}
