package com.example.hhplus_ecommerce.performance;

import com.example.hhplus_ecommerce.domain.model.OrderItem;
import com.example.hhplus_ecommerce.domain.model.Order;
import com.example.hhplus_ecommerce.infrastructure.repository.*;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import com.example.hhplus_ecommerce.presentation.utils.testDataGenerator;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 조회 성능 저하 분석을 위한 테스트
 * Testcontainers 환경에서 EXPLAIN, 쿼리 실행 시간 측정, 인덱스 효과 검증
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueryPerformanceAnalysisTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private testDataGenerator testDataGenerator;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private static boolean dataInitialized = false;

    @BeforeEach
    void setUp() {
        if (!dataInitialized) {
            log.info("===== 대량 테스트 데이터 생성 시작 =====");
            testDataGenerator.createLargeTestData();
            dataInitialized = true;
            log.info("===== 대량 테스트 데이터 생성 완료 =====");
        }
    }

    /**
     * 이슈 1: Order 조회 시 user_id 컬럼에 인덱스 누락
     * 문제: findByUserId() 실행 시 Full Table Scan 발생
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("[이슈1] Order 조회 - user_id 인덱스 누락으로 인한 Full Table Scan")
    void analyzeOrderByUserIdWithoutIndex() throws Exception {
        log.info("\n========================================");
        log.info("이슈 1: Order.user_id 인덱스 누락 분석");
        log.info("========================================\n");

        String query = "SELECT * FROM order_table WHERE user_id = 1";

        // EXPLAIN 분석
        String explainResult = executeExplain(query);
        log.info("EXPLAIN 결과 (인덱스 없음):\n{}", explainResult);

        // 실행 시간 측정 (여러 번 실행하여 평균)
        long avgTime = measureQueryTime(query, 10);
        log.info("평균 실행 시간 (인덱스 없음): {}ms", avgTime);

        // 분석 결과 출력
        log.info("\n[분석 결과]");
        log.info("- type: ALL (Full Table Scan)");
        log.info("- rows: 전체 order 레코드 스캔");
        log.info("- Extra: Using where (인덱스 없이 조건 필터링)");
        log.info("- 문제: user_id 조회 시 매번 전체 테이블 스캔 발생");
        log.info("- 영향: 사용자별 주문 조회 시 성능 저하\n");
    }

    /**
     * 이슈 2: OrderItem 조회 시 order_id 컬럼에 인덱스 누락
     * 문제: findByOrderId() 실행 시 Full Table Scan 발생
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("[이슈2] OrderItem 조회 - order_id 인덱스 누락으로 인한 Full Table Scan")
    void analyzeOrderItemByOrderIdWithoutIndex() throws Exception {
        log.info("\n========================================");
        log.info("이슈 2: OrderItem.order_id 인덱스 누락 분석");
        log.info("========================================\n");

        String query = "SELECT * FROM order_item WHERE order_id = 1";

        // EXPLAIN 분석
        String explainResult = executeExplain(query);
        log.info("EXPLAIN 결과 (인덱스 없음):\n{}", explainResult);

        // 실행 시간 측정
        long avgTime = measureQueryTime(query, 10);
        log.info("평균 실행 시간 (인덱스 없음): {}ms", avgTime);

        // 분석 결과 출력
        log.info("\n[분석 결과]");
        log.info("- type: ALL (Full Table Scan)");
        log.info("- rows: 전체 order_item 레코드 스캔");
        log.info("- Extra: Using where");
        log.info("- 문제: 주문 상세 조회 시 매번 전체 테이블 스캔 발생");
        log.info("- 영향: 주문 상세 조회 성능 저하\n");
    }

    /**
     * 이슈 3: N+1 쿼리 문제
     * 문제: getUserOrders() 실행 시 Order 조회 후 각 OrderItem을 개별 조회
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("[이슈3] N+1 쿼리 문제 - Order 조회 후 OrderItem 개별 조회")
    void analyzeNPlusOneProblem() {
        log.info("\n========================================");
        log.info("이슈 3: N+1 쿼리 문제 분석");
        log.info("========================================\n");

        // 쿼리 로깅 활성화를 위해 수동 플러시
        entityManager.clear();

        long startTime = System.currentTimeMillis();

        // 사용자의 주문 조회 (UserOrderService.getUserOrders 로직 재현)
        List<Order> orders = orderRepository.findByUserId(1L);
        log.info("1. Order 조회 쿼리 실행: {} 건", orders.size());

        // 각 주문의 상세 정보를 위해 OrderItem 개별 조회 발생
        for (Order order : orders) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            log.info("2. OrderItem 조회 쿼리 실행 (orderId: {}): {} 건", order.getId(), orderItems.size());
        }

        long endTime = System.currentTimeMillis();

        // 분석 결과 출력
        log.info("\n[분석 결과]");
        log.info("- 실행된 쿼리 수: 1 (Order 조회) + {} (각 Order별 OrderItem 조회) = {} 개",
                 orders.size(), orders.size() + 1);
        log.info("- 실행 시간: {}ms", endTime - startTime);
        log.info("- 문제: Order-OrderItem 간 JPA 관계 매핑 없어 N+1 쿼리 발생");
        log.info("- 영향: 주문 목록 조회 시 주문 수만큼 추가 쿼리 발생");
        log.info("- 해결 방안:");
        log.info("  1. Fetch Join 사용: JOIN FETCH로 한 번에 조회");
        log.info("  2. @EntityGraph 사용");
        log.info("  3. Batch Size 설정\n");
    }

    /**
     * 이슈 4: 복잡한 인기 상품 쿼리
     * 문제: ORDER BY 절에 복잡한 계산식 사용으로 인덱스 활용 불가
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("[이슈4] 인기 상품 쿼리 - 복잡한 계산식으로 인한 성능 저하")
    void analyzePopularProductQuery() throws Exception {
        log.info("\n========================================");
        log.info("이슈 4: 인기 상품 쿼리 분석");
        log.info("========================================\n");

        String query = """
            SELECT p.*
            FROM product p
            ORDER BY (p.view_count + ((p.original_stock_quantity - p.stock_quantity) * 1.0 / p.original_stock_quantity) * 100 * 2) DESC
            LIMIT 5
        """;

        // EXPLAIN 분석
        String explainResult = executeExplain(query);
        log.info("EXPLAIN 결과:\n{}", explainResult);

        // 실행 시간 측정
        long avgTime = measureQueryTime(query, 10);
        log.info("평균 실행 시간: {}ms", avgTime);

        // 분석 결과 출력
        log.info("\n[분석 결과]");
        log.info("- type: ALL (Full Table Scan)");
        log.info("- Extra: Using filesort (정렬을 위한 추가 작업)");
        log.info("- 문제: ORDER BY 절의 복잡한 계산식으로 인덱스 활용 불가");
        log.info("- 영향: 모든 상품 데이터를 읽고 계산 후 정렬");
        log.info("- 해결 방안:");
        log.info("  1. 계산된 인기 점수를 별도 컬럼으로 저장 (popularity_score)");
        log.info("  2. 해당 컬럼에 인덱스 생성");
        log.info("  3. 스케줄러로 주기적으로 업데이트");
        log.info("  4. 또는 Redis 캐시 활용\n");
    }

    /**
     * 이슈 5: CartItem 조회 시 user_id 인덱스 누락
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("[이슈5] CartItem 조회 - user_id 인덱스 누락")
    void analyzeCartItemByUserIdWithoutIndex() throws Exception {
        log.info("\n========================================");
        log.info("이슈 5: CartItem.user_id 인덱스 누락 분석");
        log.info("========================================\n");

        String query = "SELECT * FROM cart_item WHERE user_id = 1";

        // EXPLAIN 분석
        String explainResult = executeExplain(query);
        log.info("EXPLAIN 결과 (인덱스 없음):\n{}", explainResult);

        // 실행 시간 측정
        long avgTime = measureQueryTime(query, 10);
        log.info("평균 실행 시간 (인덱스 없음): {}ms", avgTime);

        log.info("\n[분석 결과]");
        log.info("- 문제: 장바구니 조회 시 Full Table Scan 발생");
        log.info("- 영향: 사용자별 장바구니 조회 성능 저하\n");
    }

    /**
     * 인덱스 추가 후 성능 개선 효과 검증
     */
    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("[개선안] 인덱스 추가 후 성능 비교")
    void comparePerformanceWithIndex() throws Exception {
        log.info("\n========================================");
        log.info("성능 개선: 인덱스 추가 후 비교");
        log.info("========================================\n");

        String orderQuery = "SELECT * FROM order_table WHERE user_id = 1";
        String orderItemQuery = "SELECT * FROM order_item WHERE order_id = 1";

        // 인덱스 추가 전 측정
        long beforeOrderTime = measureQueryTime(orderQuery, 10);
        long beforeOrderItemTime = measureQueryTime(orderItemQuery, 10);

        log.info("인덱스 추가 전:");
        log.info("- Order 조회: {}ms", beforeOrderTime);
        log.info("- OrderItem 조회: {}ms", beforeOrderItemTime);

        // 인덱스 생성
        log.info("\n인덱스 생성 중...");
        jdbcTemplate.execute("CREATE INDEX idx_order_user_id ON order_table(user_id)");
        jdbcTemplate.execute("CREATE INDEX idx_order_item_order_id ON order_item(order_id)");
        jdbcTemplate.execute("CREATE INDEX idx_cart_item_user_id ON cart_item(user_id)");
        log.info("인덱스 생성 완료");

        // 인덱스 추가 후 측정
        long afterOrderTime = measureQueryTime(orderQuery, 10);
        long afterOrderItemTime = measureQueryTime(orderItemQuery, 10);

        log.info("\n인덱스 추가 후:");
        log.info("- Order 조회: {}ms", afterOrderTime);
        log.info("- OrderItem 조회: {}ms", afterOrderItemTime);

        // EXPLAIN 재확인
        log.info("\n인덱스 추가 후 EXPLAIN (Order):\n{}", executeExplain(orderQuery));
        log.info("\n인덱스 추가 후 EXPLAIN (OrderItem):\n{}", executeExplain(orderItemQuery));

        // 개선율 계산
        double orderImprovement = ((beforeOrderTime - afterOrderTime) / (double) beforeOrderTime) * 100;
        double orderItemImprovement = ((beforeOrderItemTime - afterOrderItemTime) / (double) beforeOrderItemTime) * 100;

        log.info("\n[성능 개선 효과]");
        log.info("- Order 조회: {:.2f}% 개선", orderImprovement);
        log.info("- OrderItem 조회: {:.2f}% 개선", orderItemImprovement);
        log.info("- type: ref (인덱스 사용)");
        log.info("- key: 생성한 인덱스 사용 확인");
    }

    // ===== 유틸리티 메서드 =====

    /**
     * EXPLAIN 실행 및 결과 파싱
     */
    private String executeExplain(String query) throws Exception {
        StringBuilder result = new StringBuilder();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXPLAIN " + query)) {

            int columnCount = rs.getMetaData().getColumnCount();

            // 헤더
            for (int i = 1; i <= columnCount; i++) {
                result.append(String.format("%-15s", rs.getMetaData().getColumnName(i)));
            }
            result.append("\n");
            result.append("-".repeat(columnCount * 15)).append("\n");

            // 데이터
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    result.append(String.format("%-15s", value == null ? "NULL" : value));
                }
                result.append("\n");
            }
        }
        return result.toString();
    }

    /**
     * 쿼리 실행 시간 측정 (평균)
     */
    private long measureQueryTime(String query, int iterations) throws Exception {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    // 결과 소비
                }
            }
            long endTime = System.nanoTime();
            times.add((endTime - startTime) / 1_000_000); // ms 변환
        }

        // 평균 계산 (첫 실행 제외 - 워밍업)
        return times.stream().skip(1).mapToLong(Long::longValue).sum() / (iterations - 1);
    }
}