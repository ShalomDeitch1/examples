package com.example.localdelivery.postgresreplicas.service;

import com.example.localdelivery.postgresreplicas.util.GeoUtils;
import org.springframework.stereotype.Service;

@Service
public class TravelTimeService {
    private static final double AVG_SPEED_MPS = 10.0;

    public int estimateSeconds(double fromLat, double fromLon, double toLat, double toLon) {
        return (int) Math.ceil(GeoUtils.haversineMeters(fromLat, fromLon, toLat, toLon) / AVG_SPEED_MPS);
    }
}
