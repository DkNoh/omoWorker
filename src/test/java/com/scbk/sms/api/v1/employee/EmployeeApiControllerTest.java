package com.scbk.sms.api.v1.employee;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.exception.GlobalExceptionHandler;
import com.scbk.sms.service.employee.EmployeeQueryService;
import com.scbk.sms.vo.employee.EmployeeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EmployeeApiControllerTest {

  @Mock private EmployeeQueryService employeeQueryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new EmployeeApiController(employeeQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void EMP_복합키로_직원을_조회한다() throws Exception {
    EmployeeVO employee = employee("E001", "D001", "홍길동", "3", "Y");
    given(employeeQueryService.getEmployee("E001", "D001")).willReturn(employee);

    mockMvc
        .perform(get("/api/v1/employees/E001/D001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("SUCCESS"))
        .andExpect(jsonPath("$.data.empId").value("E001"))
        .andExpect(jsonPath("$.data.depId").value("D001"))
        .andExpect(jsonPath("$.data.empNm").value("홍길동"))
        .andExpect(jsonPath("$.data.empLev").value("3"))
        .andExpect(jsonPath("$.data.actYn").value("Y"))
        .andExpect(jsonPath("$.data.empPass").doesNotExist())
        .andExpect(jsonPath("$.data.empPhone").doesNotExist());

    then(employeeQueryService).should().getEmployee("E001", "D001");
  }

  @Test
  void 직원이_없으면_404로_응답한다() throws Exception {
    given(employeeQueryService.getEmployee("NONE", "D001"))
        .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

    mockMvc
        .perform(get("/api/v1/employees/NONE/D001"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404))
        .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
  }

  private EmployeeVO employee(
      String empId, String depId, String empNm, String empLev, String actYn) {
    EmployeeVO employee = new EmployeeVO();
    employee.setEmpId(empId);
    employee.setDepId(depId);
    employee.setEmpNm(empNm);
    employee.setEmpLev(empLev);
    employee.setActYn(actYn);
    return employee;
  }
}
