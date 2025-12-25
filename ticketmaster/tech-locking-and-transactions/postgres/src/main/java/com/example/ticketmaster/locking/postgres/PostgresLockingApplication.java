package com.example.ticketmaster.locking.postgres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PostgresLockingApplication {
    public static void main(String[] args) {
        SpringApplication.run(PostgresLockingApplication.class, args);
    }
}
