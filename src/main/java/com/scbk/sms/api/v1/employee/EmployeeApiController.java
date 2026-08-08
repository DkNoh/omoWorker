package com.scbk.sms.api.v1.employee;

import com.scbk.sms.api.v1.employee.dto.EmployeeResponse;
import com.scbk.sms.dto.common.ApiResponse;
import com.scbk.sms.service.employee.EmployeeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외부 연동용 직원 조회 API v1.
 *
 * <p>EMP의 식별자는 EMP_ID 단독이 아니라 {@code (EMP_ID, DEP_ID)} 복합키이므로 두 값을 모두 경로에 받는다. 외부 응답은
 * {@link EmployeeResponse}로 변환해 EMP_PASS, EMP_PHONE, 레거시 PERM_* 컬럼이 계약에 노출되지 않게 한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees")
public class EmployeeApiController {

  private final EmployeeQueryService employeeQueryService;

  /**
   * 복합키에 해당하는 직원 한 명을 조회한다.
   *
   * @param empId 직원 ID
   * @param depId 소속 부서 ID
   * @return 공개 가능한 EMP 필드만 포함한 공통 API 응답
   */
  @GetMapping("/{empId}/{depId}")
  public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(
      @PathVariable String empId, @PathVariable String depId) {
    EmployeeResponse response =
        EmployeeResponse.from(employeeQueryService.getEmployee(empId, depId));
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
