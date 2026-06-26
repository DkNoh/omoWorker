package com.scbk.sms.dto.basic;

import com.scbk.sms.dto.common.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeSearchRequestDTO extends PageRequestDTO {

  private String searchKeyword;
  private String noticeType;
  private String useYn;
  private String startDt;
}
