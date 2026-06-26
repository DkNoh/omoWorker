package com.scbk.sms.controller.system;

import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.system.ScaffoldApplyFileResultDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.service.system.ScaffoldService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Query Scaffold 생성기. local 전용 개발 도구라 메뉴에 등록하지 않으며, application-local.yml의
 * sms.menu.auth.exclude-paths로 접근을 연다.
 */
@Controller
@Profile("local")
@RequestMapping("/system/scaffold")
public class ScaffoldController {

  private final ScaffoldService scaffoldService;

  public ScaffoldController(ScaffoldService scaffoldService) {
    this.scaffoldService = scaffoldService;
  }

  @GetMapping
  public String page() {
    return "system/scaffold";
  }

  @ResponseBody
  @PostMapping("/analyze")
  public ResponseEntity<ApiResponse<Map<String, Object>>> analyze(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            scaffoldService.analyze(request.get("rawQuery"), request.get("targetTable"))));
  }

  @ResponseBody
  @PostMapping("/generate")
  public ResponseEntity<ApiResponse<Map<String, String>>> generate(
      @Valid @RequestBody ScaffoldRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(scaffoldService.generate(request)));
  }

  @ResponseBody
  @PostMapping("/preview")
  public ResponseEntity<ApiResponse<List<ScaffoldApplyFileResultDTO>>> preview(
      @Valid @RequestBody ScaffoldRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(scaffoldService.preview(request)));
  }

  @ResponseBody
  @PostMapping("/apply")
  public ResponseEntity<ApiResponse<List<ScaffoldApplyFileResultDTO>>> apply(
      @Valid @RequestBody ScaffoldRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(scaffoldService.apply(request)));
  }
}
