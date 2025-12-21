package com.example.localdelivery.cachewithreplicas.util;
public class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * Calculates the Haversine distance between two points on the Earth specified in decimal degrees.
     * Haversine formula: https://en.wikipedia.org/wiki/Haversine_formula
     */
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private GeoUtils() {}
}
