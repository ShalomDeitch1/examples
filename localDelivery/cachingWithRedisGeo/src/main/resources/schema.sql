-- Initialize schema if needed
CREATE EXTENSION IF NOT EXISTS postgis;

-- This project uses Redis GEO for warehouse locations
-- and in-memory data for simplicity, but you can extend 
-- to use PostgreSQL for persistent storage
