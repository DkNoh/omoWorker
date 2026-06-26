package com.scbk.sms.auth;

import com.scbk.sms.service.menu.EmployeeRoleService;
import com.scbk.sms.vo.auth.LoginEmployeeVO;
import java.util.Collection;
import java.util.List;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

public class LdapEmployeeContextMapper implements UserDetailsContextMapper {

  private final ActiveEmployeeResolver activeEmployeeResolver;
  private final EmployeeRoleService employeeRoleService;

  public LdapEmployeeContextMapper(
      ActiveEmployeeResolver activeEmployeeResolver, EmployeeRoleService employeeRoleService) {
    this.activeEmployeeResolver = activeEmployeeResolver;
    this.employeeRoleService = employeeRoleService;
  }

  @Override
  public UserDetails mapUserFromContext(
      DirContextOperations ctx,
      String username,
      Collection<? extends GrantedAuthority> authorities) {
    LoginEmployeeVO employee = activeEmployeeResolver.resolveSingleActiveEmployee(username);
    List<String> roleCodes =
        employeeRoleService.getActiveRoleCodes(employee.getEmpId(), employee.getDepId());
    return new SmsUserPrincipal(employee, roleCodes);
  }

  @Override
  public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    throw new UnsupportedOperationException("LDAP 사용자 쓰기는 지원하지 않습니다.");
  }
}
