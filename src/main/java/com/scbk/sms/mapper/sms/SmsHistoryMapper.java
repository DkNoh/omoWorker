package com.scbk.sms.mapper.sms;

import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.dto.sms.SmsHistoryUpdateRequestDTO;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsHistoryMapper {

  int count(SmsHistorySearchRequestDTO request);

  List<SmsHistoryVO> selectList(SmsHistorySearchRequestDTO request);

  /** 엑셀 다운로드용(페이징 제외, FETCH NEXT 50000 상한). 동일 검색 조건을 사용한다. */
  List<SmsHistoryVO> selectListForExcel(SmsHistorySearchRequestDTO request);

  /** PK 기준 단건 원문 조회. CAN_MASK_VIEW 전용(/unmask) — 마스킹하지 않은 원문을 반환한다. */
  SmsHistoryVO selectDetail(@Param("smsHistoryId") Integer smsHistoryId);

  int insert(SmsHistoryUpdateRequestDTO request);

  int update(SmsHistoryUpdateRequestDTO request);

  int delete(@Param("smsHistoryId") Integer smsHistoryId);
}
