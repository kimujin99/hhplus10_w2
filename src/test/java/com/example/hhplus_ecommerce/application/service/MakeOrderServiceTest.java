package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.domain.repository.*;
import com.example.hhplus_ecommerce.presentation.common.exception.BaseException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.*;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MakeOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private MakeOrderService makeOrderService;

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 없이")
    void execute_Success_WithoutCoupon() {
        // given
        Long userId = 1L;
        User user = new User();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(100)
                .build();
        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(2)
                .build();
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(20000L)
                .discountAmount(0L)
                .ordererName("홍길동")
                .deliveryAddress("서울시")
                .build();
        OrderItem orderItem = OrderItem.builder()
                .orderId(1L)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(2)
                .build();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productRepository.save(any(Product.class))).willReturn(product);
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderItemRepository.save(any(OrderItem.class))).willReturn(orderItem);
        doNothing().when(cartItemRepository).deleteByUserId(userId);

        // when
        OrderResponse result = makeOrderService.execute(request);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(cartItemRepository).findByUserId(userId);
        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(cartItemRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 사용")
    void execute_Success_WithCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        User user = new User();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(100)
                .build();
        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(2)
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(1L)
                .build();
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(20000L)
                .discountAmount(2000L)
                .ordererName("홍길동")
                .deliveryAddress("서울시")
                .build();
        OrderItem orderItem = OrderItem.builder()
                .orderId(1L)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(2)
                .build();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", userCouponId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));
        given(productRepository.save(any(Product.class))).willReturn(product);
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderItemRepository.save(any(OrderItem.class))).willReturn(orderItem);
        doNothing().when(cartItemRepository).deleteByUserId(userId);

        // when
        OrderResponse result = makeOrderService.execute(request);

        // then
        assertThat(result).isNotNull();
        verify(userCouponRepository).findById(userCouponId);
        verify(couponRepository).findById(1L);
        verify(userCouponRepository, times(2)).save(userCoupon);
    }

    @Test
    @DisplayName("주문 생성 실패 - 사용자 없음")
    void execute_Fail_UserNotFound() {
        // given
        OrderRequest request = new OrderRequest(999L, "홍길동", "서울시", null);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findById(999L);
        verify(cartItemRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 장바구니 비어있음")
    void execute_Fail_EmptyCart() {
        // given
        Long userId = 1L;
        User user = new User();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.CART_ITEM_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(cartItemRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품 재고 부족")
    void execute_Fail_InsufficientStock() {
        // given
        Long userId = 1L;
        User user = new User();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(1)
                .build();
        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(5)
                .build();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);
        verify(userRepository).findById(userId);
        verify(cartItemRepository).findByUserId(userId);
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("주문 생성 실패 - 이미 사용된 쿠폰")
    void execute_Fail_CouponAlreadyUsed() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        User user = new User();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(100)
                .build();
        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(2)
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(1L)
                .build();
        userCoupon.use();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", userCouponId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_USED);
    }

    @Test
    @DisplayName("주문 생성 실패 - 만료된 쿠폰")
    void execute_Fail_CouponExpired() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        User user = new User();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(100)
                .build();
        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(2)
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(1L)
                .build();
        Coupon coupon = Coupon.builder()
                .name("만료된 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(1)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusDays(1))
                .build();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", userCouponId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_EXPIRED);
    }
}