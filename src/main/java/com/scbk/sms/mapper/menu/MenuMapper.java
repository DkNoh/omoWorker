package com.scbk.sms.mapper.menu;

import com.scbk.sms.vo.menu.MenuItemVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuMapper {

  List<MenuItemVO> selectReadableMenus(@Param("roleCodes") List<String> roleCodes);

  /**
   * 활성 메뉴({@code USE_YN = 'Y'}) 중 {@code menuUrl} 과 정확히 일치하는 행 수를 반환한다. 역할 부여({@code
   * TB_MENU_AUTH})와 무관하게 메뉴 자체의 존재만 판단한다.
   */
  int countActiveMenuByUrl(@Param("menuUrl") String menuUrl);
}
