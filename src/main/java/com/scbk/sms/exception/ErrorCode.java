package com.scbk.sms.exception;

import org.springframework.http.HttpStatus;

/** 프로젝트 전역에서 사용할 비즈니스 에러 코드 모음. */
public enum ErrorCode {

  // 공통 에러
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
  UNSUPPORTED_CODE_TYPE(HttpStatus.BAD_REQUEST, "C003", "지원하지 않는 공통코드 타입입니다."),
  UPDATE_CONFLICT(HttpStatus.CONFLICT, "C004", "다른 사용자가 먼저 수정했거나 대상 데이터가 없습니다."),
  DELETE_CONFLICT(HttpStatus.NOT_FOUND, "C005", "이미 삭제되었거나 삭제 대상 데이터가 없습니다."),
  DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "C006", "조회 대상 데이터가 없습니다."),
  EXCEL_ROW_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "C007", "엑셀 다운로드 최대 건수를 초과했습니다."),

  // 권한/인증 에러
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증되지 않은 사용자입니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "해당 기능에 대한 접근 권한이 없습니다."),

  // 비즈니스 로직 에러
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
  DUPLICATE_USER(HttpStatus.CONFLICT, "U002", "이미 존재하는 사용자입니다."),
  DUPLICATE_MENU_URL(HttpStatus.CONFLICT, "M001", "이미 같은 URL을 사용하는 메뉴가 존재합니다."),
  SYSTEM_MENU_PROTECTED(HttpStatus.BAD_REQUEST, "M002", "시스템 메뉴는 삭제하거나 URL을 변경할 수 없습니다."),
  MENU_TYPE_URL_INVALID(
      HttpStatus.BAD_REQUEST, "M003", "그룹(G) 메뉴는 URL을 가질 수 없고, 메뉴(M)는 URL이 필수입니다."),
  DUPLICATE_MENU_ID(HttpStatus.CONFLICT, "M004", "이미 존재하는 메뉴 ID입니다."),
  MENU_HAS_CHILDREN(HttpStatus.BAD_REQUEST, "M005", "하위 메뉴가 있어 삭제할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
