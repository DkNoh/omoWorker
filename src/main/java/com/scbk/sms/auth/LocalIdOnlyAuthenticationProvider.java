package com.scbk.sms.auth;

import com.scbk.sms.service.menu.EmployeeRoleService;
import com.scbk.sms.vo.auth.LoginEmployeeVO;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalIdOnlyAuthenticationProvider implements AuthenticationProvider {

  private final ActiveEmployeeResolver activeEmployeeResolver;
  private final EmployeeRoleService employeeRoleService;

  public LocalIdOnlyAuthenticationProvider(
      ActiveEmployeeResolver activeEmployeeResolver, EmployeeRoleService employeeRoleService) {
    this.activeEmployeeResolver = activeEmployeeResolver;
    this.employeeRoleService = employeeRoleService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    LoginEmployeeVO employee =
        activeEmployeeResolver.resolveSingleActiveEmployee(authentication.getName());
    List<String> roleCodes =
        employeeRoleService.getActiveRoleCodes(employee.getEmpId(), employee.getDepId());
    SmsUserPrincipal principal = new SmsUserPrincipal(employee, roleCodes);
    return new UsernamePasswordAuthenticationToken(
        principal, authentication.getCredentials(), principal.getAuthorities());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
