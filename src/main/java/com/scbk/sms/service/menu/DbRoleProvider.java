package com.scbk.sms.service.menu;

import com.scbk.sms.mapper.menu.EmployeeRoleMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "sms.role.source", havingValue = "db")
public class DbRoleProvider implements RoleProvider {

  private final EmployeeRoleMapper employeeRoleMapper;

  public DbRoleProvider(EmployeeRoleMapper employeeRoleMapper) {
    this.employeeRoleMapper = employeeRoleMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getActiveRoleCodes(String empId, String depId) {
    return employeeRoleMapper.selectRoleCodes(empId, depId);
  }
}
