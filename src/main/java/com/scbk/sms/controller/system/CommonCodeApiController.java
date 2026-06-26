package com.scbk.sms.controller.system;

import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.service.system.CommonCodeService;
import com.scbk.sms.vo.common.CommonCodeVO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 화면 콤보/자동완성용 공통코드 API.
 *
 * <p>로그인한 모든 사용자가 사용하는 화면 보조 API이므로 메뉴 권한 Interceptor 검증 대상에서 제외한다 (WebMvcConfig 공통 제외 경로). Spring
 * Security 인증(로그인)은 그대로 적용된다.
 */
@RestController
@RequestMapping("/api/common-code")
public class CommonCodeApiController {

  private final CommonCodeService commonCodeService;

  public CommonCodeApiController(CommonCodeService commonCodeService) {
    this.commonCodeService = commonCodeService;
  }

  @GetMapping("/{codeType}")
  public ResponseEntity<ApiResponse<List<CommonCodeVO>>> getCodes(
      @PathVariable String codeType, @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(
        ApiResponse.success(commonCodeService.getCommonCodes(codeType, keyword)));
  }
}
