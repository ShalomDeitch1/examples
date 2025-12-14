package com.example.chunkS3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChunkS3Application {
    public static void main(String[] args) {
        SpringApplication.run(ChunkS3Application.class, args);
    }
}
