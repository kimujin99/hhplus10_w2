package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.CartItem;
import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.domain.repository.CartItemRepository;
import com.example.hhplus_ecommerce.domain.repository.ProductRepository;
import com.example.hhplus_ecommerce.domain.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.CartDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<CartItemResponse> getUserCart(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        return  cartItems.stream()
                .map(CartItemResponse::from)
                .toList();
    }

    public CartItemResponse addCartItem(Long userId, AddCartItemRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, request.productId())
                .map(existingCartItem -> {
                    // 카트에 상품이 있는 경우
                    int newQuantity = existingCartItem.getQuantity() + request.quantity();
                    if (product.getStockQuantity() < newQuantity) {
                        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
                    }
                    existingCartItem.updateQuantity(newQuantity);
                    return cartItemRepository.save(existingCartItem);
                })
                .orElseGet(() -> {
                    // 새로운 상품 추가
                    if (product.getStockQuantity() < request.quantity()) {
                        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
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

    public void deleteCartItem(Long cartItemId) {
        cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(cartItemId);
    }
}