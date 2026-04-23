package com.campus.trade.item.service;

import com.campus.trade.item.dto.InternalNegotiatedPriceRequest;

public interface InternalItemService {

    void updateNegotiatedPrice(String itemId, InternalNegotiatedPriceRequest request);
}
