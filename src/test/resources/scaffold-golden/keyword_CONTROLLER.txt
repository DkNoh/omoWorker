package com.scbk.sms.controller.sms;

import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.service.sms.SmsHistoryService;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
 * 업무 로직은 TODO 위치에 직접 추가한다.
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
}
