package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.domain.repository.*;
import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public OrderResponse execute(OrderRequest request) {
        // 사용자 존재 검증
        userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 장바구니 조회 (장바구니를 통해서만 주문 가능)
        // 장바구니 검증 : 존재 검증
        List<CartItem> cartItems = cartItemRepository.findByUserId(request.userId());
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 상품 조회
        // 상품 검증 : 존재 검증
        // 상품 총액 계산
        long totalAmount = 0L;
        List<Product> products = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            products.add(product);
            totalAmount += cartItem.getPrice() * cartItem.getQuantity();
        }

        // 사용자 쿠폰, 쿠폰 조회
        // 쿠폰 검증 : 존재 검증, 사용 여부 검증, 유효기간 검증
        long discountAmount = 0L;
        UserCoupon userCoupon = null;
        Coupon coupon = null;
        if (request.userCouponId() != null) {
            userCoupon = userCouponRepository.findById(request.userCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            if (userCoupon.isUsed()) {
                throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
            }

            coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            if (coupon.isExpired()) {
                throw new BusinessException(ErrorCode.COUPON_EXPIRED);
            }

            discountAmount = coupon.calculateDiscountAmount(totalAmount);
        }

        // 재고 차감
        List<Integer> succeededIndexes = new ArrayList<>();
        try {
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem cartItem = cartItems.get(i);
                Product product = products.get(i);
                product.subStockQuantity(cartItem.getQuantity());
                productRepository.save(product);
                succeededIndexes.add(i);
            }
        } catch (Exception e) {
            // 재고 차감 실패 시 성공한 것만 롤백
            log.warn("재고 차감 실패 - userId: {}, succeededIndexes: {}",
                    cartItems.isEmpty() ? null : cartItems.getFirst().getUserId(),
                    succeededIndexes, e);
            rollbackStock(succeededIndexes, cartItems, products);
            throw e;
        }

        // 쿠폰 사용 처리
        if (userCoupon != null) {
            userCoupon.use();
            userCouponRepository.save(userCoupon);
        }

        // 주문 생성
        Order order = Order.builder()
                .userId(request.userId())
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .ordererName(request.ordererName())
                .deliveryAddress(request.deliveryAddress())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 주문 아이템 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .orderId(savedOrder.getId())
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            orderItems.add(orderItemRepository.save(orderItem));
        }

        // 장바구니 비우기
        cartItemRepository.deleteByUserId(request.userId());

        // 쿠폰에 주문 ID 할당
        if (userCoupon != null) {
            userCoupon.assignOrderId(savedOrder.getId());
            userCouponRepository.save(userCoupon);
        }

        return OrderResponse.from(savedOrder, orderItems);
    }

    private void rollbackStock(List<Integer> succeededIndexes, List<CartItem> cartItems, List<Product> products) {
        try {
            for (Integer idx : succeededIndexes) {
                Product product = products.get(idx);
                CartItem cartItem = cartItems.get(idx);
                product.addStockQuantity(cartItem.getQuantity());
                productRepository.save(product);
            }
        } catch (Exception rollbackException) {
            log.error("재고 롤백 실패 - userId: {}, succeededIndexes: {}",
                    cartItems.isEmpty() ? null : cartItems.getFirst().getUserId(),
                    succeededIndexes,
                    rollbackException);
        }
    }
}