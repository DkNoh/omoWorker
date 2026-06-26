package com.scbk.sms.vo.system;

import lombok.Data;

/** SMS.TB_PRIVACY_AUDIT_LOG 기록용 VO. 행위자는 (EMP_ID, DEP_ID)로 기록한다. */
@Data
public class PrivacyAuditLogVO {

  private String empId;
  private String depId;
  private String executorIp;
  private String requestUrl;
  private String actionType;
  private String targetData;
}
