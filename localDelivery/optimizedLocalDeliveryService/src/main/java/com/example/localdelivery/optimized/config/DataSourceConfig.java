package com.example.localdelivery.optimized.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableConfigurationProperties(DualDataSourceProperties.class)
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource primaryDataSource(DualDataSourceProperties props) {
        return build(props.getPrimary());
    }

    @Bean
    public DataSource replicaDataSource(DualDataSourceProperties props) {
        return build(props.getReplica());
    }

    @Bean
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate replicaJdbcTemplate(@Qualifier("replicaDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    @Primary
    public DataSourceTransactionManager primaryTransactionManager(@Qualifier("primaryDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public DataSourceTransactionManager replicaTransactionManager(@Qualifier("replicaDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    private DataSource build(DualDataSourceProperties.DbProperties p) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(p.getUrl());
        ds.setUsername(p.getUsername());
        ds.setPassword(p.getPassword());
        return ds;
    }
}
