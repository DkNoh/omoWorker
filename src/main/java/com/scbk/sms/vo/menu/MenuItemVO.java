package com.scbk.sms.vo.menu;

import java.util.ArrayList;
import java.util.List;

public class MenuItemVO {

  private String menuId;
  private String parentMenuId;
  private String menuNm;
  private String menuUrl;
  private int menuLevel;
  private int sortOrd;
  private String menuType;
  private String iconNm;
  private String displayYn;
  private String useYn;
  private String systemYn;
  private final List<MenuItemVO> children = new ArrayList<>();

  public String getMenuId() {
    return menuId;
  }

  public void setMenuId(String menuId) {
    this.menuId = menuId;
  }

  public String getParentMenuId() {
    return parentMenuId;
  }

  public void setParentMenuId(String parentMenuId) {
    this.parentMenuId = parentMenuId;
  }

  public String getMenuNm() {
    return menuNm;
  }

  public void setMenuNm(String menuNm) {
    this.menuNm = menuNm;
  }

  public String getMenuUrl() {
    return menuUrl;
  }

  public void setMenuUrl(String menuUrl) {
    this.menuUrl = menuUrl;
  }

  public int getMenuLevel() {
    return menuLevel;
  }

  public void setMenuLevel(int menuLevel) {
    this.menuLevel = menuLevel;
  }

  public int getSortOrd() {
    return sortOrd;
  }

  public void setSortOrd(int sortOrd) {
    this.sortOrd = sortOrd;
  }

  public String getMenuType() {
    return menuType;
  }

  public void setMenuType(String menuType) {
    this.menuType = menuType;
  }

  public String getIconNm() {
    return iconNm;
  }

  public void setIconNm(String iconNm) {
    this.iconNm = iconNm;
  }

  public String getDisplayYn() {
    return displayYn;
  }

  public void setDisplayYn(String displayYn) {
    this.displayYn = displayYn;
  }

  public String getUseYn() {
    return useYn;
  }

  public void setUseYn(String useYn) {
    this.useYn = useYn;
  }

  public String getSystemYn() {
    return systemYn;
  }

  public void setSystemYn(String systemYn) {
    this.systemYn = systemYn;
  }

  public List<MenuItemVO> getChildren() {
    return children;
  }
}
