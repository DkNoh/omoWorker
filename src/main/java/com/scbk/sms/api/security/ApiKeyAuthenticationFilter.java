package com.scbk.sms.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code /api/v1/**} 요청의 API Key를 검증하는 외부 연동 전용 필터.
 *
 * <p>호출자는 {@code X-API-Client-Id}와 {@code X-API-Key}를 함께 전송한다. 서버는 수신한 키를 SHA-256으로 해시한 뒤 설정에 저장된
 * 해시와 상수 시간 비교한다. 키 원문은 필드에 보관하거나 로그에 기록하지 않는다.
 *
 * <p>인증 성공 시 외부 시스템 ID를 Spring Security principal로 등록하고 {@code ROLE_EXTERNAL_API} 권한을 부여한다. 이후 API에서
 * {@code Authentication#getName()}을 사용하면 실제 키를 노출하지 않고 호출 시스템을 감사 로그에 남길 수 있다.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  public static final String CLIENT_ID_HEADER = "X-API-Client-Id";
  public static final String API_KEY_HEADER = "X-API-Key";

  private static final String API_PATH_PREFIX = "/api/v1/";
  private static final String EXTERNAL_API_ROLE = "ROLE_EXTERNAL_API";

  private final ApiKeyProperties properties;
  private final ApiAuthenticationEntryPoint authenticationEntryPoint;

  public ApiKeyAuthenticationFilter(
      ApiKeyProperties properties, ApiAuthenticationEntryPoint authenticationEntryPoint) {
    this.properties = properties;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  /** 내부 화면과 화면용 JSON 요청에는 API Key 검사를 적용하지 않는다. */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // requestURI에는 WAR context path가 포함될 수 있으므로 이를 제거한 애플리케이션 경로로 판별한다.
    // servletPath는 서블릿 매핑과 테스트 실행 방식에 따라 빈 문자열이 될 수 있어 보안 경계 판별에 사용하지 않는다.
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    String applicationPath = requestUri.substring(contextPath.length());
    return !applicationPath.startsWith(API_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String clientId = request.getHeader(CLIENT_ID_HEADER);
    String apiKey = request.getHeader(API_KEY_HEADER);

    if (!isAuthenticatedClient(clientId, apiKey)) {
      // 실패 원인이 키 누락인지 불일치인지 구분해 주면 키 존재 여부를 추측할 수 있으므로 항상 같은 401을 반환한다.
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(
          request, response, new BadCredentialsException("External API authentication failed"));
      return;
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            clientId, null, List.of(new SimpleGrantedAuthority(EXTERNAL_API_ROLE))));
    SecurityContextHolder.setContext(context);

    try {
      filterChain.doFilter(request, response);
    } finally {
      // 서블릿 스레드는 재사용되므로 다음 요청에 외부 시스템 인증 정보가 남지 않게 반드시 정리한다.
      SecurityContextHolder.clearContext();
    }
  }

  private boolean isAuthenticatedClient(String clientId, String apiKey) {
    if (!properties.isConfigured()
        || clientId == null
        || apiKey == null
        || !properties.getClientId().equals(clientId)) {
      return false;
    }

    byte[] expectedHash = HexFormat.of().parseHex(properties.getKeySha256());
    byte[] actualHash = sha256(apiKey);
    return MessageDigest.isEqual(expectedHash, actualHash);
  }

  private byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256은 Java 필수 알고리즘이므로 발생하면 런타임 자체가 비정상인 상황이다.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
