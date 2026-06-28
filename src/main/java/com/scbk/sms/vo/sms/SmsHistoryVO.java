package com.scbk.sms.vo.sms;

import java.time.LocalDateTime;
import lombok.Data;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
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
}
