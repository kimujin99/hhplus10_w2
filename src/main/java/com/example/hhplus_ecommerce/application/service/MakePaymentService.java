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

    /**
     * 주문에 대한 결제를 처리합니다.
     * <p>
     * 이미 생성된 주문에 대해 재고 차감, 쿠폰 사용 처리, 포인트 차감을 수행하고 주문을 확정합니다.
     * 모든 작업은 트랜잭션 내에서 원자적으로 처리됩니다.
     *
     * @param orderId 결제할 주문 ID
     * @return 결제 완료된 주문 정보
     * @throws NotFoundException 주문, 사용자, 상품, 쿠폰을 찾을 수 없는 경우
     * @throws ConflictException 재고가 부족하거나 포인트가 부족한 경우
     */
    @Transactional
    public PaymentResponse execute(Long orderId) {
        Order order = orderRepository.findByIdOrThrow(orderId);
        User user = userRepository.findByIdOrThrow(order.getUserId());
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        for (OrderItem item : orderItems) {
            Product product = productRepository.findByIdOrThrow(item.getProductId());
            product.subStockQuantity(item.getQuantity());
            productRepository.save(product);
        }

        if (order.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponRepository.findByIdOrThrow(order.getUserCouponId());
            userCoupon.use();
            userCouponRepository.save(userCoupon);
        }

        user.usePoint(order.getFinalAmount());
        userRepository.save(user);
        order.confirm();
        orderRepository.save(order);

        return PaymentResponse.from(order);
    }
}