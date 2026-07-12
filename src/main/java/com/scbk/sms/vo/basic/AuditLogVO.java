package com.scbk.sms.vo.basic;

import java.time.LocalDateTime;
import lombok.Data;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Data
public class AuditLogVO {

  private Integer noticeId;
  private String title;
  private String noticeType;
  private String useYn;
  private Integer viewCnt;
  private String regId;
  private LocalDateTime regDttm;
  private String updId;
  private LocalDateTime updDttm;
}
