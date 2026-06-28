package com.scbk.sms.dto.system;

import lombok.Data;

@Data
public class ScaffoldColumnOptionDTO {

  private String columnName;
  private boolean visible = true;
  private boolean modalVisible = true;
  private boolean editable;
  private String headerName;
  private Integer width;
  private String align;
  private String dateFormat;
  private String maskType;
  private String inputMask;
  private String validate;
  private String optionsText;
}
