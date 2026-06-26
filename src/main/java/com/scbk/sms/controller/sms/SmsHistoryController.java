package com.scbk.sms.controller.sms;

import com.scbk.sms.annotation.PrivacyLog;
import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.dto.sms.SmsHistoryUpdateRequestDTO;
import com.scbk.sms.service.sms.SmsHistoryService;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 발송이력 컨트롤러.
 *
 * <p>권한 매핑(MenuAuthService 접미사 규칙):
 *
 * <ul>
 *   <li>/data → READ
 *   <li>/create, /update, /delete → CREATE/UPDATE/DELETE
 *   <li>/excel → DOWNLOAD
 *   <li>/{id}/unmask → MASK_VIEW (민감정보 원문 조회, 감사 로그 대상)
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/sms/history")
public class SmsHistoryController {

  private final SmsHistoryService service;

  @GetMapping
  public String page() {
    return "sms/history";
  }

  @ResponseBody
  @GetMapping("/data")
  public ResponseEntity<ApiResponse<PageResponseDTO<SmsHistoryVO>>> getData(
      @ModelAttribute SmsHistorySearchRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(service.search(request)));
  }

  /** 엑셀 다운로드(수신번호 마스킹 적용). 감사 로그 대상. */
  @PrivacyLog(action = "발송이력 엑셀 다운로드")
  @GetMapping("/excel")
  public void downloadExcel(
      @ModelAttribute SmsHistorySearchRequestDTO request, HttpServletResponse response) {
    service.downloadExcel(response, request);
  }

  /** 수신번호 원문 조회(CAN_MASK_VIEW). 마스킹하지 않은 원문을 반환하며 감사 로그를 남긴다. */
  @PrivacyLog(action = "발송이력 수신번호 원문 조회")
  @ResponseBody
  @GetMapping("/{smsHistoryId}/unmask")
  public ResponseEntity<ApiResponse<SmsHistoryVO>> unmask(@PathVariable Integer smsHistoryId) {
    return ResponseEntity.ok(ApiResponse.success(service.getUnmaskedDetail(smsHistoryId)));
  }

  @ResponseBody
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<String>> create(
      @Valid @RequestBody SmsHistoryUpdateRequestDTO request) {
    service.create(request);
    return ResponseEntity.ok(ApiResponse.success("등록되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/update")
  public ResponseEntity<ApiResponse<String>> update(
      @Valid @RequestBody SmsHistoryUpdateRequestDTO request) {
    service.update(request);
    return ResponseEntity.ok(ApiResponse.success("수정되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/delete")
  public ResponseEntity<ApiResponse<String>> delete(@RequestParam Integer smsHistoryId) {
    service.delete(smsHistoryId);
    return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
  }
}
