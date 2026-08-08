package com.scbk.sms.mapper.sms;

import com.scbk.sms.dto.sms.CustomerSearchSearchRequestDTO;
import com.scbk.sms.dto.sms.CustomerSearchUpdateRequestDTO;
import com.scbk.sms.vo.sms.CustomerSearchVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface CustomerSearchMapper {

  int count(CustomerSearchSearchRequestDTO request);

  List<CustomerSearchVO> selectList(CustomerSearchSearchRequestDTO request);

  CustomerSearchVO selectDetail(@Param("customerId") Integer customerId);

  int insert(CustomerSearchUpdateRequestDTO request);

  int update(CustomerSearchUpdateRequestDTO request);

  int delete(@Param("customerId") Integer customerId);
}
