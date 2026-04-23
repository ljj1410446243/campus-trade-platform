package com.campus.trade.trade.service.impl;

import com.campus.trade.trade.dto.PickupPointRequest;
import com.campus.trade.trade.dto.PickupPointResponse;
import com.campus.trade.trade.exception.BusinessException;
import com.campus.trade.trade.model.PickupPoint;
import com.campus.trade.trade.repository.PickupPointRepository;
import com.campus.trade.trade.service.PickupPointService;
import com.campus.trade.trade.util.UserAccessGuard;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PickupPointServiceImpl implements PickupPointService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final PickupPointRepository pickupPointRepository;
    private final UserAccessGuard userAccessGuard;

    public PickupPointServiceImpl(PickupPointRepository pickupPointRepository, UserAccessGuard userAccessGuard) {
        this.pickupPointRepository = pickupPointRepository;
        this.userAccessGuard = userAccessGuard;
    }

    @Override
    @Cacheable(cacheNames = "pickup:list", key = "'enabled'")
    public List<PickupPointResponse> listEnabledPickupPoints() {
        return pickupPointRepository.findByStatusOrderBySortOrderAscCreatedAtDesc(STATUS_ENABLED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PickupPointResponse> listAdminPickupPoints(String adminUserId) {
        userAccessGuard.assertAdmin(adminUserId);
        return pickupPointRepository.findAllByOrderBySortOrderAscCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = "pickup:list", allEntries = true)
    public PickupPointResponse createPickupPoint(String adminUserId, PickupPointRequest request) {
        userAccessGuard.assertAdmin(adminUserId);
        Date now = new Date();
        PickupPoint pickupPoint = new PickupPoint();
        applyRequest(pickupPoint, request);
        pickupPoint.setStatus(STATUS_ENABLED);
        pickupPoint.setCreatedAt(now);
        pickupPoint.setUpdatedAt(now);
        return toResponse(pickupPointRepository.save(pickupPoint));
    }

    @Override
    @CacheEvict(cacheNames = "pickup:list", allEntries = true)
    public PickupPointResponse updatePickupPoint(String adminUserId, String pickupPointId, PickupPointRequest request) {
        userAccessGuard.assertAdmin(adminUserId);
        PickupPoint pickupPoint = getPickupPointOrThrow(pickupPointId);
        applyRequest(pickupPoint, request);
        pickupPoint.setUpdatedAt(new Date());
        return toResponse(pickupPointRepository.save(pickupPoint));
    }

    @Override
    @CacheEvict(cacheNames = "pickup:list", allEntries = true)
    public void enablePickupPoint(String adminUserId, String pickupPointId) {
        userAccessGuard.assertAdmin(adminUserId);
        updateStatus(pickupPointId, STATUS_ENABLED);
    }

    @Override
    @CacheEvict(cacheNames = "pickup:list", allEntries = true)
    public void disablePickupPoint(String adminUserId, String pickupPointId) {
        userAccessGuard.assertAdmin(adminUserId);
        updateStatus(pickupPointId, STATUS_DISABLED);
    }

    @Override
    @CacheEvict(cacheNames = "pickup:list", allEntries = true)
    public void deletePickupPoint(String adminUserId, String pickupPointId) {
        userAccessGuard.assertAdmin(adminUserId);
        pickupPointRepository.delete(getPickupPointOrThrow(pickupPointId));
    }

    @Override
    public PickupPoint requireEnabledPickupPoint(String pickupPointId) {
        PickupPoint pickupPoint = getPickupPointOrThrow(pickupPointId);
        if (!STATUS_ENABLED.equalsIgnoreCase(pickupPoint.getStatus())) {
            throw new BusinessException(409, "自提点不可用");
        }
        return pickupPoint;
    }

    private void updateStatus(String pickupPointId, String status) {
        PickupPoint pickupPoint = getPickupPointOrThrow(pickupPointId);
        pickupPoint.setStatus(status);
        pickupPoint.setUpdatedAt(new Date());
        pickupPointRepository.save(pickupPoint);
    }

    private PickupPoint getPickupPointOrThrow(String pickupPointId) {
        return pickupPointRepository.findById(pickupPointId)
                .orElseThrow(() -> new BusinessException(404, "自提点不存在"));
    }

    private void applyRequest(PickupPoint pickupPoint, PickupPointRequest request) {
        pickupPoint.setName(request.getName().trim());
        pickupPoint.setCampusName(request.getCampusName().trim());
        pickupPoint.setAddress(request.getAddress().trim());
        pickupPoint.setLat(request.getLat());
        pickupPoint.setLng(request.getLng());
        pickupPoint.setContactPhone(trimToNull(request.getContactPhone()));
        pickupPoint.setBusinessHours(trimToNull(request.getBusinessHours()));
        pickupPoint.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PickupPointResponse toResponse(PickupPoint pickupPoint) {
        PickupPointResponse response = new PickupPointResponse();
        response.setPickupPointId(pickupPoint.getId());
        response.setName(pickupPoint.getName());
        response.setCampusName(pickupPoint.getCampusName());
        response.setAddress(pickupPoint.getAddress());
        response.setLat(pickupPoint.getLat());
        response.setLng(pickupPoint.getLng());
        response.setContactPhone(pickupPoint.getContactPhone());
        response.setBusinessHours(pickupPoint.getBusinessHours());
        response.setStatus(pickupPoint.getStatus());
        response.setSortOrder(pickupPoint.getSortOrder());
        response.setCreatedAt(pickupPoint.getCreatedAt());
        response.setUpdatedAt(pickupPoint.getUpdatedAt());
        return response;
    }
}
