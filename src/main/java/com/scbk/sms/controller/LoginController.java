package com.scbk.sms.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

  private final String authMode;

  public LoginController(@Value("${sms.auth.mode}") String authMode) {
    this.authMode = authMode;
  }

  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("localMode", "local".equalsIgnoreCase(authMode));
    return "login";
  }
}
