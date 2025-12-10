package com.example.hhplus_ecommerce.cart.infrastructure;

import com.example.hhplus_ecommerce.cart.model.CartItem;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CartErrorCode;
import com.example.hhplus_ecommerce.common.presentation.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findById(Long cartItemId);

    default CartItem findByIdOrThrow(Long cartItemId) {
        return findById(cartItemId)
                .orElseThrow(() -> new NotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND));
    }

    CartItem save(CartItem cartItem);
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    void deleteById(Long cartItemId);
    void deleteByUserId(Long userId);
}
