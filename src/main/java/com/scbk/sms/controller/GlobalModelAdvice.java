package com.scbk.sms.controller;

import com.scbk.sms.auth.SmsUserPrincipal;
import com.scbk.sms.service.menu.MenuAuthService;
import com.scbk.sms.service.menu.MenuCacheRevision;
import com.scbk.sms.service.menu.MenuSource;
import com.scbk.sms.service.menu.PageAuth;
import com.scbk.sms.vo.menu.MenuItemVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * defaultLayout(sidebar/header)이 모든 화면에서 사용하는 공통 모델을 채운다. 메뉴 렌더링은 Controller가 넘긴 메뉴 tree만 사용한다는 규칙의
 * 단일 적용 지점이다.
 */
@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

  private static final String MENU_TREE_SESSION_KEY = "menuTree";
  private static final String PAGE_AUTH_CACHE_SESSION_KEY = "pageAuthCache";
  private static final String MENU_CACHE_REVISION_SESSION_KEY = "menuCacheRevision";

  private final MenuSource menuSource;
  private final MenuAuthService menuAuthService;
  private final MenuCacheRevision menuCacheRevision;
  private final Environment environment;

  public GlobalModelAdvice(
      MenuSource menuSource,
      MenuAuthService menuAuthService,
      MenuCacheRevision menuCacheRevision,
      Environment environment) {
    this.menuSource = menuSource;
    this.menuAuthService = menuAuthService;
    this.menuCacheRevision = menuCacheRevision;
    this.environment = environment;
  }

  @ModelAttribute
  public void addLayoutAttributes(
      @AuthenticationPrincipal SmsUserPrincipal principal,
      Model model,
      HttpServletRequest request) {
    if (principal == null) {
      model.addAttribute("pageAuth", PageAuth.none());
      return;
    }
    HttpSession session = request.getSession(false);
    refreshSessionCachesIfStale(session);
    model.addAttribute("user", principal);
    model.addAttribute("menus", getCachedMenuTree(principal, session));
    model.addAttribute("pageAuth", resolvePageAuth(principal, request, session));
    model.addAttribute("clientIp", resolveClientIp(request));
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    String ip =
        (forwardedFor != null && !forwardedFor.isBlank()) ? forwardedFor : request.getRemoteAddr();
    return normalizeLoopback(ip);
  }

  /**
   * localhost 접속 시 OS/Tomcat이 IPv6 루프백("0:0:0:0:0:0:0:1" 또는 "::1")으로 remoteAddr을 돌려주는 경우가 있어, 화면
   * 표시용으로 IPv4 루프백으로 정규화한다.
   */
  private String normalizeLoopback(String ip) {
    if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
      return "127.0.0.1";
    }
    return ip;
  }

  @SuppressWarnings("unchecked")
  private List<MenuItemVO> getCachedMenuTree(SmsUserPrincipal principal, HttpSession session) {
    if (session != null) {
      Object cached = session.getAttribute(MENU_TREE_SESSION_KEY);
      if (cached instanceof List<?> list) {
        return (List<MenuItemVO>) list;
      }
    }
    List<MenuItemVO> tree = menuSource.getMenuTree(principal.getRoleCodes());
    if (session != null) {
      session.setAttribute(MENU_TREE_SESSION_KEY, tree);
    }
    return tree;
  }

  private PageAuth resolvePageAuth(
      SmsUserPrincipal principal, HttpServletRequest request, HttpSession session) {
    if (isLocalProfile()) {
      return PageAuth.all();
    }
    String path =
        normalizePath(request.getRequestURI().substring(request.getContextPath().length()));
    if (session != null) {
      PageAuth cached = getPageAuthCache(session).get(path);
      if (cached != null) {
        return cached;
      }
    }
    String authPath = menuAuthService.resolveBaseMenuPath(path, principal.getRoleCodes());
    PageAuth auth = PageAuth.from(menuSource.getPermissions(authPath, principal.getRoleCodes()));
    if (session != null) {
      getPageAuthCache(session).put(path, auth);
    }
    return auth;
  }

  @SuppressWarnings("unchecked")
  private Map<String, PageAuth> getPageAuthCache(HttpSession session) {
    Object existing = session.getAttribute(PAGE_AUTH_CACHE_SESSION_KEY);
    if (existing instanceof Map<?, ?> map) {
      return (Map<String, PageAuth>) map;
    }
    Map<String, PageAuth> cache = new HashMap<>();
    session.setAttribute(PAGE_AUTH_CACHE_SESSION_KEY, cache);
    return cache;
  }

  private void refreshSessionCachesIfStale(HttpSession session) {
    if (session == null) {
      return;
    }
    long currentRevision = menuCacheRevision.current();
    Object sessionRevision = session.getAttribute(MENU_CACHE_REVISION_SESSION_KEY);
    if (sessionRevision instanceof Long revision && revision == currentRevision) {
      return;
    }
    session.removeAttribute(MENU_TREE_SESSION_KEY);
    session.removeAttribute(PAGE_AUTH_CACHE_SESSION_KEY);
    session.setAttribute(MENU_CACHE_REVISION_SESSION_KEY, currentRevision);
  }

  private boolean isLocalProfile() {
    return Arrays.stream(environment.getActiveProfiles()).anyMatch("local"::equals);
  }

  private String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }
    if (path.length() > 1 && path.endsWith("/")) {
      return path.substring(0, path.length() - 1);
    }
    return path;
  }
}
