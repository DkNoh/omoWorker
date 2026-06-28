package com.scbk.sms.vo.system;

import lombok.Data;

/** TB_ROLE 조회용 VO. 메뉴 관리 상세 화면의 권한 매트릭스에서 활성 역할 목록을 표시한다. */
@Data
public class MenuRoleVO {

  private String roleCd;
  private String roleNm;
  private String roleDesc;
  private int sortOrd;
  private String useYn;
}
