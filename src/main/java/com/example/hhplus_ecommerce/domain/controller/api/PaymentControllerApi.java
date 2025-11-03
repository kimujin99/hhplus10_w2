package com.example.hhplus_ecommerce.domain.controller.api;

import com.example.hhplus_ecommerce.common.ApiResponse;
import com.example.hhplus_ecommerce.domain.dto.reponse.OrderDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "결제", description = "결제 관련 API")
public interface PaymentControllerApi {

    @Operation(summary = "결제 생성", description = "주문에 대한 포인트 결제를 생성합니다. 결제 실패 시 재고와 쿠폰이 복원됩니다.")
    ResponseEntity<ApiResponse<OrderDetailResponse>> createPayment(@PathVariable Long orderId, HttpServletRequest request);
}