package com.example.hhplus_ecommerce.order.application;

import com.example.hhplus_ecommerce.cart.infrastructure.CartItemRepository;
import com.example.hhplus_ecommerce.cart.model.CartItem;
import com.example.hhplus_ecommerce.coupon.domain.Coupon;
import com.example.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.CouponRepository;
import com.example.hhplus_ecommerce.coupon.infrastructure.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CartErrorCode;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CouponErrorCode;
import com.example.hhplus_ecommerce.common.presentation.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.common.presentation.exception.ConflictException;
import com.example.hhplus_ecommerce.common.presentation.exception.NotFoundException;
import com.example.hhplus_ecommerce.order.domain.Order;
import com.example.hhplus_ecommerce.order.domain.OrderItem;
import com.example.hhplus_ecommerce.order.infrastructure.OrderItemRepository;
import com.example.hhplus_ecommerce.order.infrastructure.OrderRepository;
import com.example.hhplus_ecommerce.order.presentation.dto.OrderDto.OrderRequest;
import com.example.hhplus_ecommerce.order.presentation.dto.OrderDto.OrderResponse;
import com.example.hhplus_ecommerce.product.domain.Product;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.user.domain.User;
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

    /**
     * 주문을 생성합니다.
     * <p>
     * 장바구니 상품을 기반으로 주문을 생성하며, 재고 확인, 쿠폰 유효성 검증 등을 수행합니다.
     * 주문 생성 후 장바구니는 자동으로 비워집니다.
     *
     * @param request 주문 요청 정보 (사용자 ID, 쿠폰 ID, 배송 정보 등)
     * @return 생성된 주문 정보 및 주문 아이템 목록
     * @throws NotFoundException 장바구니가 비어있거나 사용자/상품/쿠폰을 찾을 수 없는 경우
     * @throws ConflictException 재고가 부족하거나 쿠폰이 이미 사용되었거나 만료된 경우
     */
    @Transactional
    public OrderResponse execute(OrderRequest request) {
        User user = userRepository.findByIdOrThrow(request.userId());
        List<CartItem> cartItems = cartItemRepository.findByUserId(request.userId());
        if (cartItems.isEmpty()) {
            throw new NotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }

        long totalAmount = cartItems.stream()
                .mapToLong(ci -> ci.getPrice() * ci.getQuantity())
                .sum();

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findByIdOrThrow(cartItem.getProductId());
            if (!product.hasSufficientStock(cartItem.getQuantity())) {
                throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
            }
        }

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

        cartItemRepository.deleteByUserId(request.userId());

        return OrderResponse.from(order, orderItems);
    }
}