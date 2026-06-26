package com.scbk.sms.auth;

import com.scbk.sms.vo.auth.LoginEmployeeVO;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SmsUserPrincipal implements UserDetails {

  private final String empId;
  private final String depId;
  private final String empNm;
  private final String depNm;
  private final List<String> roleCodes;
  private final List<GrantedAuthority> authorities;

  public SmsUserPrincipal(LoginEmployeeVO employee, List<String> roleCodes) {
    this.empId = employee.getEmpId();
    this.depId = employee.getDepId();
    this.empNm = employee.getEmpNm();
    this.depNm = employee.getDepNm();
    this.roleCodes = List.copyOf(roleCodes);
    this.authorities =
        this.roleCodes.stream()
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();
  }

  public String getEmpId() {
    return empId;
  }

  public String getDepId() {
    return depId;
  }

  public String getEmpNm() {
    return empNm;
  }

  public String getDepNm() {
    return depNm;
  }

  public List<String> getRoleCodes() {
    return roleCodes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return empId;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
