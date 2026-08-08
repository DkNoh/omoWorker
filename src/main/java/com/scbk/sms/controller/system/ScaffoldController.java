package com.scbk.sms.controller.system;

import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.system.ScaffoldApplyFileResultDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.service.system.ScaffoldService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Query Scaffold의 화면과 JSON API를 제공하는 local 전용 진입점.
 *
 * <p>HTTP 계층은 요청 검증과 {@link ApiResponse} 포장만 담당한다. SQL 분석, 템플릿 렌더링, 파일 경로 검증과 쓰기는 {@link
 * ScaffoldService} 이하로 위임한다. 운영 프로필에서 코드 생성 API가 노출되지 않도록 {@code local} 프로필로 제한하며, 메뉴에 등록하지
 * 않고 {@code application-local.yml}의 {@code sms.menu.auth.exclude-paths}로만 접근을 허용한다.
 */
@Controller
@Profile("local")
@RequestMapping("/system/scaffold")
public class ScaffoldController {

  private final ScaffoldService scaffoldService;

  public ScaffoldController(ScaffoldService scaffoldService) {
    this.scaffoldService = scaffoldService;
  }

  /** 스캐폴드 설정과 미리보기를 수행하는 개발 도구 화면을 반환한다. */
  @GetMapping
  public String page() {
    return "system/scaffold";
  }

  /** 원본 SELECT와 대상 테이블을 분석해 컬럼, 검색변수, PK, DB comment 기본값을 반환한다. */
  @ResponseBody
  @PostMapping("/analyze")
  public ResponseEntity<ApiResponse<Map<String, Object>>> analyze(
      @RequestBody Map<String, String> request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            scaffoldService.analyze(request.get("rawQuery"), request.get("targetTable"))));
  }

  /** 파일을 쓰지 않고 요청 모델로부터 산출물별 소스 문자열을 생성한다. */
  @ResponseBody
  @PostMapping("/generate")
  public ResponseEntity<ApiResponse<Map<String, String>>> generate(
      @Valid @RequestBody ScaffoldRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(scaffoldService.generate(request)));
  }

  /** 적용 예정 경로와 NEW/UNCHANGED/OVERWRITE 상태를 계산하되 디스크는 변경하지 않는다. */
  @ResponseBody
  @PostMapping("/preview")
  public ResponseEntity<ApiResponse<List<ScaffoldApplyFileResultDTO>>> preview(
      @Valid @RequestBody ScaffoldRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(scaffoldService.preview(request)));
  }

  /** 재생성 메타데이터를 보관하고 미리보기에서 검증한 프로젝트 경로에 산출물을 기록한다. */
  @ResponseBody
  @PostMapping("/apply")
  public ResponseEntity<ApiResponse<List<ScaffoldApplyFileResultDTO>>> apply(
      @Valid @RequestBody ScaffoldRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(scaffoldService.apply(request)));
  }
}
