package com.scbk.sms.dto.sms;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 수정 요청 화이트리스트 DTO.
 *
 * <p>발송이력(SMS_HISTORY)은 실제 발송 엔진이 write한다. 화면에서의 수정은 결과 정정(상태/결과코드/결과메시지) 및 번호 정정에 한정한다. 시스템
 * 필드(REG_*, 식별자 발급값)는 제외한다.
 */
@Data
public class SmsHistoryUpdateRequestDTO {

  /** PK 필드(WHERE 조건): SMS_HISTORY_ID */
  private Integer smsHistoryId;

  private LocalDateTime sentAt;
  private String receiverNo;
  private String senderNo;
  private String sendType;
  private String sendStatus;
  private String resultCd;
  private String resultMsg;

  /** 낙관적 잠금용. 조회 시점의 UPD_DTTM(hidden으로 받는다) */
  private LocalDateTime beforeUpdDttm;
}
