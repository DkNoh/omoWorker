package com.scbk.sms.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * TB_MENU_AUTH 1행 화이트리스트 DTO.
 *
 * <p>메뉴 수정(/system/menu-manage/update) 요청이 {@code authRows}를 포함하면, Service는 해당 메뉴의 기존 권한 행을 전부 지우고
 * 이 목록으로 교체(replace)한다. {@code menuId}는 부모 DTO의 menuId로 강제 덮어쓴다(자식 행이 다른 메뉴를 가리키는 것을 방지).
 *
 * <p>각 CAN_* 플래그는 미입력 시 'N'으로 간주한다(Service가 normalize).
 */
@Data
public class MenuAuthRowDTO {

  /** PK의 일부. 부모 메뉴 ID와 다르면 Service가 부모 메뉴 ID로 덮어쓴다. */
  private String menuId;

  @NotBlank(message = "roleCd는 필수입니다.")
  private String roleCd;

  @Pattern(regexp = "[YN]", message = "canRead는 Y 또는 N이어야 합니다.")
  private String canRead;

  @Pattern(regexp = "[YN]", message = "canCreate는 Y 또는 N이어야 합니다.")
  private String canCreate;

  @Pattern(regexp = "[YN]", message = "canUpdate는 Y 또는 N이어야 합니다.")
  private String canUpdate;

  @Pattern(regexp = "[YN]", message = "canDelete는 Y 또는 N이어야 합니다.")
  private String canDelete;

  @Pattern(regexp = "[YN]", message = "canApprove는 Y 또는 N이어야 합니다.")
  private String canApprove;

  @Pattern(regexp = "[YN]", message = "canCancel는 Y 또는 N이어야 합니다.")
  private String canCancel;

  @Pattern(regexp = "[YN]", message = "canDownload는 Y 또는 N이어야 합니다.")
  private String canDownload;

  @Pattern(regexp = "[YN]", message = "canMaskView는 Y 또는 N이어야 합니다.")
  private String canMaskView;
}
