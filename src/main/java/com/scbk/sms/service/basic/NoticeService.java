package com.scbk.sms.service.basic;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.basic.NoticeUpdateRequestDTO;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.basic.NoticeMapper;
import com.scbk.sms.vo.basic.NoticeVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeService {

  private final NoticeMapper mapper;

  @Transactional(readOnly = true)
  public PageResponseDTO<NoticeVO> search(NoticeSearchRequestDTO request) {
    request.validate();
    int totalCount = mapper.count(request);
    List<NoticeVO> list = mapper.selectList(request);
    return PageResponseDTO.of(list, request, totalCount);
  }

  @Transactional
  public void create(NoticeUpdateRequestDTO request) {
    mapper.insert(request);
  }

  @Transactional
  public void update(NoticeUpdateRequestDTO request) {
    int updated = mapper.update(request);
    if (updated == 0) {
      // 다른 사용자가 먼저 수정했거나(낙관적 잠금) 대상이 없다
      throw new CustomException(ErrorCode.UPDATE_CONFLICT);
    }
  }

  @Transactional
  public void delete(Integer noticeId) {
    int deleted = mapper.delete(noticeId);
    if (deleted == 0) {
      // 다른 사용자가 먼저 삭제했거나 대상이 없다
      throw new CustomException(ErrorCode.DELETE_CONFLICT);
    }
  }
}
