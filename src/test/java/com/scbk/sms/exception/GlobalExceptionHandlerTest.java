package com.scbk.sms.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scbk.sms.dto.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    mockMvc =
        MockMvcBuilders.standaloneSetup(new InputController()).setControllerAdvice(handler).build();
  }

  @Test
  void 화면_요청의_예외는_에러_페이지로_변환한다() {
    // given : 브라우저 화면 이동
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history");
    request.addHeader("Accept", "text/html,application/xhtml+xml,*/*");

    // when
    Object result =
        handler.handleCustomException(new CustomException(ErrorCode.ACCESS_DENIED), request);

    // then
    assertThat(result).isInstanceOf(ModelAndView.class);
    ModelAndView mav = (ModelAndView) result;
    assertThat(mav.getViewName()).isEqualTo("error/error");
    assertThat(mav.getModel().get("status")).isEqualTo(403);
    assertThat(mav.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void JSON_요청의_예외는_ApiResponse로_변환한다() {
    // given : axios/fetch 호출
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history/data");
    request.addHeader("Accept", "application/json, text/plain, */*");

    // when
    Object result =
        handler.handleCustomException(new CustomException(ErrorCode.ACCESS_DENIED), request);

    // then
    assertThat(result).isInstanceOf(ResponseEntity.class);
    @SuppressWarnings("unchecked")
    ResponseEntity<ApiResponse<Void>> response = (ResponseEntity<ApiResponse<Void>>) result;
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().getCode()).isEqualTo(403);
    assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED.getMessage());
  }

  @Test
  void catch_all_예외는_원인_메시지를_노출하지_않는다() {
    // given : SQLException 등에서 테이블명/컬럼명이 노출될 수 있는 예외
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history/data");
    request.addHeader("Accept", "application/json, text/plain, */*");
    String sensitiveMessage = "ORA-00942: table or view does not exist SMS.TB_MENU";

    // when
    Object result = handler.handleException(new RuntimeException(sensitiveMessage), request);

    // then
    assertThat(result).isInstanceOf(ResponseEntity.class);
    @SuppressWarnings("unchecked")
    ResponseEntity<ApiResponse<Void>> response = (ResponseEntity<ApiResponse<Void>>) result;
    assertThat(response.getBody().getMessage())
        .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    assertThat(response.getBody().getMessage()).doesNotContain("ORA-00942");
    assertThat(response.getBody().getMessage()).doesNotContain("TB_MENU");
  }

  @Test
  void 잘못된_JSON은_400_ApiResponse로_변환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/test/json").contentType(MediaType.APPLICATION_JSON).content("{\"value\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  void 잘못된_숫자_파라미터는_400_ApiResponse로_변환한다() throws Exception {
    // when / then
    mockMvc
        .perform(get("/test/number").param("value", "invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  void 필수_파라미터_누락은_400_ApiResponse로_변환한다() throws Exception {
    // when / then
    mockMvc
        .perform(get("/test/required"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  void 지원하지_않는_ContentType은_415_ApiResponse로_변환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/test/json").contentType(MediaType.TEXT_PLAIN).content("value=1"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value(415));
  }

  @Test
  void 지원하지_않는_HTTP_메서드는_405_ApiResponse로_변환한다() throws Exception {
    // when / then
    mockMvc
        .perform(post("/test/required"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value(405));
  }

  // --- Bean Validation 필드 오류 계약 ---

  @Test
  void 빈_JSON_바디는_모든_필드_검증_오류를_400_응답에_포함한다() throws Exception {
    // given : @NotBlank name + @NotNull @Email email — 둘 다 violations
    String emptyJson = "{}";

    // when / then
    mockMvc
        .perform(
            post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(emptyJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$['errors'][*]['field']").value(containsInAnyOrder("name", "email")))
        .andExpect(jsonPath("$['errors'][*]['message']").value(containsInAnyOrder(
            "must not be blank", "must not be null")))
        // --- pair-level association (swapped messages would fail these) ---
        .andExpect(jsonPath("$['errors'][?(@.field=='name')].message").value("must not be blank"))
        .andExpect(jsonPath("$['errors'][?(@.field=='email')].message").value("must not be null"));
  }

  @RestController
  private static class InputController {

    @PostMapping(value = "/test/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    void json(@RequestBody InputPayload payload) {}

    @GetMapping("/test/number")
    void number(@RequestParam Integer value) {}

    @GetMapping("/test/required")
    void required(@RequestParam String value) {}

    @PostMapping(value = "/test/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    void validate(@Valid @RequestBody ValidationPayload payload) {}
  }

  private record InputPayload(Integer value) {}

  private record ValidationPayload(
      @NotBlank String name,
      @NotNull @Email String email
  ) {}
}
