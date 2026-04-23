package com.campus.trade.trade.service;

import com.campus.trade.trade.dto.PickupPointRequest;
import com.campus.trade.trade.dto.PickupPointResponse;
import com.campus.trade.trade.model.PickupPoint;

import java.util.List;

public interface PickupPointService {

    List<PickupPointResponse> listEnabledPickupPoints();

    List<PickupPointResponse> listAdminPickupPoints(String adminUserId);

    PickupPointResponse createPickupPoint(String adminUserId, PickupPointRequest request);

    PickupPointResponse updatePickupPoint(String adminUserId, String pickupPointId, PickupPointRequest request);

    void enablePickupPoint(String adminUserId, String pickupPointId);

    void disablePickupPoint(String adminUserId, String pickupPointId);

    void deletePickupPoint(String adminUserId, String pickupPointId);

    PickupPoint requireEnabledPickupPoint(String pickupPointId);
}
