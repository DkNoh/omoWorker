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
 * [( ${model.domainClass()} )] 화면과 JSON API의 HTTP 진입점.
 *
 * <p>요청값 바인딩과 공통 ApiResponse 변환만 담당하며 업무 규칙과 트랜잭션은 Service에 둔다.
 * Scaffold 최초 생성 후에는 개발자가 직접 수정해 소유하고, 템플릿과 자동 동기화하지 않는다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("[( ${model.screenUrl()} )]")
public class [( ${model.domainClass()} )]Controller {

    private final [( ${model.domainClass()} )]Service service;

    /** 기본 레이아웃에 결합할 업무 화면을 반환한다. */
    @GetMapping
    public String page() {
        return "[( ${model.moduleName()} )]/[( ${model.domainId()} )]";
    }

    /** 검색조건과 페이지 요청을 전달해 목록을 공통 페이지 응답으로 반환한다. */
[# th:if="${model.includePrivacy()}"]    @PrivacyLog(action = "[( ${model.domainName()} )] 목록 조회", recordParameters = false)
[/]
    @ResponseBody
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<PageResponseDTO<[( ${model.domainClass()} )]VO>>> getData(
            @ModelAttribute [( ${model.domainClass()} )]SearchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(service.search(request)));
    }
[# th:if="${model.includeCreateUpdate()}"]
    /** 검증된 화이트리스트 DTO로 신규 데이터를 등록한다. */
    @ResponseBody
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody [( ${model.domainClass()} )]UpdateRequestDTO request) {
        service.create(request);
        return ResponseEntity.ok(ApiResponse.success("등록되었습니다.", null));
    }

    /** 식별값과 수정 허용 필드만 전달해 기존 데이터를 수정한다. */
    @ResponseBody
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<String>> update(@Valid @RequestBody [( ${model.domainClass()} )]UpdateRequestDTO request) {
        service.update(request);
        return ResponseEntity.ok(ApiResponse.success("수정되었습니다.", null));
    }

    /** 복합키를 포함한 실제 PK 값으로 대상 한 건을 삭제한다. */
    @ResponseBody
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<String>> delete([# th:each="pk, iter : ${model.pkFields()}"]@RequestParam [( ${pk.javaType()} )] [( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]) {
        service.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]);
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }
[/][# th:if="${model.includeExcel()}"]
    /** 현재 검색조건 전체 결과를 서버 스트리밍 방식으로 다운로드한다. */
[# th:if="${model.includePrivacy()}"]    @PrivacyLog(action = "[( ${model.domainName()} )] 엑셀 다운로드")
[/]
    @GetMapping("/excel")
    public void downloadExcel(@ModelAttribute [( ${model.domainClass()} )]SearchRequestDTO request,
                              HttpServletResponse response) {
        service.downloadExcel(request, response);
    }
[/][# th:if="${model.includePrivacy()}"]
    /** 마스킹 해제 권한과 감사로그를 전제로 원문 상세 한 건을 반환한다. */
    @PrivacyLog(action = "[( ${model.domainName()} )] 원문 상세 조회")
    @ResponseBody
    @GetMapping("/unmask")
    public ResponseEntity<ApiResponse<[( ${model.domainClass()} )]VO>> getUnmaskedDetail(@RequestParam [( ${model.pkJavaType()} )] [( ${model.pkFieldName()} )]) {
        return ResponseEntity.ok(ApiResponse.success(service.getUnmaskedDetail([( ${model.pkFieldName()} )])));
    }
[/]}
