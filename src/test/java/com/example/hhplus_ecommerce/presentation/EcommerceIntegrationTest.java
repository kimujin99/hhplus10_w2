package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.domain.repository.CouponRepository;
import com.example.hhplus_ecommerce.domain.repository.ProductRepository;
import com.example.hhplus_ecommerce.domain.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.dto.CartDto.*;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import com.example.hhplus_ecommerce.presentation.dto.ProductDto.*;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EcommerceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 필요한 초기 데이터 생성
        setupUsers();
        setupProducts();
        setupCoupons();
    }

    private void setupUsers() {
        for (int i = 1; i <= 10; i++) {
            userRepository.save(new User());
        }
    }

    private void setupProducts() {
        productRepository.save(Product.builder()
                .productName("맥북 프로")
                .description("애플 맥북 프로 16인치")
                .price(2000000L)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());

        productRepository.save(Product.builder()
                .productName("아이패드")
                .description("애플 아이패드 프로 12.9인치")
                .price(1000000L)
                .originalStockQuantity(150)
                .stockQuantity(150)
                .build());

        productRepository.save(Product.builder()
                .productName("에어팟")
                .description("애플 에어팟 프로 2세대")
                .price(300000L)
                .originalStockQuantity(200)
                .stockQuantity(200)
                .build());

        productRepository.save(Product.builder()
                .productName("아이폰")
                .description("애플 아이폰 15 Pro")
                .price(1500000L)
                .originalStockQuantity(120)
                .stockQuantity(120)
                .build());

        productRepository.save(Product.builder()
                .productName("애플워치")
                .description("애플 워치 시리즈 9")
                .price(500000L)
                .originalStockQuantity(180)
                .stockQuantity(180)
                .build());
    }

    private void setupCoupons() {
        LocalDateTime now = LocalDateTime.now();

        couponRepository.save(Coupon.builder()
                .name("5000원 할인 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(5000L)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(now.minusDays(1))
                .validUntil(now.plusDays(30))
                .build());

        couponRepository.save(Coupon.builder()
                .name("10% 할인 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(50)
                .issuedQuantity(0)
                .validFrom(now.minusDays(1))
                .validUntil(now.plusDays(30))
                .build());

        couponRepository.save(Coupon.builder()
                .name("20000원 할인 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(20000L)
                .totalQuantity(30)
                .issuedQuantity(0)
                .validFrom(now.minusDays(1))
                .validUntil(now.plusDays(30))
                .build());
    }

    @Test
    @DisplayName("전체 E-commerce 플로우 통합 테스트: 포인트 충전 → 상품 조회 → 장바구니 추가 → 쿠폰 발급 → 주문 → 결제")
    void fullEcommerceFlow_Success() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        Long couponId = 1L;

        // 1. 사용자 포인트 조회 (초기 포인트 확인)
        mockMvc.perform(get("/api/v1/users/{userId}/points", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.point").exists());

        // 2. 포인트 충전 (맥북 프로 2개 구매 가능한 금액)
        ChargePointRequest chargeRequest = new ChargePointRequest(5000000L);
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId));

        // 3. 포인트 히스토리 조회
        mockMvc.perform(get("/api/v1/users/{userId}/points/history", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 4. 상품 목록 조회
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 5. 특정 상품 조회
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId));

        // 6. 상품 재고 조회
        mockMvc.perform(get("/api/v1/products/{productId}/stock", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId));

        // 7. 장바구니에 상품 추가
        AddCartItemRequest addCartRequest = new AddCartItemRequest(productId, 2);
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCartRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(2));

        // 8. 장바구니 조회
        mockMvc.perform(get("/api/v1/users/{userId}/cart", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productId").value(productId));

        // 9. 쿠폰 목록 조회
        mockMvc.perform(get("/api/v1/coupons"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 10. 쿠폰 발급
        IssueCouponRequest issueCouponRequest = new IssueCouponRequest(couponId);
        MvcResult couponResult = mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueCouponRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andReturn();

        // 발급받은 userCouponId 추출
        String couponResponseJson = couponResult.getResponse().getContentAsString();
        Long userCouponId = objectMapper.readTree(couponResponseJson)
                .get("userCouponId")
                .asLong();

        // 11. 사용자 쿠폰 목록 조회
        mockMvc.perform(get("/api/v1/users/{userId}/coupons", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("ISSUED"));

        // 12. 주문 생성 (쿠폰 적용)
        OrderRequest orderRequest = new OrderRequest(userId, "김우진", "서울시 강남구", userCouponId);
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.discountAmount").exists())
                .andReturn();

        // 생성된 orderId 추출
        String orderResponseJson = orderResult.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderResponseJson)
                .get("orderId")
                .asLong();

        // 13. 주문 목록 조회
        mockMvc.perform(get("/api/v1/users/{userId}/orders", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(orderId))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // 14. 특정 주문 조회
        mockMvc.perform(get("/api/v1/users/{userId}/orders/{orderId}", userId, orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 15. 결제 처리
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 16. 결제 후 주문 상태 확인
        MvcResult confirmedOrderResult = mockMvc.perform(get("/api/v1/users/{userId}/orders/{orderId}", userId, orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();

        // 최종 결제 금액 추출
        String confirmedOrderJson = confirmedOrderResult.getResponse().getContentAsString();
        Long finalAmount = objectMapper.readTree(confirmedOrderJson)
                .get("finalAmount")
                .asLong();

        // 17. 결제 후 포인트 확인 (차감 확인)
        Long expectedRemainingPoints = 5000000L - finalAmount;
        mockMvc.perform(get("/api/v1/users/{userId}/points", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.point").value(expectedRemainingPoints));

        // 18. 장바구니 비워졌는지 확인 (주문 후 자동 삭제)
        mockMvc.perform(get("/api/v1/users/{userId}/cart", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // 19. 쿠폰 사용 확인
        mockMvc.perform(get("/api/v1/users/{userId}/coupons", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userCouponId").value(userCouponId))
                .andExpect(jsonPath("$[0].status").value("USED"));
    }

    @Test
    @DisplayName("통합 테스트: 쿠폰 없이 주문 및 결제")
    void fullEcommerceFlow_WithoutCoupon() throws Exception {
        Long userId = 2L;
        Long productId = 2L;

        // 1. 포인트 충전
        ChargePointRequest chargeRequest = new ChargePointRequest(1000000L);
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 2. 장바구니에 상품 추가
        AddCartItemRequest addCartRequest = new AddCartItemRequest(productId, 1);
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCartRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 3. 주문 생성 (쿠폰 없음)
        OrderRequest orderRequest = new OrderRequest(userId, "이영희", "서울시 서초구", null);
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andReturn();

        // 4. 결제 처리
        String orderResponseJson = orderResult.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderResponseJson)
                .get("orderId")
                .asLong();

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("통합 테스트: 재고 부족으로 주문 실패")
    void orderFlow_InsufficientStock() throws Exception {
        Long userId = 3L;
        Long productId = 1L;

        // 1. 포인트 충전
        ChargePointRequest chargeRequest = new ChargePointRequest(10000000L);
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 2. 재고보다 많은 수량을 장바구니에 추가 시도
        AddCartItemRequest addCartRequest = new AddCartItemRequest(productId, 999);
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCartRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("통합 테스트: 포인트 부족으로 결제 실패")
    void paymentFlow_InsufficientPoint() throws Exception {
        Long userId = 4L;
        Long productId = 1L;

        // 1. 포인트 충전 (부족한 금액)
        ChargePointRequest chargeRequest = new ChargePointRequest(1000L);
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 2. 장바구니에 고가 상품 추가
        AddCartItemRequest addCartRequest = new AddCartItemRequest(productId, 1);
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCartRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 3. 주문 생성
        OrderRequest orderRequest = new OrderRequest(userId, "박민수", "부산시 해운대구", null);
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // 4. 결제 시도 (포인트 부족으로 실패)
        String orderResponseJson = orderResult.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderResponseJson)
                .get("orderId")
                .asLong();

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("통합 테스트: 쿠폰 품절로 발급 실패")
    void couponFlow_SoldOut() throws Exception {
        Long userId = 5L;
        Long limitedCouponId = 2L; // 수량이 제한된 쿠폰

        // 쿠폰 발급 (품절 예상)
        IssueCouponRequest request = new IssueCouponRequest(limitedCouponId);

        // 여러 번 발급 시도하여 품절 상황 유도
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print());
        }
    }

    @Test
    @DisplayName("통합 테스트: 인기 상품 조회")
    void popularProductsFlow() throws Exception {
        // 1. 여러 상품 조회로 조회수 증가
        for (Long productId = 1L; productId <= 5L; productId++) {
            mockMvc.perform(get("/api/v1/products/{productId}", productId))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        // 2. 인기 상품 목록 조회
        mockMvc.perform(get("/api/v1/products/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("통합 테스트: 장바구니 상품 삭제")
    void cartItemDeletionFlow() throws Exception {
        Long userId = 6L;
        Long productId = 3L;

        // 1. 장바구니에 상품 추가
        AddCartItemRequest addCartRequest = new AddCartItemRequest(productId, 1);
        MvcResult addResult = mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCartRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // 2. cartItemId 추출
        String addResponseJson = addResult.getResponse().getContentAsString();
        Long cartItemId = objectMapper.readTree(addResponseJson)
                .get("cartItemId")
                .asLong();

        // 3. 장바구니 상품 삭제
        mockMvc.perform(delete("/api/v1/cart/{cartItemId}", cartItemId))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 4. 장바구니 조회로 삭제 확인
        mockMvc.perform(get("/api/v1/users/{userId}/cart", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}