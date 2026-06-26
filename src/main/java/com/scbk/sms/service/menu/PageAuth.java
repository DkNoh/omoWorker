package com.scbk.sms.service.menu;

import java.util.EnumSet;
import java.util.Set;

/** 화면 렌더링과 공통 JS가 사용하는 현재 메뉴의 기능 권한. 실제 API 차단은 MenuAuthInterceptor가 별도로 수행한다. */
public class PageAuth {

  private final boolean read;
  private final boolean create;
  private final boolean update;
  private final boolean delete;
  private final boolean approve;
  private final boolean cancel;
  private final boolean download;
  private final boolean maskView;

  private PageAuth(Set<MenuPermission> permissions) {
    this.read = permissions.contains(MenuPermission.READ);
    this.create = permissions.contains(MenuPermission.CREATE);
    this.update = permissions.contains(MenuPermission.UPDATE);
    this.delete = permissions.contains(MenuPermission.DELETE);
    this.approve = permissions.contains(MenuPermission.APPROVE);
    this.cancel = permissions.contains(MenuPermission.CANCEL);
    this.download = permissions.contains(MenuPermission.DOWNLOAD);
    this.maskView = permissions.contains(MenuPermission.MASK_VIEW);
  }

  public static PageAuth from(Set<MenuPermission> permissions) {
    if (permissions == null || permissions.isEmpty()) {
      return none();
    }
    return new PageAuth(EnumSet.copyOf(permissions));
  }

  public static PageAuth all() {
    return new PageAuth(EnumSet.allOf(MenuPermission.class));
  }

  public static PageAuth none() {
    return new PageAuth(EnumSet.noneOf(MenuPermission.class));
  }

  public boolean isRead() {
    return read;
  }

  public boolean isCreate() {
    return create;
  }

  public boolean isUpdate() {
    return update;
  }

  public boolean isDelete() {
    return delete;
  }

  public boolean isApprove() {
    return approve;
  }

  public boolean isCancel() {
    return cancel;
  }

  public boolean isDownload() {
    return download;
  }

  public boolean isMaskView() {
    return maskView;
  }
}
