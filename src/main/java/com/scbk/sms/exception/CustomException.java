package com.scbk.sms.exception;

/**
 * 비즈니스 로직(Service)에서 예측 가능한 오류를 던질 때 사용한다.
 *
 * <p>if (employee == null) { throw new CustomException(ErrorCode.USER_NOT_FOUND); }
 */
public class CustomException extends RuntimeException {

  private final ErrorCode errorCode;

  public CustomException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
