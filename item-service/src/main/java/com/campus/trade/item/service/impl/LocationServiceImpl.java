package com.campus.trade.item.service.impl;

import com.campus.trade.item.config.LocationProperties;
import com.campus.trade.item.dto.LocationDTO;
import com.campus.trade.item.exception.BusinessException;
import com.campus.trade.item.service.LocationService;
import com.campus.trade.item.util.CoordinateTransformUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class LocationServiceImpl implements LocationService {

    private static final String COORD_TYPE_WGS84 = "WGS84";
    private static final String COORD_TYPE_GCJ02 = "GCJ02";
    private static final String SOURCE_DEVICE = "device";
    private static final String SOURCE_SEARCH = "search";

    private final RestClient restClient;
    private final LocationProperties locationProperties;

    public LocationServiceImpl(RestClient restClient, LocationProperties locationProperties) {
        this.restClient = restClient;
        this.locationProperties = locationProperties;
    }

    @Override
    public LocationDTO reverseGeocode(Double lat, Double lng, String coordType) {
        validateCoordinates(lat, lng, "定位坐标");
        String normalizedCoordType = normalizeCoordType(coordType, COORD_TYPE_WGS84);
        double[] gcjCoordinates = convertToGcj02(lat, lng, normalizedCoordType);

        try {
            JsonNode body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/geocode/regeo")
                            .queryParam("key", requireAmapKey())
                            .queryParam("location", gcjCoordinates[1] + "," + gcjCoordinates[0])
                            .queryParam("extensions", "base")
                            .queryParam("radius", 1000)
                            .queryParam("roadlevel", 0)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            ensureAmapSuccess(body, "逆地理解析");

            JsonNode regeocode = body.path("regeocode");
            if (regeocode.isMissingNode() || regeocode.isNull()) {
                throw new BusinessException(502, "高德逆地理解析返回为空");
            }

            LocationDTO response = new LocationDTO();
            response.setLat(gcjCoordinates[0]);
            response.setLng(gcjCoordinates[1]);
            response.setAddress(readText(regeocode, "formatted_address"));
            response.setPoiName(resolvePoiName(regeocode));
            response.setCoordType(COORD_TYPE_GCJ02);
            response.setSource(SOURCE_DEVICE);
            return response;
        } catch (RestClientException e) {
            throw new BusinessException(502, "高德逆地理解析调用失败");
        }
    }

    @Override
    public List<LocationDTO> searchLocations(String q, Double lat, Double lng) {
        if (q == null || q.isBlank()) {
            throw new BusinessException("搜索关键词不能为空");
        }

        boolean hasLat = lat != null;
        boolean hasLng = lng != null;
        if (hasLat != hasLng) {
            throw new BusinessException("搜索定位坐标必须同时提供");
        }

        final String locationValue;
        if (hasLat) {
            validateCoordinates(lat, lng, "搜索定位坐标");
            double[] gcjCoordinates = convertToGcj02(lat, lng, COORD_TYPE_WGS84);
            locationValue = gcjCoordinates[1] + "," + gcjCoordinates[0];
        } else {
            locationValue = null;
        }

        try {
            JsonNode body = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/v3/assistant/inputtips")
                                .queryParam("key", requireAmapKey())
                                .queryParam("keywords", q.trim())
                                .queryParam("datatype", "poi");
                        if (locationValue != null) {
                            builder.queryParam("location", locationValue);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);

            ensureAmapSuccess(body, "地点搜索");

            List<LocationDTO> locations = new ArrayList<>();
            JsonNode tips = body.path("tips");
            if (!tips.isArray()) {
                return locations;
            }

            for (JsonNode tip : tips) {
                String location = readText(tip, "location");
                if (location == null || location.isBlank() || !location.contains(",")) {
                    continue;
                }

                String[] parts = location.split(",");
                if (parts.length != 2) {
                    continue;
                }

                Double tipLng = parseDouble(parts[0]);
                Double tipLat = parseDouble(parts[1]);
                if (tipLat == null || tipLng == null) {
                    continue;
                }

                LocationDTO item = new LocationDTO();
                item.setLat(tipLat);
                item.setLng(tipLng);
                item.setPoiName(readText(tip, "name"));
                item.setAddress(readText(tip, "address"));
                item.setCoordType(COORD_TYPE_GCJ02);
                item.setSource(SOURCE_SEARCH);
                locations.add(item);
            }

            return locations;
        } catch (RestClientException e) {
            throw new BusinessException(502, "高德地点搜索调用失败");
        }
    }

    private String requireAmapKey() {
        String key = locationProperties.getAmap().getKey();
        if (key == null || key.isBlank() || key.toUpperCase(Locale.ROOT).contains("YOUR_AMAP")) {
            throw new BusinessException(500, "高德地图Key未配置");
        }
        return key;
    }

    private void ensureAmapSuccess(JsonNode body, String action) {
        if (body == null) {
            throw new BusinessException(502, "高德" + action + "返回为空");
        }

        String status = readText(body, "status");
        if (!"1".equals(status)) {
            String info = readText(body, "info");
            if (info == null) {
                info = "未知错误";
            }
            throw new BusinessException(502, "高德" + action + "失败: " + info);
        }
    }

    private String resolvePoiName(JsonNode regeocode) {
        JsonNode pois = regeocode.path("pois");
        if (pois.isArray() && !pois.isEmpty()) {
            String poiName = readText(pois.get(0), "name");
            if (poiName != null && !poiName.isBlank()) {
                return poiName;
            }
        }

        JsonNode aois = regeocode.path("aois");
        if (aois.isArray() && !aois.isEmpty()) {
            String poiName = readText(aois.get(0), "name");
            if (poiName != null && !poiName.isBlank()) {
                return poiName;
            }
        }

        return null;
    }

    private double[] convertToGcj02(Double lat, Double lng, String coordType) {
        if (COORD_TYPE_WGS84.equals(coordType)) {
            return CoordinateTransformUtil.wgs84ToGcj02(lat, lng);
        }
        return new double[]{lat, lng};
    }

    private String normalizeCoordType(String coordType, String defaultCoordType) {
        if (coordType == null || coordType.isBlank()) {
            return defaultCoordType;
        }

        String normalized = coordType.trim().toUpperCase(Locale.ROOT);
        if (!COORD_TYPE_WGS84.equals(normalized) && !COORD_TYPE_GCJ02.equals(normalized)) {
            throw new BusinessException("不支持的坐标类型: " + coordType);
        }
        return normalized;
    }

    private void validateCoordinates(Double lat, Double lng, String fieldName) {
        if (lat == null || lng == null) {
            throw new BusinessException(fieldName + "不能为空");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new BusinessException(fieldName + "超出合法范围");
        }
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return null;
        }
        String value = field.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
