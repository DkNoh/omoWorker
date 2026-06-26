package com.scbk.sms.service.menu;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuAuthServiceTest {

  private static final List<String> ROLES = List.of("ROLE_USER");

  @Mock private MenuSource menuSource;

  private MenuAuthService menuAuthService;

  @BeforeEach
  void setUp() {
    menuAuthService = new MenuAuthService(menuSource);
  }

  @Test
  void 화면_URL은_READ_권한이_있으면_통과한다() {
    // given
    given(menuSource.getPermissions("/sms/history", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then
    assertThatCode(() -> menuAuthService.checkAccess("/sms/history", ROLES))
        .doesNotThrowAnyException();
  }

  @Test
  void suffix와_겹치는_화면_URL은_정확_일치가_우선이다() {
    // given : /campaign/sms/register는 /register suffix가 아니라 화면 메뉴 자체다
    given(menuSource.getPermissions("/campaign/sms/register", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then
    assertThatCode(() -> menuAuthService.checkAccess("/campaign/sms/register", ROLES))
        .doesNotThrowAnyException();
  }

  @Test
  void data_suffix는_부모_화면의_READ_권한으로_판단한다() {
    // given
    given(menuSource.getPermissions("/sms/history/data", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/sms/history", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then
    assertThatCode(() -> menuAuthService.checkAccess("/sms/history/data", ROLES))
        .doesNotThrowAnyException();
  }

  @Test
  void excel_suffix는_DOWNLOAD_권한이_없으면_거부한다() {
    // given : READ만 있고 DOWNLOAD가 없다
    given(menuSource.getPermissions("/sms/history/excel", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/sms/history", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then
    assertThatThrownBy(() -> menuAuthService.checkAccess("/sms/history/excel", ROLES))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void legacy_save는_CREATE와_UPDATE를_모두_요구한다() {
    // given : CREATE만 있다
    given(menuSource.getPermissions("/system/message/save", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/system/message", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ, MenuPermission.CREATE));

    // when / then
    assertThatThrownBy(() -> menuAuthService.checkAccess("/system/message/save", ROLES))
        .isInstanceOf(CustomException.class);
  }

  @Test
  void legacy_save는_CREATE와_UPDATE가_모두_있으면_통과한다() {
    // given
    given(menuSource.getPermissions("/system/message/save", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/system/message", ROLES))
        .willReturn(EnumSet.of(MenuPermission.CREATE, MenuPermission.UPDATE));

    // when / then
    assertThatCode(() -> menuAuthService.checkAccess("/system/message/save", ROLES))
        .doesNotThrowAnyException();
  }

  @Test
  void 화면_URL에_READ가_없으면_거부한다() {
    // given : 메뉴 권한 행은 있지만 READ가 'N'이다
    given(menuSource.getPermissions("/sms/history", ROLES))
        .willReturn(EnumSet.of(MenuPermission.DOWNLOAD));

    // when / then
    assertThatThrownBy(() -> menuAuthService.checkAccess("/sms/history", ROLES))
        .isInstanceOf(CustomException.class);
  }

  @Test
  void 메뉴에_연결되지_않은_URL은_거부한다() {
    // given
    given(menuSource.getPermissions("/unknown/path", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));

    // when / then
    assertThatThrownBy(() -> menuAuthService.checkAccess("/unknown/path", ROLES))
        .isInstanceOf(CustomException.class);
  }
}
