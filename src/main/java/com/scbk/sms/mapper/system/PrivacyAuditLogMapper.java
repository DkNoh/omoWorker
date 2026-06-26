package com.scbk.sms.mapper.system;

import com.scbk.sms.vo.system.PrivacyAuditLogVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrivacyAuditLogMapper {

  int insertAuditLog(PrivacyAuditLogVO logVO);
}
