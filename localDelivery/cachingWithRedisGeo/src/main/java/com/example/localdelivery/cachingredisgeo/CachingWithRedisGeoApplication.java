package com.example.localdelivery.cachingredisgeo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CachingWithRedisGeoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CachingWithRedisGeoApplication.class, args);
    }
}
