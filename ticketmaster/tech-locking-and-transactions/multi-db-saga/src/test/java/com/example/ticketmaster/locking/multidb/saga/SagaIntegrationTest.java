package com.example.ticketmaster.locking.multidb.saga;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(classes = {MultiDbSagaApplication.class, SagaIntegrationTest.TestConfig.class})
class SagaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> inventoryDb = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @Container
    static final PostgreSQLContainer<?> paymentsDb = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.inventory.jdbcUrl", inventoryDb::getJdbcUrl);
        registry.add("app.inventory.username", inventoryDb::getUsername);
        registry.add("app.inventory.password", inventoryDb::getPassword);

        registry.add("app.payments.jdbcUrl", paymentsDb::getJdbcUrl);
        registry.add("app.payments.username", paymentsDb::getUsername);
        registry.add("app.payments.password", paymentsDb::getPassword);
    }

    @Autowired
    @Qualifier("inventoryJdbc") org.springframework.jdbc.core.JdbcTemplate inventoryJdbc;

    @Autowired
    @Qualifier("paymentsJdbc") org.springframework.jdbc.core.JdbcTemplate paymentsJdbc;

    @Autowired
    SagaOrchestrator orchestrator;

    @BeforeEach
    void schemaAndSeed() {
        inventoryJdbc.execute("DROP TABLE IF EXISTS seats");
        inventoryJdbc.execute("CREATE TABLE seats (seat_id TEXT PRIMARY KEY, status TEXT NOT NULL, reserved_by UUID NULL)");
        inventoryJdbc.update("INSERT INTO seats (seat_id, status) VALUES ('s1', 'AVAILABLE')");

        paymentsJdbc.execute("DROP TABLE IF EXISTS payments");
        paymentsJdbc.execute("CREATE TABLE payments (saga_id UUID PRIMARY KEY, amount_cents INT NOT NULL, status TEXT NOT NULL)");
    }

    @Test
    void happy_path_confirms_seat_and_authorizes_payment() {
        UUID sagaId = UUID.randomUUID();

        SagaOrchestrator.SagaResult result = orchestrator.purchase(sagaId, "s1", 5000);

        assertThat(result.success()).isTrue();
        assertThat(result.seatStatus()).isEqualTo("CONFIRMED");
        assertThat(result.paymentStatus()).isEqualTo("AUTHORIZED");
    }

    @Test
    void payment_decline_compensates_by_releasing_reservation() {
        UUID sagaId = UUID.randomUUID();

        // our fake gateway declines amount == 13
        SagaOrchestrator.SagaResult result = orchestrator.purchase(sagaId, "s1", 13);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("PAYMENT_DECLINED");
        assertThat(result.seatStatus()).isEqualTo("AVAILABLE");
        assertThat(result.paymentStatus()).isEqualTo("DECLINED");
    }

    @Test
    void retry_with_same_saga_id_is_idempotent() {
        UUID sagaId = UUID.randomUUID();

        SagaOrchestrator.SagaResult first = orchestrator.purchase(sagaId, "s1", 5000);
        SagaOrchestrator.SagaResult second = orchestrator.purchase(sagaId, "s1", 5000);

        assertThat(first.success()).isTrue();
        assertThat(second.success()).isTrue();
        assertThat(second.seatStatus()).isEqualTo("CONFIRMED");
        assertThat(second.paymentStatus()).isEqualTo("AUTHORIZED");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        PaymentGateway paymentGateway() {
            return (sagaId, amountCents) -> amountCents != 13;
        }
    }
}
