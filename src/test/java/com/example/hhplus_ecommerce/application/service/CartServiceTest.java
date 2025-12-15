package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.cart.application.CartService;
import com.example.hhplus_ecommerce.cart.model.CartItem;
import com.example.hhplus_ecommerce.product.domain.Product;
import com.example.hhplus_ecommerce.user.domain.User;
import com.example.hhplus_ecommerce.cart.infrastructure.CartItemRepository;
import com.example.hhplus_ecommerce.product.infrastructure.repository.ProductRepository;
import com.example.hhplus_ecommerce.user.infrastructure.UserRepository;
import com.example.hhplus_ecommerce.common.presentation.exception.NotFoundException;
import com.example.hhplus_ecommerce.common.presentation.exception.ConflictException;
import com.example.hhplus_ecommerce.common.presentation.errorCode.UserErrorCode;
import com.example.hhplus_ecommerce.common.presentation.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.common.presentation.errorCode.CartErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("장바구니 조회 성공")
    void getUserCart_Success() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .point(0L)
                .build();
        CartItem cartItem1 = CartItem.builder()
                .userId(userId)
                .productId(1L)
                .productName("상품1")
                .price(10000L)
                .quantity(2)
                .build();
        CartItem cartItem2 = CartItem.builder()
                .userId(userId)
                .productId(2L)
                .productName("상품2")
                .price(20000L)
                .quantity(1)
                .build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(cartItem1, cartItem2));

        // when
        List<CartItemResponse> result = cartService.getUserCart(userId);

        // then
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).productName()).isEqualTo("상품1"),
                () -> assertThat(result.get(1).productName()).isEqualTo("상품2")
        );

        verify(userRepository).findByIdOrThrow(userId);
        verify(cartItemRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("장바구니 조회 실패 - 사용자 없음")
    void getUserCart_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findByIdOrThrow(userId))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> cartService.getUserCart(userId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(cartItemRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("장바구니 상품 추가 성공 - 새로운 상품")
    void addCartItem_Success_NewProduct() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        User user = User.builder().point(0L).build();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(100)
                .build();
        AddCartItemRequest request = new AddCartItemRequest(productId, 5);
        CartItem newCartItem = CartItem.builder()
                .userId(userId)
                .productId(productId)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(5)
                .build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(productRepository.findByIdOrThrow(productId)).willReturn(product);
        given(cartItemRepository.findByUserIdAndProductId(userId, productId)).willReturn(Optional.empty());
        given(cartItemRepository.save(any(CartItem.class))).willReturn(newCartItem);

        // when
        CartItemResponse result = cartService.addCartItem(userId, request);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.productName()).isEqualTo("테스트 상품"),
                () -> assertThat(result.quantity()).isEqualTo(5)
        );

        verify(userRepository).findByIdOrThrow(userId);
        verify(productRepository).findByIdOrThrow(productId);
        verify(cartItemRepository).findByUserIdAndProductId(userId, productId);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("장바구니 상품 추가 성공 - 기존 상품 수량 증가")
    void addCartItem_Success_ExistingProduct() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        User user = User.builder().point(0L).build();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(100)
                .build();
        CartItem existingCartItem = CartItem.builder()
                .userId(userId)
                .productId(productId)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(5)
                .build();
        AddCartItemRequest request = new AddCartItemRequest(productId, 3);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(productRepository.findByIdOrThrow(productId)).willReturn(product);
        given(cartItemRepository.findByUserIdAndProductId(userId, productId)).willReturn(Optional.of(existingCartItem));
        given(cartItemRepository.save(any(CartItem.class))).willReturn(existingCartItem);

        // when
        CartItemResponse result = cartService.addCartItem(userId, request);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.quantity()).isEqualTo(8)
        );

        verify(userRepository).findByIdOrThrow(userId);
        verify(productRepository).findByIdOrThrow(productId);
        verify(cartItemRepository).findByUserIdAndProductId(userId, productId);
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("장바구니 상품 추가 실패 - 재고 부족")
    void addCartItem_Fail_InsufficientStock() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        User user = User.builder().point(0L).build();
        Product product = Product.builder()
                .productName("테스트 상품")
                .description("설명")
                .price(10000L)
                .stockQuantity(3)
                .build();
        AddCartItemRequest request = new AddCartItemRequest(productId, 5);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(productRepository.findByIdOrThrow(productId)).willReturn(product);
        given(cartItemRepository.findByUserIdAndProductId(userId, productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartService.addCartItem(userId, request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);
        verify(userRepository).findByIdOrThrow(userId);
        verify(productRepository).findByIdOrThrow(productId);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("장바구니 상품 추가 실패 - 사용자 없음")
    void addCartItem_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        AddCartItemRequest request = new AddCartItemRequest(1L, 5);
        given(userRepository.findByIdOrThrow(userId))
                .willThrow(new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> cartService.addCartItem(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(productRepository, never()).findByIdOrThrow(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("장바구니 상품 추가 실패 - 상품 없음")
    void addCartItem_Fail_ProductNotFound() {
        // given
        Long userId = 1L;
        Long productId = 999L;
        User user = User.builder().point(0L).build();
        AddCartItemRequest request = new AddCartItemRequest(productId, 5);

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);
        given(productRepository.findByIdOrThrow(productId))
                .willThrow(new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> cartService.addCartItem(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_NOT_FOUND);
        verify(userRepository).findByIdOrThrow(userId);
        verify(productRepository).findByIdOrThrow(productId);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("장바구니 아이템 삭제 성공")
    void deleteCartItem_Success() {
        // given
        Long cartItemId = 1L;
        CartItem cartItem = CartItem.builder()
                .userId(1L)
                .productId(1L)
                .productName("테스트 상품")
                .price(10000L)
                .quantity(5)
                .build();

        given(cartItemRepository.findByIdOrThrow(cartItemId)).willReturn(cartItem);
        doNothing().when(cartItemRepository).deleteById(cartItemId);

        // when
        cartService.deleteCartItem(cartItemId);

        // then
        verify(cartItemRepository).findByIdOrThrow(cartItemId);
        verify(cartItemRepository).deleteById(cartItemId);
    }

    @Test
    @DisplayName("장바구니 아이템 삭제 실패 - 존재하지 않는 아이템")
    void deleteCartItem_Fail_NotFound() {
        // given
        Long cartItemId = 999L;
        given(cartItemRepository.findByIdOrThrow(cartItemId))
                .willThrow(new NotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> cartService.deleteCartItem(cartItemId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.CART_ITEM_NOT_FOUND);
        verify(cartItemRepository).findByIdOrThrow(cartItemId);
        verify(cartItemRepository, never()).deleteById((Long) any());
    }
}