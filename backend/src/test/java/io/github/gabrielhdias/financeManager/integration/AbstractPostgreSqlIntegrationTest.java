package io.github.gabrielhdias.financeManager.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgreSqlIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
        new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("finance_manager_test")
            .withUsername("finance_manager")
            .withPassword("finance_manager_test");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(
        DynamicPropertyRegistry registry
    ) {
        registry.add(
            "spring.datasource.url",
            POSTGRESQL_CONTAINER::getJdbcUrl
        );

        registry.add(
            "spring.datasource.username",
            POSTGRESQL_CONTAINER::getUsername
        );

        registry.add(
            "spring.datasource.password",
            POSTGRESQL_CONTAINER::getPassword
        );

        registry.add(
            "security.jwt.secret",
            () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }
}
