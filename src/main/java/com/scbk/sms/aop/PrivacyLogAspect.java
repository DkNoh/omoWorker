package com.scbk.sms.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.annotation.PrivacyLog;
import com.scbk.sms.auth.SmsUserPrincipal;
import com.scbk.sms.service.system.AuditLogService;
import com.scbk.sms.util.MaskingUtil;
import com.scbk.sms.vo.system.PrivacyAuditLogVO;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @PrivacyLog가 붙은 메서드 실행 시 감사 로그를 기록한다.
 *
 * <p>- 행위자는 principal의 (EMP_ID, DEP_ID)로 기록한다. - 파라미터 직렬화 결과는 MaskingUtil.maskPrivacyInText로 마스킹 후
 * 저장한다. (감사 로그에 개인정보 원문을 남기지 않는다) - 감사 로그 저장 실패는 전파되어 본 업무도 실패한다.
 */
@Aspect
@Component
public class PrivacyLogAspect {

  private static final int TARGET_DATA_MAX_LENGTH = 490;
  private static final String SYSTEM_EXECUTOR = "SYSTEM";

  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;

  public PrivacyLogAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
    this.auditLogService = auditLogService;
    this.objectMapper = objectMapper;
  }

  @Around("@annotation(privacyLog)")
  public Object doAuditLog(ProceedingJoinPoint joinPoint, PrivacyLog privacyLog) throws Throwable {
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    PrivacyAuditLogVO logVO = new PrivacyAuditLogVO();
    logVO.setRequestUrl(request.getRequestURI());
    logVO.setActionType(privacyLog.action());
    logVO.setExecutorIp(resolveExecutorIp(request));
    applyExecutor(logVO);
    logVO.setTargetData(serializeArgs(joinPoint.getArgs()));

    auditLogService.saveLog(logVO);

    return joinPoint.proceed();
  }

  private void applyExecutor(PrivacyAuditLogVO logVO) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof SmsUserPrincipal principal) {
      logVO.setEmpId(principal.getEmpId());
      logVO.setDepId(principal.getDepId());
      return;
    }
    logVO.setEmpId(SYSTEM_EXECUTOR);
    logVO.setDepId(SYSTEM_EXECUTOR);
  }

  private String resolveExecutorIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor;
    }
    return request.getRemoteAddr();
  }

  private String serializeArgs(Object[] args) {
    if (args == null || args.length == 0) {
      return "";
    }
    String json;
    try {
      json = objectMapper.writeValueAsString(args);
    } catch (Exception e) {
      // HttpServletResponse처럼 직렬화 불가능한 파라미터가 섞일 수 있다.
      // targetData는 보조 추적 정보이므로 직렬화 실패가 업무를 막지는 않는다.
      return "파라미터 직렬화 실패";
    }
    String masked = MaskingUtil.maskPrivacyInText(json);
    if (masked.length() > TARGET_DATA_MAX_LENGTH) {
      masked = masked.substring(0, TARGET_DATA_MAX_LENGTH) + "...";
    }
    return masked;
  }
}
