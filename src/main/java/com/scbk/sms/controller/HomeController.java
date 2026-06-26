package com.scbk.sms.controller;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  private final Environment environment;

  public HomeController(Environment environment) {
    this.environment = environment;
  }

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("activeProfiles", String.join(",", environment.getActiveProfiles()));
    model.addAttribute("menuSource", environment.getProperty("sms.menu.source"));
    model.addAttribute("roleSource", environment.getProperty("sms.role.source"));
    return "index";
  }
}
