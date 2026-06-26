package com.scbk.sms.service.menu;

/** TB_MENU_AUTH의 CAN_* 컬럼과 1:1로 대응하는 메뉴 기능 권한. */
public enum MenuPermission {
  READ,
  CREATE,
  UPDATE,
  DELETE,
  APPROVE,
  CANCEL,
  DOWNLOAD,
  MASK_VIEW
}
