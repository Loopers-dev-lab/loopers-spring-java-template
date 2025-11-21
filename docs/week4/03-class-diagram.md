# 03. 클래스 다이어그램

## 도메인 모델 개요

이커머스 서비스의 핵심 도메인 객체들과 그들 간의 관계를 정의합니다.
DDD 기반 레이어드 아키텍처를 따르며, BaseEntity를 상속한 엔티티 설계를 적용합니다.

---

## 주요 도메인 규칙

### 비즈니스 제약사항

| 도메인 | 핵심 규칙 |
|--------|-----------|
| **User** | 포인트는 음수 불가, 차감 시 잔액 검증 필수 |
| **Product** | 재고는 음수 불가, 예약 시 가용 수량 검증 필수 |
| **Order** | 재고 예약과 포인트 차감은 원자적 처리 |
| **Like** | 사용자당 상품별 1개만 가능, 멱등성 보장 |
| **Brand** | 브랜드명 중복 불가, 소프트 삭제 지원 |
| **Coupon** | 1회성 사용, 사용 후 재사용 불가, 동시성 제어 필수 |

### 도메인 관계 요약

| 관계                   | 설명                | 다중성 | 비고 |
|----------------------|-------------------|-----|------|
| Product -> Brand     | 상품은 브랜드에 속함       | N:1 | 필수 관계 |
| Like -> User         | 좋아요는 사용자가 생성      | N:1 | 필수 관계 |
| Like -> Product      | 좋아요는 상품에 대한 것     | N:1 | 필수 관계 |
| Order -> User        | 주문은 사용자가 생성       | N:1 | 필수 관계 |
| OrderItem -> Order   | 주문 상세내역은 주문 정보 포함 | N:1 | 필수 관계 |
| OrderItem -> Product | 주문항목은 상품을 참조      | N:1 | 필수 관계 |
| PointHistory -> User | 포인트 이력은 사용자별 관리  | N:1 | 필수 관계 |
| Coupon -> User       | 쿠폰은 사용자에게 발급됨    | N:1 | 필수 관계 |
| OrderItem -> Coupon  | 주문항목에 쿠폰 적용 가능   | N:1 | 선택 관계 |
| CouponUsageHistory -> Coupon | 쿠폰 사용 이력 기록 | N:1 | 필수 관계 |
| CouponUsageHistory -> OrderItem | 어떤 주문에 사용되었는지 추적 | N:1 | 필수 관계 |

---

## 전체 도메인 모델

```mermaid
classDiagram
    class UserEntity {
        -String username
        -String email
        -LocalDate birthdate
        -Gender gender
        -BigDecimal pointAmount
        +createUserEntity() UserEntity
        +chargePoint() void
        +usePoint() void
        +getPointAmount() BigDecimal
    }

    class BrandEntity {
        -String name
        -String description
        +createBrandEntity() BrandEntity
    }

    class ProductEntity {
        -Long brandId
        -String name
        -String description
        -Price price
        -Integer stockQuantity
        -Long likeCount
        +createEntity() ProductEntity
        +deductStock() void
        +restoreStock() void
        +increaseLikeCount() void
        +decreaseLikeCount() void
        +hasStock() boolean
        +canOrder() boolean
        +isDiscounted() boolean
        +getSellingPrice() BigDecimal
    }
    
    class Price {
        <<Value Object>>
        -BigDecimal originPrice
        -BigDecimal discountPrice
        +of() Price
        +getSellingPrice() BigDecimal
        +getDiscountRate() BigDecimal
        +isDiscounted() boolean
        +applyDiscount() void
        +removeDiscount() void
    }

    class LikeEntity {
        -Long userId
        -Long productId
        +createEntity() LikeEntity
    }

    class OrderEntity {
        -Long userId
        -BigDecimal totalAmount
        -OrderStatus status
        +createOrder() OrderEntity
        +confirmOrder() void
        +cancelOrder() void
        +isPending() boolean
        +isConfirmed() boolean
        +isCancelled() boolean
    }

    class OrderItemEntity {
        -Long orderId
        -Long productId
        -Integer quantity
        -BigDecimal unitPrice
        -BigDecimal totalPrice
        +createOrderItem() OrderItemEntity
        +calculateItemTotal() BigDecimal
    }

    class PointHistoryEntity {
        -UserEntity user
        -BigDecimal amount
        -PointTransactionType transactionType
        -BigDecimal balanceAfter
        +createChargeHistory() PointHistoryEntity
        +createUseHistory() PointHistoryEntity
    }

    class CouponEntity {
        -Long userId
        -CouponType couponType
        -BigDecimal fixedAmount
        -BigDecimal percentage
        -CouponStatus status
        +createCoupon() CouponEntity
        +use() void
        +isUsed() boolean
        +canUse() boolean
        +calculateDiscount() BigDecimal
    }

    class CouponUsageHistoryEntity {
        -Long couponId
        -Long orderItemId
        -BigDecimal discountAmount
        +createHistory() CouponUsageHistoryEntity
    }

    UserEntity "1" --> "*" LikeEntity : userId
    UserEntity "1" --> "*" OrderEntity : userId
    UserEntity "1" --> "*" PointHistoryEntity : ManyToOne
    UserEntity "1" --> "*" CouponEntity : userId
    ProductEntity "1" --> "*" LikeEntity : productId
    BrandEntity "1" --> "*" ProductEntity : brandId
    ProductEntity "1" *-- "1" Price : 임베디드
    OrderEntity "1" --> "1..*" OrderItemEntity : orderId
    OrderItemEntity "*" --> "1" ProductEntity : productId
    OrderItemEntity "*" --> "0..1" CouponEntity : couponId (선택)
    CouponEntity "1" --> "*" CouponUsageHistoryEntity : couponId
    OrderItemEntity "1" --> "*" CouponUsageHistoryEntity : orderItemId
```

---

## 도메인별 상세 다이어그램

### 1. 사용자 도메인 (User Domain)

```mermaid
classDiagram
    class UserEntity {
        -String username
        -String email
        -LocalDate birthdate
        -Gender gender
        -BigDecimal pointAmount
        +createUserEntity() UserEntity
        +chargePoint() void
        +usePoint() void
        +getPointAmount() BigDecimal
    }

    class PointHistoryEntity {
        -UserEntity user
        -BigDecimal amount
        -PointTransactionType transactionType
        -BigDecimal balanceAfter
        +createChargeHistory() PointHistoryEntity
        +createUseHistory() PointHistoryEntity
    }

    UserEntity "1" --> "*" PointHistoryEntity : ManyToOne 관계
```

### 2. 상품 도메인 (Product Domain)

```mermaid
classDiagram
    class BrandEntity {
        -String name
        -String description
        +createBrandEntity() BrandEntity
    }

    class ProductEntity {
        -Long brandId
        -String name
        -String description
        -Price price
        -Integer stockQuantity
        -Long likeCount
        +createEntity() ProductEntity
        +deductStock() void
        +restoreStock() void
        +increaseLikeCount() void
        +decreaseLikeCount() void
        +hasStock() boolean
        +canOrder() boolean
        +isDiscounted() boolean
        +getSellingPrice() BigDecimal
    }
    
    class Price {
        <<Value Object>>
        -BigDecimal originPrice
        -BigDecimal discountPrice
        +of() Price
        +getSellingPrice() BigDecimal
        +getDiscountRate() BigDecimal
        +isDiscounted() boolean
        +getDiscountAmount() BigDecimal
        +applyDiscount() void
        +removeDiscount() void
    }

    BrandEntity "1" --> "*" ProductEntity : brandId 참조
    ProductEntity "1" *-- "1" Price : 임베디드 타입
```

### 3. 좋아요 도메인 (Like Domain)

```mermaid
classDiagram
    class LikeEntity {
        -Long userId
        -Long productId
        +createEntity() LikeEntity
    }

    class UserEntity {
        -String username
        -String email
        -LocalDate birthdate
        -Gender gender
        -BigDecimal pointAmount
        +createUserEntity() UserEntity
        +chargePoint() void
        +usePoint() void
    }

    class ProductEntity {
        -Long brandId
        -String name
        -Price price
        -Integer stockQuantity
        -Long likeCount
        +createEntity() ProductEntity
        +increaseLikeCount() void
        +decreaseLikeCount() void
    }

    LikeEntity "*" --> "1" UserEntity : userId 참조
    LikeEntity "*" --> "1" ProductEntity : productId 참조
```

### 4. 주문 도메인 (Order Domain)

```mermaid
classDiagram
    class OrderEntity {
        -Long userId
        -BigDecimal totalAmount
        -OrderStatus status
        +createOrder() OrderEntity
        +confirmOrder() void
        +cancelOrder() void
        +isPending() boolean
        +isConfirmed() boolean
        +isCancelled() boolean
    }

    class OrderItemEntity {
        -Long orderId
        -Long productId
        -Long couponId
        -Integer quantity
        -BigDecimal unitPrice
        -BigDecimal discountAmount
        -BigDecimal totalPrice
        +createOrderItem() OrderItemEntity
        +createOrderItemWithCoupon() OrderItemEntity
        +calculateItemTotal() BigDecimal
        +hasCoupon() boolean
    }

    class UserEntity {
        -String username
        -String email
        -LocalDate birthdate
        -Gender gender
        -BigDecimal pointAmount
        +createUserEntity() UserEntity
        +chargePoint() void
        +usePoint() void
    }

    class ProductEntity {
        -Long brandId
        -String name
        -Price price
        -Integer stockQuantity
        -Long likeCount
        +createEntity() ProductEntity
        +deductStock() void
        +restoreStock() void
    }

    OrderEntity "1" --> "*" OrderItemEntity : orderId 참조
    OrderEntity "*" --> "1" UserEntity : userId 참조
    OrderItemEntity "*" --> "1" ProductEntity : productId 참조
```

### 5. 쿠폰 도메인 (Coupon Domain)

```mermaid
classDiagram
    class CouponEntity {
        -Long userId
        -CouponType couponType
        -BigDecimal fixedAmount
        -BigDecimal percentage
        -CouponStatus status
        +createCoupon() CouponEntity
        +use() void
        +isUsed() boolean
        +canUse() boolean
        +calculateDiscount() BigDecimal
        +validateOwnership() void
    }

    class CouponUsageHistoryEntity {
        -Long couponId
        -Long orderItemId
        -BigDecimal discountAmount
        +createHistory() CouponUsageHistoryEntity
    }

    class UserEntity {
        -String username
        -String email
        -LocalDate birthdate
        -Gender gender
        -BigDecimal pointAmount
        +createUserEntity() UserEntity
    }

    class OrderItemEntity {
        -Long orderId
        -Long productId
        -Long couponId
        -Integer quantity
        -BigDecimal unitPrice
        -BigDecimal discountAmount
        -BigDecimal totalPrice
        +createOrderItemWithCoupon() OrderItemEntity
    }

    CouponEntity "*" --> "1" UserEntity : userId 참조
    CouponEntity "1" --> "*" CouponUsageHistoryEntity : couponId 참조
    OrderItemEntity "0..1" --> "1" CouponEntity : couponId 참조 (선택)
    CouponUsageHistoryEntity "*" --> "1" OrderItemEntity : orderItemId 참조
```

**쿠폰 타입 (CouponType)**
- `FIXED_AMOUNT`: 정액 할인 (예: 5,000원 할인)
- `PERCENTAGE`: 배율 할인 (예: 10% 할인)

**쿠폰 상태 (CouponStatus)**
- `UNUSED`: 미사용 (사용 가능)
- `USED`: 사용됨 (재사용 불가)

---

## 도메인 엔티티 핵심 책임

### 1. UserEntity - 사용자 도메인의 핵심 책임

| 메서드 | 책임       | 비즈니스 규칙                  | 구현 세부사항 |
|--------|----------|--------------------------|---------------|
| `createUserEntity()` | 사용자 생성   | 사용자명/이메일 유효성 검증, 생년월일 검증 | 정적 팩토리 메서드, 불변 객체 생성 |
| `chargePoint()` | 포인트 충전   | 충전 금액 양수 검증    | BigDecimal 정밀 계산, 원자적 연산 |
| `usePoint()` | 포인트 사용   | 잔액 부족 시 예외 발생, 음수 방지     | 원자적 연산, 상태 변경 후 검증 |
| `getPointAmount()` | 포인트 조회   | 소수점 2자리로 반올림    | RoundingMode.HALF_UP 적용 |

### 2. ProductEntity - 상품 도메인의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `createEntity()` | 상품 생성 | 가격 유효성, 재고 음수 방지, 브랜드 ID 필수 | 정적 팩토리 메서드, Price 임베디드 타입 |
| `hasStock()` | 재고 확인 | 재고 수량 > 0 | 단순 재고 존재 여부 확인 |
| `canOrder()` | 주문 가능 여부 | 재고 >= 수량, 수량 > 0 | 복합 조건 검증 |
| `deductStock()` | 재고 차감 | 재고 부족 시 INSUFFICIENT_STOCK 예외 | 원자적 재고 차감 연산 |
| `restoreStock()` | 재고 복구 | 취소 시 재고 복원 | 재고 복구 연산 |
| `increaseLikeCount()` | 좋아요 수 증가 | 음수 방지, 동시성 제어 | 원자적 증가 연산 |
| `decreaseLikeCount()` | 좋아요 수 감소 | 0 이하로 감소 방지 | 최소값 0 보장 |
| `isDiscounted()` | 할인 여부 | Price 객체의 할인가 존재 여부 | Price.isDiscounted() 위임 |
| `getSellingPrice()` | 판매가 조회 | 할인가 우선, 없으면 정가 | Price.getSellingPrice() 위임 |

### 3. OrderEntity - 주문 도메인의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `createOrder()` | 주문 생성 | 사용자 ID 필수, 총액 > 0 | 정적 팩토리 메서드, 초기 상태 PENDING |
| `confirmOrder()` | 주문 확정 | PENDING → CONFIRMED 상태 전이 | INVALID_ORDER_STATUS 예외 처리 |
| `cancelOrder()` | 주문 취소 | PENDING/CONFIRMED → CANCELLED | 취소 가능 상태 검증 |
| `isPending()` | 대기 상태 확인 | 주문 상태가 PENDING인지 확인 | 상태 검증 메서드 |
| `isConfirmed()` | 확정 상태 확인 | 주문 상태가 CONFIRMED인지 확인 | 상태 검증 메서드 |
| `isCancelled()` | 취소 상태 확인 | 주문 상태가 CANCELLED인지 확인 | 상태 검증 메서드 |

### 4. OrderItemEntity - 주문 항목 도메인의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `createOrderItem()` | 주문 항목 생성 | 주문 ID, 상품 ID 필수, 수량 >= 1, 단가 > 0 | 정적 팩토리 메서드, 스냅샷 가격 |
| `calculateItemTotal()` | 항목 총액 계산 | 단가 × 수량 = 총액 | BigDecimal 정밀 계산 |

### 5. LikeEntity - 좋아요 도메인의 핵심 책임

| 메서드                   | 책임          | 비즈니스 규칙                 | 구현 세부사항           |
|-----------------------|-------------|-------------------------|-------------------|
| `createEntity()`        | 좋아요 생성      | 사용자 ID, 상품 ID 필수, 복합 유니크 제약   | 정적 팩토리 메서드, 복합키 검증 |

### 6. BrandEntity - 브랜드 도메인의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `createBrandEntity()` | 브랜드 생성 | 브랜드명 필수, 설명 선택사항, 길이 <= 100 | 정적 팩토리 메서드, 유니크 제약 |

### 7. PointHistoryEntity - 포인트 이력 도메인의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `createChargeHistory()` | 충전 이력 생성 | 충전 금액 양수, 잔액 일치성 | 정적 팩토리 메서드, CHARGE 타입 |
| `createUseHistory()` | 사용 이력 생성 | 사용 금액 양수, 잔액 일치성 | 정적 팩토리 메서드, USE 타입 |

### 8. CouponEntity - 쿠폰 도메인의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `createCoupon()` | 쿠폰 생성 | 사용자 ID 필수, 쿠폰 타입별 금액/비율 검증 | 정적 팩토리 메서드, 초기 상태 UNUSED |
| `use()` | 쿠폰 사용 | UNUSED → USED 상태 전이, 이미 사용된 경우 예외 | 1회성 보장, 동시성 제어 필요 |
| `isUsed()` | 사용 여부 확인 | 쿠폰 상태가 USED인지 확인 | 상태 검증 메서드 |
| `canUse()` | 사용 가능 여부 | 쿠폰 상태가 UNUSED인지 확인 | 사용 전 검증 메서드 |
| `calculateDiscount()` | 할인액 계산 | FIXED_AMOUNT: fixedAmount 반환<br/>PERCENTAGE: 상품가 × percentage / 100 | 쿠폰 타입별 할인 계산 로직 |
| `validateOwnership()` | 소유권 검증 | 쿠폰 소유자와 사용자 일치 확인 | 권한 검증, 불일치 시 예외 |

### 9. CouponUsageHistoryEntity - 쿠폰 사용 이력 도메인의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `createHistory()` | 사용 이력 생성 | 쿠폰 ID, 주문항목 ID, 할인액 필수 | 정적 팩토리 메서드, 추적 가능성 보장 |

### 10. Price - 가격 임베디드 타입의 핵심 책임

| 메서드 | 책임 | 비즈니스 규칙 | 구현 세부사항 |
|--------|------|---------------|---------------|
| `of()` | Price 객체 생성 | 정가 필수, 할인가 선택사항 | 정적 팩토리 메서드, 불변 객체 |
| `ofOriginOnly()` | 정가만으로 생성 | 정가 > 0 | 할인가 없는 Price 생성 |
| `getSellingPrice()` | 실제 판매가 반환 | 할인가 우선, 없으면 정가 | 할인가 존재 여부에 따른 분기 |
| `getDiscountRate()` | 할인율 계산 | (정가-할인가)/정가 * 100 | BigDecimal 정밀 계산, 소수점 2자리 |
| `isDiscounted()` | 할인 여부 확인 | 할인가가 null이 아니고 정가보다 작음 | 할인가 유효성 검증 |
| `getDiscountAmount()` | 할인 금액 계산 | 정가 - 할인가 | 할인 절약 금액 반환 |
| `applyDiscount()` | 할인 적용 | 할인가 >= 0, 할인가 <= 정가 | 할인가 유효성 검증 후 적용 |
| `removeDiscount()` | 할인 제거 | 할인가를 null로 설정 | 할인 정보 초기화 |

---

## 쿠폰 도메인 상세 설계

### 쿠폰 타입별 할인 계산 로직

#### 1. 정액 쿠폰 (FIXED_AMOUNT)
```
할인액 = fixedAmount
최종 가격 = 상품 가격 - fixedAmount
```

**예시**:
- 상품 가격: 50,000원
- 쿠폰: 5,000원 정액 할인
- 할인액: 5,000원
- 최종 가격: 45,000원

#### 2. 배율 쿠폰 (PERCENTAGE)
```
할인액 = 상품 가격 × (percentage / 100)
최종 가격 = 상품 가격 - 할인액
```

**예시**:
- 상품 가격: 50,000원
- 쿠폰: 10% 할인
- 할인액: 5,000원
- 최종 가격: 45,000원

### 쿠폰 사용 제약사항

| 제약사항 | 설명 | 검증 시점 | 예외 타입 |
|---------|------|----------|----------|
| **1회성** | 쿠폰은 한 번만 사용 가능 | 주문 생성 시 | `ALREADY_USED_COUPON` |
| **소유권** | 쿠폰 소유자만 사용 가능 | 주문 생성 시 | `FORBIDDEN_COUPON` |
| **존재 여부** | 유효한 쿠폰 ID 필수 | 주문 생성 시 | `NOT_FOUND_COUPON` |
| **동시성** | 동시 사용 시도 시 1건만 성공 | 주문 생성 시 | 비관적 락으로 제어 |

### 쿠폰 적용 범위 (현재 설계)

**주문 단위 쿠폰 적용**:
- 1개 주문에 최대 1개의 쿠폰 적용
- 주문 전체 금액에 대해 할인 적용
- 여러 쿠폰 조합 사용은 추후 검토 예정

**향후 확장 가능성**:
- 상품별 쿠폰 적용
- 쿠폰 조합 사용
- 최소 주문 금액 조건
- 쿠폰 유효기간 관리

### 쿠폰 동시성 제어 전략

#### 비관적 락 (Pessimistic Lock) 사용
```java
// CouponRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM CouponEntity c WHERE c.id = :couponId")
Optional<CouponEntity> findByIdWithLock(@Param("couponId") Long couponId);
```

**동작 방식**:
1. 쿠폰 조회 시 행 레벨 락 획득 (SELECT ... FOR UPDATE)
2. 트랜잭션 종료 시까지 다른 트랜잭션의 접근 차단
3. 쿠폰 상태 변경 (UNUSED → USED)
4. 트랜잭션 커밋 시 락 해제

**장점**:
- 쿠폰의 1회성을 확실하게 보장
- 동시 사용 시도 시 순차적으로 처리
- 데이터 정합성 보장

---