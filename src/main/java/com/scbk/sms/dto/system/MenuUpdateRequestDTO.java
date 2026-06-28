package com.scbk.sms.dto.system;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

/**
 * 메뉴 관리 화면 등록/수정 요청 화이트리스트 DTO.
 *
 * <p>menuId는 PK로 WHERE 조건에만 쓰이고 UPDATE SET 에는 절대 포함되지 않는다 (규약: MENU_ID 변경
 * 금지). systemYn은 시스템 보호 플래그이므로 화면에서 편집하지 않고 Service가 기존값을 보존한다.
 * regId/regDttm/updId/updDttm은 서버가 채운다.
 *
 * <p>{@code authRows}는 선택(optional)이다. {@code null}이 아니고 빈 리스트가 아니면 Service가
 * 해당 메뉴의 TB_MENU_AUTH 행을 이 목록으로 교체(replace)한다. {@code null}이면 기존 권한 행은
 * 그대로 둔다(메뉴 정보만 수정).
 */
@Data
public class MenuUpdateRequestDTO {

  /** PK. 등록 시 필수이며, 수정/삭제 시 WHERE 키로만 사용된다. */
  @NotBlank(message = "menuId는 필수입니다.")
  private String menuId;

  private String parentMenuId;

  @NotBlank(message = "menuNm은 필수입니다.")
  private String menuNm;

  private String menuUrl;

  private int menuLevel;
  private int sortOrd;

  @NotBlank(message = "menuType은 필수입니다.")
  @Pattern(regexp = "[GM]", message = "menuType은 G(그룹) 또는 M(메뉴)만 가능합니다.")
  private String menuType;

  private String iconNm;
  private String displayYn;
  private String useYn;
  private String remark;

  /**
   * 선택 권한 행 목록. {@code null}이면 메뉴 정보만 수정(권한 행 보존). 빈 리스트 포함 not-null이면
   * 권한 행을 전부 지우고 이 목록으로 교체한다. 자식 DTO의 menuId는 부모 menuId로 강제 덮어쓴다.
   */
  @Valid
  private List<MenuAuthRowDTO> authRows;
}
