package com.scbk.sms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.auth.SmsUserPrincipal;
import com.scbk.sms.service.menu.MenuPermission;
import com.scbk.sms.service.menu.MenuSource;
import com.scbk.sms.service.menu.PageAuth;
import com.scbk.sms.vo.auth.LoginEmployeeVO;
import com.scbk.sms.vo.menu.MenuItemVO;
import java.util.EnumSet;
import java.util.List;
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
    GlobalModelAdvice advice = new GlobalModelAdvice(menuSource, environment);
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
    GlobalModelAdvice advice = new GlobalModelAdvice(menuSource, environment);
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
    GlobalModelAdvice advice = new GlobalModelAdvice(menuSource, environment);
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

  private SmsUserPrincipal principal() {
    LoginEmployeeVO employee = new LoginEmployeeVO();
    employee.setEmpId("admin");
    employee.setDepId("D001");
    employee.setEmpNm("관리자");
    employee.setDepNm("관리부");
    return new SmsUserPrincipal(employee, List.of("ROLE_ADMIN"));
  }
}
