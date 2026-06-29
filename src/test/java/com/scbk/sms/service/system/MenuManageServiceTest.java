package com.scbk.sms.service.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.system.MenuAuthRowDTO;
import com.scbk.sms.dto.system.MenuDetailResponseDTO;
import com.scbk.sms.dto.system.MenuSearchRequestDTO;
import com.scbk.sms.dto.system.MenuUpdateRequestDTO;
import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.system.MenuManageMapper;
import com.scbk.sms.vo.system.MenuAuthDetailVO;
import com.scbk.sms.vo.system.MenuManageVO;
import com.scbk.sms.vo.system.MenuRoleVO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuManageServiceTest {

  @Mock private MenuManageMapper mapper;

  private MenuManageService service;

  @BeforeEach
  void setUp() {
    service = new MenuManageService(mapper);
  }

  @Test
  void 목록_조회는_페이지_응답으로_감싼다() {
    // given
    MenuSearchRequestDTO request = new MenuSearchRequestDTO();
    request.setPage(1);
    request.setSize(10);
    given(mapper.count(request)).willReturn(1);
    given(mapper.selectList(request)).willReturn(List.of(new MenuManageVO()));

    // when
    PageResponseDTO<MenuManageVO> result = service.search(request);

    // then
    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getContents()).hasSize(1);
  }

  @Test
  void 트리는_평탄_목록을_그대로_반환한다() {
    // given
    given(mapper.selectTree())
        .willReturn(List.of(menu("G_BASIC", null, "G"), menu("M1", "G_BASIC", "M")));

    // when
    List<MenuManageVO> tree = service.getTree();

    // then
    assertThat(tree).hasSize(2);
    assertThat(tree.get(0).getMenuId()).isEqualTo("G_BASIC");
    then(mapper).should().selectTree();
  }

  @Test
  void 상세는_메뉴_역할_권한을_한번에_반환한다() {
    // given
    MenuManageVO menu = menu("M1", "G_BASIC", "M");
    menu.setMenuUrl("/x");
    given(mapper.selectByMenuId("M1")).willReturn(menu);
    given(mapper.selectActiveRoles()).willReturn(List.of(role("ROLE_ADMIN")));
    given(mapper.selectMenuAuthDetails("M1")).willReturn(List.of(authDetail("ROLE_ADMIN")));

    // when
    MenuDetailResponseDTO detail = service.getDetail("M1");

    // then
    assertThat(detail.getMenu().getMenuId()).isEqualTo("M1");
    assertThat(detail.getActiveRoles()).hasSize(1);
    assertThat(detail.getAuthRows()).hasSize(1);
  }

  @Test
  void 상세는_메뉴가_없으면_DATA_NOT_FOUND를_던진다() {
    given(mapper.selectByMenuId("NONE")).willReturn(null);

    assertThatThrownBy(() -> service.getDetail("NONE"))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.DATA_NOT_FOUND);
  }

  @Test
  void 상세는_역할이나_권한이_null이면_빈_목록으로_채운다() {
    // given
    MenuManageVO menu = menu("M1", null, "M");
    menu.setMenuUrl("/x");
    given(mapper.selectByMenuId("M1")).willReturn(menu);
    given(mapper.selectActiveRoles()).willReturn(null);
    given(mapper.selectMenuAuthDetails("M1")).willReturn(null);

    // when
    MenuDetailResponseDTO detail = service.getDetail("M1");

    // then
    assertThat(detail.getActiveRoles()).isNotNull().isEmpty();
    assertThat(detail.getAuthRows()).isNotNull().isEmpty();
  }

  @Test
  void 등록_시_메뉴타입_M_은_URL이_필수다() {
    // given
    MenuUpdateRequestDTO request = menuRequest("NEW_MENU", "M", null);

    // when / then
    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.MENU_TYPE_URL_INVALID);
    then(mapper).shouldHaveNoInteractions();
  }

  @Test
  void 등록_시_메뉴타입_G_는_URL을_가질_수_없다() {
    // given
    MenuUpdateRequestDTO request = menuRequest("NEW_GROUP", "G", "/should/not/have");

    // when / then
    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.MENU_TYPE_URL_INVALID);
  }

  @Test
  void 등록_시_이미_존재하는_메뉴ID면_실패한다() {
    // given
    MenuUpdateRequestDTO request = menuRequest("EXISTING", "M", "/x");
    given(mapper.selectByMenuId("EXISTING")).willReturn(new MenuManageVO());

    // when / then
    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_MENU_ID);
  }

  @Test
  void 등록_시_URL이_중복되면_실패한다() {
    // given
    MenuUpdateRequestDTO request = menuRequest("NEW_MENU", "M", "/dup");
    given(mapper.selectByMenuId("NEW_MENU")).willReturn(null);
    given(mapper.countByUrlExceptMenuId("/dup", "NEW_MENU")).willReturn(1);

    // when / then
    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_MENU_URL);
  }

  @Test
  void 등록_정상_시_Mapper에_위임한다() {
    // given
    MenuUpdateRequestDTO request = menuRequest("NEW_MENU", "M", "/ok");
    given(mapper.selectByMenuId("NEW_MENU")).willReturn(null);
    given(mapper.countByUrlExceptMenuId("/ok", "NEW_MENU")).willReturn(0);

    // when
    service.create(request);

    // then
    then(mapper).should().insert(request);
  }

  @Test
  void 수정_대상이_없으면_충돌로_실패한다() {
    // given
    MenuUpdateRequestDTO request = menuRequest("NONE", "M", "/x");
    given(mapper.selectByMenuId("NONE")).willReturn(null);

    // when / then
    assertThatThrownBy(() -> service.update(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.UPDATE_CONFLICT);
  }

  @Test
  void 시스템_메뉴는_URL_변경이_불가하다() {
    // given : 기존 메뉴가 시스템 메뉴이고 요청이 URL을 바꾼다
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("G_BASIC");
    existing.setSystemYn("Y");
    existing.setMenuUrl(null);
    given(mapper.selectByMenuId("G_BASIC")).willReturn(existing);

    MenuUpdateRequestDTO request = menuRequest("G_BASIC", "G", "/changed");

    // when / then
    assertThatThrownBy(() -> service.update(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.SYSTEM_MENU_PROTECTED);
  }

  @Test
  void 시스템_메뉴는_URL이_같으면_다른_필드_수정이_가능하다() {
    // given : 시스템 메뉴지만 URL을 유지하면서 메뉴명만 바꾼다
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("G_BASIC");
    existing.setSystemYn("Y");
    existing.setMenuUrl(null);
    given(mapper.selectByMenuId("G_BASIC")).willReturn(existing);
    given(mapper.update(any())).willReturn(1);

    MenuUpdateRequestDTO request = menuRequest("G_BASIC", "G", null);

    // when
    service.update(request);

    // then
    then(mapper).should().update(request);
  }

  @Test
  void 수정_결과가_0건이면_충돌로_실패한다() {
    // given
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("M");
    existing.setSystemYn("N");
    existing.setMenuUrl("/old");
    given(mapper.selectByMenuId("M")).willReturn(existing);
    given(mapper.update(any())).willReturn(0);

    MenuUpdateRequestDTO request = menuRequest("M", "M", "/old");

    // when / then
    assertThatThrownBy(() -> service.update(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.UPDATE_CONFLICT);
  }

  @Test
  void 수정_시_authRows가_null이면_권한_행을_건드리지_않는다() {
    // given
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("M");
    existing.setSystemYn("N");
    existing.setMenuUrl("/old");
    given(mapper.selectByMenuId("M")).willReturn(existing);
    given(mapper.update(any())).willReturn(1);

    MenuUpdateRequestDTO request = menuRequest("M", "M", "/old");
    request.setAuthRows(null);

    // when
    service.update(request);

    // then
    then(mapper).should(never()).deleteMenuAuthByMenuId(any());
    then(mapper).should(never()).insertMenuAuth(any());
  }

  @Test
  void 수정_시_authRows가_있으면_삭제후_재삽입_한다() {
    // given
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("M");
    existing.setSystemYn("N");
    existing.setMenuUrl("/old");
    given(mapper.selectByMenuId("M")).willReturn(existing);
    given(mapper.update(any())).willReturn(1);

    MenuUpdateRequestDTO request = menuRequest("M", "M", "/old");
    MenuAuthRowDTO row = new MenuAuthRowDTO();
    row.setRoleCd("ROLE_ADMIN");
    row.setCanRead("Y");
    row.setCanCreate(null); // null은 'N'으로 정규화
    request.setAuthRows(new ArrayList<>(List.of(row)));

    // when
    service.update(request);

    // then : 기존 행을 지우고 1건을 다시 넣는다
    then(mapper).should().deleteMenuAuthByMenuId("M");
    then(mapper).should().insertMenuAuth(any());
    assertThat(row.getMenuId()).isEqualTo("M"); // 부모 menuId로 강제
    assertThat(row.getCanRead()).isEqualTo("Y");
    assertThat(row.getCanCreate()).isEqualTo("N"); // null → N 정규화
  }

  @Test
  void 수정_시_authRows가_빈_리스트면_권한을_전부_지운다() {
    // given
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("M");
    existing.setSystemYn("N");
    existing.setMenuUrl("/old");
    given(mapper.selectByMenuId("M")).willReturn(existing);
    given(mapper.update(any())).willReturn(1);

    MenuUpdateRequestDTO request = menuRequest("M", "M", "/old");
    request.setAuthRows(new ArrayList<>());

    // when
    service.update(request);

    // then : 삭제만 수행, insert 없음
    then(mapper).should().deleteMenuAuthByMenuId("M");
    then(mapper).should(never()).insertMenuAuth(any());
  }

  @Test
  void 삭제_대상이_없으면_충돌로_실패한다() {
    given(mapper.selectByMenuId("NONE")).willReturn(null);

    assertThatThrownBy(() -> service.delete("NONE"))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.DELETE_CONFLICT);
  }

  @Test
  void 시스템_메뉴는_삭제할_수_없다() {
    // given
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("G_BASIC");
    existing.setSystemYn("Y");
    given(mapper.selectByMenuId("G_BASIC")).willReturn(existing);

    // when / then
    assertThatThrownBy(() -> service.delete("G_BASIC"))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.SYSTEM_MENU_PROTECTED);
  }

  @Test
  void 하위_메뉴가_있으면_삭제할_수_없다() {
    // given
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("G_BASIC");
    existing.setSystemYn("N");
    given(mapper.selectByMenuId("G_BASIC")).willReturn(existing);
    given(mapper.countChildren("G_BASIC")).willReturn(2);

    // when / then
    assertThatThrownBy(() -> service.delete("G_BASIC"))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.MENU_HAS_CHILDREN);
    then(mapper).should(never()).delete(any());
    then(mapper).should(never()).deleteMenuAuthByMenuId(any());
  }

  @Test
  void 일반_메뉴_삭제는_권한행_삭제_후_메뉴행을_삭제한다() {
    // given
    MenuManageVO existing = new MenuManageVO();
    existing.setMenuId("CUSTOM");
    existing.setSystemYn("N");
    given(mapper.selectByMenuId("CUSTOM")).willReturn(existing);
    given(mapper.countChildren("CUSTOM")).willReturn(0);
    willReturn(1).given(mapper).delete(eq("CUSTOM"));

    // when
    service.delete("CUSTOM");

    // then : auth 먼저 지우고 그 다음 메뉴 행을 지운다 (순서: delete → deleteMenuAuthByMenuId)
    then(mapper).should().deleteMenuAuthByMenuId("CUSTOM");
    then(mapper).should().delete("CUSTOM");
  }

  private MenuManageVO menu(String menuId, String parentMenuId, String menuType) {
    MenuManageVO m = new MenuManageVO();
    m.setMenuId(menuId);
    m.setParentMenuId(parentMenuId);
    m.setMenuNm(menuId);
    m.setMenuType(menuType);
    return m;
  }

  private MenuRoleVO role(String roleCd) {
    MenuRoleVO r = new MenuRoleVO();
    r.setRoleCd(roleCd);
    r.setRoleNm(roleCd);
    r.setUseYn("Y");
    r.setSortOrd(10);
    return r;
  }

  private MenuAuthDetailVO authDetail(String roleCd) {
    MenuAuthDetailVO a = new MenuAuthDetailVO();
    a.setRoleCd(roleCd);
    a.setRoleNm(roleCd);
    a.setCanRead("Y");
    return a;
  }

  private MenuUpdateRequestDTO menuRequest(String menuId, String menuType, String menuUrl) {
    MenuUpdateRequestDTO request = new MenuUpdateRequestDTO();
    request.setMenuId(menuId);
    request.setMenuNm(menuId);
    request.setMenuType(menuType);
    request.setMenuUrl(menuUrl);
    return request;
  }
}
