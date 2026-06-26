package com.scbk.sms.vo.basic;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NoticeVO {

  private Integer noticeId;
  private String title;
  private String noticeType;
  private String useYn;
  private LocalDateTime startDt;
  private LocalDateTime endDt;
  private Integer viewCnt;
  private LocalDateTime regDttm;
}
