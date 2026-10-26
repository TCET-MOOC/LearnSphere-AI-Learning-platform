package com.MOOC.OnlineLearningPlatfrom.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real PostgreSQL database.
 *
 * <p>Every test class gets a throw-away container. It never reads the
 * developer's local {@code MOOC} database, so a clean checkout only needs
 * Docker.</p>
 */
@Testcontainers
public abstract class PostgresIntegrationTest {

    protected static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("MOOC")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
