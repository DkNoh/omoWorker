package com.scbk.sms.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.system.MenuDetailResponseDTO;
import com.scbk.sms.dto.system.MenuSearchRequestDTO;
import com.scbk.sms.service.system.MenuManageService;
import com.scbk.sms.vo.system.MenuManageVO;
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
class MenuManageControllerTest {

  @Mock private MenuManageService service;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new MenuManageController(service)).build();
  }

  @Test
  void tree는_ApiResponse_포맷으로_평탄_목록을_반환한다() throws Exception {
    // given
    MenuManageVO vo = new MenuManageVO();
    vo.setMenuId("G_BASIC");
    given(service.getTree()).willReturn(List.of(vo));

    // when / then
    mockMvc
        .perform(get("/system/menu-manage/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data[0].menuId").value("G_BASIC"));
  }

  @Test
  void detail은_선택_메뉴의_상세를_반환한다() throws Exception {
    // given
    MenuDetailResponseDTO detail = new MenuDetailResponseDTO();
    MenuManageVO menu = new MenuManageVO();
    menu.setMenuId("M1");
    detail.setMenu(menu);
    detail.setActiveRoles(List.of());
    detail.setAuthRows(List.of());
    given(service.getDetail(eq("M1"))).willReturn(detail);

    // when / then
    mockMvc
        .perform(get("/system/menu-manage/detail").param("menuId", "M1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.menu.menuId").value("M1"));
  }

  @Test
  void data는_기존_그리드_조회를_유지한다() throws Exception {
    // given
    given(service.search(any()))
        .willReturn(PageResponseDTO.of(java.util.List.of(), new MenuSearchRequestDTO(), 0));

    // when / then
    mockMvc
        .perform(get("/system/menu-manage/data"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.totalCount").value(0));
  }

  @Test
  void create는_등록_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(
            post("/system/menu-manage/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"menuId\":\"NEW_MENU\",\"menuNm\":\"메뉴\",\"menuType\":\"M\","
                        + "\"menuUrl\":\"/ok\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("등록되었습니다."));

    then(service).should().create(any());
  }

  @Test
  void update는_수정_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(
            post("/system/menu-manage/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"menuId\":\"M1\",\"menuNm\":\"메뉴\",\"menuType\":\"M\","
                        + "\"menuUrl\":\"/ok\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("수정되었습니다."));

    then(service).should().update(any());
  }

  @Test
  void delete는_삭제_성공_메시지를_반환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/system/menu-manage/delete").param("menuId", "CUSTOM"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("삭제되었습니다."));

    then(service).should().delete("CUSTOM");
  }
}
