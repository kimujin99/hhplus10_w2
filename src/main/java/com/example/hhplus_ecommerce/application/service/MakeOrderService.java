package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.infrastructure.repository.*;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CartErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.OrderRequest;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MakeOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public OrderResponse execute(OrderRequest request) {
        // 사용자, 장바구니 검증
        // 주문은 장바구니를 통해서만 가능
        User user = userRepository.findByIdOrThrow(request.userId());
        List<CartItem> cartItems = cartItemRepository.findByUserId(request.userId());
        if (cartItems.isEmpty()) {
            throw new NotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 총 금액 계산
        long totalAmount = cartItems.stream()
                .mapToLong(ci -> ci.getPrice() * ci.getQuantity())
                .sum();

        // 재고 검증
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findByIdOrThrow(cartItem.getProductId());
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // 쿠폰 검증
        long discountAmount = 0L;
        Long userCouponId = null;
        if (request.userCouponId() != null) {
            UserCoupon userCoupon = userCouponRepository.findByIdOrThrow(request.userCouponId());
            if (userCoupon.isUsed()) {
                throw new ConflictException(CouponErrorCode.COUPON_ALREADY_USED);
            }
            Coupon coupon = couponRepository.findByIdOrThrow(userCoupon.getCoupon().getId());
            if (coupon.isExpired()) {
                throw new ConflictException(CouponErrorCode.COUPON_EXPIRED);
            }
            discountAmount = coupon.calculateDiscountAmount(totalAmount);
            userCouponId = userCoupon.getId();
        }

        // 주문 및 주문 아이템 생성
        Order order = orderRepository.save(Order.builder()
                .userId(user.getId())
                .userCouponId(userCouponId)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .ordererName(request.ordererName())
                .deliveryAddress(request.deliveryAddress())
                .build());

        List<OrderItem> orderItems = cartItems.stream()
                .map(ci -> OrderItem.builder()
                        .orderId(order.getId())
                        .productId(ci.getProductId())
                        .productName(ci.getProductName())
                        .price(ci.getPrice())
                        .quantity(ci.getQuantity())
                        .build())
                .map(orderItemRepository::save)
                .toList();

        // 장바구니 비우기
        cartItemRepository.deleteByUserId(request.userId());

        return OrderResponse.from(order, orderItems);
    }
}