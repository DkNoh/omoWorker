package com.scbk.sms.vo.sms;

import java.time.LocalDateTime;
import lombok.Data;

// 개인정보 컬럼은 Service에서 MaskingUtil로 마스킹한 값을 담는다.
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Data
public class CustomerSearchVO {

  private Integer customerId;
  private String customerNm;
  private String mobileNo;
  private String email;
  private String birthDt;
  private String genderCd;
  private String agreeYn;
  private String useYn;
  private LocalDateTime regDttm;
}
