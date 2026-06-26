package com.scbk.sms.dto.common;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 JSON API 응답을 감싸는 공통 규격. 성공/실패와 관계없이 프론트엔드는 항상 이 규격의 JSON을 받는다.
 *
 * <p>성공: return ResponseEntity.ok(ApiResponse.success(data)); 실패: return
 * ResponseEntity.status(400).body(ApiResponse.error(400, "잘못된 요청"));
 */
public class ApiResponse<T> {

  private final String timestamp;
  private final int code;
  private final String message;
  private final T data;
  private final List<FieldError> errors;

  /** 필드 단위 검증 에러 정보 (@Valid 실패 시 프론트엔드에서 필드별 매핑용) */
  public record FieldError(String field, String message) {}

  private ApiResponse(int code, String message, T data, List<FieldError> errors) {
    this.timestamp = LocalDateTime.now().toString();
    this.code = code;
    this.message = message;
    this.data = data;
    this.errors = errors;
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(200, "SUCCESS", data, null);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(200, message, data, null);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null, null);
  }

  public static <T> ApiResponse<T> error(int code, String message, List<FieldError> errors) {
    return new ApiResponse<>(code, message, null, errors);
  }

  public String getTimestamp() {
    return timestamp;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }

  public List<FieldError> getErrors() {
    return errors;
  }
}
