package com.scbk.sms.service.system;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.scbk.sms.mapper.system.PrivacyAuditLogMapper;
import com.scbk.sms.vo.system.PrivacyAuditLogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

  @Mock private PrivacyAuditLogMapper privacyAuditLogMapper;

  private AuditLogService auditLogService;

  @BeforeEach
  void setUp() {
    auditLogService = new AuditLogService(privacyAuditLogMapper);
  }

  @Test
  void 감사_로그를_저장한다() {
    // given
    PrivacyAuditLogVO logVO = new PrivacyAuditLogVO();

    // when
    auditLogService.saveLog(logVO);

    // then
    then(privacyAuditLogMapper).should().insertAuditLog(logVO);
  }

  @Test
  void 감사_로그_저장_실패는_삼키지_않고_전파한다() {
    // given : v2는 예외를 삼켰지만 v3는 기록되지 않은 접근을 허용하지 않는다
    given(privacyAuditLogMapper.insertAuditLog(any()))
        .willThrow(new RuntimeException("DB insert 실패"));

    // when / then
    assertThatThrownBy(() -> auditLogService.saveLog(new PrivacyAuditLogVO()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB insert 실패");
  }
}
