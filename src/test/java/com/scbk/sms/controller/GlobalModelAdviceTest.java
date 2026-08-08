package com.scbk.sms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.scbk.sms.auth.SmsUserPrincipal;
import com.scbk.sms.service.menu.MenuAuthService;
import com.scbk.sms.service.menu.MenuCacheRevision;
import com.scbk.sms.service.menu.MenuPermission;
import com.scbk.sms.service.menu.MenuSource;
import com.scbk.sms.service.menu.PageAuth;
import com.scbk.sms.vo.auth.LoginEmployeeVO;
import com.scbk.sms.vo.menu.MenuItemVO;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

@ExtendWith(MockitoExtension.class)
class GlobalModelAdviceTest {

  @Mock private MenuSource menuSource;

  @Test
  void local에서는_화면용_권한을_모두_허용한다() {
    // given
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), new MenuCacheRevision(), environment);
    SmsUserPrincipal principal = principal();
    ExtendedModelMap model = new ExtendedModelMap();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history");
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(List.of(new MenuItemVO()));

    // when
    advice.addLayoutAttributes(principal, model, request);

    // then
    PageAuth pageAuth = (PageAuth) model.get("pageAuth");
    assertThat(pageAuth.isCreate()).isTrue();
    assertThat(pageAuth.isUpdate()).isTrue();
    assertThat(pageAuth.isDelete()).isTrue();
    assertThat(pageAuth.isDownload()).isTrue();
  }

  @Test
  void auth_mode_local만으로는_화면용_권한을_모두_허용하지_않는다() {
    // given
    MockEnvironment environment = new MockEnvironment().withProperty("sms.auth.mode", "local");
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), new MenuCacheRevision(), environment);
    SmsUserPrincipal principal = principal();
    ExtendedModelMap model = new ExtendedModelMap();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history");
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(List.of(new MenuItemVO()));
    given(menuSource.getPermissions("/sms/history", principal.getRoleCodes()))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when
    advice.addLayoutAttributes(principal, model, request);

    // then
    PageAuth pageAuth = (PageAuth) model.get("pageAuth");
    assertThat(pageAuth.isRead()).isTrue();
    assertThat(pageAuth.isCreate()).isFalse();
    assertThat(pageAuth.isUpdate()).isFalse();
    assertThat(pageAuth.isDelete()).isFalse();
  }

  @Test
  void nonLocal에서는_현재_URL의_메뉴권한을_pageAuth로_내려준다() {
    // given
    MockEnvironment environment = new MockEnvironment();
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), new MenuCacheRevision(), environment);
    SmsUserPrincipal principal = principal();
    ExtendedModelMap model = new ExtendedModelMap();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history");
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(List.of(new MenuItemVO()));
    given(menuSource.getPermissions("/sms/history", principal.getRoleCodes()))
        .willReturn(
            EnumSet.of(MenuPermission.READ, MenuPermission.UPDATE, MenuPermission.DOWNLOAD));

    // when
    advice.addLayoutAttributes(principal, model, request);

    // then
    PageAuth pageAuth = (PageAuth) model.get("pageAuth");
    assertThat(pageAuth.isRead()).isTrue();
    assertThat(pageAuth.isCreate()).isFalse();
    assertThat(pageAuth.isUpdate()).isTrue();
    assertThat(pageAuth.isDelete()).isFalse();
    assertThat(pageAuth.isDownload()).isTrue();
  }

  @Test
  void 같은_revision에서는_LNB와_pageAuth를_세션에서_재사용한다() {
    MockEnvironment environment = new MockEnvironment();
    MenuCacheRevision revision = new MenuCacheRevision();
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), revision, environment);
    SmsUserPrincipal principal = principal();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history");
    request.getSession();
    List<MenuItemVO> menus = List.of(new MenuItemVO());
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(menus);
    given(menuSource.hasMenu("/sms/history")).willReturn(true);
    given(menuSource.getPermissions("/sms/history", principal.getRoleCodes()))
        .willReturn(EnumSet.of(MenuPermission.READ));

    advice.addLayoutAttributes(principal, new ExtendedModelMap(), request);
    ExtendedModelMap secondModel = new ExtendedModelMap();
    advice.addLayoutAttributes(principal, secondModel, request);

    assertThat(secondModel.get("menus")).isSameAs(menus);
    assertThat(((PageAuth) secondModel.get("pageAuth")).isRead()).isTrue();
    then(menuSource).should(times(1)).getMenuTree(principal.getRoleCodes());
    then(menuSource).should(times(1)).hasMenu("/sms/history");
    then(menuSource).should(times(1)).getPermissions("/sms/history", principal.getRoleCodes());
  }

  @Test
  void 메뉴_revision이_바뀌면_기존_세션의_LNB와_pageAuth를_함께_다시_조회한다() {
    MockEnvironment environment = new MockEnvironment();
    MenuCacheRevision revision = new MenuCacheRevision();
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), revision, environment);
    SmsUserPrincipal principal = principal();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/history");
    request.getSession();
    List<MenuItemVO> oldMenus = List.of(new MenuItemVO());
    List<MenuItemVO> newMenus = List.of(new MenuItemVO());
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(oldMenus, newMenus);
    given(menuSource.hasMenu("/sms/history")).willReturn(true);
    given(menuSource.getPermissions("/sms/history", principal.getRoleCodes()))
        .willReturn(
            EnumSet.of(MenuPermission.READ),
            EnumSet.of(MenuPermission.READ, MenuPermission.UPDATE));

    advice.addLayoutAttributes(principal, new ExtendedModelMap(), request);
    revision.invalidateAfterCommit();
    ExtendedModelMap refreshedModel = new ExtendedModelMap();
    advice.addLayoutAttributes(principal, refreshedModel, request);

    assertThat(refreshedModel.get("menus")).isSameAs(newMenus);
    PageAuth refreshedAuth = (PageAuth) refreshedModel.get("pageAuth");
    assertThat(refreshedAuth.isRead()).isTrue();
    assertThat(refreshedAuth.isUpdate()).isTrue();
    then(menuSource).should(times(2)).getMenuTree(principal.getRoleCodes());
    then(menuSource).should(times(2)).hasMenu("/sms/history");
    then(menuSource).should(times(2)).getPermissions("/sms/history", principal.getRoleCodes());
  }

  @Test
  void 팝업_pageAuth는_읽기전용_부모권한에서_쓰기버튼을_모두_끈다() {
    PageAuth pageAuth = popupPageAuth(EnumSet.of(MenuPermission.READ));

    assertThat(pageAuth.isRead()).isTrue();
    assertThat(pageAuth.isCreate()).isFalse();
    assertThat(pageAuth.isUpdate()).isFalse();
    assertThat(pageAuth.isDelete()).isFalse();
  }

  @Test
  void 팝업_pageAuth는_등록전용_부모권한에서_create만_켠다() {
    PageAuth pageAuth = popupPageAuth(EnumSet.of(MenuPermission.READ, MenuPermission.CREATE));

    assertThat(pageAuth.isCreate()).isTrue();
    assertThat(pageAuth.isUpdate()).isFalse();
    assertThat(pageAuth.isDelete()).isFalse();
  }

  @Test
  void 팝업_pageAuth는_수정전용_부모권한에서_update만_켠다() {
    PageAuth pageAuth = popupPageAuth(EnumSet.of(MenuPermission.READ, MenuPermission.UPDATE));

    assertThat(pageAuth.isCreate()).isFalse();
    assertThat(pageAuth.isUpdate()).isTrue();
    assertThat(pageAuth.isDelete()).isFalse();
  }

  @Test
  void 팝업_pageAuth는_삭제전용_부모권한에서_delete만_켠다() {
    PageAuth pageAuth = popupPageAuth(EnumSet.of(MenuPermission.READ, MenuPermission.DELETE));

    assertThat(pageAuth.isCreate()).isFalse();
    assertThat(pageAuth.isUpdate()).isFalse();
    assertThat(pageAuth.isDelete()).isTrue();
  }

  private PageAuth popupPageAuth(Set<MenuPermission> basePermissions) {
    MockEnvironment environment = new MockEnvironment();
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), new MenuCacheRevision(), environment);
    SmsUserPrincipal principal = principal();
    ExtendedModelMap model = new ExtendedModelMap();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/basic/notice/popup");
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(List.of(new MenuItemVO()));
    given(menuSource.getPermissions("/basic/notice/popup", principal.getRoleCodes()))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/basic/notice", principal.getRoleCodes()))
        .willReturn(basePermissions);

    advice.addLayoutAttributes(principal, model, request);
    return (PageAuth) model.get("pageAuth");
  }

  // ----- Characterization: non-local 팝업 pageAuth의 부모 메뉴 상속 (v2 의도 보존) -----

  /**
   * 보호된 시나리오: /basic/notice/popup 처럼 메뉴로 등록되지 않은 suffix 자식 URL은 부모 메뉴의 쓰기 권한(CREATE/UPDATE/DELETE)을
   * pageAuth 로 그대로 상속한다. v2 의도(팝업은 부모 권한을 따른다)를 향후 리팩터에서 보존하기 위해 잠근다.
   */
  @Test
  void nonLocal_팝업은_부모_메뉴의_쓰기권한을_pageAuth로_상속한다() {
    // given : /basic/notice/popup 은 메뉴로 등록되지 않았고 부모 /basic/notice 가 풀 권한을 가진다
    MockEnvironment environment = new MockEnvironment();
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), new MenuCacheRevision(), environment);
    SmsUserPrincipal principal = principal();
    ExtendedModelMap model = new ExtendedModelMap();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/basic/notice/popup");
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(List.of(new MenuItemVO()));
    given(menuSource.getPermissions("/basic/notice/popup", principal.getRoleCodes()))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/basic/notice", principal.getRoleCodes()))
        .willReturn(
            EnumSet.of(
                MenuPermission.READ,
                MenuPermission.CREATE,
                MenuPermission.UPDATE,
                MenuPermission.DELETE));

    // when
    advice.addLayoutAttributes(principal, model, request);

    // then : 팝업은 부모의 CREATE/UPDATE/DELETE 를 그대로 노출한다 (버튼 게이팅용)
    PageAuth pageAuth = (PageAuth) model.get("pageAuth");
    assertThat(pageAuth.isRead()).isTrue();
    assertThat(pageAuth.isCreate()).isTrue();
    assertThat(pageAuth.isUpdate()).isTrue();
    assertThat(pageAuth.isDelete()).isTrue();
  }

  // ----- New: 정확 활성 메뉴 + 권한 없음 → 부모 쓰기권한 상속 차단 (UI 권한 상승 방어) -----

  /**
   * {@code /basic/notice/popup} 이 활성 메뉴로 등록됐지만( {@code hasMenu=true}) 현재 역할에 부여된 권한이 없는 경우, 부모
   * {@code /basic/notice} 의 풀 권한이 pageAuth 에 노출되면 안 된다. 정확 메뉴 자신의 빈 권한({@link PageAuth#none()})이
   * 내려가야 한다.
   */
  @Test
  void nonLocal_정확_메뉴가_등록됐지만_권한없으면_부모_쓰기권한을_노출하지_않는다() {
    // given
    MockEnvironment environment = new MockEnvironment();
    GlobalModelAdvice advice =
        new GlobalModelAdvice(
            menuSource, new MenuAuthService(menuSource), new MenuCacheRevision(), environment);
    SmsUserPrincipal principal = principal();
    ExtendedModelMap model = new ExtendedModelMap();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/basic/notice/popup");
    given(menuSource.getMenuTree(principal.getRoleCodes())).willReturn(List.of(new MenuItemVO()));
    given(menuSource.hasMenu("/basic/notice/popup")).willReturn(true);
    given(menuSource.getPermissions("/basic/notice/popup", principal.getRoleCodes()))
        .willReturn(EnumSet.noneOf(MenuPermission.class));

    // when
    advice.addLayoutAttributes(principal, model, request);

    // then : 정확 메뉴가 존재하므로 기준 URL은 /basic/notice/popup 자신. 부모 권한이 노출되지 않는다.
    PageAuth pageAuth = (PageAuth) model.get("pageAuth");
    assertThat(pageAuth.isRead()).isFalse();
    assertThat(pageAuth.isCreate()).isFalse();
    assertThat(pageAuth.isUpdate()).isFalse();
    assertThat(pageAuth.isDelete()).isFalse();
  }

  private SmsUserPrincipal principal() {
    LoginEmployeeVO employee = new LoginEmployeeVO();
    employee.setEmpId("admin");
    employee.setDepId("D001");
    employee.setEmpNm("관리자");
    employee.setDepNm("관리부");
    return new SmsUserPrincipal(employee, List.of("ROLE_ADMIN"));
  }
}
