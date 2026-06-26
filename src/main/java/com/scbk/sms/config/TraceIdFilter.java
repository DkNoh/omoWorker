package com.scbk.sms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에 MDC traceId/requestId/method/uri 를 심는 필터.
 *
 * <p>구조화(JSON) 로그(logback-spring.xml prod 프로파일)에서 traceId로 요청 단위 추적이 가능해진다. Spring Security 체인보다 먼저
 * 실행되도록 {@link TraceIdFilterConfig}에서 HIGHEST_PRECEDENCE로 등록한다.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String MDC_TRACE_ID = "traceId";
  public static final String MDC_REQUEST_ID = "requestId";
  public static final String MDC_METHOD = "method";
  public static final String MDC_URI = "uri";

  /** 클라이언트가 X-Request-Id 헤더로 전달한 값이 있으면 그대로 사용(분산 추적 호환). */
  public static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    String requestId = resolveRequestId(request, traceId);
    try {
      MDC.put(MDC_TRACE_ID, traceId);
      MDC.put(MDC_REQUEST_ID, requestId);
      MDC.put(MDC_METHOD, request.getMethod());
      MDC.put(MDC_URI, request.getRequestURI());
      // 응답에도 노출해서 클라이언트/프론트가 동일 traceId로 매칭 가능
      response.setHeader(MDC_TRACE_ID, traceId);
      response.setHeader(REQUEST_ID_HEADER, requestId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private String resolveRequestId(HttpServletRequest request, String fallback) {
    String header = request.getHeader(REQUEST_ID_HEADER);
    return (header != null && !header.isBlank()) ? header : fallback;
  }
}
