package com.example.localdelivery.cachingredisgeo.util;

public class GridKey {
    private static final int GRID_SIZE_METERS = 1000; // 1km grid
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    public static String compute(double latitude, double longitude) {
        double metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude));
        
        int gridX = (int) Math.floor(longitude * metersPerDegreeLon / GRID_SIZE_METERS);
        int gridY = (int) Math.floor(latitude * METERS_PER_DEGREE_LAT / GRID_SIZE_METERS);
        
        return String.format("%d:%d", gridX, gridY);
    }

    public static double[] getGridCenter(String gridId) {
        String[] parts = gridId.split(":");
        int gridX = Integer.parseInt(parts[0]);
        int gridY = Integer.parseInt(parts[1]);
        
        double centerLat = (gridY + 0.5) * GRID_SIZE_METERS / METERS_PER_DEGREE_LAT;
        double metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(centerLat));
        double centerLon = (gridX + 0.5) * GRID_SIZE_METERS / metersPerDegreeLon;
        
        return new double[]{centerLat, centerLon};
    }
}
