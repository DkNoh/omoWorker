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
}
