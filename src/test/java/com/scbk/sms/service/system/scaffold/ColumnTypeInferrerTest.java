package com.scbk.sms.service.system.scaffold;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.config.ScaffoldProperties;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ColumnTypeInferrerTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void 타입_추론_실패_시_ORA_루트_원인을_메시지에_노출한다() {
    // given : Spring 래퍼가 ORA 원문을 cause 체인 안쪽에 감싼다
    ColumnTypeInferrer inferrer = new ColumnTypeInferrer(jdbcTemplate, new ScaffoldProperties());
    SQLException oraError = new SQLException("ORA-00904: \"A\".\"SEND_DT\": 부적합한 식별자");
    given(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .willThrow(new RuntimeException("ConnectionCallback; bad SQL grammar []", oraError));

    // when / then
    assertThatThrownBy(
            () -> inferrer.inferTypes("SELECT A.SEND_DT FROM SMS_HISTORY A", List.of("SEND_DT")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ORA-00904")
        .hasMessageContaining("SEND_DT");
  }
}
