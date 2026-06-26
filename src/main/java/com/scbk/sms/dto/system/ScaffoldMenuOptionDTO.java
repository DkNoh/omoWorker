package com.scbk.sms.dto.system;

import lombok.Data;

@Data
public class ScaffoldMenuOptionDTO {

  private String menuId;
  private String parentMenuId;
  private String roleCode;
  private Integer sortOrd;
}
