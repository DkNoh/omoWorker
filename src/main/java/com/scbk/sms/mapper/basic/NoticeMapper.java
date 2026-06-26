package com.scbk.sms.mapper.basic;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.basic.NoticeUpdateRequestDTO;
import com.scbk.sms.vo.basic.NoticeVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeMapper {

  int count(NoticeSearchRequestDTO request);

  List<NoticeVO> selectList(NoticeSearchRequestDTO request);

  int insert(NoticeUpdateRequestDTO request);

  int update(NoticeUpdateRequestDTO request);

  int delete(@Param("noticeId") Integer noticeId);
}
