package com.example.localdelivery.postgresgeo;

/**
 * A simple 1km x 1km grid ID.
 *
 * This is not a true projection; it is a lightweight approximation that's fine for
 * demo purposes and for generating a stable "prefix-like" key.
 */
public class GridKey {
    private static final int GRID_SIZE_METERS = 1000;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    public static String compute(double latitude, double longitude) {
        double metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude));

        int gridX = (int) Math.floor(longitude * metersPerDegreeLon / GRID_SIZE_METERS);
        int gridY = (int) Math.floor(latitude * METERS_PER_DEGREE_LAT / GRID_SIZE_METERS);

        return gridX + ":" + gridY;
    }

    public static String[] neighborsIncludingSelf(String gridId) {
        String[] parts = gridId.split(":");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);

        String[] out = new String[9];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                out[i++] = (x + dx) + ":" + (y + dy);
            }
        }
        return out;
    }

    private GridKey() {}
}
