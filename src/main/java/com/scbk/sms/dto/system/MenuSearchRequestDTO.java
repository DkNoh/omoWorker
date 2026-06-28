package com.scbk.sms.dto.system;

import com.scbk.sms.dto.common.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 메뉴 관리 화면 목록 조회 요청. keyword는 MENU_ID/MENU_NM 부분 일치, 나머지는 정확 일치. */
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuSearchRequestDTO extends PageRequestDTO {

  private String searchKeyword;
  private String menuType;
  private String useYn;
  private String displayYn;
}
