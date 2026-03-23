package com.campus.trade.chat.common;

public class BaseException extends RuntimeException {

  private final Integer code;

  public BaseException(String message) {
    super(message);
    this.code = 400;
  }

  public BaseException(Integer code, String message) {
    super(message);
    this.code = code;
  }

  public Integer getCode() {
    return code;
  }
}
