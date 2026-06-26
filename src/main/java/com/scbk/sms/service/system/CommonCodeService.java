package com.scbk.sms.service.system;

import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.system.CommonCodeMapper;
import com.scbk.sms.vo.common.CommonCodeVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 화면 콤보/자동완성용 공통코드 조회.
 *
 * <p>새 코드 타입이 필요하면 이 클래스와 CommonCodeMapper에 case를 추가하고, screen-convention.md의 지원 타입 표를 함께 갱신한다.
 * 지원하지 않는 타입은 빈 목록으로 숨기지 않고 오류로 보고한다.
 */
@Service
public class CommonCodeService {

  private final CommonCodeMapper commonCodeMapper;

  public CommonCodeService(CommonCodeMapper commonCodeMapper) {
    this.commonCodeMapper = commonCodeMapper;
  }

  @Transactional(readOnly = true)
  public List<CommonCodeVO> getCommonCodes(String codeType, String keyword) {
    if ("dept".equalsIgnoreCase(codeType)) {
      return commonCodeMapper.selectDepartments(keyword);
    }
    if ("role".equalsIgnoreCase(codeType)) {
      return commonCodeMapper.selectRoles(keyword);
    }
    throw new CustomException(ErrorCode.UNSUPPORTED_CODE_TYPE);
  }
}
