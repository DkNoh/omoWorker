package com.scbk.sms.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

  @Test
  void success는_code_200과_데이터를_담는다() {
    // when
    ApiResponse<String> response = ApiResponse.success("payload");

    // then
    assertThat(response.getCode()).isEqualTo(200);
    assertThat(response.getMessage()).isEqualTo("SUCCESS");
    assertThat(response.getData()).isEqualTo("payload");
    assertThat(response.getTimestamp()).isNotBlank();
  }

  @Test
  void error는_데이터_없이_code와_메시지를_담는다() {
    // when
    ApiResponse<Void> response = ApiResponse.error(404, "사용자를 찾을 수 없습니다.");

    // then
    assertThat(response.getCode()).isEqualTo(404);
    assertThat(response.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
    assertThat(response.getData()).isNull();
  }
}
