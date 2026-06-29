package com.scbk.sms.service.sms;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.mapper.sms.SmsHistoryMapper;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. Scaffold 생성 코드. 업무 로직은 이 파일에 직접 추가한다. */
@Service
@RequiredArgsConstructor
public class SmsHistoryService {

  private final SmsHistoryMapper mapper;

  @Transactional(readOnly = true)
  public PageResponseDTO<SmsHistoryVO> search(SmsHistorySearchRequestDTO request) {
    request.validate();
    int totalCount = mapper.count(request);
    List<SmsHistoryVO> list = mapper.selectList(request);
    return PageResponseDTO.of(list, request, totalCount);
  }
}
