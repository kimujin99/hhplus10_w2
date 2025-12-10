package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.cart.infrastructure.CartItemRepository;
import com.example.hhplus_ecommerce.cart.model.CartItem;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CartErrorCode;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.common.presentation.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.common.presentation.errorCode.UserErrorCode;
import com.example.hhplus_ecommerce.coupon.domain.Coupon;
import com.example.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.common.presentation.exception.BaseException;
import com.example.hhplus_ecommerce.common.presentation.exception.NotFoundException;
import com.example.hhplus_ecommerce.order.application.MakeOrderService;
import com.example.hhplus_ecommerce.order.domain.Order;
import com.example.hhplus_ecommerce.order.domain.OrderItem;
import com.example.hhplus_ecommerce.order.infrastructure.OrderItemRepository;
import com.example.hhplus_ecommerce.order.infrastructure.OrderRepository;
import com.example.hhplus_ecommerce.order.presentation.dto.OrderDto.*;
import com.example.hhplus_ecommerce.product.domain.Product;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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
        User user = User.builder().point(0L).build();
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

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findByIdOrThrow(1L)).willReturn(product);
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderItemRepository.save(any(OrderItem.class))).willReturn(orderItem);
        doNothing().when(cartItemRepository).deleteByUserId(userId);

        // when
        OrderResponse result = makeOrderService.execute(request);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findByIdOrThrow(userId);
        verify(cartItemRepository).findByUserId(userId);
        verify(productRepository).findByIdOrThrow(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(cartItemRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void execute_Success_WithCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        User user = User.builder().point(0L).build();
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
        Coupon coupon = Coupon.builder()
                .id(1L)
                .name("테스트 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
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

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findByIdOrThrow(1L)).willReturn(product);
        given(userCouponRepository.findByIdOrThrow(userCouponId)).willReturn(userCoupon);
        given(couponRepository.findByIdOrThrow(1L)).willReturn(coupon);
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderItemRepository.save(any(OrderItem.class))).willReturn(orderItem);
        doNothing().when(cartItemRepository).deleteByUserId(userId);

        // when
        OrderResponse result = makeOrderService.execute(request);

        // then
        assertThat(result).isNotNull();
        verify(userCouponRepository).findByIdOrThrow(userCouponId);
        verify(couponRepository).findByIdOrThrow(1L);
    }

    @Test
    @DisplayName("주문 생성 실패 - 사용자 없음")
    void execute_Fail_UserNotFound() {
        // given
        OrderRequest request = new OrderRequest(999L, "홍길동", "서울시", null);
        given(userRepository.findByIdOrThrow(999L)).willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(999L);
        verify(cartItemRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 장바구니 비어있음")
    void execute_Fail_EmptyCart() {
        // given
        Long userId = 1L;
        User user = User.builder().point(0L).build();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", null);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.CART_ITEM_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(cartItemRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품 재고 부족")
    void execute_Fail_InsufficientStock() {
        // given
        Long userId = 1L;
        User user = User.builder().point(0L).build();
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

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findByIdOrThrow(1L)).willReturn(product);

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);
        verify(userRepository).findByIdOrThrow(userId);
        verify(cartItemRepository).findByUserId(userId);
        verify(productRepository).findByIdOrThrow(1L);
    }

    @Test
    @DisplayName("주문 생성 실패 - 이미 사용된 쿠폰")
    void execute_Fail_CouponAlreadyUsed() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        User user = User.builder().point(0L).build();
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
        Coupon coupon = Coupon.builder().id(1L).build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
        userCoupon.use();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", userCouponId);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findByIdOrThrow(1L)).willReturn(product);
        given(userCouponRepository.findByIdOrThrow(userCouponId)).willReturn(userCoupon);

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
        User user = User.builder().point(0L).build();
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
        Coupon coupon = Coupon.builder()
                .id(1L)
                .name("만료된 쿠폰")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(10L)
                .totalQuantity(100)
                .issuedQuantity(1)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusDays(1))
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
        OrderRequest request = new OrderRequest(userId, "홍길동", "서울시", userCouponId);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem));
        given(productRepository.findByIdOrThrow(1L)).willReturn(product);
        given(userCouponRepository.findByIdOrThrow(userCouponId)).willReturn(userCoupon);
        given(couponRepository.findByIdOrThrow(1L)).willReturn(coupon);

        // when & then
        assertThatThrownBy(() -> makeOrderService.execute(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_EXPIRED);
    }
}