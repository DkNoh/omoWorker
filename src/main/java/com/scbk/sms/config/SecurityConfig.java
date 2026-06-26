package com.scbk.sms.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
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
