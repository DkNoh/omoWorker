package com.scbk.sms.api.v1.employee.dto;

import com.scbk.sms.vo.employee.EmployeeVO;
import lombok.Data;

/** 외부 API에 공개할 수 있는 EMP 필드만 담은 응답 계약. */
@Data
public class EmployeeResponse {

  private final String empId;
  private final String depId;
  private final String empNm;
  private final String empLev;
  private final String actYn;

  public static EmployeeResponse from(EmployeeVO employee) {
    return new EmployeeResponse(
        employee.getEmpId(),
        employee.getDepId(),
        employee.getEmpNm(),
        employee.getEmpLev(),
        employee.getActYn());
  }
}
