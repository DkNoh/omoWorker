package com.scbk.sms.mapper.employee;

import com.scbk.sms.vo.employee.EmployeeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeQueryMapper {

  EmployeeVO selectById(@Param("empId") String empId, @Param("depId") String depId);
}
