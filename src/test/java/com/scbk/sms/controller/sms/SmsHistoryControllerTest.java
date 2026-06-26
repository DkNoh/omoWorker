package com.scbk.sms.controller.sms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.service.sms.SmsHistoryService;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SmsHistoryControllerTest {

  @Mock private SmsHistoryService service;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    // standaloneSetup: AOP/Interceptor가 로드되지 않는다. @PrivacyLog 부착에도 안전하다.
    mockMvc = MockMvcBuilders.standaloneSetup(new SmsHistoryController(service)).build();
  }

  @Test
  void data는_ApiResponse_포맷으로_응답한다() throws Exception {
    // given
    given(service.search(any()))
        .willReturn(PageResponseDTO.of(List.of(), new SmsHistorySearchRequestDTO(), 0));

    // when / then
    mockMvc
        .perform(get("/sms/history/data"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.totalCount").value(0));
  }

  @Test
  void excel은_서비스에_위임한다() throws Exception {
    // when / then : service @Mock이라 void 메서드는 stub 없이 no-op, 위임만 검증한다.
    mockMvc.perform(get("/sms/history/excel")).andExpect(status().isOk());

    then(service).should().downloadExcel(any(HttpServletResponse.class), any());
  }

  @Test
  void unmask는_원문을_ApiResponse로_반환한다() throws Exception {
    // given
    SmsHistoryVO vo = new SmsHistoryVO();
    vo.setSmsHistoryId(7);
    vo.setReceiverNo("01012345678"); // 마스킹 안 한 원문
    given(service.getUnmaskedDetail(eq(7))).willReturn(vo);

    // when / then
    mockMvc
        .perform(get("/sms/history/7/unmask"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.smsHistoryId").value(7))
        .andExpect(jsonPath("$.data.receiverNo").value("01012345678"));
  }

  @Test
  void create는_등록_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/sms/history/create").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("등록되었습니다."));

    then(service).should().create(any());
  }

  @Test
  void update는_수정_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/sms/history/update").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("수정되었습니다."));

    then(service).should().update(any());
  }

  @Test
  void delete는_삭제_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/sms/history/delete").param("smsHistoryId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("삭제되었습니다."));

    then(service).should().delete(1);
  }
}
