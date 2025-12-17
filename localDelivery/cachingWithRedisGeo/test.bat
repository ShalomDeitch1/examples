@echo off
REM Test script for cachingWithRedisGeo (Windows)

echo Testing cachingWithRedisGeo API...
echo.

REM Test coordinates (New York City area)
set LAT=40.7128
set LON=-74.0060

echo 1. Testing GET /items?lat=%LAT%^&lon=%LON%
curl -s "http://localhost:8093/items?lat=%LAT%&lon=%LON%"
echo.

echo 2. Testing different location
set LAT2=40.7580
set LON2=-73.9855
curl -s "http://localhost:8093/items?lat=%LAT2%&lon=%LON2%"
echo.

echo Test complete!
