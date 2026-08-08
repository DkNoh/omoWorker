package com.scbk.sms.dto.system;

import lombok.Data;

/** SELECT 결과 컬럼 하나의 그리드·모달·입력·표시 형식 생성 옵션. */
@Data
public class ScaffoldColumnOptionDTO {

  /** SQL SELECT 결과 컬럼명. 내부 비교를 위해 생성 모델에서 대문자로 정규화한다. */
  private String columnName;

  /** 목록 그리드 표시 여부. 숨겨도 VO 식별값으로는 유지될 수 있다. */
  private boolean visible = true;

  /** CRUD 상세 모달에 필드를 배치할지 여부. */
  private boolean modalVisible = true;

  /** 등록·수정 DTO와 Mapper SET 절에 포함할 화이트리스트 여부. */
  private boolean editable;

  /** DB comment 또는 사용자가 지정한 화면 표시명. */
  private String headerName;

  /** 그리드 컬럼 너비(px). 비어 있으면 생성 모델의 타입별 기본값을 사용한다. */
  private Integer width;

  /** 그리드 정렬 방향(left/center/right). */
  private String align;

  /** 날짜 표시 규칙(NONE/DATE/DATETIME/AUTO). */
  private String dateFormat;

  /** 개인정보 마스킹 규칙. Service와 Excel 산출물에 같은 규칙을 적용한다. */
  private String maskType;

  /** 생성 HTML의 {@code data-mask}에 기록할 입력 형식. */
  private String inputMask;

  /** 생성 HTML의 {@code data-validate}와 서버 DTO 필수 검증에 사용할 규칙. */
  private String validate;

  /** 선택형 입력과 배지 렌더링에 사용할 값/라벨 목록 원문. */
  private String optionsText;
}
