package com.scbk.sms.controller.sms;

import com.scbk.sms.annotation.PrivacyLog;
import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.CustomerSearchSearchRequestDTO;
import com.scbk.sms.dto.sms.CustomerSearchUpdateRequestDTO;
import com.scbk.sms.service.sms.CustomerSearchService;
import com.scbk.sms.vo.sms.CustomerSearchVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. 업무 로직은 TODO 위치에 직접 추가한다. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/sms/customer-search")
public class CustomerSearchController {

  private final CustomerSearchService service;

  @GetMapping
  public String page() {
    return "sms/customer-search";
  }

  @PrivacyLog(action = "고객별 조회 목록 조회", recordParameters = false)
  @ResponseBody
  @GetMapping("/data")
  public ResponseEntity<ApiResponse<PageResponseDTO<CustomerSearchVO>>> getData(
      @ModelAttribute CustomerSearchSearchRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(service.search(request)));
  }

  @ResponseBody
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<String>> create(
      @Valid @RequestBody CustomerSearchUpdateRequestDTO request) {
    service.create(request);
    return ResponseEntity.ok(ApiResponse.success("등록되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/update")
  public ResponseEntity<ApiResponse<String>> update(
      @Valid @RequestBody CustomerSearchUpdateRequestDTO request) {
    service.update(request);
    return ResponseEntity.ok(ApiResponse.success("수정되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/delete")
  public ResponseEntity<ApiResponse<String>> delete(@RequestParam Integer customerId) {
    service.delete(customerId);
    return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
  }

  @PrivacyLog(action = "고객별 조회 원문 상세 조회")
  @ResponseBody
  @GetMapping("/unmask")
  public ResponseEntity<ApiResponse<CustomerSearchVO>> getUnmaskedDetail(
      @RequestParam Integer customerId) {
    return ResponseEntity.ok(ApiResponse.success(service.getUnmaskedDetail(customerId)));
  }
}
