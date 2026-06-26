package com.scbk.sms.controller.system;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** local 전용 메뉴 트리 확인 화면. 운영 메뉴 관리는 DB 메뉴 테이블을 기준으로 별도 구현한다. */
@Controller
@Profile("local")
@RequestMapping("/system/menu-tree")
public class MenuTreeController {

  private final Environment environment;

  public MenuTreeController(Environment environment) {
    this.environment = environment;
  }

  @GetMapping
  public String page(Model model) {
    model.addAttribute("activeProfiles", String.join(",", environment.getActiveProfiles()));
    model.addAttribute("menuSource", environment.getProperty("sms.menu.source"));
    model.addAttribute("roleSource", environment.getProperty("sms.role.source"));
    return "system/menu-tree";
  }
}
