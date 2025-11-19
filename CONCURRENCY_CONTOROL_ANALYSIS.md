# 동시성 제어가 필요한 기능 선별 및 분석 보고서

## 📋 목차
1. [개요](#개요)
2. [분석 환경](#분석-환경)
3. [발견된 동시성 제어 이슈](#발견된-동시성-제어-이슈)
4. [상세 분석](#상세-분석)
5. [해결 방안](#해결-방안)
6. [결론](#결론)

---

## 개요

본 문서는 e-커머스 애플리케이션의 동시성 제어가 필요한 기능을 식별하고 분석한 결과를 정리합니다.

### 분석 목적
- 다중 사용자 환경에서 발생 가능한 동시성 문제 식별
- Race Condition, Lost Update 등의 동시성 이슈 방지
- 데이터 일관성 및 정합성 보장

### 분석 범위
- Application Layer의 모든 Service 클래스
- 공유 자원에 대한 읽기/쓰기 작업
- 트랜잭션 경계 및 격리 수준

---

## 분석 환경

### 기술 스택
| 항목 | 버전/설정                       |
|------|-----------------------------|
| Framework | Spring Boot 3.5.7           |
| Language | Java 21                     |
| ORM | Spring Data JPA (Hibernate) |
| Database | Testcontainers (MySQL)      |
| Build Tool | Gradle 8.x                  |

### 트랜잭션 기본 설정
- 기본 격리 수준: `READ_COMMITTED` (Spring 기본값)
- 트랜잭션 전파: `REQUIRED` (기본값)
- 읽기 전용 최적화: `@Transactional(readOnly = true)` 활용

---

## 발견된 동시성 제어 이슈

### 📊 우선순위별 요약

| 우선순위 | 서비스            | 해결 방안         | 문제 영역                |
|----------|-------------------|---------------|----------------------|
| 🔴 긴급   | CouponService     | 낙관적 락 → 비관적 락 | 선착순 쿠폰 발급            |
| 🔴 긴급   | UserService       | 비관적 락         | 포인트 충전               |
| 🔴 긴급   | MakePaymentService| 비관적 락         | 재고 차감, 포인트 사용, 쿠폰 사용 |
| 🟡 중간   | MakeOrderService  | 비관적 락         | 쿠폰 중복 사용, 재고 검증      |
| 🟡 중간   | CartService       | 생략 또는 비관적 락   | 장바구니 수량 업데이트         |
| 🟢 낮음   | ProductService    | 원자적 쿼리 또는 생략  | 조회수 증가               |

---

## 상세 분석

### 1. 🔴 CouponService

#### Service
`src/main/java/com/example/hhplus_ecommerce/application/service/CouponService.java:43-64`

```java
@Transactional
@Retryable(
        value = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = MAX_RETRY_COUNT,
        backoff = @Backoff(delay = 1000)
)
public UserCouponResponse issueCoupon(Long userId, IssueCouponRequest request) {
    userRepository.findByIdOrThrow(userId);
    Coupon coupon = couponRepository.findByIdOrThrow(request.couponId());

    userCouponRepository.findByUserIdAndCoupon_Id(userId, request.couponId())
            .ifPresent(uc -> {
                throw new ConflictException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            });

    coupon.issue();
    couponRepository.save(coupon);

    UserCoupon userCoupon = UserCoupon.builder()
            .userId(userId)
            .coupon(coupon)
            .build();
    UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

    return UserCouponResponse.from(savedUserCoupon, coupon);
}
```

#### Entity
`src/main/java/com/example/hhplus_ecommerce/domain/model/Coupon.java:33-42`

```java
@Entity
public class Coupon extends BaseEntity {
    private Integer totalQuantity;
    private Integer issuedQuantity;

    @Version
    protected Long version;
    
    public void issue() {
        if(getRemainingQuantity() <= 0) {
            throw new ConflictException(CouponErrorCode.COUPON_SOLD_OUT);
        }
        this.issuedQuantity++;
    }
}
```

#### 동시성 문제 시나리오
- **시나리오 상황**  
  여러명이 동시에 쿠폰 발급을 요청함
  
- **동시성 제어를 하지 않은 경우**
```
시간 | Thread(User1)               | Thread(User2)
-----|----------------------------|---------------------------
T1   | issuedQuantity = 9 읽기     |
T2   |                            | issuedQuantity = 9 읽기
T3   | issuedQuantity = 10 쓰기    |
T4   |                            | issuedQuantity = 10 쓰기
T5   | COMMIT                     |
T6   |                            | COMMIT

- issue 메소드에서 충돌 오류가 발생하지 않아 UserCoupons이 정해진 10개보다 더 발급될 수 있음
- Lost Update 발생
```

- **현재 구현된 낙관적 락의 경우**
```
- 상황: version = 1 상태에서 User1, User2가 거의 동시에 쿠폰 발급 요청

시간 | Thread(User1)                 | Thread(User2)
-----|------------------------------|-------------------------------
T1   | version=1 읽기                |  
T2   |                              | version=1 읽기
T3   | issuedQuantity=9 쓰기        |
T4   | COMMIT (version=2로 증가)      |
T5   |                              | issuedQuantity=9 쓰기 시도
T6   |                              | COMMIT 시 OptimisticLockException 발생


- 상황: version = 2 상태에서 User2(재시도), User3가 거의 동시에 발급 요청

시간 | Thread(User3)                 | Thread(User2 재시도)
-----|------------------------------|-------------------------------
T1   | version=2 읽기                |
T2   |                              | version=2 읽기
T3   | issuedQuantity=10 쓰기       |
T4   | COMMIT (version=3로 증가)      |
T5   |                              | issuedQuantity=10 쓰기 시도
T6   |                              | COMMIT 시 OptimisticLockException 발생

- 먼저 발급 요청을 보낸 User2는 충돌이 일어나서 나중에 발급 요청을 보낸 User3보다 늦게 발급받거나 못 받을 수 있음
- 선착순 발급이 보장되지 않음 (비즈니스 정책 위반)
```

---

### 2. 🔴 UserService

#### Service
`src/main/java/com/example/hhplus_ecommerce/application/service/UserService.java:36-51`

```java
@Transactional
public PointResponse chargePoint(Long userId, ChargePointRequest request) {
    User user = userRepository.findByIdOrThrow(userId);

    user.chargePoint(request.amount());
    User savedUser = userRepository.save(user);

    PointHistory pointHistory = PointHistory.builder()
            .userId(userId)
            .transactionType(PointHistory.TransactionType.CHARGE)
            .amount(request.amount())
            .balanceAfter(savedUser.getPoint())
            .build();
    pointHistoryRepository.save(pointHistory);

    return PointResponse.from(savedUser);
}
```

#### Entity
`src/main/java/com/example/hhplus_ecommerce/domain/model/User.java:33-41`

```java
@Entity
public class User extends BaseEntity {
    @ColumnDefault("0L")
    @Builder.Default
    private Long point = 0L;

    public void chargePoint(Long point) {
        if(point <= 0 || point % 1000 != 0) {
            throw new BadRequestException(PointErrorCode.INVALID_CHARGE_AMOUNT);
        }
        this.point += point;
    }
}
```

#### 동시성 문제 시나리오
- **시나리오 상황**  
  Lost update. 동일한 사용자가 동시에 1000원을 10번 충전함

```
시간  | Thread(User1)            | Thread(User1)
-----|--------------------------|---------------------------
T1   | point = 0 읽기            |
T2   |                          | point = 0 읽기
T3   | point = 1,000  쓰기       |
T4   |                          | point = 1,000 쓰기
T5   | COMMIT                   | 
T6   |                          | COMMIT

- 동시에 같은 잔여 포인트를 불러와서 같은 값을 업데이트 함
```

---

### 3. 🔴 MakePaymentService

#### Service
`src/main/java/com/example/hhplus_ecommerce/application/service/MakePaymentService.java:36-59`

```java
@Transactional
public PaymentResponse execute(Long orderId) {
    Order order = orderRepository.findByIdOrThrow(orderId);
    User user = userRepository.findByIdOrThrow(order.getUserId());
    List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

    for (OrderItem item : orderItems) {
        Product product = productRepository.findByIdOrThrow(item.getProductId());
        product.subStockQuantity(item.getQuantity());
        productRepository.save(product);
    }

    if (order.getUserCouponId() != null) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrThrow(order.getUserCouponId());
        userCoupon.use();
        userCouponRepository.save(userCoupon);
    }

    user.usePoint(order.getFinalAmount());
    userRepository.save(user);

    order.confirm();
    orderRepository.save(order);

    return PaymentResponse.from(order);
}
```

##### Entity
`src/main/java/com/example/hhplus_ecommerce/domain/model/Product.java:25-30`
`src/main/java/com/example/hhplus_ecommerce/domain/model/UserCoupon.java:27-32`

```java
public void subStockQuantity(Integer stockQuantity) {
    if(!hasSufficientStock(stockQuantity)) {
        throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
    }
    this.stockQuantity -= stockQuantity;  // ⚠️ Lost Update 가능
}
```
```java
public void use() {
    if(isUsed()) {
        throw new ConflictException(CouponErrorCode.COUPON_ALREADY_USED);
    }
    this.status = UserCouponStatus.USED;  // ⚠️ Lost Update 가능
}
```

#### 동시성 문제 시나리오
- **시나리오 상황(1)**  
  재고가 1개 남은 상황에서 두 명이 동시에 결제 요청을 보냄

```
시간  | Thread(User1)                | Thread(User2)
-----|------------------------------|---------------------------
T1   | stockQuantity = 1 읽기        |
T2   |                              | stockQuantity = 1 읽기
T3   | hasSufficientStock() = true  |
T4   |                              | hasSufficientStock() = true
T5   | stockQuantity = 0 쓰기        |
T6   |                              | stockQuantity = 0 쓰기 (덮어씀)
T7   | COMMIT                       |
T8   |                              | COMMIT

- 같은 재고수를 불러와서 재고 검증 유효성 체크에 통과함
- 재고 1개인데 2개 주문 승인
```

- **시나리오 상황(2)**  
  사용자의 잔여 포인트가 10000원인 상황에서 10000원짜리 상품을 동시에 여러 번 결제함

```
시간  | Thread(User1)            | Thread(User1)
-----|--------------------------|---------------------------
T1   | point = 10,000 읽기       |
T2   |                          | point = 10,000 읽기
T3   | point = 0 쓰기            |
T4   |                          | point = 0 쓰기 (덮어씀)
T5   | COMMIT                   |
T6   |                          | COMMIT

- 같은 잔여 포인트를 불러와서 포인트 부족 검증에 통과함
- 포인트 10,000원만 차감, 주문 2개 승인
```

- **시나리오 상황(3)**  
  쿠폰 하나를 여러 결제에 적용하여 동시에 결제 요청함

```
시간  | Thread(User1)            | Thread(User1)
-----|--------------------------|---------------------------
T1   | status = ISSUED 읽기      |
T2   |                          | status = ISSUED 읽기
T3   | isUsed() = false         |
T4   |                          | isUsed() = false
T5   | status = USED 쓰기        |
T6   |                          | status = USED 쓰기 (덮어씀)
T7   | COMMIT                   |
T8   |                          | COMMIT

- 같은 쿠폰 상태를 불러와서 쿠폰 유효성 검증에 통과함
- 하나의 쿠폰으로 2개 주문에 할인 적
```

---

### 4. 🟡 MakeOrderService

#### Service
`src/main/java/com/example/hhplus_ecommerce/application/service/MakeOrderService.java:44-100`

```java
@Transactional
public OrderResponse execute(OrderRequest request) {
    User user = userRepository.findByIdOrThrow(request.userId());
    List<CartItem> cartItems = cartItemRepository.findByUserId(request.userId());

    for (CartItem cartItem : cartItems) {
        Product product = productRepository.findByIdOrThrow(cartItem.getProductId());
        if (product.hasSufficientStock(cartItem.getQuantity())) {
            throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
    }

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

    Order order = orderRepository.save(Order.builder()...build());
    List<OrderItem> orderItems = cartItems.stream()...toList();
    cartItemRepository.deleteByUserId(request.userId());

    return OrderResponse.from(order, orderItems);
}
```

#### 동시성 문제 시나리오
- **시나리오 상황**  
  TOCTOU (Time-of-Check to Time-of-Use). 재고가 하나 남은 상태에서 주문을 생성한 후 다른 사용자와 동시에 결제 요청을 함

```
시간  | Thread A                   | MakePaymentService
-----|----------------------------|---------------------------
T1   | 재고 검증: 1 >= 1 (OK)       |
T2   |                            | (Thread B가 먼저 결제 완료)
T3   |                            | 재고 차감: 1 - 1 = 0
T4   | 주문 생성 완료                |
T5   | 결제 요청...                 |
T6   |                            | 재고 차감 시도: 0 - 1 = -1 ❌

- 재고가 하나 남은 상태에서 주문이 생성되었으나 다른 사용자에 의해 결제되며 재고 부족으로 결제 실패
```

---

### 5. 🟡 CartService

#### Service
`src/main/java/com/example/hhplus_ecommerce/application/service/CartService.java:35-65`

```java
@Transactional
public CartItemResponse addCartItem(Long userId, AddCartItemRequest request) {
    userRepository.findByIdOrThrow(userId);
    Product product = productRepository.findByIdOrThrow(request.productId());  // ⚠️ 락 없음

    CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, request.productId())
            .map(existingCartItem -> {
                // 카트에 상품이 있는 경우
                int newQuantity = existingCartItem.getQuantity() + request.quantity();
                if (product.getStockQuantity() < newQuantity) {  // ⚠️ Race Condition
                    throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
                }
                existingCartItem.updateQuantity(newQuantity);  // ⚠️ 동시 추가 시 Lost Update
                return cartItemRepository.save(existingCartItem);
            })
            .orElseGet(() -> {
                // 새로운 상품 추가
                if (product.getStockQuantity() < request.quantity()) {
                    throw new ConflictException(ProductErrorCode.INSUFFICIENT_STOCK);
                }
                CartItem newCartItem = CartItem.builder()...build();
                return cartItemRepository.save(newCartItem);
            });

    return CartItemResponse.from(cartItem);
}
```

#### 동시성 문제 시나리오
- **시나리오 상황**  
  Lost update. 동일 상품을 동시에 여러개 추가함

```
현재 장바구니: 상품A 5개

시간  | Thread A (3개 추가)        | Thread B (2개 추가)
-----|--------------------------|---------------------------
T1   | quantity = 5 읽기         |
T2   |                          | quantity = 5 읽기
T3   | newQuantity = 5 + 3 = 8  |
T4   |                          | newQuantity = 5 + 2 = 7
T5   | quantity = 8 쓰기         |
T6   |                          | quantity = 7 쓰기 (덮어씀)
T7   | COMMIT                   |
T8   |                          | COMMIT

- 예상: 5개, 실제: 3개 ❌
```

**심각도 평가**: 중간
- 주문 단계에서 재고를 다시 검증하므로 실제 over-selling은 방지됨
- 하지만 사용자 경험(UX)이 나쁠수 있음 (장바구니 추가 성공 → 주문 실패)

---

### 6. 🟢 ProductService

#### Service
`src/main/java/com/example/hhplus_ecommerce/application/service/ProductService.java:26-31`

```java
@Transactional
public ProductResponse getProduct(Long productId) {
    Product product = productRepository.findByIdOrThrow(productId);
    productRepository.incrementViewCount(productId);  // ⚠️ 동시성 이슈
    return ProductResponse.from(product);
}
```

#### Entity
`src/main/java/com/example/hhplus_ecommerce/domain/model/Product.java:40-42`

```java
public void incrementViewCount() {
    this.viewCount++;  // ⚠️ Lost Update 가능
}
```

#### 동시성 문제 시나리오
- **시나리오 상황**  
  Lost update. 조회수를 동시에 올림

```
현재 조회수: 100

시간 | Thread A                   | Thread B
-----|----------------------------|---------------------------
T1   | viewCount = 100 읽기       |
T2   |                            | viewCount = 100 읽기
T3   | viewCount = 101 쓰기       |
T4   |                            | viewCount = 101 쓰기 (덮어씀)
T5   | COMMIT                      |
T6   |                            | COMMIT

예상: 102
실제: 101 ❌
```

**심각도 평가**: 낮음
- 비즈니스 크리티컬하지 않음 (조회수는 대략적인 지표)
- 약간의 부정확성은 허용 가능
- 성능 우선 고려 시 동시성 제어 생략 가능

---

## 해결 방안

### 동시성 제어 전략 개요

동시성 제어 방법은 크게 세 가지로 분류할 수 있습니다:

| 방법 | 설명 | 장점 | 단점 | 적용 시나리오 |
|------|------|------|------|--------------|
| **비관적 락** | 트랜잭션 시작 시 데이터에 락을 걸어 다른 트랜잭션의 접근을 차단 | - 데이터 정합성 보장<br>- 선착순 보장 | - 성능 저하<br>- 데드락 위험 | 충돌 빈도가 높은 경우<br>(쿠폰, 재고, 포인트) |
| **낙관적 락** | 트랜잭션 커밋 시점에 충돌을 감지하고 재시도 | - 높은 처리량<br>- 데드락 없음 | - 재시도 오버헤드<br>- 선착순 미보장 | 충돌 빈도가 낮은 경우 |
| **원자적 쿼리** | DB 수준에서 원자적 연산 수행 (e.g., `UPDATE ... SET count = count + 1`) | - 최고 성능<br>- 락 불필요 | - 복잡한 로직 구현 어려움 | 단순 증감 연산<br>(조회수) |

---

### 🔴 1. CouponService - 비관적 락 적용

#### 문제점
- 현재 낙관적 락(`@Version`) 사용 중
- 재시도 로직으로 인해 선착순이 보장되지 않음
- 먼저 요청한 사용자가 나중에 발급받거나 못 받을 수 있음

#### 해결 방안: 비관적 락(Pessimistic Lock) 전환

```java
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(Long id);
}
```

#### 동작 원리
```
시간 | Thread(User1)                      | Thread(User2)
-----|------------------------------------|---------------------------------
T1   | SELECT ... FOR UPDATE (락 획득)     |
T2   |                                    | SELECT ... FOR UPDATE (대기)
T3   | issuedQuantity = 9 읽기            |
T4   | 검증 통과                           |
T5   | issuedQuantity = 10 쓰기           |
T6   | COMMIT (락 해제)                   |
T7   |                                    | 락 획득
T8   |                                    | issuedQuantity = 10 읽기
T9   |                                    | 검증 실패 (COUPON_SOLD_OUT)
```

#### 장점
- ✅ 선착순 보장 (FIFO)
- ✅ 재시도 로직 불필요
- ✅ 정확한 발급 수량 제어

---

### 🔴 2. UserService - 비관적 락 적용

#### 문제점
- 동일 사용자가 동시에 포인트 충전 시 Lost Update 발생
- 잔액이 정확하게 반영되지 않음

#### 해결 방안: 비관적 락(Pessimistic Lock)

**Repository 수정**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(Long id);
}
```

#### 동작 원리
```
시간 | Thread(User1)                 | Thread(User1 - 동시 요청)
-----|-------------------------------|---------------------------
T1   | SELECT ... FOR UPDATE (락 획득) |
T2   |                               | SELECT ... FOR UPDATE (대기)
T3   | point = 0 읽기                 |
T4   | point = 1,000 쓰기             |
T5   | COMMIT (락 해제)               |
T6   |                               | 락 획득
T7   |                               | point = 1,000 읽기
T8   |                               | point = 2,000 쓰기
T9   |                               | COMMIT
```

#### 장점
- ✅ Lost Update 완전 방지
- ✅ 포인트 정합성 보장
- ✅ 구현 단순

---

### 🔴 3. MakePaymentService - 비관적 락 적용

#### 문제점
- 재고 차감, 포인트 사용, 쿠폰 사용 모두 동시성 이슈 존재
- 여러 리소스를 동시에 수정하므로 복잡도 높음

#### 해결 방안: 비관적 락 + 락 순서 일관성 유지

**Repository 수정**
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(Long id);
}

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.id = :id")
    Optional<UserCoupon> findByIdWithPessimisticLock(Long id);
}
```

#### 데드락 방지 전략
1. **락 획득 순서 일관성**: 항상 동일한 순서로 락 획득 (Product ID 오름차순 → UserCoupon → User)
2. **타임아웃 설정**: 락 대기 시간 제한
3. **트랜잭션 최소화**: 불필요한 로직은 트랜잭션 밖으로 이동

#### 동작 원리 (재고 차감)
```
시간 | Thread(User1)                      | Thread(User2)
-----|------------------------------------|---------------------------------
T1   | SELECT product FOR UPDATE (락 획득) |
T2   |                                    | SELECT product FOR UPDATE (대기)
T3   | stockQuantity = 1 읽기             |
T4   | hasSufficientStock() = true        |
T5   | stockQuantity = 0 쓰기             |
T6   | COMMIT (락 해제)                   |
T7   |                                    | 락 획득
T8   |                                    | stockQuantity = 0 읽기
T9   |                                    | hasSufficientStock() = false
T10  |                                    | INSUFFICIENT_STOCK 예외 발생
```

---

### 🟡 4. MakeOrderService - 비관적 락 적용

#### 문제점
- TOCTOU (Time-of-Check to Time-of-Use) 문제
- 주문 생성 시점의 재고 검증이 결제 시점에는 무의미

#### 해결 방안: 재고 검증 시 비관적 락 적용

#### 장점
- ✅ TOCTOU 문제 해결
- ✅ 재고 검증부터 주문 생성까지 원자적 수행
- ✅ 쿠폰 중복 사용 방지

#### 주의사항
- ⚠️ MakePaymentService에서도 동일한 순서로 락 획득 필요 (데드락 방지)
- ⚠️ 트랜잭션이 길어질 수 있으므로 타임아웃 설정 권장

---

### 🟡 5. CartService - 선택적 적용

#### 문제점
- 동일 상품 동시 추가 시 Lost Update
- 하지만 주문 단계에서 재검증하므로 실제 피해는 제한적

#### 해결 방안 A: 비관적 락 적용

```java
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartItem c WHERE c.userId = :userId AND c.productId = :productId")
    Optional<CartItem> findByUserIdAndProductIdWithPessimisticLock(Long userId, Long productId);
}
```

#### 해결 방안 B: 동시성 제어 생략

장바구니는 임시 데이터이고 주문 시점에 재검증하므로, UX 저하를 감수한다면 동시성 제어를 생략할 수도 있습니다.

**트레이드오프**
- 방안 A: 완벽한 동시성 제어, 약간의 성능 저하
- 방안 B: 높은 성능, 드물게 UX 이슈 발생 가능

---

### 🟢 6. ProductService - 원자적 쿼리 사용

#### 문제점
- 조회수 증가 시 Lost Update
- 하지만 비즈니스 크리티컬하지 않음

#### 해결 방안: 원자적 쿼리 (Atomic Update)

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :productId")
    void incrementViewCount(Long productId);
}
```

```java
@Entity
public class Product extends BaseEntity {
    private Integer viewCount;

    // incrementViewCount() 메서드 제거 (Repository에서 직접 처리)
}
```

#### 동작 원리
```sql
-- DB 수준에서 원자적으로 수행
UPDATE product SET view_count = view_count + 1 WHERE id = 1;
```

- 락 없이 원자적 연산 수행
- 최고의 성능
- Lost Update 발생하지 않음

#### 장점
- ✅ 락 불필요 (최고 성능)
- ✅ 동시성 안전
- ✅ 단순한 구현

#### 대안: 동시성 제어 생략
조회수는 대략적인 지표이므로, 약간의 부정확성을 허용하고 동시성 제어를 완전히 생략하는 것도 가능합니다.

---

## 결론

본 분석을 통해 총 6가지 동시성 이슈를 발견하고 각각에 대한 해결 방안을 제시했습니다.

### 핵심 요약
| 서비스 | 적용 방법 | 우선순위 | 예상 효과 |
|--------|----------|---------|----------|
| CouponService | 비관적 락 | 🔴 긴급 | 선착순 보장 + 정확한 발급 수량 |
| UserService | 비관적 락 | 🔴 긴급 | 포인트 정합성 100% 보장 |
| MakePaymentService | 비관적 락 | 🔴 긴급 | 재고/포인트/쿠폰 정합성 보장 |
| MakeOrderService | 비관적 락 | 🟡 중간 | TOCTOU 문제 해결 |
| CartService | 비관적 락 | 🟡 중간 | UX 개선 (선택) |
| ProductService | 원자적 쿼리 | 🟢 낮음 | 성능 + 정확성 |