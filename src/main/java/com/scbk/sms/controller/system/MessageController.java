package com.scbk.sms.controller.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/system/message")
public class MessageController {

  @GetMapping("")
  public String edit() {
    return "system/message-edit";
  }
}
