package com.campus.trade.trade.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.trade.dto.PaymentCallbackRequest;
import com.campus.trade.trade.dto.PaymentStatusResponse;
import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.service.TradeService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/callbacks")
public class PaymentCallbackController {

    private final TradeService tradeService;

    public PaymentCallbackController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/{channel}")
    public ApiResponse<PaymentStatusResponse> handleCallback(
            @PathVariable PayChannel channel,
            @RequestBody PaymentCallbackRequest request) {

        request.setChannel(channel);
        return ApiResponse.success(tradeService.handlePaymentCallback(request));
    }
}
