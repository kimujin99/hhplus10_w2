# API 명세서

## 개요
- **Base URL**: `http://localhost:8080/api/v1`
- **API Version**: v1
- **Content-Type**: `application/json`

---

## 공통 응답 형식

모든 API는 다음 형식의 응답을 반환합니다.

```json
{
  "timestamp": "2025-10-30T17:00:00",
  "path": "/api/v1/products",
  "success": true,
  "data": {
    "productId": 1,
    "productName": "상품명",
    "description": "상세한 상품 설명...",
    "price": 29900,
    "stockQuantity": 100,
    "createdAt": "2024-01-10T09:00:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "error": null
}
```
```json
{
  "timestamp": "2025-10-30T17:00:00",
  "path": "/api/v1/products/999",
  "success": false,
  "data": null,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "상품을 찾을 수 없습니다"
  }
}
```

### 비즈니스 에러 코드

| HTTP Status | Error Code | Description |
|-------------|------------|------------|
| 400 | INVALID_INPUT | 입력값이 유효하지 않음 |
| 400 | INVALID_QUANTITY | 수량이 유효하지 않음 (0 이하) |
| 400 | INSUFFICIENT_STOCK | 재고 부족 |
| 400 | INSUFFICIENT_POINT | 포인트 잔액 부족 |
| 400 | INVALID_CHARGE_AMOUNT | 충전 금액이 유효하지 않음 |
| 400 | COUPON_SOLD_OUT | 쿠폰 발급 마감 |
| 400 | COUPON_EXPIRED | 쿠폰 유효 기간 만료 |
| 400 | COUPON_ALREADY_USED | 이미 사용된 쿠폰 |
| 400 | INVALID_COUPON_STATUS | 사용할 수 없는 쿠폰 상태 |
| 400 | ORDER_ALREADY_PAID | 이미 결제된 주문 |
| 400 | INVALID_ORDER_STATUS | 결제할 수 없는 주문 상태 |
| 400 | MISSING_DELIVERY_INFO | 배송 정보 누락 |
| 403 | FORBIDDEN_RESOURCE | 접근 권한 없음 |
| 404 | USER_NOT_FOUND | 사용자를 찾을 수 없음 |
| 404 | PRODUCT_NOT_FOUND | 상품을 찾을 수 없음 |
| 404 | CART_ITEM_NOT_FOUND | 장바구니 항목을 찾을 수 없음 |
| 404 | COUPON_NOT_FOUND | 쿠폰을 찾을 수 없음 |
| 404 | ORDER_NOT_FOUND | 주문을 찾을 수 없음 |
| 409 | COUPON_ALREADY_ISSUED | 이미 발급받은 쿠폰 |
| 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류 |

---

## 1. 상품 (Products)

### 1.1 상품 목록 조회
**GET** `/products`

상품 목록을 조회합니다.

**Response** `200 OK`
```json
{
  "content": [
    {
      "productId": 1,
      "productName": "상품명",
      "description": "상품 설명",
      "price": 29900,
      "stockQuantity": 100,
      "createdAt": "2024-01-10T09:00:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

---

### 1.2 상품 상세 조회
**GET** `/products/{productId}`

특정 상품의 상세 정보를 조회합니다.

**Path Parameters**
- `productId`: 상품 ID

**Response** `200 OK`
```json
{
  "productId": 1,
  "productName": "상품명",
  "description": "상세한 상품 설명...",
  "price": 29900,
  "stockQuantity": 100,
  "createdAt": "2024-01-10T09:00:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Error Responses**

- `404`, `PRODUCT_NOT_FOUND`: 상품을 찾을 수 없습니다

---

### 1.3 상품 재고 조회
**GET** `/products/{productId}/stock`

상품의 실시간 재고를 조회합니다.

**Path Parameters**
- `productId`: 상품 ID

**Response** `200 OK`
```json
{
  "productId": 1,
  "productName": "상품명",
  "stockQuantity": 100
}
```

**Error Responses**

- `404`, `PRODUCT_NOT_FOUND`: 상품을 찾을 수 없습니다

---

### 1.4 인기 상품 통계 조회
**GET** `/products/popular`

최근 3일간의 인기 상품 상위 5개를 조회합니다.

**Response** `200 OK`
```json
{
  "statistics": [
    {
      "rank": 1,
      "productId": 1,
      "productName": "인기 상품 1",
      "totalOrderQuantity": 150,
      "orderCount": 45
    },
    {
      "rank": 2,
      "productId": 5,
      "productName": "인기 상품 2",
      "totalOrderQuantity": 120,
      "orderCount": 38
    }
  ],
  "period": {
    "from": "2024-01-17T00:00:00",
    "to": "2024-01-20T23:59:59"
  }
}
```

---

## 2. 포인트 (Points)

### 2.1 포인트 조회
**GET** `/users/{userId}/points`

사용자의 현재 포인트 잔액을 조회합니다.

**Path Parameters**
- `userId`: 사용자 ID

**Response** `200 OK`
```json
{
  "userId": 1,
  "userName": "홍길동",
  "pointBalance": 50000,
  "updatedAt": "2024-01-20T10:30:00"
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다

---

### 2.2 포인트 사용 내역 조회
**GET** `/users/{userId}/points/history`

사용자의 포인트 거래 내역을 조회합니다.

**Path Parameters**
- `userId`: 사용자 ID

**Response** `200 OK`
```json
{
  "content": [
    {
      "pointHistoryId": 1,
      "userId": 1,
      "orderId": 5,
      "transactionType": "USE",
      "amount": 29900,
      "balanceAfter": 120100,
      "createdAt": "2024-01-20T16:40:00"
    },
    {
      "pointHistoryId": 2,
      "userId": 1,
      "orderId": null,
      "transactionType": "CHARGE",
      "amount": 100000,
      "balanceAfter": 150000,
      "createdAt": "2024-01-20T11:00:00"
    }
  ]
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다

---

## 3. 장바구니 (Cart)

### 3.1 장바구니 조회
**GET** `/users/{userId}/cart`

사용자의 장바구니를 조회합니다.

**Path Parameters**
- `userId`: 사용자 ID

**Response** `200 OK`
```json
{
  "userId": 1,
  "items": [
    {
      "cartItemId": 1,
      "product": {
        "productId": 1,
        "productName": "상품명",
        "price": 29900,
        "stockQuantity": 100
      },
      "quantity": 2
    }
  ]
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다

---

### 3.2 장바구니에 상품 추가
**POST** `/users/{userId}/cart`

장바구니에 상품을 추가합니다.

**Path Parameters**
- `userId`: 사용자 ID

**Request Body**
```json
{
  "productId": 1,
  "quantity": 2
}
```

**Response** `201 Created`
```json
{
  "cartItemId": 1,
  "product": {
    "productId": 1,
    "productName": "상품명",
    "price": 29900
  },
  "quantity": 2,
  "createdAt": "2024-01-20T14:00:00"
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다
- `404`, `PRODUCT_NOT_FOUND`: 상품을 찾을 수 없습니다
- `400`, `INVALID_QUANTITY`: 수량은 1개 이상이어야 합니다

---

### 3.3 장바구니 상품 수량 변경
**PATCH** `/users/{userId}/cart/{cartItemId}`

장바구니 상품의 수량을 변경합니다.

**Path Parameters**
- `userId`: 사용자 ID
- `cartItemId`: 장바구니 항목 ID

**Request Body**
```json
{
  "quantity": 3
}
```

**Response** `200 OK`
```json
{
  "cartItemId": 1,
  "product": {
    "productId": 1,
    "productName": "상품명",
    "price": 29900
  },
  "quantity": 3,
  "updatedAt": "2024-01-20T14:30:00"
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다
- `404`, `CART_ITEM_NOT_FOUND`: 장바구니 항목을 찾을 수 없습니다
- `400`, `INVALID_QUANTITY`: 수량은 1개 이상이어야 합니다

---

### 3.4 장바구니 상품 삭제
**DELETE** `/users/{userId}/cart/{cartItemId}`

장바구니에서 상품을 삭제합니다.

**Path Parameters**
- `userId`: 사용자 ID
- `cartItemId`: 장바구니 항목 ID

**Response** `204 No Content`

**Error Responses**

- `404`, `CART_ITEM_NOT_FOUND`: 장바구니 항목을 찾을 수 없습니다

---

## 4. 쿠폰 (Coupons)

### 4.1 발급 가능한 쿠폰 목록 조회
**GET** `/coupons`

현재 발급 가능한 쿠폰 목록을 조회합니다.

**Response** `200 OK`
```json
{
  "coupons": [
    {
      "couponId": 1,
      "name": "신규 가입 쿠폰",
      "discountType": "FIXED",
      "discountValue": 5000,
      "totalQuantity": 100,
      "issuedQuantity": 45,
      "remainingQuantity": 55,
      "validFrom": "2024-01-01T00:00:00",
      "validUntil": "2024-12-31T23:59:59"
    },
    {
      "couponId": 2,
      "name": "10% 할인 쿠폰",
      "discountType": "PERCENTAGE",
      "discountValue": 10,
      "totalQuantity": 50,
      "issuedQuantity": 50,
      "remainingQuantity": 0,
      "validFrom": "2024-01-15T00:00:00",
      "validUntil": "2024-01-31T23:59:59"
    }
  ]
}
```

---

### 4.2 쿠폰 발급
**POST** `/users/{userId}/coupons`

사용자에게 쿠폰을 발급합니다. 선착순으로 인당 하나씩만 발급 가능합니다.

**Path Parameters**
- `userId`: 사용자 ID

**Request Body**
```json
{
  "couponId": 1
}
```

**Response** `201 Created`
```json
{
  "userCouponId": 1,
  "userId": 1,
  "coupon": {
    "couponId": 1,
    "name": "신규 가입 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000,
    "validFrom": "2024-01-01T00:00:00",
    "validUntil": "2024-12-31T23:59:59"
  },
  "status": "ISSUED",
  "issuedAt": "2024-01-20T15:00:00"
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다
- `404`, `COUPON_NOT_FOUND`: 쿠폰을 찾을 수 없습니다
- `409`, `COUPON_ALREADY_ISSUED`: 이미 발급받은 쿠폰입니다
- `400`, `COUPON_SOLD_OUT`: 쿠폰 발급이 마감되었습니다
- `400`, `COUPON_EXPIRED`: 쿠폰 유효 기간이 아닙니다

---

### 4.3 사용자 쿠폰 목록 조회
**GET** `/users/{userId}/coupons`

사용자가 보유한 쿠폰 목록을 조회합니다.

**Path Parameters**
- `userId`: 사용자 ID

**Response** `200 OK`
```json
{
  "userId": 1,
  "coupons": [
    {
      "userCouponId": 1,
      "coupon": {
        "couponId": 1,
        "name": "신규 가입 쿠폰",
        "discountType": "FIXED",
        "discountValue": 5000,
        "validFrom": "2024-01-01T00:00:00",
        "validUntil": "2024-12-31T23:59:59"
      },
      "status": "ISSUED",
      "issuedAt": "2024-01-20T15:00:00",
      "usedAt": null
    },
    {
      "userCouponId": 2,
      "coupon": {
        "couponId": 2,
        "name": "10% 할인 쿠폰",
        "discountType": "PERCENTAGE",
        "discountValue": 10,
        "validFrom": "2024-01-15T00:00:00",
        "validUntil": "2024-01-31T23:59:59"
      },
      "status": "USED",
      "issuedAt": "2024-01-15T10:00:00",
      "usedAt": "2024-01-18T14:30:00"
    }
  ]
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다

---

## 5. 주문 (Orders)

### 5.1 주문 생성
**POST** `/orders`

장바구니의 상품으로 주문을 생성합니다. 주문 시 재고와 쿠폰이 차감됩니다.

**Request Body**
```json
{
  "userId": 1,
  "ordererName" : "홍길동",
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "userCouponId": 1
}
```

**Response** `201 Created`
```json
{
  "orderId": 1,
  "userId": 1,
  "totalAmount": 59800,
  "discountAmount": 5000,
  "finalAmount": 54800,
  "status": "PENDING",
  "ordererName" : "홍길동",
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "orderedAt": "2024-01-20T16:30:00",
  "appliedCoupon": {
    "userCouponId": 1,
    "couponName": "신규 가입 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000
  },
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "상품명",
      "price": 29900,
      "quantity": 2,
      "subtotal": 59800
    }
  ]
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다
- `404`, `CART_ITEM_NOT_FOUND`: 장바구니 항목을 찾을 수 없습니다
- `404`, `COUPON_NOT_FOUND`: 쿠폰을 찾을 수 없습니다
- `400`, `INSUFFICIENT_STOCK`: 상품 재고가 부족합니다
- `400`, `COUPON_ALREADY_USED`: 이미 사용된 쿠폰입니다
- `400`, `COUPON_EXPIRED`: 쿠폰 유효 기간이 만료되었습니다
- `400`, `MISSING_DELIVERY_INFO`: 배송 정보를 입력해주세요

---

### 5.2 주문 목록 조회
**GET** `/users/{userId}/orders`

사용자의 주문 목록을 조회합니다.

**Path Parameters**
- `userId`: 사용자 ID

**Response** `200 OK`
```json
{
  "content": [
    {
      "orderId": 1,
      "totalAmount": 59800,
      "discountAmount": 5000,
      "finalAmount": 54800,
      "status": "CONFIRMED",
      "orderedAt": "2024-01-20T16:30:00",
      "updatedAt": "2024-01-20T16:35:00"
    }
  ]
}
```

**Error Responses**

- `404`, `USER_NOT_FOUND`: 사용자를 찾을 수 없습니다

---

### 5.3 주문 상세 조회
**GET** `/users/{userId}/orders/{orderId}`

주문의 상세 정보를 조회합니다.

**Path Parameters**
- `userId`: 사용자 ID
- `orderId`: 주문 ID

**Response** `200 OK`
```json
{
  "orderId": 1,
  "userId": 1,
  "totalAmount": 59800,
  "discountAmount": 5000,
  "finalAmount": 54800,
  "status": "CONFIRMED",
  "ordererName" : "홍길동",
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "orderedAt": "2024-01-20T16:30:00",
  "updatedAt": "2024-01-20T16:35:00",
  "appliedCoupon": {
    "userCouponId": 1,
    "couponName": "신규 가입 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000
  },
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "상품명",
      "price": 29900,
      "quantity": 2,
      "subtotal": 59800
    }
  ]
}
```

**Error Responses**

- `404`, `ORDER_NOT_FOUND`: 주문을 찾을 수 없습니다
- `403`, `FORBIDDEN_RESOURCE`: 해당 주문에 대한 접근 권한이 없습니다

---

## 6. 결제 (Payments)

### 6.1 결제 생성
**POST** `/orders/{orderId}/payments`

주문에 대한 포인트 결제를 생성합니다. 결제 실패 시 재고와 쿠폰이 복원됩니다.

**Path Parameters**
- `orderId`: 주문 ID

**Response** `201 Created`
```json
{
  "orderId": 1,
  "userId": 1,
  "totalAmount": 59800,
  "discountAmount": 5000,
  "finalAmount": 54800,
  "status": "CONFIRMED",
  "ordererName" : "홍길동",
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "orderedAt": "2024-01-20T16:30:00",
  "appliedCoupon": {
    "userCouponId": 1,
    "couponName": "신규 가입 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000
  },
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "상품명",
      "price": 29900,
      "quantity": 2,
      "subtotal": 59800
    }
  ]
}
```

**Error Responses**

- `404`, `ORDER_NOT_FOUND`: 주문을 찾을 수 없습니다
- `400`, `INSUFFICIENT_POINT`: 포인트 잔액이 부족합니다
- `400`, `ORDER_ALREADY_PAID`: 이미 결제된 주문입니다
- `400`, `INVALID_ORDER_STATUS`: 결제할 수 없는 주문 상태입니다

**Note**: 결제 실패 시 자동으로 다음 작업이 수행됩니다:
- 주문 상태가 `FAILED`로 변경
- 차감된 재고 복원
- 사용된 쿠폰 복원 (상태가 `ISSUED`로 변경)

---

## 비즈니스 로직 플로우

### 주문 및 결제 프로세스

1. **주문 생성** (`POST /orders`)
   - 장바구니 항목 검증
   - 재고 확인 및 차감
   - 쿠폰 적용 (선택적)
   - 쿠폰 사용 처리 (status: ISSUED → USED)
   - 주문 생성 (status: PENDING)
   - ORDER_PRODUCT에 상품 스냅샷 저장

2. **결제 생성** (`POST /orders/{orderId}/payments`)
   - 포인트 잔액 검증
   - 포인트 차감
   - POINT_HISTORY 기록 생성 (transaction_type: USE)
   - 성공 시: 주문 상태 → CONFIRMED
   - 실패 시: 보상 트랜잭션 (재고 복원, 쿠폰 복원, 주문 상태 → FAILED)

### 쿠폰 발급 프로세스 (선착순)

1. **쿠폰 발급** (`POST /users/{userId}/coupons`)
   - 쿠폰 유효기간 검증
   - 남은 수량 검증 (issued_quantity < total_quantity)
   - 중복 발급 검증 (user_id + coupon_id 유니크)
   - issued_quantity 증가
   - USER_COUPON 생성 (status: ISSUED)