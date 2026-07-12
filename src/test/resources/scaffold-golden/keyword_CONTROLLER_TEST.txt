package com.scbk.sms.controller.sms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.service.sms.SmsHistoryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
class SmsHistoryControllerTest {

    @Mock
    private SmsHistoryService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SmsHistoryController(service)).build();
    }

    @Test
    void data는_ApiResponse_포맷으로_응답한다() throws Exception {
        // given
        given(service.search(any())).willReturn(
            PageResponseDTO.of(List.of(), new SmsHistorySearchRequestDTO(), 0));

        // when / then
        mockMvc.perform(get("/sms/history/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalCount").value(0));
    }
}
