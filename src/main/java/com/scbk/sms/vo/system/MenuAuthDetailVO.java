package com.scbk.sms.vo.system;

import lombok.Data;

/**
 * TB_MENU_AUTH 1행 조회용 VO. 메뉴 관리 상세 화면의 권한 매트릭스 한 줄을 그릴 때 사용하며, ROLE_NM을
 * 함께 조인해 roleCd만으로도 화면이 읽히도록 한다.
 */
@Data
public class MenuAuthDetailVO {

  private String menuId;
  private String roleCd;
  private String roleNm;
  private String canRead;
  private String canCreate;
  private String canUpdate;
  private String canDelete;
  private String canApprove;
  private String canCancel;
  private String canDownload;
  private String canMaskView;
}
