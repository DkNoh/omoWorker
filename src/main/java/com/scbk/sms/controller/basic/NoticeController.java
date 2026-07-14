package com.scbk.sms.controller.basic;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.basic.NoticeUpdateRequestDTO;
import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.service.basic.NoticeService;
import com.scbk.sms.vo.basic.NoticeVO;
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
@RequestMapping("/basic/notice")
public class NoticeController {

  private final NoticeService service;

  @GetMapping
  public String page() {
    return "basic/notice";
  }

  @GetMapping("/popup")
  public String popup() {
    return "basic/notice-popup";
  }

  @ResponseBody
  @GetMapping("/data")
  public ResponseEntity<ApiResponse<PageResponseDTO<NoticeVO>>> getData(
      @ModelAttribute NoticeSearchRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(service.search(request)));
  }

  @ResponseBody
  @GetMapping("/detail")
  public ResponseEntity<ApiResponse<NoticeVO>> getDetail(@RequestParam Integer noticeId) {
    return ResponseEntity.ok(ApiResponse.success(service.getDetail(noticeId)));
  }

  @ResponseBody
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<String>> create(
      @Valid @RequestBody NoticeUpdateRequestDTO request) {
    service.create(request);
    return ResponseEntity.ok(ApiResponse.success("등록되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/update")
  public ResponseEntity<ApiResponse<String>> update(
      @Valid @RequestBody NoticeUpdateRequestDTO request) {
    service.update(request);
    return ResponseEntity.ok(ApiResponse.success("수정되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/delete")
  public ResponseEntity<ApiResponse<String>> delete(@RequestParam Integer noticeId) {
    service.delete(noticeId);
    return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
  }
}
