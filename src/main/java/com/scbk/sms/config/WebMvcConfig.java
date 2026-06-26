package com.scbk.sms.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /** 메뉴 권한 검증을 거치지 않는 공통 경로. 화면/업무 URL은 여기에 추가하지 않는다. */
  private static final List<String> COMMON_EXCLUDE_PATHS =
      List.of(
          "/",
          "/login",
          "/logout",
          "/error",
          "/css/**",
          "/js/**",
          "/lib/**",
          "/vendor/**",
          "/img/**",
          "/favicon.ico",
          // 공통코드 콤보/자동완성: 로그인한 모든 사용자가 사용하는 화면 보조 API
          "/api/common-code/**");

  private final MenuAuthInterceptor menuAuthInterceptor;
  private final List<String> configuredExcludePaths;

  public WebMvcConfig(
      MenuAuthInterceptor menuAuthInterceptor,
      @Value("${sms.menu.auth.exclude-paths:}") String excludePaths) {
    this.menuAuthInterceptor = menuAuthInterceptor;
    this.configuredExcludePaths =
        Arrays.stream(excludePaths.split(","))
            .map(String::trim)
            .filter(path -> !path.isEmpty())
            .toList();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    InterceptorRegistration registration =
        registry
            .addInterceptor(menuAuthInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(COMMON_EXCLUDE_PATHS);
    if (!configuredExcludePaths.isEmpty()) {
      registration.excludePathPatterns(configuredExcludePaths);
    }
  }
}
