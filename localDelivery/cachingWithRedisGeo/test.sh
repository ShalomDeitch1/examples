#!/bin/bash

# Test script for cachingWithRedisGeo

echo "Testing cachingWithRedisGeo API..."
echo ""

# Test coordinates (New York City area)
LAT=40.7128
LON=-74.0060

echo "1. Testing GET /items?lat=$LAT&lon=$LON"
curl -s "http://localhost:8093/items?lat=$LAT&lon=$LON" | python -m json.tool
echo ""

echo "2. Testing cache hit (should be faster)"
time curl -s "http://localhost:8093/items?lat=$LAT&lon=$LON" > /dev/null
echo ""

echo "3. Testing different location"
LAT2=40.7580
LON2=-73.9855
curl -s "http://localhost:8093/items?lat=$LAT2&lon=$LON2" | python -m json.tool
echo ""

echo "Test complete!"
