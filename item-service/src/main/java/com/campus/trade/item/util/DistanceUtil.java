package com.campus.trade.item.util;

public final class DistanceUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000D;

    private DistanceUtil() {
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double sinLat = Math.sin(latDistance / 2);
        double sinLng = Math.sin(lngDistance / 2);
        double a = sinLat * sinLat
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sinLng * sinLng;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
