package com.scbk.sms.mapper.system;

import com.scbk.sms.vo.common.CommonCodeVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonCodeMapper {

  List<CommonCodeVO> selectDepartments(@Param("keyword") String keyword);

  List<CommonCodeVO> selectRoles(@Param("keyword") String keyword);
}
