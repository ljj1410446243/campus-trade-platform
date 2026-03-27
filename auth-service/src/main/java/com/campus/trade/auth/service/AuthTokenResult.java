package com.campus.trade.auth.service;

import com.campus.trade.auth.dto.LoginResponse;

public record AuthTokenResult(LoginResponse loginResponse, String refreshToken) {
}
