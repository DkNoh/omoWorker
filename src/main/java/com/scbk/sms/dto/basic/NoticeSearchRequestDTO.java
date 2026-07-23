package com.scbk.sms.dto.basic;

import com.scbk.sms.dto.common.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 개발자 소유 수동 참조: scaffold 복사 후 커스터마이즈한 window.open CRUD 예제. 재생성하지 않고 직접 수정한다. */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeSearchRequestDTO extends PageRequestDTO {

  private String startdt;
  private String enddt;
  private String useYn;
}
