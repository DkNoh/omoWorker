package com.scbk.sms.controller.basic;

import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.basic.AuditLogSearchRequestDTO;
import com.scbk.sms.service.basic.AuditLogService;
import com.scbk.sms.vo.basic.AuditLogVO;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/basic/audit-log")
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    public String page() {
        return "basic/audit-log";
    }

    @ResponseBody
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<PageResponseDTO<AuditLogVO>>> getData(
            @ModelAttribute AuditLogSearchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(service.search(request)));
    }

    @GetMapping("/excel")
    public void downloadExcel(@ModelAttribute AuditLogSearchRequestDTO request,
                              HttpServletResponse response) {
        service.downloadExcel(request, response);
    }
}
