package com.scbk.sms.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class ExcelUtilTest {

  @Test
  void 엑셀_파일을_응답_스트림으로_내려준다() {
    // given
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] headers = {"사번", "이름"};
    String[] keys = {"empId", "empNm"};
    List<Map<String, Object>> dataList =
        List.of(
            Map.of("empId", "admin", "empNm", "최고관리자"), Map.of("empId", "user1", "empNm", "사용자일"));

    // when
    ExcelUtil.downloadExcel(response, "사용자_export", headers, dataList, keys);

    // then
    assertThat(response.getContentType())
        .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(response.getHeader("Content-Disposition")).contains(".xlsx");
    assertThat(response.getContentAsByteArray()).isNotEmpty();
  }
}
