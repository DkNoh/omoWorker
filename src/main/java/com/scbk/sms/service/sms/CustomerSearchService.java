package com.scbk.sms.service.sms;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.CustomerSearchSearchRequestDTO;
import com.scbk.sms.dto.sms.CustomerSearchUpdateRequestDTO;
import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.sms.CustomerSearchMapper;
import com.scbk.sms.vo.sms.CustomerSearchVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. Scaffold 생성 코드. 업무 로직은 이 파일에 직접 추가한다. */
@Service
@RequiredArgsConstructor
public class CustomerSearchService {

  private final CustomerSearchMapper mapper;

  @Transactional(readOnly = true)
  public PageResponseDTO<CustomerSearchVO> search(CustomerSearchSearchRequestDTO request) {
    request.validate();
    int totalCount = mapper.count(request);
    List<CustomerSearchVO> list = mapper.selectList(request);
    return PageResponseDTO.of(list, request, totalCount);
  }

  @Transactional
  public void create(CustomerSearchUpdateRequestDTO request) {
    // TODO: 등록 전 업무 규칙 검증(중복 체크, 필수값 보정 등)을 여기에 추가한다.
    mapper.insert(request);
  }

  @Transactional
  public void update(CustomerSearchUpdateRequestDTO request) {
    // TODO: 수정 전 업무 규칙 검증(상태 전이, 권한 확인 등)을 여기에 추가한다.
    int updated = mapper.update(request);
    if (updated == 0) {
      // 다른 사용자가 먼저 수정했거나(낙관적 잠금) 대상이 없다
      throw new CustomException(ErrorCode.UPDATE_CONFLICT);
    }
  }

  @Transactional
  public void delete(Integer customerId) {
    int deleted = mapper.delete(customerId);
    if (deleted == 0) {
      // 다른 사용자가 먼저 삭제했거나 대상이 없다
      throw new CustomException(ErrorCode.DELETE_CONFLICT);
    }
  }

  @Transactional(readOnly = true)
  public CustomerSearchVO getUnmaskedDetail(Integer customerId) {
    CustomerSearchVO vo = mapper.selectDetail(customerId);
    if (vo == null) {
      throw new CustomException(ErrorCode.DATA_NOT_FOUND);
    }
    return vo;
  }
}
