package com.scbk.sms.mapper.basic;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.basic.NoticeUpdateRequestDTO;
import com.scbk.sms.vo.basic.NoticeVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface NoticeMapper {

    int count(NoticeSearchRequestDTO request);

    List<NoticeVO> selectList(NoticeSearchRequestDTO request);

    int insert(NoticeUpdateRequestDTO request);

    int update(NoticeUpdateRequestDTO request);

    int delete(@Param("noticeId") Integer noticeId);
}
