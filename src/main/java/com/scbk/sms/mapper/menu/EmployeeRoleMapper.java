package com.scbk.sms.mapper.menu;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeRoleMapper {

  List<String> selectRoleCodes(@Param("empId") String empId, @Param("depId") String depId);
}
