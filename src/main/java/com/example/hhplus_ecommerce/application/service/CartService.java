package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.CartItem;
import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.domain.model.User;
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
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        return  cartItems.stream()
                .map(CartItemResponse::from)
                .toList();
    }

    public CartItemResponse addCartItem(Long userId, AddCartItemRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Product product = productRepository.findById(request.productId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        CartItem existingCartItem = cartItemRepository.findByUserIdAndProductId(userId, request.productId());
        CartItem cartItem;
        if (existingCartItem != null) {
            // 카트에 상품이 있는 경우
            int newQuantity = existingCartItem.getQuantity() + request.quantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }

            existingCartItem.updateQuantity(newQuantity);
            cartItem = cartItemRepository.save(existingCartItem);
        } else {
            // 새로운 상품 추가
            if (product.getStockQuantity() < request.quantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            cartItem = CartItem.builder()
                    .userId(userId)
                    .productId(request.productId())
                    .productName(product.getProductName())
                    .price(product.getPrice())
                    .quantity(request.quantity())
                    .build();
            cartItem = cartItemRepository.save(cartItem);
        }

        return CartItemResponse.from(cartItem);
    }

    public void deleteCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId);
        if (cartItem == null) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        cartItemRepository.delete(cartItemId);
    }
}