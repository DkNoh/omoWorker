package com.scbk.sms.service.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

  @Test
  void popup_suffix는_부모_화면의_READ_권한으로_판단한다() {
    // given : window.open 팝업 화면(/basic/notice/popup)은 부모 메뉴의 READ 자식 화면이다
    given(menuSource.getPermissions("/basic/notice/popup", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/basic/notice", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then : READ 사용자는 팝업 화면을 열 수 있다 (쓰기 권한 불필요)
    assertThatCode(() -> menuAuthService.checkAccess("/basic/notice/popup", ROLES))
        .doesNotThrowAnyException();
  }

  @Test
  void popup_suffix는_READ_권한이_없으면_거부한다() {
    // given : 부모 메뉴에 READ가 없다 (메뉴 권한 행 자체가 없다)
    given(menuSource.getPermissions(anyString(), anyList()))
        .willReturn(EnumSet.noneOf(MenuPermission.class));

    // when / then
    assertThatThrownBy(() -> menuAuthService.checkAccess("/basic/notice/popup", ROLES))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void detail_suffix는_READ_권한이_있으면_통과한다() {
    // given : /basic/notice/detail (JSON)은 READ 자식 액션이다
    given(menuSource.getPermissions("/basic/notice/detail", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/basic/notice", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then
    assertThatCode(() -> menuAuthService.checkAccess("/basic/notice/detail", ROLES))
        .doesNotThrowAnyException();
  }

  @Test
  void detail_suffix는_READ_권한이_없으면_거부한다() {
    // given : 부모 메뉴에는 쓰기 권한만 있고 READ가 없다
    given(menuSource.getPermissions("/basic/notice/detail", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/basic/notice", ROLES))
        .willReturn(EnumSet.of(MenuPermission.CREATE, MenuPermission.UPDATE));

    // when / then : 쓰기 권한으로 detail READ 권한을 넓히지 않는다
    assertThatThrownBy(() -> menuAuthService.checkAccess("/basic/notice/detail", ROLES))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void baseMenuPath는_popup_suffix를_붙인_자식_화면을_부모_메뉴_URL로_정규화한다() {
    // given : /basic/notice/popup 은 등록된 메뉴 URL이 아니다(메뉴는 /basic/notice)
    given(menuSource.getPermissions("/basic/notice/popup", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));

    // when
    String base = menuAuthService.resolveBaseMenuPath("/basic/notice/popup", ROLES);

    // then : checkAccess 가 /popup suffix를 떼고 부모 화면으로 취급하는 것과 동일한 기준 URL
    assertThat(base).isEqualTo("/basic/notice");
  }

  @Test
  void baseMenuPath는_등록된_화면_URL은_suffix와_겹쳐도_그대로_둔다() {
    // given : /campaign/sms/register 는 /register suffix가 아니라 화면 메뉴 자체다
    given(menuSource.getPermissions("/campaign/sms/register", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when
    String base = menuAuthService.resolveBaseMenuPath("/campaign/sms/register", ROLES);

    // then : 정확 일치가 suffix strip 보다 우선한다 (checkAccess 1단계와 동일)
    assertThat(base).isEqualTo("/campaign/sms/register");
  }

  @Test
  void baseMenuPath는_등록된_suffix가_없는_URL은_원래대로_반환한다() {
    // given : 메뉴도 아니고 알려진 suffix도 아니다
    given(menuSource.getPermissions("/unknown/path", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));

    // when
    String base = menuAuthService.resolveBaseMenuPath("/unknown/path", ROLES);

    // then : 호출자가 빈 권한(none)으로 처리하도록 원래 URL을 돌려준다
    assertThat(base).isEqualTo("/unknown/path");
  }

  // ----- Characterization (baseline lock, refactors must preserve these) -----

  /**
   * 보호된 시나리오: 정확 일치 메뉴가 READ 권한으로 등록돼 있을 때 /register suffix 규칙(CREATE 요구)이 우회하지 않는다. 이 특성은 향후 리팩터에서
   * 의도치 않게 깨지지 않아야 한다.
   */
  @Test
  void baseline_정확_일치_메뉴는_suffix_규칙보다_항상_우선한다() {
    // given : /campaign/sms/register 는 /register suffix 와 충돌하지만 정확히 일치하는 화면 메뉴다.
    // 정확 메뉴로 등록된 경우 READ 만 있어도 접근이 허용된다(suffix 규칙의 CREATE 요구를 적용하지 않는다).
    given(menuSource.getPermissions("/campaign/sms/register", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then
    assertThatCode(() -> menuAuthService.checkAccess("/campaign/sms/register", ROLES))
        .doesNotThrowAnyException();
  }

  /**
   * 보호된 시나리오: /popup suffix URL이 메뉴로 등록되지 않았을 때 부모 메뉴의 READ 권한만으로 접근을 허용한다. (v2 의도: window.open 팝업은
   * 부모의 READ 자식)
   */
  @Test
  void baseline_popup_suffix는_부모_READ만으로_접근을_허용한다() {
    // given : /basic/notice/popup 은 등록된 메뉴가 없고 /popup suffix 로 부모 READ 를 검증한다.
    given(menuSource.getPermissions("/basic/notice/popup", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/basic/notice", ROLES))
        .willReturn(EnumSet.of(MenuPermission.READ));

    // when / then : 쓰기 권한 없이 READ 만 있어도 팝업 화면 접근(화면 열기)은 허용된다
    assertThatCode(() -> menuAuthService.checkAccess("/basic/notice/popup", ROLES))
        .doesNotThrowAnyException();
  }

  // ----- New: 정확 활성 메뉴 + 역할 권한 없음 → 부모 권한 상속 차단 (UI 권한 상승 방어) -----

  /**
   * 정확히 일치하는 활성 메뉴(예: /basic/notice/popup 이 메뉴로 등록됨)에 현재 역할이 아무 권한도 부여받지 못한 경우, suffix 규칙(/popup →
   * 부모 READ 요구)을 우회해 부모 READ 로 접근을 허용하지 않는다. 메뉴는 존재하지만 권한이 없다는 명시적 거부다.
   */
  @Test
  void 정확_popup_메뉴에_권한이_없으면_부모_READ로_접근할_수_없다() {
    // given : /basic/notice/popup 이 활성 메뉴로 등록됐지만 현재 역할에는 부여된 권한이 없다.
    // 부모 /basic/notice 는 READ 권한을 가진다(유혹). suffix 규칙 alone 으로는 접근을 허용해 버린다.
    given(menuSource.getPermissions(anyString(), anyList()))
        .willAnswer(
            invocation ->
                "/basic/notice".equals(invocation.getArgument(0))
                    ? EnumSet.of(MenuPermission.READ)
                    : EnumSet.noneOf(MenuPermission.class));
    given(menuSource.hasMenu("/basic/notice/popup")).willReturn(true);

    // when / then : 메뉴가 존재하면 부모 READ 로 승격하지 않고 ACCESS_DENIED 를 던진다.
    assertThatThrownBy(() -> menuAuthService.checkAccess("/basic/notice/popup", ROLES))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  /**
   * /campaign/sms/register 처럼 /register suffix 와 충돌하지만 실제 메뉴로 등록된 URL에서, 현재 역할에 권한이 없으면 부모
   * /campaign/sms 의 CREATE 권한으로 접근을 허용하지 않는다.
   */
  @Test
  void 정확_register_메뉴에_권한이_없으면_부모_CREATE로_접근할_수_없다() {
    // given
    given(menuSource.getPermissions(anyString(), anyList()))
        .willAnswer(
            invocation ->
                "/campaign/sms".equals(invocation.getArgument(0))
                    ? EnumSet.of(MenuPermission.CREATE)
                    : EnumSet.noneOf(MenuPermission.class));
    given(menuSource.hasMenu("/campaign/sms/register")).willReturn(true);

    // when / then
    assertThatThrownBy(() -> menuAuthService.checkAccess("/campaign/sms/register", ROLES))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  /**
   * 활성 메뉴가 등록되지 않은 suffix URL(legacy /register 같은 action URL)은 기존대로 부모 메뉴의 권한으로 접근을 허용한다. {@code
   * hasMenu} 가 false 인 경우에만 suffix fallback 이 동작한다.
   */
  @Test
  void 정확한_자식_메뉴가_없으면_suffix가_요구하는_부모_권한으로_접근한다() {
    // given : 정확 메뉴가 없고 /register 가 요구하는 CREATE 를 부모가 보유한다
    given(menuSource.getPermissions("/campaign/sms/register", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));
    given(menuSource.getPermissions("/campaign/sms", ROLES))
        .willReturn(EnumSet.of(MenuPermission.CREATE));

    // when / then
    assertThatCode(() -> menuAuthService.checkAccess("/campaign/sms/register", ROLES))
        .doesNotThrowAnyException();
    verify(menuSource).hasMenu("/campaign/sms/register");
  }

  // ----- resolveBaseMenuPath: hasMenu 로 정확 메뉴 존재와 권한 부재를 구분한다 -----

  /**
   * 활성 메뉴로 등록됐지만 현재 역할에 부여된 권한이 없는 URL(빈 Set)은 {@code resolveBaseMenuPath} 가 정확 URL 자신을 반환한다.
   * suffix 부모 URL 을 반환하면 pageAuth 가 부모의 쓰기 권한을 노출한다.
   */
  @Test
  void baseMenuPath는_정확_메뉴가_등록됐지만_권한이_없으면_부모로_넘어가지_않는다() {
    // given : /basic/notice/popup 이 활성 메뉴로 등록됐지만 현재 역할에는 부여된 권한이 없다
    given(menuSource.hasMenu("/basic/notice/popup")).willReturn(true);

    // when
    String base = menuAuthService.resolveBaseMenuPath("/basic/notice/popup", ROLES);

    // then : 정확 메뉴 자신이 기준 → pageAuth 는 PageAuth.none() 이 된다.
    // 부모 /basic/notice 로 넘어가면 안 된다 (부모 쓰기 권한 노출 = UI 권한 상승).
    assertThat(base).isEqualTo("/basic/notice/popup");
  }

  /**
   * 활성 메뉴로 등록되지 않았고 현재 역할에 권한도 없는 URL(빈 Set)은 기존대로 suffix 를 떼어낸 부모 URL 을 반환한다. ({@link
   * #baseline_popup_suffix는_부모_READ만으로_접근을_허용한다} 의 pageAuth 대응)
   */
  @Test
  void baseMenuPath는_정확_메뉴가_없고_권한도_없으면_suffix_부모를_반환한다() {
    // given : /basic/notice/popup 은 등록된 메뉴가 아니고 권한도 없다 → 기존 suffix 상속 경로
    given(menuSource.hasMenu("/basic/notice/popup")).willReturn(false);
    given(menuSource.getPermissions("/basic/notice/popup", ROLES))
        .willReturn(EnumSet.noneOf(MenuPermission.class));

    // when
    String base = menuAuthService.resolveBaseMenuPath("/basic/notice/popup", ROLES);

    // then : 메뉴가 없으면 /popup suffix 로 부모 /basic/notice 를 반환한다 (기존 동작 보존)
    assertThat(base).isEqualTo("/basic/notice");
  }
}
