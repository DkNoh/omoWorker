package com.scbk.sms.vo.auth;

import lombok.Data;

@Data
public class LoginEmployeeVO {

  private String empId;
  private String depId;
  private String empNm;
  private String depNm;
  private String actYn;
  private String depActYn;
}
