package com.scbk.sms.controller.@@MODULE_NAME@@;

@@IMPORTS@@
/**
 * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
 * 업무 로직은 TODO 위치에 직접 추가한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("@@SCREEN_URL@@")
public class @@DOMAIN_CLASS@@Controller {

    private final @@DOMAIN_CLASS@@Service service;

    @GetMapping
    public String page() {
        return "@@MODULE_NAME@@/@@DOMAIN_ID@@";
    }

@@DATA_PRIVACY_LOG@@    @ResponseBody
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<PageResponseDTO<@@DOMAIN_CLASS@@VO>>> getData(
            @ModelAttribute @@DOMAIN_CLASS@@SearchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(service.search(request)));
    }
@@CRUD_SECTION@@@@EXCEL_SECTION@@@@PRIVACY_SECTION@@}
