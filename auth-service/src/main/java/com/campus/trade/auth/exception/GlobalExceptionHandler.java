package com.campus.trade.auth.exception;

import com.campus.trade.common.response.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理参数校验异常
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult()
            .getFieldError()
            .getDefaultMessage();

    return ApiResponse.error(400, message);
  }

  /**
   * 处理业务异常
   */
  @ExceptionHandler(BusinessException.class)
  public ApiResponse<Void> handleBusinessException(BusinessException e) {
    return ApiResponse.error(e.getCode(), e.getMessage());
  }

  /**
   * 处理其他未知异常
   */
  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleException(Exception e) {
    return ApiResponse.error(500, "服务器内部错误");
  }
}
