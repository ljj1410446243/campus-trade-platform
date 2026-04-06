package com.campus.trade.item.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.item.dto.LocationDTO;
import com.campus.trade.item.dto.LocationSearchResponse;
import com.campus.trade.item.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping({"/reverse-geocode", "/reverse-geocode/"})
    public ApiResponse<LocationDTO> reverseGeocode(@RequestParam Double lat,
                                                   @RequestParam Double lng,
                                                   @RequestParam(defaultValue = "WGS84") String coordType) {
        return ApiResponse.success(locationService.reverseGeocode(lat, lng, coordType));
    }

    @GetMapping({"/search", "/search/"})
    public ApiResponse<LocationSearchResponse> searchLocations(@RequestParam String q,
                                                               @RequestParam(required = false) Double lat,
                                                               @RequestParam(required = false) Double lng) {
        return ApiResponse.success(new LocationSearchResponse(locationService.searchLocations(q, lat, lng)));
    }
}
