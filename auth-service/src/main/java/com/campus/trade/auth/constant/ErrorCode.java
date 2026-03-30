package com.campus.trade.auth.constant;

/**
 * 错误码常量
 */
public final class ErrorCode {

  private ErrorCode() {
  }

  /**
   * 通用参数错误
   */
  public static final int BAD_REQUEST = 400;

  /**
   * 未授权
   */
  public static final int UNAUTHORIZED = 401;

  /**
   * 认证相关业务错误
   */
  public static final int AUTH_ERROR = 4001;

  /**
   * 用户已存在
   */
  public static final int USER_ALREADY_EXISTS = 4002;

  /**
   * 用户不存在
   */
  public static final int USER_NOT_FOUND = 4003;
}
