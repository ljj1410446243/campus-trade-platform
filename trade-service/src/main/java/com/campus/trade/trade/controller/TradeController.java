package com.campus.trade.trade.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;
import com.campus.trade.trade.service.TradeService;
import com.campus.trade.trade.util.LoginUserHelper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trades")
public class TradeController {

    private final TradeService tradeService;
    private final LoginUserHelper loginUserHelper;

    public TradeController(TradeService tradeService, LoginUserHelper loginUserHelper) {
        this.tradeService = tradeService;
        this.loginUserHelper = loginUserHelper;
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success(tradeService.ping());
    }

    @PostMapping
    public ApiResponse<Map<String, String>> createTrade(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateTradeRequest request) {

        String buyerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        String tradeId = tradeService.createTrade(buyerId, request);
        return ApiResponse.success(Map.of("tradeId", tradeId));
    }

    @GetMapping("/buying")
    public ApiResponse<List<TradeListResponse>> listBuyingTrades(
            @RequestHeader("Authorization") String authorizationHeader) {

        return ApiResponse.success(tradeService.listBuyingTrades(loginUserHelper.getCurrentUserId(authorizationHeader)));
    }

    @GetMapping("/selling")
    public ApiResponse<List<TradeListResponse>> listSellingTrades(
            @RequestHeader("Authorization") String authorizationHeader) {

        return ApiResponse.success(tradeService.listSellingTrades(loginUserHelper.getCurrentUserId(authorizationHeader)));
    }

    @GetMapping("/{tradeId}")
    public ApiResponse<TradeDetailResponse> getTradeDetail(@PathVariable String tradeId) {
        return ApiResponse.success(tradeService.getTradeDetail(tradeId));
    }

    @PostMapping("/{tradeId}/cancel")
    public ApiResponse<Void> cancelTrade(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String tradeId) {

        tradeService.cancelTrade(loginUserHelper.getCurrentUserId(authorizationHeader), tradeId);
        return ApiResponse.success();
    }

    @PostMapping("/{tradeId}/complete")
    public ApiResponse<Void> completeTrade(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String tradeId) {

        tradeService.completeTrade(loginUserHelper.getCurrentUserId(authorizationHeader), tradeId);
        return ApiResponse.success();
    }
}
