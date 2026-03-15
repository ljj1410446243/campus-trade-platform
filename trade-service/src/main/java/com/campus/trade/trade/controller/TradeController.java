package com.campus.trade.trade.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;
import com.campus.trade.trade.service.TradeService;
import com.campus.trade.trade.util.JwtUtil;
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
    private final JwtUtil jwtUtil;

    public TradeController(TradeService tradeService, JwtUtil jwtUtil) {
        this.tradeService = tradeService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success(tradeService.ping());
    }

    @PostMapping
    public ApiResponse<Map<String, String>> createTrade(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateTradeRequest request) {

        String token = authorizationHeader.replace("Bearer ", "");
        String buyerId = jwtUtil.getUserId(token);

        String tradeId = tradeService.createTrade(buyerId, request);
        return ApiResponse.success(Map.of("tradeId", tradeId));
    }

    @GetMapping("/buying")
    public ApiResponse<List<TradeListResponse>> listBuyingTrades(
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.replace("Bearer ", "");
        String buyerId = jwtUtil.getUserId(token);

        return ApiResponse.success(tradeService.listBuyingTrades(buyerId));
    }

    @GetMapping("/selling")
    public ApiResponse<List<TradeListResponse>> listSellingTrades(
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.replace("Bearer ", "");
        String sellerId = jwtUtil.getUserId(token);

        return ApiResponse.success(tradeService.listSellingTrades(sellerId));
    }

    @GetMapping("/{tradeId}")
    public ApiResponse<TradeDetailResponse> getTradeDetail(@PathVariable String tradeId) {
        return ApiResponse.success(tradeService.getTradeDetail(tradeId));
    }

    @PostMapping("/{tradeId}/cancel")
    public ApiResponse<Void> cancelTrade(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String tradeId) {

        String token = authorizationHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        tradeService.cancelTrade(userId, tradeId);

        return ApiResponse.success();
    }

    @PostMapping("/{tradeId}/complete")
    public ApiResponse<Void> completeTrade(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String tradeId) {

        String token = authorizationHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        tradeService.completeTrade(userId, tradeId);

        return ApiResponse.success();
    }
}
