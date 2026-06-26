package com.scbk.sms.service.menu;

import com.scbk.sms.vo.menu.MenuItemVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MenuTreeBuilder {

  public List<MenuItemVO> build(List<MenuItemVO> flatMenus) {
    Map<String, MenuItemVO> byId = new LinkedHashMap<>();
    for (MenuItemVO menu : flatMenus) {
      validateMenuId(menu);
      menu.getChildren().clear();
      MenuItemVO previous = byId.put(menu.getMenuId(), menu);
      if (previous != null) {
        throw new IllegalStateException("Duplicate menuId: " + menu.getMenuId());
      }
    }

    List<MenuItemVO> roots = new ArrayList<>();
    for (MenuItemVO menu : flatMenus) {
      String parentMenuId = menu.getParentMenuId();
      if (parentMenuId == null || parentMenuId.isBlank()) {
        roots.add(menu);
        continue;
      }

      MenuItemVO parent = byId.get(parentMenuId);
      if (parent == null) {
        throw new IllegalStateException(
            "Parent menu not found: " + menu.getMenuId() + " -> " + parentMenuId);
      }
      parent.getChildren().add(menu);
    }
    return roots;
  }

  private void validateMenuId(MenuItemVO menu) {
    if (menu.getMenuId() == null || menu.getMenuId().isBlank()) {
      throw new IllegalStateException("menuId must not be blank");
    }
  }
}
