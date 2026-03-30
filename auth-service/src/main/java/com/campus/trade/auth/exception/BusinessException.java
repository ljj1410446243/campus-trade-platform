package com.campus.trade.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {

  private final int code;
  private final HttpStatus httpStatus;

  public BusinessException(int code, String message) {
    this(code, HttpStatus.BAD_REQUEST, message);
  }

  public BusinessException(int code, HttpStatus httpStatus, String message) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
  }

  public int getCode() {
    return code;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
