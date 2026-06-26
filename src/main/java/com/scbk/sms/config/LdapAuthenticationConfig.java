package com.scbk.sms.config;

import com.scbk.sms.auth.ActiveEmployeeResolver;
import com.scbk.sms.auth.LdapEmployeeContextMapper;
import com.scbk.sms.service.menu.EmployeeRoleService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

@Configuration
@Profile({"dev", "prod"})
@EnableConfigurationProperties(SmsLdapProperties.class)
public class LdapAuthenticationConfig {

  @Bean
  public AuthenticationProvider ldapAuthenticationProvider(
      SmsLdapProperties properties,
      ActiveEmployeeResolver activeEmployeeResolver,
      EmployeeRoleService employeeRoleService)
      throws Exception {
    DefaultSpringSecurityContextSource contextSource =
        new DefaultSpringSecurityContextSource(properties.getUrl() + "/" + properties.getBaseDn());
    contextSource.setUserDn(properties.getManagerDn());
    contextSource.setPassword(properties.getManagerPassword());
    contextSource.afterPropertiesSet();

    BindAuthenticator authenticator = new BindAuthenticator(contextSource);
    authenticator.setUserSearch(
        new FilterBasedLdapUserSearch(
            properties.getUserSearchBase(), properties.getUserSearchFilter(), contextSource));

    DefaultLdapAuthoritiesPopulator authoritiesPopulator =
        new DefaultLdapAuthoritiesPopulator(contextSource, null);
    LdapAuthenticationProvider provider =
        new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
    provider.setUserDetailsContextMapper(
        new LdapEmployeeContextMapper(activeEmployeeResolver, employeeRoleService));
    return provider;
  }
}
