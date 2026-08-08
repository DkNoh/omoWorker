package com.scbk.sms.dto.system;

import lombok.Data;

/** {@code rawQuery}의 {@code $검색변수} 하나를 화면 검색 컨트롤로 렌더링하기 위한 옵션. */
@Data
public class ScaffoldSearchParamOptionDTO {

  /** snake_case SQL 변수를 camelCase로 변환한 요청 파라미터명. */
  private String name;

  /** 검색영역에 표시할 라벨. DB comment 또는 SELECT alias가 기본값이다. */
  private String label;

  /** 입력 유형(text/date/select/radio). */
  private String inputType;

  /** 최초 조회와 초기화에 적용할 기본값 토큰 또는 리터럴. */
  private String defaultValue;

  /** select/radio의 값과 라벨 목록 원문. */
  private String optionsText;
}
