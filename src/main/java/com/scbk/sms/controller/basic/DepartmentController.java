package com.scbk.sms.controller.basic;

import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.basic.DepartmentSearchRequestDTO;
import com.scbk.sms.service.basic.DepartmentService;
import com.scbk.sms.vo.basic.DepartmentVO;
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
@RequestMapping("/basic/department")
public class DepartmentController {

    private final DepartmentService service;

    @GetMapping
    public String page() {
        return "basic/department";
    }

    @ResponseBody
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<PageResponseDTO<DepartmentVO>>> getData(
            @ModelAttribute DepartmentSearchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(service.search(request)));
    }
}
