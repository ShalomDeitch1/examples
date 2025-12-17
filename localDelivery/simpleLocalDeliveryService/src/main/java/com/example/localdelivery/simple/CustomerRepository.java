package com.example.localdelivery.simple;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Customer> findById(UUID customerId) {
        return jdbcTemplate.query(
                "SELECT customer_id, name, latitude, longitude FROM customers WHERE customer_id = ?",
                (rs, rowNum) -> new Customer(
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                ),
                customerId
        ).stream().findFirst();
    }
}
