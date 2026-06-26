package com.scbk.sms.mapper.auth;

import com.scbk.sms.vo.auth.LoginEmployeeVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginEmployeeMapper {

  List<LoginEmployeeVO> selectActiveEmployeesByEmpId(@Param("empId") String empId);
}
