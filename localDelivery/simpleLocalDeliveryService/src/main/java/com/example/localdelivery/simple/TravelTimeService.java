package com.example.localdelivery.simple;

import org.springframework.stereotype.Service;

@Service
public class TravelTimeService {
    // ~36km/h average urban speed.
    private static final double AVG_SPEED_MPS = 10.0;

    public int estimateSeconds(double fromLat, double fromLon, double toLat, double toLon) {
        double meters = GeoUtils.haversineMeters(fromLat, fromLon, toLat, toLon);
        return (int) Math.ceil(meters / AVG_SPEED_MPS);
    }
}
