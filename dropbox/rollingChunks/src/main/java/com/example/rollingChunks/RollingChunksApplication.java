package com.example.rollingChunks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RollingChunksApplication {
    public static void main(String[] args) {
        SpringApplication.run(RollingChunksApplication.class, args);
    }
}
