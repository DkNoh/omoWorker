package com.scbk.sms.vo.system;

import java.time.LocalDateTime;
import lombok.Data;

/** SMS.TB_MENU 1행 조회용 VO. 메뉴 관리 화면의 목록/상세 응답에 사용한다. */
@Data
public class MenuManageVO {

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
  private String remark;
  private String regId;
  private LocalDateTime regDttm;
  private String updId;
  private LocalDateTime updDttm;
}
