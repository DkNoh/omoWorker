package com.scbk.sms.service.menu;

import com.scbk.sms.vo.menu.MenuItemVO;
import java.util.List;
import java.util.Set;

/**
 * 메뉴 트리와 메뉴 권한의 단일 출처. {@code sms.menu.source}(db|static)로 구현이 선택된다. 트리와 권한은 같은
 * 출처(TB_MENU/TB_MENU_AUTH 또는 static baseline)에서 오므로 하나로 묶는다.
 */
public interface MenuSource {

  List<MenuItemVO> getMenuTree(List<String> roleCodes);

  /** 메뉴 URL과 역할 목록으로 보유 권한을 조회한다. 메뉴가 없거나 부여된 권한이 없으면 빈 Set을 반환한다. */
  Set<MenuPermission> getPermissions(String menuUrl, List<String> roleCodes);

  /**
   * 메뉴 URL이 활성 메뉴로 등록돼 있는지 조회한다. 역할 부여({@code TB_MENU_AUTH})와 무관하게 메뉴 자체의 존재만 판단한다.
   *
   * <p>이 값은 {@link #getPermissions} 결과가 빈 {@code Set}일 때 "메뉴는 있지만 현재 역할에 부여된 권한이 없는 경우"와 "메뉴 자체가 없는
   * 경우"를 구분하는 데 쓰인다. 화면용 pageAuth 계산이 이 구분 없이 suffix 부모로 넘어가면, 정확 메뉴에 권한이 없는 사용자가 부모 메뉴의 쓰기 권한을 UI
   * 에 노출받게 된다.
   */
  boolean hasMenu(String menuUrl);
}
