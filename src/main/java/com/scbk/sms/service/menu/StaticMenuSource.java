package com.scbk.sms.service.menu;

import com.scbk.sms.vo.menu.MenuItemVO;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * static source 전용. 메뉴 테이블 생성 전 local 화면 검증 용도이므로 baseline에 존재하는 메뉴 URL에는 모든 권한을 부여한다. 최종 권한 검증은
 * 반드시 db source에서 확인한다.
 */
@Service
@ConditionalOnProperty(name = "sms.menu.source", havingValue = "static")
public class StaticMenuSource implements MenuSource {

  private static final String MENU_TYPE_GROUP = "G";
  private static final String MENU_TYPE_MENU = "M";
  private static final String YES = "Y";
  private static final String NO = "N";

  private final MenuTreeBuilder menuTreeBuilder;

  public StaticMenuSource(MenuTreeBuilder menuTreeBuilder) {
    this.menuTreeBuilder = menuTreeBuilder;
  }

  @Override
  public List<MenuItemVO> getMenuTree(List<String> roleCodes) {
    return menuTreeBuilder.build(createFlatMenus());
  }

  @Override
  public Set<MenuPermission> getPermissions(String menuUrl, List<String> roleCodes) {
    Set<String> menuUrls = new HashSet<>();
    collectUrls(getMenuTree(roleCodes), menuUrls);
    if (menuUrls.contains(menuUrl)) {
      return EnumSet.allOf(MenuPermission.class);
    }
    return EnumSet.noneOf(MenuPermission.class);
  }

  private void collectUrls(List<MenuItemVO> menus, Set<String> menuUrls) {
    for (MenuItemVO menu : menus) {
      if (menu.getMenuUrl() != null) {
        menuUrls.add(menu.getMenuUrl());
      }
      collectUrls(menu.getChildren(), menuUrls);
    }
  }

  private List<MenuItemVO> createFlatMenus() {
    List<MenuItemVO> menus = new ArrayList<>();
    menus.add(menu("G_BASIC", null, "기본메뉴", null, 1, 10, MENU_TYPE_GROUP));
    menus.add(menu("BASIC_INTRO", "G_BASIC", "SMS관리시스템 안내", "/basic/intro", 2, 10, MENU_TYPE_MENU));
    menus.add(menu("BASIC_NOTICE", "G_BASIC", "공지사항", "/basic/notice", 2, 20, MENU_TYPE_MENU));
    menus.add(menu("BASIC_MESSAGE", "G_BASIC", "메시지조회", "/basic/message", 2, 30, MENU_TYPE_MENU));
    menus.add(
        menu("BASIC_USER_SEARCH", "G_BASIC", "사용자조회", "/basic/user-search", 2, 40, MENU_TYPE_MENU));
    menus.add(menu("BASIC_MFA", "G_BASIC", "MFA사용자관리", "/basic/mfa", 2, 50, MENU_TYPE_MENU));

    menus.add(menu("G_SMS_SEARCH", null, "SMS발송조회", null, 1, 20, MENU_TYPE_GROUP));
    menus.add(menu("SMS_HISTORY", "G_SMS_SEARCH", "발송이력조회", "/sms/history", 2, 10, MENU_TYPE_MENU));
    menus.add(
        menu(
            "SMS_CUSTOMER_SEARCH",
            "G_SMS_SEARCH",
            "고객별 조회",
            "/sms/customer-search",
            2,
            20,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "SMS_SSN_SEARCH", "G_SMS_SEARCH", "주민번호 조회", "/sms/ssn-search", 2, 30, MENU_TYPE_MENU));

    menus.add(menu("G_CAMPAIGN", null, "캠페인SMS", null, 1, 30, MENU_TYPE_GROUP));
    menus.add(
        menu(
            "CAMPAIGN_TARGET",
            "G_CAMPAIGN",
            "발송대상관리",
            "/campaign/target-manage",
            2,
            10,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_TARGET_APPROVAL",
            "G_CAMPAIGN",
            "발송대상승인",
            "/approval",
            2,
            20,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_SMS_REGISTER",
            "G_CAMPAIGN",
            "SMS등록",
            "/campaign/sms/register",
            2,
            30,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_LMS_REGISTER",
            "G_CAMPAIGN",
            "LMS등록",
            "/campaign/lms/register",
            2,
            40,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_ALIMTALK_REGISTER",
            "G_CAMPAIGN",
            "알림톡등록",
            "/campaign/alimtalk/register",
            2,
            50,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_SMS_APPROVE",
            "G_CAMPAIGN",
            "SMS승인",
            "/campaign/sms/approve",
            2,
            60,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_LMS_APPROVE",
            "G_CAMPAIGN",
            "LMS승인",
            "/campaign/lms/approve",
            2,
            70,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_ALIMTALK_APPROVE",
            "G_CAMPAIGN",
            "알림톡승인",
            "/campaign/alimtalk/approve",
            2,
            80,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_SMS_HISTORY",
            "G_CAMPAIGN",
            "발송이력조회",
            "/sms/campaign",
            2,
            90,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_LMS_HISTORY",
            "G_CAMPAIGN",
            "LMS발송이력조회",
            "/sms/campaign-lms",
            2,
            100,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "CAMPAIGN_ALIMTALK_HISTORY",
            "G_CAMPAIGN",
            "알림톡 발송이력조회",
            "/sms/campaign-alimtalk",
            2,
            110,
            MENU_TYPE_MENU));

    menus.add(menu("G_SYSTEM", null, "시스템관리", null, 1, 40, MENU_TYPE_GROUP));
    menus.add(
        menu(
            "SYSTEM_MENU_TREE", "G_SYSTEM", "메뉴 트리 확인", "/system/menu-tree", 2, 5, MENU_TYPE_MENU));
    menus.add(menu("SYSTEM_DEP", "G_SYSTEM", "부서관리", "/system/dept-manage", 2, 10, MENU_TYPE_MENU));
    menus.add(
        menu("SYSTEM_MESSAGE", "G_SYSTEM", "메시지 관리", "/system/message", 2, 20, MENU_TYPE_MENU));
    menus.add(
        menu(
            "SYSTEM_KAKAO_TEMPLATE",
            "G_SYSTEM",
            "카카오템플릿관리",
            "/system/kakao-template",
            2,
            30,
            MENU_TYPE_MENU));
    menus.add(
        menu(
            "SYSTEM_AD_MESSAGE",
            "G_SYSTEM",
            "광고성 메시지관리",
            "/system/ad-message",
            2,
            40,
            MENU_TYPE_MENU));
    menus.add(
        menu("SYSTEM_HOURLY_STATS", "G_SYSTEM", "시간대별조회", "/sms/dept-stat", 2, 50, MENU_TYPE_MENU));

    menus.add(menu("G_ACCOUNT", null, "시스템관리 계정관리", null, 1, 50, MENU_TYPE_GROUP));
    menus.add(
        menu("ACCOUNT_USER", "G_ACCOUNT", "사용자관리", "/account/user-manage", 2, 10, MENU_TYPE_MENU));

    menus.add(menu("G_STATISTICS", null, "통계 관리", null, 1, 60, MENU_TYPE_GROUP));
    menus.add(
        menu(
            "STAT_MARKETING_OPTOUT",
            "G_STATISTICS",
            "마케팅 철회 통계",
            "/statistics/marketing-optout",
            2,
            10,
            MENU_TYPE_MENU));
    return menus;
  }

  private MenuItemVO menu(
      String menuId,
      String parentMenuId,
      String menuNm,
      String menuUrl,
      int menuLevel,
      int sortOrd,
      String menuType) {
    MenuItemVO menu = new MenuItemVO();
    menu.setMenuId(menuId);
    menu.setParentMenuId(parentMenuId);
    menu.setMenuNm(menuNm);
    menu.setMenuUrl(menuUrl);
    menu.setMenuLevel(menuLevel);
    menu.setSortOrd(sortOrd);
    menu.setMenuType(menuType);
    menu.setDisplayYn(YES);
    menu.setUseYn(YES);
    menu.setSystemYn(NO);
    return menu;
  }
}
