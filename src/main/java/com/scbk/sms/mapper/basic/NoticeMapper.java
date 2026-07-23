package com.scbk.sms.mapper.basic;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.basic.NoticeUpdateRequestDTO;
import com.scbk.sms.vo.basic.NoticeVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 개발자 소유 수동 참조: scaffold 복사 후 커스터마이즈한 window.open CRUD 예제. 재생성하지 않고 직접 수정한다. */
@Mapper
public interface NoticeMapper {

  int count(NoticeSearchRequestDTO request);

  List<NoticeVO> selectList(NoticeSearchRequestDTO request);

  NoticeVO selectDetail(@Param("noticeId") Integer noticeId);

  int insert(NoticeUpdateRequestDTO request);

  int update(NoticeUpdateRequestDTO request);

  int delete(@Param("noticeId") Integer noticeId);
}
