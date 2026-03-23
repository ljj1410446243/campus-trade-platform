package com.campus.trade.chat.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  public ApiResponse<Void> handleBaseException(BaseException e) {
    return ApiResponse.error(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldError() != null
            ? e.getBindingResult().getFieldError().getDefaultMessage()
            : "参数校验失败";
    return ApiResponse.error(400, message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
    return ApiResponse.error(400, "参数校验失败");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
    return ApiResponse.error(400, "请求体格式错误");
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleException(Exception e) {
    e.printStackTrace();
    return ApiResponse.error(500, "服务器内部错误");
  }
}
