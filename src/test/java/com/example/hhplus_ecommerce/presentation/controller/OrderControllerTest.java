package com.example.hhplus_ecommerce.presentation.controller;

import com.example.hhplus_ecommerce.application.service.MakeOrderService;
import com.example.hhplus_ecommerce.application.service.MakePaymentService;
import com.example.hhplus_ecommerce.application.service.UserOrderService;
import com.example.hhplus_ecommerce.presentation.common.exception.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.errorCode.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.OrderDto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MakeOrderService makeOrderService;

    @MockitoBean
    private MakePaymentService makePaymentService;

    @MockitoBean
    private UserOrderService userOrderService;

    @Test
    @DisplayName("POST /api/v1/orders - 주문 생성 성공")
    void makeOrder_Success() throws Exception {
        // given
        OrderRequest request = new OrderRequest(1L, "김우진", "서울시 강남구", null);
        OrderResponse response = new OrderResponse(
                1L,
                1L,
                100000L,
                0L,
                100000L,
                "PENDING",
                "김우진",
                "서울시 강남구",
                List.of(
                        new OrderItemResponse(1L, 1L, "맥북 프로", 2000000L, 1, 2000000L)
                ),
                LocalDateTime.now()
        );
        when(makeOrderService.execute(any(OrderRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(100000))
                .andExpect(jsonPath("$.data.discountAmount").value(0))
                .andExpect(jsonPath("$.data.finalAmount").value(100000))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.ordererName").value("김우진"))
                .andExpect(jsonPath("$.data.deliveryAddress").value("서울시 강남구"));
    }

    @Test
    @DisplayName("POST /api/v1/orders - 주문 생성 성공 (쿠폰 사용)")
    void makeOrder_SuccessWithCoupon() throws Exception {
        // given
        OrderRequest request = new OrderRequest(1L, "김우진", "서울시 강남구", 1L);
        OrderResponse response = new OrderResponse(
                1L,
                1L,
                100000L,
                10000L,
                90000L,
                "PENDING",
                "김우진",
                "서울시 강남구",
                List.of(
                        new OrderItemResponse(1L, 1L, "맥북 프로", 100000L, 1, 100000L)
                ),
                LocalDateTime.now()
        );
        when(makeOrderService.execute(any(OrderRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discountAmount").value(10000))
                .andExpect(jsonPath("$.data.finalAmount").value(90000));
    }

    @Test
    @DisplayName("POST /api/v1/orders - 주문 생성 실패 (장바구니 비어있음)")
    void makeOrder_FailEmptyCart() throws Exception {
        // given
        OrderRequest request = new OrderRequest(1L, "김우진", "서울시 강남구", null);
        when(makeOrderService.execute(any(OrderRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/orders - 주문 생성 실패 (재고 부족)")
    void makeOrder_FailInsufficientStock() throws Exception {
        // given
        OrderRequest request = new OrderRequest(1L, "김우진", "서울시 강남구", null);
        when(makeOrderService.execute(any(OrderRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK));

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/orders - 사용자 주문 목록 조회 성공")
    void getUserOrders_Success() throws Exception {
        // given
        Long userId = 1L;
        List<UserOrderResponse> orders = List.of(
                new UserOrderResponse(1L, 100000L, 0L, 100000L, "CONFIRMED", LocalDateTime.now()),
                new UserOrderResponse(2L, 200000L, 20000L, 180000L, "PENDING", LocalDateTime.now())
        );
        when(userOrderService.getUserOrders(anyLong())).thenReturn(orders);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/orders", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].orderId").value(1))
                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data[1].orderId").value(2))
                .andExpect(jsonPath("$.data[1].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/orders/{orderId} - 특정 주문 조회 성공")
    void getUserOrder_Success() throws Exception {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        OrderResponse response = new OrderResponse(
                orderId,
                userId,
                100000L,
                10000L,
                90000L,
                "CONFIRMED",
                "김우진",
                "서울시 강남구",
                List.of(
                        new OrderItemResponse(1L, 1L, "맥북 프로", 100000L, 1, 100000L)
                ),
                LocalDateTime.now()
        );
        when(userOrderService.getUserOrder(anyLong(), anyLong())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/orders/{orderId}", userId, orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/orders/{orderId} - 주문 조회 실패 (존재하지 않음)")
    void getUserOrder_NotFound() throws Exception {
        // given
        Long userId = 1L;
        Long orderId = 999L;
        when(userOrderService.getUserOrder(anyLong(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/orders/{orderId}", userId, orderId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - 결제 성공")
    void makePayment_Success() throws Exception {
        // given
        Long orderId = 1L;
        PaymentResponse response = new PaymentResponse(
                orderId,
                90000L,
                "CONFIRMED",
                LocalDateTime.now()
        );
        when(makePaymentService.execute(anyLong())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.paymentAmount").value(90000))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - 결제 실패 (포인트 부족)")
    void makePayment_FailInsufficientPoint() throws Exception {
        // given
        Long orderId = 1L;
        when(makePaymentService.execute(anyLong()))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT));

        // when & then
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - 결제 실패 (주문 없음)")
    void makePayment_FailOrderNotFound() throws Exception {
        // given
        Long orderId = 999L;
        when(makePaymentService.execute(anyLong()))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}