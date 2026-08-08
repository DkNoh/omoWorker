package com.scbk.sms.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.employee.EmployeeQueryMapper;
import com.scbk.sms.vo.employee.EmployeeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeQueryServiceTest {

  @Mock private EmployeeQueryMapper employeeQueryMapper;

  private EmployeeQueryService employeeQueryService;

  @BeforeEach
  void setUp() {
    employeeQueryService = new EmployeeQueryService(employeeQueryMapper);
  }

  @Test
  void EMP_복합키_조회_결과를_반환한다() {
    EmployeeVO employee = new EmployeeVO();
    employee.setEmpId("E001");
    employee.setDepId("D001");
    given(employeeQueryMapper.selectById("E001", "D001")).willReturn(employee);

    EmployeeVO result = employeeQueryService.getEmployee("E001", "D001");

    assertThat(result).isSameAs(employee);
  }

  @Test
  void EMP_복합키에_해당하는_직원이_없으면_USER_NOT_FOUND를_던진다() {
    given(employeeQueryMapper.selectById("NONE", "D001")).willReturn(null);

    assertThatThrownBy(() -> employeeQueryService.getEmployee("NONE", "D001"))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
  }
}
