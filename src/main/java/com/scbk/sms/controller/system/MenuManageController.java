package com.scbk.sms.controller.system;

import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.system.MenuDetailResponseDTO;
import com.scbk.sms.dto.system.MenuSearchRequestDTO;
import com.scbk.sms.dto.system.MenuUpdateRequestDTO;
import com.scbk.sms.service.system.MenuManageService;
import com.scbk.sms.vo.system.MenuManageVO;
import jakarta.validation.Valid;
import java.util.List;
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

/**
 * 시스템관리 > 메뉴관리(TB_MENU). 트리 UI 화면과 상세/권한 매트릭스 데이터를 제공한다. 등록/수정/삭제는 MenuAuthInterceptor가
 * /create·/update·/delete suffix 권한으로 검증한다. /tree·/detail·/data는 READ.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/system/menu-manage")
public class MenuManageController {

  private final MenuManageService service;

  @GetMapping
  public String page() {
    return "system/menu-manage";
  }

  /** 메뉴 트리 평탄 목록. 클라이언트가 트리로 조립한다. */
  @ResponseBody
  @GetMapping("/tree")
  public ResponseEntity<ApiResponse<List<MenuManageVO>>> getTree() {
    return ResponseEntity.ok(ApiResponse.success(service.getTree()));
  }

  /** 선택 메뉴 상세(메뉴 + 활성 역할 + 현재 권한 행). */
  @ResponseBody
  @GetMapping("/detail")
  public ResponseEntity<ApiResponse<MenuDetailResponseDTO>> getDetail(@RequestParam String menuId) {
    return ResponseEntity.ok(ApiResponse.success(service.getDetail(menuId)));
  }

  /**
   * 기존 v1 그리드 조회 호환 엔드포인트. 트리 UI 도입 이후 화면이 사용하지 않더라도 READ 접미사로 권한이 열려 있으므로, 다른 클라이언트/테스트가 영향받지 않도록
   * 유지한다.
   */
  @ResponseBody
  @GetMapping("/data")
  public ResponseEntity<ApiResponse<PageResponseDTO<MenuManageVO>>> getData(
      @ModelAttribute MenuSearchRequestDTO request) {
    return ResponseEntity.ok(ApiResponse.success(service.search(request)));
  }

  @ResponseBody
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<String>> create(
      @Valid @RequestBody MenuUpdateRequestDTO request) {
    service.create(request);
    return ResponseEntity.ok(ApiResponse.success("등록되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/update")
  public ResponseEntity<ApiResponse<String>> update(
      @Valid @RequestBody MenuUpdateRequestDTO request) {
    service.update(request);
    return ResponseEntity.ok(ApiResponse.success("수정되었습니다.", null));
  }

  @ResponseBody
  @PostMapping("/delete")
  public ResponseEntity<ApiResponse<String>> delete(@RequestParam String menuId) {
    service.delete(menuId);
    return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
  }
}
