package com.scbk.sms.api.v1.employee;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scbk.sms.api.security.ApiAuthenticationEntryPoint;
import com.scbk.sms.api.security.ApiKeyAuthenticationFilter;
import com.scbk.sms.api.security.ApiKeyProperties;
import com.scbk.sms.service.employee.EmployeeQueryService;
import com.scbk.sms.vo.employee.EmployeeVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 외부 시스템이 API Key 헤더로 EMP API를 호출하는 전체 흐름을 검증한다. */
@ExtendWith(MockitoExtension.class)
class EmployeeExternalIntegrationTest {

  private static final String TEST_CLIENT_ID = "partner-test";
  private static final String TEST_API_KEY = "integration-test-key";

  @Mock private EmployeeQueryService employeeQueryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() throws Exception {
    ApiKeyProperties properties = new ApiKeyProperties();
    properties.setClientId(TEST_CLIENT_ID);
    // 운영 설정과 동일하게 테스트에서도 키 원문이 아니라 SHA-256 해시만 필터에 전달한다.
    properties.setKeySha256(sha256Hex(TEST_API_KEY));

    ApiAuthenticationEntryPoint entryPoint =
        new ApiAuthenticationEntryPoint(new ObjectMapper().findAndRegisterModules());
    ApiKeyAuthenticationFilter apiKeyFilter =
        new ApiKeyAuthenticationFilter(properties, entryPoint);

    mockMvc =
        MockMvcBuilders.standaloneSetup(new EmployeeApiController(employeeQueryService))
            .addFilters(apiKeyFilter)
            .build();
  }

  @Test
  void 올바른_외부_키면_EMP_API를_호출한다() throws Exception {
    EmployeeVO employee = new EmployeeVO();
    employee.setEmpId("E001");
    employee.setDepId("D001");
    employee.setEmpNm("홍길동");
    employee.setEmpLev("3");
    employee.setActYn("Y");
    given(employeeQueryService.getEmployee("E001", "D001")).willReturn(employee);

    mockMvc
        .perform(
            get("/api/v1/employees/E001/D001")
                .header(ApiKeyAuthenticationFilter.CLIENT_ID_HEADER, TEST_CLIENT_ID)
                .header(ApiKeyAuthenticationFilter.API_KEY_HEADER, TEST_API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.empId").value("E001"))
        .andExpect(jsonPath("$.data.depId").value("D001"));

    then(employeeQueryService).should().getEmployee("E001", "D001");
  }

  @Test
  void 외부_키가_없으면_401이고_업무를_호출하지_않는다() throws Exception {
    mockMvc
        .perform(get("/api/v1/employees/E001/D001"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401));

    then(employeeQueryService).shouldHaveNoInteractions();
  }

  @Test
  void 외부_키가_틀리면_401이고_업무를_호출하지_않는다() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/employees/E001/D001")
                .header(ApiKeyAuthenticationFilter.CLIENT_ID_HEADER, TEST_CLIENT_ID)
                .header(ApiKeyAuthenticationFilter.API_KEY_HEADER, "wrong-key"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401));

    then(employeeQueryService).shouldHaveNoInteractions();
  }

  private String sha256Hex(String value) throws Exception {
    byte[] digest =
        MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }
}
