package com.campus.trade.trade.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.trade.dto.PickupPointRequest;
import com.campus.trade.trade.dto.PickupPointResponse;
import com.campus.trade.trade.service.PickupPointService;
import com.campus.trade.trade.util.LoginUserHelper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PickupPointController {

    private final PickupPointService pickupPointService;
    private final LoginUserHelper loginUserHelper;

    public PickupPointController(PickupPointService pickupPointService, LoginUserHelper loginUserHelper) {
        this.pickupPointService = pickupPointService;
        this.loginUserHelper = loginUserHelper;
    }

    @GetMapping({"/pickup-points", "/pickup-points/"})
    public ApiResponse<List<PickupPointResponse>> listPickupPoints() {
        return ApiResponse.success(pickupPointService.listEnabledPickupPoints());
    }

    @GetMapping({"/admin/pickup-points", "/admin/pickup-points/"})
    public ApiResponse<List<PickupPointResponse>> listAdminPickupPoints(
            @RequestHeader("Authorization") String authorizationHeader) {

        return ApiResponse.success(
                pickupPointService.listAdminPickupPoints(loginUserHelper.getCurrentUserId(authorizationHeader))
        );
    }

    @PostMapping({"/admin/pickup-points", "/admin/pickup-points/"})
    public ApiResponse<PickupPointResponse> createPickupPoint(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody PickupPointRequest request) {

        return ApiResponse.success(
                pickupPointService.createPickupPoint(loginUserHelper.getCurrentUserId(authorizationHeader), request)
        );
    }

    @PutMapping({"/admin/pickup-points/{pickupPointId}", "/admin/pickup-points/{pickupPointId}/"})
    public ApiResponse<PickupPointResponse> updatePickupPoint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String pickupPointId,
            @Valid @RequestBody PickupPointRequest request) {

        return ApiResponse.success(
                pickupPointService.updatePickupPoint(loginUserHelper.getCurrentUserId(authorizationHeader), pickupPointId, request)
        );
    }

    @PostMapping({"/admin/pickup-points/{pickupPointId}/enable", "/admin/pickup-points/{pickupPointId}/enable/"})
    public ApiResponse<Void> enablePickupPoint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String pickupPointId) {

        pickupPointService.enablePickupPoint(loginUserHelper.getCurrentUserId(authorizationHeader), pickupPointId);
        return ApiResponse.success();
    }

    @PostMapping({"/admin/pickup-points/{pickupPointId}/disable", "/admin/pickup-points/{pickupPointId}/disable/"})
    public ApiResponse<Void> disablePickupPoint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String pickupPointId) {

        pickupPointService.disablePickupPoint(loginUserHelper.getCurrentUserId(authorizationHeader), pickupPointId);
        return ApiResponse.success();
    }

    @DeleteMapping({"/admin/pickup-points/{pickupPointId}", "/admin/pickup-points/{pickupPointId}/"})
    public ApiResponse<Void> deletePickupPoint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String pickupPointId) {

        pickupPointService.deletePickupPoint(loginUserHelper.getCurrentUserId(authorizationHeader), pickupPointId);
        return ApiResponse.success();
    }
}
