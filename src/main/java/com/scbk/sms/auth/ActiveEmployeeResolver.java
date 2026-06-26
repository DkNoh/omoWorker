package com.scbk.sms.auth;

import com.scbk.sms.mapper.auth.LoginEmployeeMapper;
import com.scbk.sms.vo.auth.LoginEmployeeVO;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ActiveEmployeeResolver {

  private final LoginEmployeeMapper loginEmployeeMapper;

  public ActiveEmployeeResolver(LoginEmployeeMapper loginEmployeeMapper) {
    this.loginEmployeeMapper = loginEmployeeMapper;
  }

  public LoginEmployeeVO resolveSingleActiveEmployee(String empId) {
    if (!StringUtils.hasText(empId)) {
      throw new BadCredentialsException("사번을 입력해야 합니다.");
    }

    List<LoginEmployeeVO> employees =
        loginEmployeeMapper.selectActiveEmployeesByEmpId(empId.trim());
    if (employees.isEmpty()) {
      throw new BadCredentialsException("활성 사용자 정보를 찾을 수 없습니다.");
    }
    if (employees.size() > 1) {
      throw new BadCredentialsException("동일 사번에 활성 부서가 여러 건입니다. EMP 데이터를 정리해야 합니다.");
    }

    return employees.get(0);
  }
}
