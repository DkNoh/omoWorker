package com.scbk.sms.controller.sms;

import static org.hamcrest.Matchers.emptyIterable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SmsRegisterControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new SmsRegisterController()).build();
  }

  @Test
  void 등록_화면은_메뉴_URL에서_캠페인_레이아웃을_반환한다() throws Exception {
    mockMvc
        .perform(get("/campaign/sms/register"))
        .andExpect(status().isOk())
        .andExpect(view().name("sms/campaign-register"))
        .andExpect(model().attribute("targetGroups", emptyIterable()))
        .andExpect(model().attribute("businessTypes", emptyIterable()))
        .andExpect(model().attribute("messageTemplates", emptyIterable()));
  }
}
