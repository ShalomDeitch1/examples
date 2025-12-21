package com.example.localdelivery.cachewithreplicas.util;

public class GridKey {
    private static final int GRID_SIZE_METERS = 1000;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    /**
     * Devides the world into a grid of squares of size GRID_SIZE_METERS,
     * and computes a key for the grid cell containing the given latitude and longitude.
     * 
     * This allows for quick spatial indexing of locations.
     */
    public static String compute(double latitude, double longitude) {
        double metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude));

        int gridX = (int) Math.floor(longitude * metersPerDegreeLon / GRID_SIZE_METERS);
        int gridY = (int) Math.floor(latitude * METERS_PER_DEGREE_LAT / GRID_SIZE_METERS);

        return gridX + ":" + gridY;
    }

    private GridKey() {}
}
