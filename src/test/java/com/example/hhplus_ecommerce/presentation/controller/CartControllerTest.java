package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.CartService;
import com.example.hhplus_ecommerce.presentation.common.exception.NotFoundException;
import com.example.hhplus_ecommerce.presentation.common.exception.ConflictException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ProductErrorCode;
import com.example.hhplus_ecommerce.presentation.common.errorCode.CartErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.CartDto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @Test
    @DisplayName("GET /api/v1/users/{userId}/cart - 장바구니 조회 성공")
    void getUserCart_Success() throws Exception {
        // given
        Long userId = 1L;
        List<CartItemResponse> cartItems = List.of(
                new CartItemResponse(1L, userId, 1L, "맥북 프로", 2000000L, 1, 2000000L),
                new CartItemResponse(2L, userId, 2L, "아이패드", 1000000L, 2, 2000000L)
        );
        when(cartService.getUserCart(anyLong())).thenReturn(cartItems);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/cart", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cartItemId").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].productName").value("맥북 프로"))
                .andExpect(jsonPath("$[0].quantity").value(1))
                .andExpect(jsonPath("$[0].subtotal").value(2000000));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/cart - 빈 장바구니 조회")
    void getUserCart_Empty() throws Exception {
        // given
        Long userId = 1L;
        when(cartService.getUserCart(anyLong())).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/cart", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/cart - 장바구니 상품 추가 성공")
    void addCartItem_Success() throws Exception {
        // given
        Long userId = 1L;
        AddCartItemRequest request = new AddCartItemRequest(1L, 2);
        CartItemResponse response = new CartItemResponse(
                1L, userId, 1L, "맥북 프로", 2000000L, 2, 4000000L
        );
        when(cartService.addCartItem(anyLong(), any(AddCartItemRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value(1))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("맥북 프로"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(4000000));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/cart - 장바구니 추가 실패 (상품 없음)")
    void addCartItem_FailProductNotFound() throws Exception {
        // given
        Long userId = 1L;
        AddCartItemRequest request = new AddCartItemRequest(999L, 1);
        when(cartService.addCartItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new NotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/cart - 장바구니 추가 실패 (재고 부족)")
    void addCartItem_FailInsufficientStock() throws Exception {
        // given
        Long userId = 1L;
        AddCartItemRequest request = new AddCartItemRequest(1L, 100);
        when(cartService.addCartItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/cart", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/{cartItemId} - 장바구니 상품 삭제 성공")
    void deleteCartItem_Success() throws Exception {
        // given
        Long cartItemId = 1L;
        doNothing().when(cartService).deleteCartItem(anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/cart/{cartItemId}", cartItemId))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(cartService, times(1)).deleteCartItem(cartItemId);
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/{cartItemId} - 장바구니 삭제 실패 (존재하지 않음)")
    void deleteCartItem_NotFound() throws Exception {
        // given
        Long cartItemId = 999L;
        doThrow(new NotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND))
                .when(cartService).deleteCartItem(anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/cart/{cartItemId}", cartItemId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}