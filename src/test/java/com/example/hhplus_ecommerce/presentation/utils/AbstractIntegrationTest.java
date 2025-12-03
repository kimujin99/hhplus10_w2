package com.example.hhplus_ecommerce.presentation.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.io.IOException;
import java.util.Set;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

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

    /**
     * 각 테스트 메서드 실행 후 DB와 Redis 데이터 정리
     * <p>
     * @Transactional을 사용하지 않는 통합 테스트를 위해
     * 테스트 격리를 보장하기 위한 cleanup 로직입니다.
     */
    @AfterEach
    void cleanupAfterEach() {
        // Redis 데이터 정리 (분산 락 키 등)
        cleanupRedis();

        // DB 데이터 정리
        cleanupDatabase();
    }

    /**
     * Redis의 모든 키 삭제
     * <p>
     * 분산 락 키, 캐시 데이터 등을 정리합니다.
     * RedisTemplate이 없으면 스킵합니다.
     */
    private void cleanupRedis() {
        if (redisTemplate == null) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // Redis 연결 실패 시 무시 (로컬 환경에서 Redis 없을 수 있음)
        }
    }

    /**
     * DB의 모든 테이블 데이터 삭제
     * <p>
     * 각 테스트의 독립성을 보장하기 위해 모든 테이블을 초기화합니다.
     */
    private void cleanupDatabase() {
        // 모든 테이블 데이터 삭제
        jdbcTemplate.execute("TRUNCATE TABLE point_history");
        jdbcTemplate.execute("TRUNCATE TABLE order_item");
        jdbcTemplate.execute("TRUNCATE TABLE `order_table`");
        jdbcTemplate.execute("TRUNCATE TABLE cart_item");
        jdbcTemplate.execute("TRUNCATE TABLE user_coupon");
        jdbcTemplate.execute("TRUNCATE TABLE coupon");
        jdbcTemplate.execute("TRUNCATE TABLE product");
        jdbcTemplate.execute("TRUNCATE TABLE user");
    }

    @AfterAll
    static void afterAll() throws IOException, InterruptedException {
        if (container.isRunning()) {
            container.copyFileFromContainer("/var/lib/mysql/slow.log", "build/slow-query.log");
        }
    }
}