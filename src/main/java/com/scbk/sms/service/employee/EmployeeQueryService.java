package com.scbk.sms.service.employee;

import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.employee.EmployeeQueryMapper;
import com.scbk.sms.vo.employee.EmployeeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** EMP 조회 업무 서비스. API 계약과 분리해 다른 진입점에서도 재사용한다. */
@Service
@RequiredArgsConstructor
public class EmployeeQueryService {

  private final EmployeeQueryMapper employeeQueryMapper;

  @Transactional(readOnly = true)
  public EmployeeVO getEmployee(String empId, String depId) {
    EmployeeVO employee = employeeQueryMapper.selectById(empId, depId);
    if (employee == null) {
      throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }
    return employee;
  }
}
