package com.scbk.sms.controller.sms;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SmsRegisterController {

  @GetMapping("/campaign/sms/register")
  public String campaignRegister(Model model) {
    model.addAttribute("targetGroups", List.of());
    model.addAttribute("businessTypes", List.of());
    model.addAttribute("messageTemplates", List.of());
    return "sms/campaign-register";
  }
}
