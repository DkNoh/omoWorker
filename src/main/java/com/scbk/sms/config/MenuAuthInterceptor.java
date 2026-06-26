package com.scbk.sms.config;

import com.scbk.sms.auth.SmsUserPrincipal;
import com.scbk.sms.service.menu.MenuAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 좌측 메뉴 표시와 별개로 모든 URL/API 요청을 TB_MENU_AUTH 기준으로 다시 검증한다. 권한이 없으면 CustomException(ACCESS_DENIED)이
 * GlobalExceptionHandler에서 403으로 변환된다.
 */
@Component
public class MenuAuthInterceptor implements HandlerInterceptor {

  private final MenuAuthService menuAuthService;

  public MenuAuthInterceptor(MenuAuthService menuAuthService) {
    this.menuAuthService = menuAuthService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof SmsUserPrincipal principal)) {
      // 미인증 요청 차단은 Spring Security 1차 인가가 담당한다.
      return true;
    }

    String path = request.getRequestURI().substring(request.getContextPath().length());
    menuAuthService.checkAccess(path, principal.getRoleCodes());
    return true;
  }
}
