package com.scbk.sms.dto.system;

/**
 * 스캐폴드 산출물 한 파일의 적용 미리보기 결과.
 *
 * <p>{@code status}는 화면과 자동화가 사용하는 안정된 코드({@code NEW}, {@code UNCHANGED}, {@code OVERWRITE})이고,
 * {@code statusLabel}은 사용자에게 보여 줄 한글 설명이다. {@code path}는 프로젝트 루트 기준 상대 경로만 노출한다.
 */
public class ScaffoldApplyFileResultDTO {

  private final String fileName;
  private final String path;
  private final String status;
  private final String statusLabel;

  public ScaffoldApplyFileResultDTO(
      String fileName, String path, String status, String statusLabel) {
    this.fileName = fileName;
    this.path = path;
    this.status = status;
    this.statusLabel = statusLabel;
  }

  public String getFileName() {
    return fileName;
  }

  public String getPath() {
    return path;
  }

  public String getStatus() {
    return status;
  }

  public String getStatusLabel() {
    return statusLabel;
  }
}
