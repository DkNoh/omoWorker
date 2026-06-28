package com.scbk.sms.dto.system;

import com.scbk.sms.vo.system.MenuAuthDetailVO;
import com.scbk.sms.vo.system.MenuManageVO;
import com.scbk.sms.vo.system.MenuRoleVO;
import java.util.List;
import lombok.Data;

/**
 * 메뉴 관리 상세 응답. 좌측 트리에서 메뉴를 선택하면 우측 패널이 이 응답 하나로 그려진다.
 *
 * <ul>
 *   <li>{@code menu} — 선택한 TB_MENU 1행
 *   <li>{@code activeRoles} — USE_YN='Y' 역할 전체(매트릭스 행 기준)
 *   <li>{@code authRows} — 선택 메뉴의 현재 TB_MENU_AUTH 행(ROLE_NM 조인)
 * </ul>
 */
@Data
public class MenuDetailResponseDTO {

  private MenuManageVO menu;
  private List<MenuRoleVO> activeRoles;
  private List<MenuAuthDetailVO> authRows;
}
