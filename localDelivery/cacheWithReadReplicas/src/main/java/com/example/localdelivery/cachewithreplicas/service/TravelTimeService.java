package com.example.localdelivery.cachewithreplicas.service;

import org.springframework.stereotype.Service;

import com.example.localdelivery.cachewithreplicas.util.GeoUtils;

@Service
public class TravelTimeService {
    private static final double AVG_SPEED_MPS = 10.0;

    public int estimateSeconds(double fromLat, double fromLon, double toLat, double toLon) {
        return (int) Math.ceil(GeoUtils.haversineMeters(fromLat, fromLon, toLat, toLon) / AVG_SPEED_MPS);
    }
}
