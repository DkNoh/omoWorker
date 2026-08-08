package com.scbk.sms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.api.security.ApiAuthenticationEntryPoint;
import com.scbk.sms.api.security.ApiKeyAuthenticationFilter;
import com.scbk.sms.api.security.ApiKeyProperties;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ApiKeyProperties.class)
public class SecurityConfig {

  @Bean
  public ApiAuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper objectMapper) {
    return new ApiAuthenticationEntryPoint(objectMapper);
  }

  /**
   * 외부 REST API 전용 보안 체인.
   *
   * <p>이 체인은 {@code /api/v1/**}에만 가장 먼저 적용된다. 외부 시스템은 브라우저 세션이나 로그인 폼을 사용하지 않으므로 세션을 만들지 않고,
   * CSRF 대신 API Key로 인증한다. 인증 실패 시 로그인 페이지로 이동하지 않고 JSON 401을 반환한다.
   */
  @Bean
  @Order(1)
  public SecurityFilterChain externalApiSecurityFilterChain(
      HttpSecurity http,
      ApiKeyProperties apiKeyProperties,
      ApiAuthenticationEntryPoint authenticationEntryPoint)
      throws Exception {
    // Filter 자체를 Spring Bean으로 만들면 Boot가 서블릿 필터로도 자동 등록해 Security 체인과 중복 실행할 수 있다.
    // 외부 API 체인 안에서만 한 번 실행되도록 여기서 명시적으로 생성한다.
    ApiKeyAuthenticationFilter apiKeyAuthenticationFilter =
        new ApiKeyAuthenticationFilter(apiKeyProperties, authenticationEntryPoint);

    http.securityMatcher("/api/v1/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
        .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("EXTERNAL_API"))
        .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** 기존 Thymeleaf MVC와 화면 내부 AJAX 요청용 세션 기반 보안 체인. */
  @Bean
  @Order(2)
  public SecurityFilterChain webSecurityFilterChain(
      HttpSecurity http, List<AuthenticationProvider> authenticationProviders) throws Exception {
    for (AuthenticationProvider provider : authenticationProviders) {
      http.authenticationProvider(provider);
    }

    http
        // CSRF 활성화(Spring Security 기본값). Thymeleaf 폼(th:action)은 토큰을 자동 주입하고,
        // axios 호출은 common-utils.js 요청 인터셉터가 <meta name="_csrf">를 헤더로 싣는다.
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/login", "/error")
                    .permitAll()
                    .requestMatchers(
                        "/css/**", "/js/**", "/lib/**", "/vendor/**", "/img/**", "/favicon.ico")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("empId")
                    .passwordParameter("password")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error")
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
