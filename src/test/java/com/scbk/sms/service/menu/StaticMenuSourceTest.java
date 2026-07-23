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
  void 메뉴관리_URL에는_모든_권한을_부여한다() {
    // when
    Set<MenuPermission> permissions = provider.getPermissions("/system/menu-manage", ROLES);

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

  // ----- hasMenu: 메뉴 존재와 권한 부여를 구분한다 (정확 메뉴 + 권한 없음 거부의 핵심) -----

  /**
   * baseline 에 등록된 메뉴 URL 은 {@code hasMenu} 가 {@code true} 를 반환한다. 권한 부여 ({@link #getPermissions})와
   * 무관하게 메뉴 자체의 존재만 판별한다.
   */
  @Test
  void baseline_메뉴_URL은_hasMenu_true를_반환한다() {
    // when / then
    assertThat(provider.hasMenu("/sms/history")).isTrue();
    assertThat(provider.hasMenu("/basic/notice")).isTrue();
    assertThat(provider.hasMenu("/system/menu-tree")).isTrue();
    assertThat(provider.hasMenu("/system/menu-manage")).isTrue();
  }

  /**
   * baseline 에 없는 URL(예: /basic/notice/popup 같은 suffix 자식)은 {@code hasMenu} 가 {@code false} 를 반환한다.
   * 이 경우 {@code MenuAuthService.resolveBaseMenuPath} 는 suffix 부모로 넘어간다.
   */
  @Test
  void baseline에_없는_URL은_hasMenu_false를_반환한다() {
    // when / then : /basic/notice/popup 은 baseline 에 없으므로 hasMenu=false.
    assertThat(provider.hasMenu("/basic/notice/popup")).isFalse();
    assertThat(provider.hasMenu("/unknown/path")).isFalse();
    assertThat(provider.hasMenu("")).isFalse();
    assertThat(provider.hasMenu(null)).isFalse();
  }
}
