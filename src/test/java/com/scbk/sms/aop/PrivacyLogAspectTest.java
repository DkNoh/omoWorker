package com.scbk.sms.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.annotation.PrivacyLog;
import com.scbk.sms.auth.SmsUserPrincipal;
import com.scbk.sms.service.system.AuditLogService;
import com.scbk.sms.vo.system.PrivacyAuditLogVO;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class PrivacyLogAspectTest {

  @Mock private AuditLogService auditLogService;

  @Mock private ProceedingJoinPoint joinPoint;

  private PrivacyLogAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new PrivacyLogAspect(auditLogService, new ObjectMapper());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/ssn-search/data");
    request.setRemoteAddr("10.0.0.7");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    SmsUserPrincipal principal = Mockito.mock(SmsUserPrincipal.class);
    given(principal.getEmpId()).willReturn("admin");
    given(principal.getDepId()).willReturn("D001");
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(principal, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void 행위자를_EMP_ID와_DEP_ID로_기록하고_파라미터를_마스킹한다() throws Throwable {
    // given
    given(joinPoint.getArgs()).willReturn(new Object[] {Map.of("phone", "01012345678")});
    given(joinPoint.proceed()).willReturn("result");
    PrivacyLog privacyLog = privacyLogOf("주민번호 조회");

    // when
    Object result = aspect.doAuditLog(joinPoint, privacyLog);

    // then
    ArgumentCaptor<PrivacyAuditLogVO> captor = ArgumentCaptor.forClass(PrivacyAuditLogVO.class);
    then(auditLogService).should().saveLog(captor.capture());
    PrivacyAuditLogVO saved = captor.getValue();

    assertThat(saved.getEmpId()).isEqualTo("admin");
    assertThat(saved.getDepId()).isEqualTo("D001");
    assertThat(saved.getRequestUrl()).isEqualTo("/sms/ssn-search/data");
    assertThat(saved.getActionType()).isEqualTo("주민번호 조회");
    assertThat(saved.getExecutorIp()).isEqualTo("10.0.0.7");
    assertThat(saved.getTargetData()).contains("010-****-5678");
    assertThat(saved.getTargetData()).doesNotContain("01012345678");
    assertThat(result).isEqualTo("result");
  }

  @Test
  void 감사_로그_저장_실패면_업무_로직을_실행하지_않는다() throws Throwable {
    // given
    given(joinPoint.getArgs()).willReturn(new Object[0]);
    Mockito.doThrow(new RuntimeException("감사 로그 저장 실패")).when(auditLogService).saveLog(any());

    // when
    Throwable thrown =
        org.assertj.core.api.Assertions.catchThrowable(
            () -> aspect.doAuditLog(joinPoint, privacyLogOf("조회")));

    // then
    assertThat(thrown).isInstanceOf(RuntimeException.class);
    then(joinPoint).should(Mockito.never()).proceed();
  }

  private PrivacyLog privacyLogOf(String action) {
    return new PrivacyLog() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return PrivacyLog.class;
      }

      @Override
      public String action() {
        return action;
      }
    };
  }
}
