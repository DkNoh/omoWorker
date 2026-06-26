package com.scbk.sms.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 메뉴/역할 source 조합을 부팅 시점에 강제한다. 잘못된 조합이면 컨텍스트 기동을 실패시킨다.
 *
 * <p>보호하는 불변식 (menu-authority.md, menu-source-policy.md 기준): 1. prod profile은 db source만 허용한다.
 * static이 운영에 새면 baseline 전 권한 부여 사고가 난다. (StaticMenuSource는 baseline URL에 모든 권한을 부여한다) 2.
 * role=static은 menu=static과만 함께 쓴다. db 메뉴를 static 임시 역할로 거르는 조합은 금지한다. (menu=static + role=db는 dev
 * 메뉴 검증용으로 허용되는 정상 조합이다)
 */
@Component
public class AuthSourceGuard {

  private static final String DB = "db";
  private static final String STATIC = "static";

  public AuthSourceGuard(
      Environment environment,
      @Value("${sms.menu.source:db}") String menuSource,
      @Value("${sms.role.source:db}") String roleSource,
      @Value("${sms.auth.mode:ldap}") String authMode) {
    List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
    boolean local = activeProfiles.contains("local");
    boolean prod = activeProfiles.contains("prod");

    if (!local && "local".equalsIgnoreCase(authMode)) {
      throw new IllegalStateException(
          "sms.auth.mode=local은 spring profile local에서만 허용한다. "
              + "activeProfiles="
              + activeProfiles);
    }

    if (prod && (!DB.equals(menuSource) || !DB.equals(roleSource))) {
      throw new IllegalStateException(
          "prod profile은 db source만 허용한다. "
              + "sms.menu.source="
              + menuSource
              + ", sms.role.source="
              + roleSource);
    }

    if (DB.equals(menuSource) && STATIC.equals(roleSource)) {
      throw new IllegalStateException(
          "잘못된 조합: db 메뉴는 static 역할과 함께 쓸 수 없다. "
              + "role=static은 menu=static(local)에서만 쓴다. "
              + "sms.menu.source="
              + menuSource
              + ", sms.role.source="
              + roleSource);
    }
  }
}
