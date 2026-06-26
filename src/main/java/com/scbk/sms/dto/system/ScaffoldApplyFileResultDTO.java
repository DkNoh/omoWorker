package com.scbk.sms.dto.system;

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
