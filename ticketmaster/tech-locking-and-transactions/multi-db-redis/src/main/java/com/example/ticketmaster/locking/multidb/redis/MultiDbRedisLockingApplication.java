package com.example.ticketmaster.locking.multidb.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import redis.clients.jedis.JedisPool;

@SpringBootApplication
public class MultiDbRedisLockingApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiDbRedisLockingApplication.class, args);
    }

    @Bean
    JedisPool jedisPool(
            @Value("${app.redis.host:localhost}") String host,
            @Value("${app.redis.port:6379}") int port
    ) {
        return new JedisPool(host, port);
    }
}
