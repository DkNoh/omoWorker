package com.scbk.sms.service.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaticMenuSourceTest {

  private static final List<String> ROLES = List.of("ROLE_ADMIN");

  private StaticMenuSource provider;

  @BeforeEach
  void setUp() {
    provider = new StaticMenuSource(new MenuTreeBuilder());
  }

  @Test
  void baseline_메뉴_URL에는_모든_권한을_부여한다() {
    // when
    Set<MenuPermission> permissions = provider.getPermissions("/sms/history", ROLES);

    // then
    assertThat(permissions).containsExactlyInAnyOrder(MenuPermission.values());
  }

  @Test
  void local_메뉴_트리_확인_URL에는_모든_권한을_부여한다() {
    // when
    Set<MenuPermission> permissions = provider.getPermissions("/system/menu-tree", ROLES);

    // then
    assertThat(permissions).containsExactlyInAnyOrder(MenuPermission.values());
  }

  @Test
  void baseline에_없는_URL에는_권한을_부여하지_않는다() {
    // when
    Set<MenuPermission> permissions = provider.getPermissions("/unknown/path", ROLES);

    // then
    assertThat(permissions).isEmpty();
  }
}
