package com.scbk.sms.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthSourceGuardTest {

  @Test
  void local_profile이_아니면_auth_mode_local을_허용하지_않는다() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("dev");

    assertThatThrownBy(() -> new AuthSourceGuard(environment, "db", "db", "local"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sms.auth.mode=local");
  }

  @Test
  void local_profile에서는_auth_mode_local을_허용한다() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");

    assertThatCode(() -> new AuthSourceGuard(environment, "static", "static", "local"))
        .doesNotThrowAnyException();
  }
}
