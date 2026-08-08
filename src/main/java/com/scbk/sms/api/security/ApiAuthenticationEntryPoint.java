package com.scbk.sms.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 외부 API 인증 실패를 HTML 로그인 화면이 아닌 JSON 401 응답으로 변환한다.
 *
 * <p>기존 MVC 보안 체인은 인증되지 않은 브라우저를 {@code /login}으로 이동시킨다. 시스템 간 연동 호출은 리다이렉트를 성공 응답으로 오해할
 * 수 있으므로, {@code /api/v1/**}에서는 이 진입점을 사용해 HTTP 401과 공통 JSON 규격을 명시적으로 반환한다.
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        ApiResponse.error(
            ErrorCode.UNAUTHORIZED.getStatus().value(), ErrorCode.UNAUTHORIZED.getMessage()));
  }
}
