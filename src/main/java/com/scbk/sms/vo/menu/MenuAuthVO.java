package com.scbk.sms.vo.menu;

import lombok.Data;

/** TB_MENU_AUTH 조회 결과. 역할 여러 건은 MAX 집계로 합쳐 'Y' 우선이다. */
@Data
public class MenuAuthVO {

  private String canRead;
  private String canCreate;
  private String canUpdate;
  private String canDelete;
  private String canApprove;
  private String canCancel;
  private String canDownload;
  private String canMaskView;
}
