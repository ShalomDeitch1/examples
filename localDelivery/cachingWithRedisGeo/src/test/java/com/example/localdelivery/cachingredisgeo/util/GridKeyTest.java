package com.example.localdelivery.cachingredisgeo.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GridKeyTest {

    @Test
    void testComputeGridKey() {
        // Test NYC coordinates
        String gridId1 = GridKey.compute(40.7128, -74.0060);
        assertNotNull(gridId1);
        assertTrue(gridId1.contains(":"));

        // Same location should produce same grid ID
        String gridId2 = GridKey.compute(40.7128, -74.0060);
        assertEquals(gridId1, gridId2);
    }
    
    @Test
    void testDifferentGrids() {
        // These locations are far apart and should be in different grids
        String nycGrid = GridKey.compute(40.7128, -74.0060);
        String laGrid = GridKey.compute(34.0522, -118.2437);
        assertNotEquals(nycGrid, laGrid);
    }
    
    @Test
    void testGetGridCenter() {
        String gridId = GridKey.compute(40.7128, -74.0060);
        double[] center = GridKey.getGridCenter(gridId);
        
        assertNotNull(center);
        assertEquals(2, center.length);
        
        // Center should be reasonably close to original coordinates
        assertTrue(Math.abs(center[0] - 40.7128) < 0.5);
        assertTrue(Math.abs(center[1] - (-74.0060)) < 0.5);
    }
}
