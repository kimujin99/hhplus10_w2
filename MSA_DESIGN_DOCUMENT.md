# 마이크로서비스 아키텍처 전환을 위한 설계 문서

## 목표
서비스의 확장에 따라 어플리케이션 서버와 DB를 도메인별로 분리했을 때, **트랜잭션 처리의 한계와 대응 방안**에 대한 설계 문서

### 평가 기준
- ✅ 배포 단위의 도메인이 적절히 분리되어 있는지
- ✅ 트랜잭션의 분리에 따라 발생할 수 있는 문제를 명확히 이해하고 설명하고 있는지

---

## 핵심 의문점 분석

### 질문 1: 유사 Entity 그룹화 기준으로 MSA 분리가 맞는가?

**현황:**
```
- user 패키지: User, PointHistory (함께 관리)
- product 패키지: Product, ProductStock, PopularityScore
- coupon 패키지: Coupon, UserCoupon
- order 패키지: Order, OrderItem
```

**분석:**
✅ **맞습니다.** 이는 Domain-Driven Design (DDD)의 **Bounded Context** 개념과 일치합니다.

- User와 PointHistory는 **사용자 금융 관리**라는 하나의 비즈니스 맥락
- Product와 ProductStock은 **상품 관리**라는 하나의 맥락
- 같은 맥락 내 Entity들은 같은 DB를 사용하는 것이 자연스러움

---

### 질문 2: 도메인 간 조인이 필요하면 어디서 구현하는가?

**예시:**
```
주문 상세 조회 API
→ 사용자 이름, 상품명, 쿠폰명이 모두 필요한 상황
```

**해결책: API 조합 (API Composition)**

```
Order Service (주문 조회)
  ├─→ User Service API 호출 → 사용자 정보 조회
  ├─→ Product Service API 호출 → 상품 정보 조회
  └─→ Coupon Service API 호출 → 쿠폰 정보 조회

  결과 합성 → 클라이언트에 반환
```

**성능 최적화:**
- 캐싱: 자주 조회되는 데이터는 Redis에 캐시
- 배치 조회: 여러 상품 정보를 한 번에 조회
- 비동기: 필요 없는 데이터는 비동기로 조회

---

### 질문 3: 여러 도메인을 사용하는 서비스(UseCase)를 개별 애플리케이션으로 분리해야하는가?

**답변: 맞습니다.**

MakePaymentUseCase는 **Saga 패턴의 Orchestrator** 역할을 합니다.

```
MakePaymentUseCase (Order Service 내)
  ├─→ Step 1: Product Service API 호출 (재고 차감)
  ├─→ Step 2: User Service API 호출 (포인트 차감)
  ├─→ Step 3: Coupon Service API 호출 (쿠폰 사용)
  ├─→ Step 4: Order Service (주문 저장)

  실패 시:
  ├─→ Compensation 1: 재고 복구
  ├─→ Compensation 2: 포인트 환급
  └─→ Compensation 3: 쿠폰 복구
```

**구현 원칙:**
- Order Service가 조정자(Orchestrator) 역할
- 각 단계는 별도의 API 호출로 격리
- 실패 시 보상 트랜잭션으로 일관성 복구

---

### 질문 4: 2PC vs Saga 패턴 - MSA 환경에서 Saga 패턴을 주로 차용하는 이유는?

**2PC (Two-Phase Commit)를 사용할 수 없는 이유:**

```
2PC란 모든 서비스가 "준비됐다" 신호를 보낸 후에
동시에 커밋하는 분산 트랜잭션 패턴입니다.

1단계 (Prepare Phase):
  Order Service: "커밋 가능한가?" → YES
  Product Service: "커밋 가능한가?" → YES
  User Service: "커밋 가능한가?" → YES

2단계 (Commit Phase):
  모두 YES일 때만 → COMMIT (원자성 보장!)
  하나라도 NO면 → 모두 ROLLBACK

장점: 원자성 보장 (모놀리식처럼)
단점:
  - 성능 저하 (오버헤드 높음)
  - 네트워크 지연에 취약
  - 한 서비스 지연 → 전체 지연
  - 분산 시스템에서 실패 가능성 높음
```

**왜 MSA에서는 2PC를 피하는가?**

```
실제 MSA 환경:

┌──────────────┐
│ Order Service │ (Master)
└────────┬─────┘
         │ "Prepare?"
    ┌────▼────────────┐
    │                 │
    ▼                 ▼
┌─────────────┐  ┌─────────────┐
│Product Svc  │  │ User Service │
└─────────────┘  └─────────────┘

문제 1: 네트워크 지연
Product Service가 응답 안 함 → 모두 블로킹
(마이크로서비스는 네트워크 호출이 많음)

문제 2: 분산 락 필요
Prepare 단계에서 모든 리소스 락 유지
(리소스 낭비, 교착 상태 위험)

문제 3: 복구 불가능
네트워크 실패 시 어떤 서비스는 커밋, 어떤 서비스는 롤백
(일관성 깨짐)
```

**그래서 MSA는 Saga 패턴을 사용:**

```
Saga는 "최종 일관성"을 보장
각 단계가 독립적인 트랜잭션 + 실패 시 보상

Step 1: 재고 차감 (Product Service) → COMMIT ✅
Step 2: 포인트 차감 (User Service) → COMMIT ✅
Step 3: 주문 저장 (Order Service) → COMMIT ❌ (실패!)

실패 시:
  보상 2: 포인트 복구 (User Service)
  보상 1: 재고 복구 (Product Service)

결과: 최종적으로 일관성 복구됨 ✅

장점:
  - 빠른 응답 (각 단계가 독립)
  - 네트워크 지연에 강함
  - 부분 실패 처리 가능
```

**비교표:**

| 항목 | 2PC | Saga |
|------|-----|------|
| 원자성 | 즉시 보장 | 최종 일관성 |
| 성능 | 느림 (블로킹) | 빠름 (비동기) |
| 네트워크 강건성 | 약함 | 강함 |
| 구현 복잡도 | 낮음 | 높음 |
| MSA 적합성 | ❌ 부적합 | ✅ 최적 |

**결론: 현재 프로젝트에서 Saga를 선택한 이유**

1. ✅ 도메인이 4개 → 보상 로직 관리 가능
2. ✅ 네트워크 지연 가능성 > 원자성 필요성
3. ✅ 최종 일관성(Outbox 패턴)으로 데이터 안전성 보장
4. ✅ 부분 실패(예: User Service 장애)에 대응 가능

---

### 질문 5: MSA 환경의 동시성 제어 - 분산락 필수인가?

#### 5.1 상황 분석

**시나리오:**
```java
class MakePaymentUseCase {
    public void execute(Long orderId) {
        productStock_차감();      // Product Service (별도 DB)
        coupon_차감();           // Coupon Service (별도 DB)
        point_차감();           // User Service (별도 DB)
    }
}
```

**질문: 주문 ID로 분산락을 거는 게 아닌데, 왜 비관락만으로 안 되는가?**

#### 5.2 비관락의 한계

```java
// 비관락 (Pessimistic Lock) - 같은 DB 트랜잭션 내에서만 유효
@Lock(LockModeType.PESSIMISTIC_WRITE)
public Product getProductForUpdate(Long productId) {
    // 같은 DB에서의 동시 접근 제어만 가능
}

// 문제: 서로 다른 DB에서는 효과 없음!

Thread A (Order Service 1)           Thread B (Order Service 2)
  ├─ Product Service API 호출         ├─ Product Service API 호출
  │   ├─ 같은 상품 조회              │   ├─ 같은 상품 조회
  │   └─ 비관락 획득 ✅              │   └─ 비관락 획득 ✅ (별도 트랜잭션!)
  │   └─ 재고 100 → 99로 차감         │   └─ 재고 100 → 99로 차감 ← 문제!
  └─ Commit                          └─ Commit

결과: 재고가 98이어야 하는데 99로 됨 (동시성 제어 실패!)
```

#### 5.3 분산락이 필수인 이유

```
MSA 환경의 특성:
┌────────────┐     API      ┌────────────┐
│Order Svc 1 │──────────→   │Product Svc │
└────────────┘              └────────────┘
                            (별도 프로세스)
┌────────────┐              ┌────────────┐
│Order Svc 2 │──────────→   │ Product DB │
└────────────┘              └────────────┘

각각 독립된 트랜잭션!
→ 비관락으로 제어 불가능
→ Redis 분산락 필수
```

#### 5.4 정리

**질문: 비관락만으로 안 되는가?**

❌ **안 됩니다.** MSA 환경에서는 비관락이 다른 서비스의 DB에 영향을 주지 못합니다.

✅ **분산락(Redis)이 필수**입니다.

- **같은 DB 내 리소스**: 비관락 + 로컬 트랜잭션
- **다른 서비스의 리소스**: 분산락 필수

---

## 1. MSA로 분리할 적절한 도메인 구조

### 1.1 적용할 도메인 구조

**각 도메인은 다음 기준으로 독립적인 서비스로 분리:**

| 도메인 | DB | Entity | 책임 | 독립성 |
|--------|----|----|------|--------|
| **User Service** | user_db | User, PointHistory | 사용자, 포인트 관리 | ✅ 높음 |
| **Product Service** | product_db | Product, ProductStock, PopularityScore | 상품, 재고, 인기도 | ✅ 높음 |
| **Coupon Service** | coupon_db | Coupon, UserCoupon | 쿠폰 발급, 사용 | ✅ 높음 |
| **Order Service** | order_db | Order, OrderItem | 주문, 결제 | ⚠️ 중간 |

---

## 2. 분리된 서비스 간의 트랜잭션 한계와 문제

### 2.1 모놀리식 vs MSA의 트랜잭션 모델

**모놀리식 (현재):**
```
한 개의 트랜잭션으로 처리
BEGIN
  - 재고 차감 (Product Table)
  - 포인트 차감 (PointHistory Table)
  - 주문 저장 (Order Table)
COMMIT or ROLLBACK (원자성 보장)
```

**MSA:**
```
도메인별 독립 트랜잭션
Product Service: BEGIN → 재고 차감 → COMMIT ✅
User Service: BEGIN → 포인트 차감 → COMMIT ✅
Order Service: BEGIN → 주문 저장 → COMMIT ❌ (실패!)

결과: 재고와 포인트는 차감되었는데 주문은 안 됨 (불일치!)
```

### 2.2 핵심 문제점

| 문제 | 원인 | 영향 |
|------|------|------|
| **원자성 부재** | 각 서비스가 독립 트랜잭션 | 부분 실패 가능 |
| **2PC 불가** | 서비스 간 물리적 분리 | 전체 롤백 불가능 |
| **최종 일관성** | 비동기 처리 필요 | 일시적 불일치 허용 |
| **보상 트랜잭션** | 실패 시 역작업 필요 | 구현 복잡도 증가 |

---

## 3. 적용할 패턴: Orchestrated Saga

### 3.1 Saga 패턴 개요

**Saga란:**
분산 트랜잭션을 일련의 로컬 트랜잭션들로 분해하고, 실패 시 보상 트랜잭션으로 일관성을 복구하는 패턴입니다.

### 3.2 Choreographed Saga vs Orchestrated Saga

**Choreographed Saga (이벤트 기반)**
```
Order Service → 발행 "PaymentCompleted" 이벤트
  ↓
Product Service (리스너) → "재고 차감" → 발행 "StockDecreased" 이벤트
  ↓
User Service (리스너) → "포인트 차감" → 발행 "PointDeducted" 이벤트
  ↓
Order Service (리스너) → "주문 확정" → COMMIT

장점: 느슨한 결합, 확장성 좋음
단점: 실패 처리 복잡, 디버깅 어려움, 순환 의존성 위험
```

**Orchestrated Saga (중앙 조정, 권장)**
```
MakePaymentUseCase (Orchestrator)
  ├─> 1. ProductService.decreaseStock()
  │     └─> 실패 시: ProductService.restoreStock()
  │
  ├─> 2. UserService.deductPoint()
  │     └─> 실패 시: UserService.refundPoint()
  │
  ├─> 3. CouponService.useCoupon()
  │     └─> 실패 시: CouponService.cancelUseCoupon()
  │
  └─> 4. OrderRepository.save(order)
        └─> 실패 시: 전체 보상 트랜잭션 실행

장점: 흐름이 명확, 실패 처리 간단, 테스트 용이
단점: 중앙 조정자가 병목, 확장성 제한
```

### 3.3 Orchestrated Saga 패턴을 선택한 이유

1. **도메인 수 (4개)** → 복잡도 관리 가능
2. **기존 설계 (MakePaymentUseCase)** → 자연스러운 Orchestrator로 확장
3. **팀 규모** → 한 팀에서 관리 가능
4. **디버깅 용이** → 실패 흐름이 명확

---

## 4. 동시성 제어 전략

### 4.1 상황별 제어 방식

| 상황 | 제어 방식 | 이유 |
|------|---------|------|
| **단일 서비스 내 리소스** (User의 PointHistory) | 비관락 (Pessimistic Lock) | 같은 DB 트랜잭션 |
| **여러 서비스의 리소스** (Product 재고, Coupon) | Redis 분산락 (Distributed Lock) | 물리적으로 분리된 DB |
| **고빈도 접근** (인기상품 점수) | Redis 분산락 | 동시성 높음 |

---

## 5. 구현 로드맵

### Phase 1: 기초 (현재)
- ✅ 도메인별 Entity 분리 완료
- ✅ 패키지 구조 설정
- ⏳ Saga 패턴 이해 및 설계

### Phase 2: Saga 패턴 도입
- Orchestrated Saga 구현
- 보상 트랜잭션 로직 추가
- 이벤트 발행 메커니즘 구축

### Phase 3: 신뢰성 강화
- Outbox 패턴 구현 (이벤트 유실 방지)
- 분산 락 적용 (동시성 제어)
- 모니터링 및 알림 체계 구축

### Phase 4: 마이크로서비스 완전 분리
- 각 도메인을 독립 서비스로 배포
- API Gateway 구성
- 서비스 간 비동기 통신 확대
- 분산 추적(Distributed Tracing) 도입

---

## 6. 결론

### 6.1 핵심 설계 결정

**채택할 아키텍처:**

| 항목 | 결정 | 이유 |
|------|------|------|
| **도메인 분리 기준** | Bounded Context (DDD) | Entity 그룹과 일치 |
| **도메인 간 상호작용** | REST API (API Composition) | 느슨한 결합 |
| **트랜잭션 패턴** | Orchestrated Saga | 명확한 흐름, 관리 용이 |
| **일관성 보장** | 최종 일관성 (Eventual Consistency) | 분산 환경에 적합 |
| **이벤트 신뢰성** | Outbox 패턴 | 이벤트 유실 방지 |
| **동시성 제어** | 비관락 + 분산락 혼합 | 상황별 최적화 |

### 6.2 기대 효과

```
모놀리식 → MSA 전환을 통해:

1. 확장성 (Scalability)
   - 도메인별 독립 배포 가능
   - 트래픽에 따른 선택적 확장

2. 유지보수성 (Maintainability)
   - 도메인별 독립적 개발
   - 버전 관리 용이

3. 신뢰성 (Reliability)
   - 서비스 격리로 장애 전파 방지
   - 보상 트랜잭션으로 일관성 복구

4. 기술 다양성 (Technology Heterogeneity)
   - 도메인별 최적의 기술 스택 선택 가능
   - 독립적인 기술 버전 관리
```

### 6.3 주의사항

```
1. 복잡도 증가
   - 분산 트랜잭션 관리 필요
   - 디버깅 및 모니터링 어려움
   → 해결책: 명확한 로깅, 분산 추적

2. 네트워크 지연
   - 서비스 간 API 호출로 인한 지연
   → 해결책: 캐싱, 비동기 처리

3. 데이터 일관성
   - 물리적 트랜잭션 분리로 인한 부정합
   → 해결책: Saga + Outbox 패턴

4. 운영 복잡도
   - 여러 서비스 모니터링 필요
   → 해결책: 중앙화된 로깅, 알림 시스템