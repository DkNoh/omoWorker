package com.scbk.sms.controller.[( ${model.moduleName()} )];

[# th:if="${model.includePrivacy()}"]import com.scbk.sms.annotation.PrivacyLog;
[/]import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]SearchRequestDTO;
[# th:if="${model.includeCreateUpdate()}"]import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]UpdateRequestDTO;
[/]import com.scbk.sms.service.[( ${model.moduleName()} )].[( ${model.domainClass()} )]Service;
import com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO;
[# th:if="${model.includeExcel()}"]import jakarta.servlet.http.HttpServletResponse;
[/][# th:if="${model.includeCreateUpdate()}"]import jakarta.validation.Valid;
[/][# th:each="pkImport : ${model.pkParamImports()}"]import [( ${pkImport} )];
[/]import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
[# th:if="${model.includeCreateUpdate()}"]import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
[/]import org.springframework.web.bind.annotation.RequestMapping;
[# th:if="${model.includeCreateUpdate() or model.includePrivacy()}"]import org.springframework.web.bind.annotation.RequestParam;
[/]import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
 * 업무 로직은 TODO 위치에 직접 추가한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("[( ${model.screenUrl()} )]")
public class [( ${model.domainClass()} )]Controller {

    private final [( ${model.domainClass()} )]Service service;

    @GetMapping
    public String page() {
        return "[( ${model.moduleName()} )]/[( ${model.domainId()} )]";
    }

[# th:if="${model.includePrivacy()}"]    @PrivacyLog(action = "[( ${model.domainName()} )] 목록 조회", recordParameters = false)
[/]    @ResponseBody
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<PageResponseDTO<[( ${model.domainClass()} )]VO>>> getData(
            @ModelAttribute [( ${model.domainClass()} )]SearchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(service.search(request)));
    }
[# th:if="${model.includeCreateUpdate()}"]
    @ResponseBody
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody [( ${model.domainClass()} )]UpdateRequestDTO request) {
        service.create(request);
        return ResponseEntity.ok(ApiResponse.success("등록되었습니다.", null));
    }

    @ResponseBody
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<String>> update(@Valid @RequestBody [( ${model.domainClass()} )]UpdateRequestDTO request) {
        service.update(request);
        return ResponseEntity.ok(ApiResponse.success("수정되었습니다.", null));
    }

    @ResponseBody
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<String>> delete([# th:each="pk, iter : ${model.pkFields()}"]@RequestParam [( ${pk.javaType()} )] [( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]) {
        service.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]);
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }
[/][# th:if="${model.includeExcel()}"]
[# th:if="${model.includePrivacy()}"]    @PrivacyLog(action = "[( ${model.domainName()} )] 엑셀 다운로드")
[/]    @GetMapping("/excel")
    public void downloadExcel(@ModelAttribute [( ${model.domainClass()} )]SearchRequestDTO request,
                              HttpServletResponse response) {
        service.downloadExcel(request, response);
    }
[/][# th:if="${model.includePrivacy()}"]
    @PrivacyLog(action = "[( ${model.domainName()} )] 원문 상세 조회")
    @ResponseBody
    @GetMapping("/unmask")
    public ResponseEntity<ApiResponse<[( ${model.domainClass()} )]VO>> getUnmaskedDetail(@RequestParam [( ${model.pkJavaType()} )] [( ${model.pkFieldName()} )]) {
        return ResponseEntity.ok(ApiResponse.success(service.getUnmaskedDetail([( ${model.pkFieldName()} )])));
    }
[/]}
