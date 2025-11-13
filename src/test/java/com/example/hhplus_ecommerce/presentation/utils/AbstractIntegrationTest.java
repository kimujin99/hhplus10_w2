package com.example.hhplus_ecommerce.presentation.utils;

import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.io.IOException;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // Singleton 패턴으로 모든 테스트가 하나의 컨테이너 인스턴스를 공유
    protected static final MySQLContainer<?> container;

    static {
        container = new MySQLContainer<>("mysql:8.0.33")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("schema.sql")
                .withCommand(
                        "--slow_query_log=1",
                        "--slow_query_log_file=/var/lib/mysql/slow.log",
                        "--long_query_time=1"
                );
        container.start();
    }

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @AfterAll
    static void afterAll() throws IOException, InterruptedException {
        if (container.isRunning()) {
            container.copyFileFromContainer("/var/lib/mysql/slow.log", "build/slow-query.log");
        }
    }
}