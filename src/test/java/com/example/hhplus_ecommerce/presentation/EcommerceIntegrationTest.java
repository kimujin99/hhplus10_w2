package com.example.hhplus_ecommerce.presentation;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.infrastructure.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.dto.CartDto.*;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import com.example.hhplus_ecommerce.presentation.dto.UserDto.*;
import com.example.hhplus_ecommerce.presentation.utils.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EcommerceIntegrationTest extends AbstractIntegrationTest {

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

    private Long getSavedUserId() {
        User user = userRepository.save(User.builder().build());
        return user.getId();
    }

    @Test
    @DisplayName("전체 E-commerce 플로우 통합 테스트: 포인트 충전 → 상품 조회 → 장바구니 추가 → 쿠폰 발급 → 주문 → 결제")
    void fullEcommerceFlow_Success() throws Exception {
        // 1. 사용자 생성
        Long userId = getSavedUserId();

        // 2. 상품 생성
        Product product = productRepository.save(Product.builder()
                .productName("맥북 프로")
                .description("애플 맥북 프로 16인치")
                .price(2000000L)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());
        Long productId = product.getId();

        // 3. 쿠폰 생성
        Coupon coupon = couponRepository.save(Coupon.builder()
                .name("할인 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(50000L)
                .totalQuantity(10)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(2))
                .build());
        Long couponId = coupon.getId();

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
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.point").exists());

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
        // 1. 사용자 생성
        User user = userRepository.save(User.builder().build());
        Long userId = user.getId();

        // 2. 상품 생성
        Product product = productRepository.save(Product.builder()
                .productName("맥북 프로")
                .description("애플 맥북 프로 16인치")
                .price(2000000L)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());
        Long productId = product.getId();

        // 3. 포인트 충전
        ChargePointRequest chargeRequest = new ChargePointRequest(3000000L);
        mockMvc.perform(post("/api/v1/users/{userId}/points/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 4. 장바구니에 상품 추가
        AddCartItemRequest addCartRequest = new AddCartItemRequest(productId, 1);
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCartRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 5. 주문 생성 (쿠폰 없음)
        OrderRequest orderRequest = new OrderRequest(userId, "이영희", "서울시 서초구", null);
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andReturn();

        // 6. 결제 처리
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
        // 1. 사용자 생성
        Long userId = getSavedUserId();

        // 2. 상품 생성
        Product product = productRepository.save(Product.builder()
                .productName("맥북 프로")
                .description("애플 맥북 프로 16인치")
                .price(2000000L)
                .originalStockQuantity(100)
                .stockQuantity(0)
                .build());
        Long productId = product.getId();

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
        // 1. 사용자 생성
        Long userId = getSavedUserId();

        // 2. 상품 생성
        Product product = productRepository.save(Product.builder()
                .productName("맥북 프로")
                .description("애플 맥북 프로 16인치")
                .price(2000000L)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());
        Long productId = product.getId();

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
        // 1. 재고가 제한된 쿠폰 생성
        Coupon coupon = couponRepository.save(Coupon.builder()
                .name("할인 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(50000L)
                .totalQuantity(10)
                .issuedQuantity(9)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(2))
                .build());
        Long limitedCouponId = coupon.getId();

        IssueCouponRequest request = new IssueCouponRequest(limitedCouponId);

        // 마지막 재고 발급
        Long userId1 = getSavedUserId();
        mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // 재고 소진 확인
        mockMvc.perform(get("/api/v1/coupons/{couponId}", limitedCouponId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuedQuantity").value(10));

        // 재고 부족 상태 발급
        Long userId2 = getSavedUserId();
        mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").exists())
                        .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("통합 테스트: 쿠폰 중복 발급 실패")
    void couponFlow_Duplicate() throws Exception {
        // 1. 쿠폰 생성
        Coupon coupon = couponRepository.save(Coupon.builder()
                .name("할인 쿠폰")
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(50000L)
                .totalQuantity(10)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(2))
                .build());
        Long couponId = coupon.getId();

        IssueCouponRequest request = new IssueCouponRequest(couponId);

        // 쿠폰 발급
        Long userId = getSavedUserId();
        mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // 중복 발급 시도
        mockMvc.perform(post("/api/v1/users/{userId}/coupons", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("통합 테스트: 인기 상품 조회")
    void popularProductsFlow() throws Exception {
        // 1. 인기도가 다른 상품 6개 생성 (viewCount + salesRatio * 100 * 2)
        // (1위) 인기도=203
        Product p1 = productRepository.save(Product.builder()
                .originalStockQuantity(10)
                .stockQuantity(0)
                .viewCount(3)
                .build());
        // (2위) 인기도=202
        Product p2 = productRepository.save(Product.builder()
                .originalStockQuantity(10)
                .stockQuantity(0)
                .viewCount(2)
                .build());

        // 2. 인기 상품 목록 조회
        mockMvc.perform(get("/api/v1/products/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productId").exists())
                .andExpect(jsonPath("$[1].productId").exists());

    }

    @Test
    @DisplayName("통합 테스트: 장바구니 상품 삭제")
    void cartItemDeletionFlow() throws Exception {
        // 1. 사용자 생성
        Long userId = getSavedUserId();

        // 2. 상품 생성
        Product product = productRepository.save(Product.builder()
                .productName("맥북 프로")
                .description("애플 맥북 프로 16인치")
                .price(2000000L)
                .originalStockQuantity(100)
                .stockQuantity(100)
                .build());
        Long productId = product.getId();

        // 3. 장바구니에 상품 추가
        AddCartItemRequest addCartRequest = new AddCartItemRequest(productId, 1);
        MvcResult addResult = mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCartRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // 4. cartItemId 추출
        String addResponseJson = addResult.getResponse().getContentAsString();
        Long cartItemId = objectMapper.readTree(addResponseJson)
                .get("cartItemId")
                .asLong();

        // 5. 장바구니 상품 삭제
        mockMvc.perform(delete("/api/v1/cart/{cartItemId}", cartItemId))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 6. 장바구니 조회로 삭제 확인
        mockMvc.perform(get("/api/v1/users/{userId}/cart", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}