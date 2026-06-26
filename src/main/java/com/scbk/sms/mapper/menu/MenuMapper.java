package com.scbk.sms.mapper.menu;

import com.scbk.sms.vo.menu.MenuItemVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuMapper {

  List<MenuItemVO> selectReadableMenus(@Param("roleCodes") List<String> roleCodes);
}
