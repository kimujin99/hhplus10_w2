package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.domain.repository.*;
import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MakePaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;

    public PaymentResponse execute(Long orderId) {
        // 주문 조회
        // 이미 생성된 주문으로 바로 결제
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 사용자 조회
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // 결제 처리 시도
        boolean pointDeducted = false;
        try {
            // 포인트 차감
            user.usePoint(order.getFinalAmount());
            userRepository.save(user);
            pointDeducted = true;

            // 주문 확정
            order.confirm();
            orderRepository.save(order);
        } catch (Exception e) {
            log.error("결제 실패 - orderId: {}, userId: {}, pointDeducted: {}",
                    order.getId(), order.getUserId(), pointDeducted, e);
            rollbackPayment(order, user, orderItems, pointDeducted);
            throw e;
        }

        return PaymentResponse.from(order);
    }

    private void rollbackPayment(Order order, User user, List<OrderItem> orderItems, boolean pointDeducted) {
        try {
            // 1. 포인트 복구 (차감된 경우만)
            if (pointDeducted) {
                user.chargePoint(order.getFinalAmount());
                userRepository.save(user);
            }

            // 2. 주문 실패 처리
            order.fail();
            orderRepository.save(order);

            // 3. 재고 복구
            restoreStock(orderItems);

            // 4. 쿠폰 복구
            restoreCoupon(order);
        } catch (Exception rollbackException) {
            log.error("결제 롤백 실패 - orderId: {}, userId: {}, pointDeducted: {}",
                    order.getId(), order.getUserId(), pointDeducted, rollbackException);
        }
    }

    private void restoreStock(List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            productRepository.findById(orderItem.getProductId())
                    .ifPresent(product -> {
                        product.addStockQuantity(orderItem.getQuantity());
                        productRepository.save(product);
                    });
        }
    }

    private void restoreCoupon(Order order) {
        if (order.getDiscountAmount() > 0) {
            userCouponRepository.findByOrderId(order.getId())
                    .ifPresent(userCoupon -> {
                        userCoupon.restore();
                        userCouponRepository.save(userCoupon);
                    });
        }
    }
}