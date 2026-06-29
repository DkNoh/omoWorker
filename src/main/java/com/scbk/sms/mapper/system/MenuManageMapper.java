package com.scbk.sms.mapper.system;

import com.scbk.sms.dto.system.MenuAuthRowDTO;
import com.scbk.sms.dto.system.MenuSearchRequestDTO;
import com.scbk.sms.dto.system.MenuUpdateRequestDTO;
import com.scbk.sms.vo.system.MenuAuthDetailVO;
import com.scbk.sms.vo.system.MenuManageVO;
import com.scbk.sms.vo.system.MenuRoleVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuManageMapper {

  int count(MenuSearchRequestDTO request);

  List<MenuManageVO> selectList(MenuSearchRequestDTO request);

  /**
   * 메뉴 관리 트리용 평탄 목록. 클라이언트가 트리로 조립하며 페이지네이션 없이 전체를 반환한다. MENU_LEVEL/PARENT_MENU_ID/SORT_ORD/MENU_ID
   * 결정적 정렬을 보장한다.
   */
  List<MenuManageVO> selectTree();

  /** 단건 조회. 시스템 메뉴 보호 판정과 삭제/수정 대상 존재 여부 확인에 사용한다. */
  MenuManageVO selectByMenuId(@Param("menuId") String menuId);

  /** URL 중복 검사(본인 제외). MENU_TYPE='M'만 URL을 가지므로 menuUrl이 비어있으면 호출하지 않는다. */
  int countByUrlExceptMenuId(@Param("menuUrl") String menuUrl, @Param("menuId") String menuId);

  /** 자식 메뉴 존재 여부. 삭제 시 1건 이상이면 부모 삭제를 거부한다. */
  int countChildren(@Param("parentMenuId") String parentMenuId);

  /** USE_YN='Y' 역할 전체. 매트릭스의 행 기준이며 SORT_ORD/ROLE_CD 정렬을 보장한다. */
  List<MenuRoleVO> selectActiveRoles();

  /** 메뉴 1건의 권한 행을 ROLE_NM과 함께 조회. 존재하지 않으면 빈 목록. */
  List<MenuAuthDetailVO> selectMenuAuthDetails(@Param("menuId") String menuId);

  int insert(MenuUpdateRequestDTO request);

  /** MENU_ID는 WHERE 에만 쓴다. SET 에 포함하지 않는다 (MENU_ID 변경 금지 규약). */
  int update(MenuUpdateRequestDTO request);

  /** TB_MENU_AUTH 행을 메뉴 단위로 일괄 삭제. TB_MENU 삭제 전/권한 교체 시 호출한다. */
  int deleteMenuAuthByMenuId(@Param("menuId") String menuId);

  /** TB_MENU_AUTH 1행 등록. Service가 menuId/roleCd/CAN_* 값을 채운 DTO를 넘긴다. */
  int insertMenuAuth(MenuAuthRowDTO row);

  int delete(@Param("menuId") String menuId);
}
