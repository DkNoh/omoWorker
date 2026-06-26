package com.scbk.sms.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.scbk.sms.dto.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
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
}
