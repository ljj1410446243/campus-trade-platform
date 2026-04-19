package com.campus.trade.item.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationSearchResponse {

    private List<LocationDTO> locations = new ArrayList<>();
    private List<LocationDTO> list = new ArrayList<>();

    public LocationSearchResponse() {
    }

    public LocationSearchResponse(List<LocationDTO> locations) {
        setLocations(locations);
    }

    public List<LocationDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationDTO> locations) {
        List<LocationDTO> normalizedLocations = locations == null ? new ArrayList<>() : locations;
        this.locations = normalizedLocations;
        this.list = normalizedLocations;
    }

    public List<LocationDTO> getList() {
        return list;
    }

    public void setList(List<LocationDTO> list) {
        List<LocationDTO> normalizedList = list == null ? new ArrayList<>() : list;
        this.list = normalizedList;
        this.locations = normalizedList;
    }
}
