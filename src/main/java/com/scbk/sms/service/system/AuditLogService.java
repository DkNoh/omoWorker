package com.scbk.sms.service.system;

import com.scbk.sms.mapper.system.PrivacyAuditLogMapper;
import com.scbk.sms.vo.system.PrivacyAuditLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인정보 조회/반출/변경 행위의 감사 로그를 적재한다. 주로 @PrivacyLog AOP에서 호출된다.
 *
 * <p>감사 로그 저장 실패는 삼키지 않고 전파한다. 기록되지 않은 개인정보 접근을 허용하지 않기 위한 규칙이다. (v2와 다른 v3 확정 동작)
 */
@Service
public class AuditLogService {

  private final PrivacyAuditLogMapper privacyAuditLogMapper;

  public AuditLogService(PrivacyAuditLogMapper privacyAuditLogMapper) {
    this.privacyAuditLogMapper = privacyAuditLogMapper;
  }

  @Transactional
  public void saveLog(PrivacyAuditLogVO logVO) {
    privacyAuditLogMapper.insertAuditLog(logVO);
  }
}
