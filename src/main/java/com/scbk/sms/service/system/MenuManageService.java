package com.scbk.sms.service.system;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.system.MenuAuthRowDTO;
import com.scbk.sms.dto.system.MenuDetailResponseDTO;
import com.scbk.sms.dto.system.MenuSearchRequestDTO;
import com.scbk.sms.dto.system.MenuUpdateRequestDTO;
import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.system.MenuManageMapper;
import com.scbk.sms.vo.system.MenuAuthDetailVO;
import com.scbk.sms.vo.system.MenuManageVO;
import com.scbk.sms.vo.system.MenuRoleVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 관리(TB_MENU) 비즈니스 규칙.
 *
 * <p>DB 기록 전 강제 규칙:
 *
 * <ul>
 *   <li>MENU_TYPE='G' → MENU_URL must be blank
 *   <li>MENU_TYPE='M' → MENU_URL must be present
 *   <li>MENU_URL uniqueness (only for M-type)
 *   <li>SYSTEM_YN='Y' 행은 삭제 금지, URL 변경 금지
 *   <li>자식 메뉴가 있는 행은 삭제 금지
 *   <li>UPDATE SQL은 MENU_ID를 SET 하지 않는다 (변경 금지). 본 규칙은 mapper XML로 강제한다.
 *   <li>권한 행 교체는 동일 트랜잭션에서 delete+insert로 이뤄진다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MenuManageService {

  private static final String MENU_TYPE_GROUP = "G";
  private static final String YES = "Y";
  private static final String NO = "N";

  private final MenuManageMapper mapper;

  @Transactional(readOnly = true)
  public PageResponseDTO<MenuManageVO> search(MenuSearchRequestDTO request) {
    request.validate();
    int totalCount = mapper.count(request);
    List<MenuManageVO> list = mapper.selectList(request);
    return PageResponseDTO.of(list, request, totalCount);
  }

  /** 트리용 평탄 목록. 클라이언트가 트리로 조립한다. 항상 결정적 정렬을 보장한다. */
  @Transactional(readOnly = true)
  public List<MenuManageVO> getTree() {
    return mapper.selectTree();
  }

  /**
   * 선택 메뉴 상세. 메뉴 1건 + 활성 역할 전체 + 현재 권한 행을 한 번에 반환한다.
   *
   * @throws CustomException {@link ErrorCode#DATA_NOT_FOUND} 메뉴가 없으면
   */
  @Transactional(readOnly = true)
  public MenuDetailResponseDTO getDetail(String menuId) {
    MenuManageVO menu = mapper.selectByMenuId(menuId);
    if (menu == null) {
      throw new CustomException(ErrorCode.DATA_NOT_FOUND);
    }
    List<MenuRoleVO> roles = mapper.selectActiveRoles();
    List<MenuAuthDetailVO> authRows = mapper.selectMenuAuthDetails(menuId);

    MenuDetailResponseDTO response = new MenuDetailResponseDTO();
    response.setMenu(menu);
    response.setActiveRoles(roles == null ? Collections.emptyList() : roles);
    response.setAuthRows(authRows == null ? Collections.emptyList() : authRows);
    return response;
  }

  @Transactional
  public void create(MenuUpdateRequestDTO request) {
    validateMenuTypeUrlRule(request);
    if (mapper.selectByMenuId(request.getMenuId()) != null) {
      throw new CustomException(ErrorCode.DUPLICATE_MENU_ID);
    }
    if (hasUrl(request) && mapper.countByUrlExceptMenuId(request.getMenuUrl(), request.getMenuId()) > 0) {
      throw new CustomException(ErrorCode.DUPLICATE_MENU_URL);
    }
    mapper.insert(request);
  }

  /**
   * 메뉴 수정. {@code authRows}가 {@code null}이 아니면 메뉴 수정 직후 동일 트랜잭션에서 권한 행을
   * 전부 지우고 다시 채운다(replace). {@code null}이면 권한 행은 그대로 둔다.
   */
  @Transactional
  public void update(MenuUpdateRequestDTO request) {
    MenuManageVO existing = mapper.selectByMenuId(request.getMenuId());
    if (existing == null) {
      throw new CustomException(ErrorCode.UPDATE_CONFLICT);
    }
    if (YES.equals(existing.getSystemYn())
        && !Objects.equals(existing.getMenuUrl(), request.getMenuUrl())) {
      throw new CustomException(ErrorCode.SYSTEM_MENU_PROTECTED);
    }
    validateMenuTypeUrlRule(request);
    if (hasUrl(request) && mapper.countByUrlExceptMenuId(request.getMenuUrl(), request.getMenuId()) > 0) {
      throw new CustomException(ErrorCode.DUPLICATE_MENU_URL);
    }
    int updated = mapper.update(request);
    if (updated == 0) {
      throw new CustomException(ErrorCode.UPDATE_CONFLICT);
    }
    replaceAuthRowsIfPresent(request);
  }

  /**
   * 메뉴 삭제. 자식이 있으면 삭제를 거부하고, 시스템 메뉴도 거부한다. 허용 시 TB_MENU_AUTH 행을 먼저
   * 지우고(FK 참조가 살아있어도 메뉴 행 삭제에 방해가 되지 않도록) TB_MENU 행을 지운다.
   */
  @Transactional
  public void delete(String menuId) {
    MenuManageVO existing = mapper.selectByMenuId(menuId);
    if (existing == null) {
      throw new CustomException(ErrorCode.DELETE_CONFLICT);
    }
    if (YES.equals(existing.getSystemYn())) {
      throw new CustomException(ErrorCode.SYSTEM_MENU_PROTECTED);
    }
    if (mapper.countChildren(menuId) > 0) {
      throw new CustomException(ErrorCode.MENU_HAS_CHILDREN);
    }
    mapper.deleteMenuAuthByMenuId(menuId);
    int deleted = mapper.delete(menuId);
    if (deleted == 0) {
      throw new CustomException(ErrorCode.DELETE_CONFLICT);
    }
  }

  /** G는 URL 없음, M은 URL 필수. DB CHECK 제약과 동일한 규칙을 기록 전에 사전 검증한다. */
  private void validateMenuTypeUrlRule(MenuUpdateRequestDTO request) {
    boolean hasUrl = hasUrl(request);
    if (MENU_TYPE_GROUP.equals(request.getMenuType()) && hasUrl) {
      throw new CustomException(ErrorCode.MENU_TYPE_URL_INVALID);
    }
    if (!MENU_TYPE_GROUP.equals(request.getMenuType()) && !hasUrl) {
      throw new CustomException(ErrorCode.MENU_TYPE_URL_INVALID);
    }
  }

  private boolean hasUrl(MenuUpdateRequestDTO request) {
    return request.getMenuUrl() != null && !request.getMenuUrl().isBlank();
  }

  /**
   * authRows가 null이면 아무 것도 하지 않는다(메뉴 정보만 수정). not-null이면(빈 리스트 포함) 해당
   * 메뉴의 기존 권한 행을 전부 지우고 이 목록으로 채운다. 자식 DTO의 menuId는 부모 menuId로 덮어쓴다.
   * CAN_* 값이 null/blank면 'N'으로 normalize 한다.
   */
  private void replaceAuthRowsIfPresent(MenuUpdateRequestDTO request) {
    List<MenuAuthRowDTO> rows = request.getAuthRows();
    if (rows == null) {
      return;
    }
    String menuId = request.getMenuId();
    mapper.deleteMenuAuthByMenuId(menuId);
    if (rows.isEmpty()) {
      return;
    }
    List<MenuAuthRowDTO> normalized = new ArrayList<>(rows.size());
    for (MenuAuthRowDTO row : rows) {
      row.setMenuId(menuId);
      row.setCanRead(normalizeFlag(row.getCanRead()));
      row.setCanCreate(normalizeFlag(row.getCanCreate()));
      row.setCanUpdate(normalizeFlag(row.getCanUpdate()));
      row.setCanDelete(normalizeFlag(row.getCanDelete()));
      row.setCanApprove(normalizeFlag(row.getCanApprove()));
      row.setCanCancel(normalizeFlag(row.getCanCancel()));
      row.setCanDownload(normalizeFlag(row.getCanDownload()));
      row.setCanMaskView(normalizeFlag(row.getCanMaskView()));
      normalized.add(row);
    }
    for (MenuAuthRowDTO row : normalized) {
      mapper.insertMenuAuth(row);
    }
  }

  private String normalizeFlag(String value) {
    return YES.equals(value) ? YES : NO;
  }
}
