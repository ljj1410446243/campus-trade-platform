package com.campus.trade.item.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationSearchResponse {

    private List<LocationDTO> locations = new ArrayList<>();

    public LocationSearchResponse() {
    }

    public LocationSearchResponse(List<LocationDTO> locations) {
        this.locations = locations;
    }

    public List<LocationDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationDTO> locations) {
        this.locations = locations;
    }
}
