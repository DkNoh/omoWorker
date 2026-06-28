package com.scbk.sms.mapper.sms;

import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface SmsHistoryMapper {

    int count(SmsHistorySearchRequestDTO request);

    List<SmsHistoryVO> selectList(SmsHistorySearchRequestDTO request);
}
