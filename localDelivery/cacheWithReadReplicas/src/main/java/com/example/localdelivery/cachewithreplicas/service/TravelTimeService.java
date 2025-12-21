package com.example.localdelivery.cachewithreplicas.service;

import org.springframework.stereotype.Service;

import com.example.localdelivery.cachewithreplicas.util.GeoUtils;

@Service
public class TravelTimeService {
    private static final double AVG_SPEED_MPS = 10.0;

    /**
     * In a real service, this would call out to a proper routing engine.
     * Which would check actual roads, traffic, etc.
     * Here we just do a simple haversine distance / average speed calculation.
     * to give some impression of travel time.
     * 
     *  Haversine formula: https://en.wikipedia.org/wiki/Haversine_formula
     */
    public int estimateSeconds(double fromLat, double fromLon, double toLat, double toLon) {
        return (int) Math.ceil(GeoUtils.haversineMeters(fromLat, fromLon, toLat, toLon) / AVG_SPEED_MPS);
    }
}
