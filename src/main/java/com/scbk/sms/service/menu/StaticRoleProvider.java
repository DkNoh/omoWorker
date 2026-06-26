package com.scbk.sms.service.menu;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sms.role.source", havingValue = "static")
public class StaticRoleProvider implements RoleProvider {

  private final String staticDefaultRole;

  public StaticRoleProvider(@Value("${sms.role.static-default}") String staticDefaultRole) {
    this.staticDefaultRole = staticDefaultRole;
  }

  @Override
  public List<String> getActiveRoleCodes(String empId, String depId) {
    String roleCode = staticDefaultRole.trim();
    if (roleCode.isEmpty()) {
      throw new IllegalStateException("sms.role.static-default must not be blank");
    }
    return List.of(roleCode);
  }
}
