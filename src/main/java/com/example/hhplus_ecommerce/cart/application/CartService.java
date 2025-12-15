package com.example.hhplus_ecommerce.cart.application;

import com.example.hhplus_ecommerce.cart.model.CartItem;
import com.example.hhplus_ecommerce.product.domain.Product;
import com.example.hhplus_ecommerce.cart.infrastructure.CartItemRepository;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.common.presentation.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.common.presentation.exception.ConflictException;
import com.example.hhplus_ecommerce.cart.presentation.dto.CartDto.AddCartItemRequest;
import com.example.hhplus_ecommerce.cart.presentation.dto.CartDto.CartItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<CartItemResponse> getUserCart(Long userId) {
        userRepository.findByIdOrThrow(userId);
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        return  CartItemResponse.fromList(cartItems);
    }

    @Transactional
    public CartItemResponse addCartItem(Long userId, AddCartItemRequest request) {
        userRepository.findByIdOrThrow(userId);
        Product product = productRepository.findByIdOrThrow(request.productId());

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, request.productId())
                .map(existingCartItem -> {
                    // 카트에 상품이 있는 경우
                    int newQuantity = existingCartItem.getQuantity() + request.quantity();
                    if (product.getStockQuantity() < newQuantity) {
                        throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
                    }
                    existingCartItem.updateQuantity(newQuantity);
                    return cartItemRepository.save(existingCartItem);
                })
                .orElseGet(() -> {
                    // 새로운 상품 추가
                    if (product.getStockQuantity() < request.quantity()) {
                        throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
                    }
                    CartItem newCartItem = CartItem.builder()
                            .userId(userId)
                            .productId(request.productId())
                            .productName(product.getProductName())
                            .price(product.getPrice())
                            .quantity(request.quantity())
                            .build();
                    return cartItemRepository.save(newCartItem);
                });

        return CartItemResponse.from(cartItem);
    }

    @Transactional
    public void deleteCartItem(Long cartItemId) {
        cartItemRepository.findByIdOrThrow(cartItemId);
        cartItemRepository.deleteById(cartItemId);
    }
}