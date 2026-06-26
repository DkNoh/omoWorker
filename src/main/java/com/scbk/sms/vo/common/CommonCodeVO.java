package com.scbk.sms.vo.common;

import lombok.Data;

/** 공통코드 콤보/자동완성 응답 항목. CommonUtils.initCombos가 {code, name}을 기대한다. */
@Data
public class CommonCodeVO {

  private String code;
  private String name;
}
