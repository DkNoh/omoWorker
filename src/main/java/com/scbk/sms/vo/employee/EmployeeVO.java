package com.scbk.sms.vo.employee;

import lombok.Data;

/** EMP 조회 결과. 비밀번호와 레거시 권한 컬럼은 조회하지 않는다. */
@Data
public class EmployeeVO {

  private String empId;
  private String depId;
  private String empNm;
  private String empLev;
  private String actYn;
}
