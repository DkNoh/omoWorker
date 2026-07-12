package com.scbk.sms.mapper.basic;

import com.scbk.sms.dto.basic.DepartmentSearchRequestDTO;
import com.scbk.sms.vo.basic.DepartmentVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface DepartmentMapper {

  int count(DepartmentSearchRequestDTO request);

  List<DepartmentVO> selectList(DepartmentSearchRequestDTO request);
}
