package com.campus.trade.item.service;

import com.campus.trade.item.dto.LocationDTO;

import java.util.List;

public interface LocationService {

    LocationDTO reverseGeocode(Double lat, Double lng, String coordType);

    List<LocationDTO> searchLocations(String q, Double lat, Double lng);
}
