package com.scbk.sms.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** TraceIdFilter를 Spring Security 체인보다 먼저 실행되도록 최우선 순위로 등록. */
@Configuration
public class TraceIdFilterConfig {

  @Bean
  public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
    FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setFilter(filter);
    registration.addUrlPatterns("/*");
    registration.setName("traceIdFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
