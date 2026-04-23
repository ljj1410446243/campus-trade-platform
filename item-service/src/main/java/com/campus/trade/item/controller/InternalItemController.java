package com.campus.trade.item.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.item.dto.InternalNegotiatedPriceRequest;
import com.campus.trade.item.service.InternalItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/items")
public class InternalItemController {

    private final InternalItemService internalItemService;

    public InternalItemController(InternalItemService internalItemService) {
        this.internalItemService = internalItemService;
    }

    @PostMapping("/{itemId}/negotiated-price")
    public ApiResponse<Void> updateNegotiatedPrice(@PathVariable String itemId,
                                                   @Valid @RequestBody InternalNegotiatedPriceRequest request) {
        internalItemService.updateNegotiatedPrice(itemId, request);
        return ApiResponse.success();
    }
}
