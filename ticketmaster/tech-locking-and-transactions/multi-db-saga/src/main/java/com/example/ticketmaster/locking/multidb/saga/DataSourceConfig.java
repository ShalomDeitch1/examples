package com.example.ticketmaster.locking.multidb.saga;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataSourceConfig {

    @Bean("inventoryDataSource")
    DataSource inventoryDataSource(
            @Value("${app.inventory.jdbcUrl}") String jdbcUrl,
            @Value("${app.inventory.username}") String username,
            @Value("${app.inventory.password}") String password
    ) {
        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }

    @Bean("paymentsDataSource")
    DataSource paymentsDataSource(
            @Value("${app.payments.jdbcUrl}") String jdbcUrl,
            @Value("${app.payments.username}") String username,
            @Value("${app.payments.password}") String password
    ) {
        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }

    @Bean("inventoryJdbc")
    JdbcTemplate inventoryJdbc(@Qualifier("inventoryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean("paymentsJdbc")
    JdbcTemplate paymentsJdbc(@Qualifier("paymentsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
