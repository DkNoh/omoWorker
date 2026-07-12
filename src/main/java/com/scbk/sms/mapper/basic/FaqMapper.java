package com.scbk.sms.mapper.basic;

import com.scbk.sms.dto.basic.FaqSearchRequestDTO;
import com.scbk.sms.dto.basic.FaqUpdateRequestDTO;
import com.scbk.sms.vo.basic.FaqVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface FaqMapper {

  int count(FaqSearchRequestDTO request);

  List<FaqVO> selectList(FaqSearchRequestDTO request);

  int insert(FaqUpdateRequestDTO request);

  int update(FaqUpdateRequestDTO request);

  int delete(@Param("noticeId") Integer noticeId);
}
