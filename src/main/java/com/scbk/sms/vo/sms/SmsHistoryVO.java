package com.scbk.sms.vo.sms;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SmsHistoryVO {

  private Integer smsHistoryId;
  private String requestId;
  private LocalDateTime sentAt;
  private String receiverNo;
  private String senderNo;
  private String sendType;
  private String sendStatus;
  private String resultCd;
  private String resultMsg;
  private LocalDateTime updDttm;
}
