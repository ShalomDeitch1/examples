package com.example.localdelivery.cachingredisgeo.service;

import org.springframework.stereotype.Service;

@Service
public class TravelTimeService {
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double AVG_SPEED_MPS = 10.0; // ~36 km/h average urban speed

    public int estimateTravelTime(double fromLat, double fromLon, double toLat, double toLon) {
        double metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians((fromLat + toLat) / 2));
        
        double deltaLat = toLat - fromLat;
        double deltaLon = toLon - fromLon;
        
        double distanceMeters = Math.sqrt(
            Math.pow(deltaLat * METERS_PER_DEGREE_LAT, 2) + 
            Math.pow(deltaLon * metersPerDegreeLon, 2)
        );
        
        return (int) Math.ceil(distanceMeters / AVG_SPEED_MPS);
    }
}
