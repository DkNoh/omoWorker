package com.scbk.sms.controller.basic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.service.basic.NoticeService;
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
class NoticeControllerTest {

  @Mock private NoticeService service;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new NoticeController(service)).build();
  }

  @Test
  void data는_ApiResponse_포맷으로_응답한다() throws Exception {
    // given
    given(service.search(any()))
        .willReturn(PageResponseDTO.of(List.of(), new NoticeSearchRequestDTO(), 0));

    // when / then
    mockMvc
        .perform(get("/basic/notice/data"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.totalCount").value(0));
  }

  @Test
  void create는_등록_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/basic/notice/create").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("등록되었습니다."));

    then(service).should().create(any());
  }

  @Test
  void update는_수정_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/basic/notice/update").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("수정되었습니다."));

    then(service).should().update(any());
  }

  @Test
  void delete는_삭제_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/basic/notice/delete").param("noticeId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("삭제되었습니다."));

    then(service).should().delete(1);
  }
}
