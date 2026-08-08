package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import com.scbk.sms.vo.menu.MenuItemVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class MenuHierarchyUiContractTest {

  private static final Path RESOURCES = Path.of("src", "main", "resources");

  @Test
  void 메뉴_관리_부모_후보는_타입과_무관하고_자신과_자손만_제외한다() throws IOException {
    String js = read("static/js/system/menu-manage.js");

    assertThat(js)
        .contains("const excluded = new Set()")
        .contains("excluded.add(excludeMenuId)")
        .contains(".filter(m => !excluded.has(m.menuId))")
        .doesNotContain("m.menuType === 'G'", "하위 메뉴는 그룹(G) 메뉴 아래에만 추가할 수 있습니다.");
  }

  @Test
  void 자식이_있는_화면_메뉴는_이동_링크와_펼침_버튼을_따로_제공한다() throws IOException {
    String sidebar = read("templates/fragments/sidebar-menu-items.html");
    String header = read("templates/fragments/header.html");

    assertThat(sidebar)
        .contains("sidebar-parent-link", "th:href=\"@{${menuItem.menuUrl}}\"")
        .contains("sidebar-submenu-toggle")
        .contains("sidebar-menu-items :: render(${menuItem.children}, ${depth + 1})");
    assertThat(header)
        .contains(":scope > .sidebar-parent-link span")
        .contains("groupName !== path[0]");
  }

  @Test
  void 화면_메뉴_M과_하위_메뉴가_재귀적으로_렌더링된다() {
    MenuItemVO parent = menu("PARENT", "부모 화면", "parent");
    MenuItemVO child = menu("CHILD", "자식 화면", "parent/child");
    parent.getChildren().add(child);
    Context context = new Context();
    context.setVariable("items", List.of(parent));
    context.setVariable("depth", 0);

    String rendered = templateEngine().process("fragments/sidebar-menu-items", context);

    assertThat(rendered)
        .contains("href=\"parent\"")
        .contains("sidebar-parent-link", "sidebar-submenu-toggle")
        .contains("부모 화면", "href=\"parent/child\"", "자식 화면");
  }

  private static MenuItemVO menu(String menuId, String menuNm, String menuUrl) {
    MenuItemVO menu = new MenuItemVO();
    menu.setMenuId(menuId);
    menu.setMenuNm(menuNm);
    menu.setMenuUrl(menuUrl);
    menu.setMenuType("M");
    return menu;
  }

  private static SpringTemplateEngine templateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode("HTML");
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }

  private static String read(String relativePath) throws IOException {
    return Files.readString(RESOURCES.resolve(relativePath));
  }
}
