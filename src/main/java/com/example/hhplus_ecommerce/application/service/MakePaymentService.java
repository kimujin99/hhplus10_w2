package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.*;
import com.example.hhplus_ecommerce.infrastructure.repository.*;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public PaymentResponse execute(Long orderId) {
        // 이미 생성된 주문으로 바로 결제
        Order order = orderRepository.findByIdOrThrow(orderId);
        User user = userRepository.findByIdOrThrow(order.getUserId());
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // 재고 차감
        for (OrderItem item : orderItems) {
            Product product = productRepository.findByIdOrThrow(item.getProductId());
            product.subStockQuantity(item.getQuantity());
            productRepository.save(product);
        }

        // 쿠폰 사용 처리
        if (order.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponRepository.findByIdOrThrow(order.getUserCouponId());
            userCoupon.use();
            userCouponRepository.save(userCoupon);
        }

        // 포인트 차감 및 주문 확정
        user.usePoint(order.getFinalAmount());
        userRepository.save(user);
        order.confirm();
        orderRepository.save(order);

        return PaymentResponse.from(order);
    }
}