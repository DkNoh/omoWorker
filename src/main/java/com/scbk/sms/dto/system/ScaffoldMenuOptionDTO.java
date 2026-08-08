package com.scbk.sms.dto.system;

import lombok.Data;

/** 생성 화면을 {@code TB_MENU}/{@code TB_MENU_AUTH}에 등록하기 위한 seed 옵션. */
@Data
public class ScaffoldMenuOptionDTO {

  /** 비어 있으면 모듈명과 도메인 ID에서 대문자 메뉴 ID를 생성한다. */
  private String menuId;

  /** 생성 메뉴가 속할 기존 그룹 메뉴 ID. 적용 전 실제 운영 트리와 일치하는지 확인한다. */
  private String parentMenuId;

  /** 최초 권한 행을 부여할 역할 코드. */
  private String roleCode;

  /** 같은 부모 아래 표시 순서. */
  private Integer sortOrd;
}
