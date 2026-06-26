package com.scbk.sms.service.menu;

import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class EmployeeRoleService {

  private final RoleProvider roleProvider;

  public EmployeeRoleService(RoleProvider roleProvider) {
    this.roleProvider = roleProvider;
  }

  public List<String> getActiveRoleCodes(String empId, String depId) {
    List<String> roleCodes = roleProvider.getActiveRoleCodes(empId, depId);
    if (roleCodes.isEmpty()) {
      throw new BadCredentialsException("No active role assigned to employee.");
    }
    return List.copyOf(roleCodes);
  }
}
