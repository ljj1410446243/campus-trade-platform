package com.campus.trade.auth.exception;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {

  private final int code;

  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
