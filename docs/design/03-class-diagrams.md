# 클래스 다이어그램 (도메인 중심 설계)

> 시퀀스 다이어그램 분석을 통해 도출된 도메인 클래스들의 최소 스펙

```mermaid
%% + : public
%% - : private
%% # : protected

classDiagram
    class User {
        +id: Long
        +loginId: String
        +name: String
        +email: String
        +role: UserRole
        +isSeller() boolean
        +isActive() boolean
    }
    class cupon{
        -userId: Long
        -type: String
        -useYn : String
        -orderId : Long
        -issued_at : LocalDateTime
        -expired_at : LocalDateTime
        
    }
    
    class Point {
        -userId: Long
        -amount: int
        -updatedAt: LocalDateTime
        +charge(amount: int)
        +use(amount: int)
        +restore(amount: int)
        +hasEnoughBalance(amount: int) boolean
    }
    
    class Brand {
        +id: Long
        +name: String
        +description: String
        +snsLinks: String
        +likeCount: int
        +isActive: boolean
        +createdAt: LocalDateTime
        +incrementLikeCount()
        +decrementLikeCount()
        +isAvailableForLike() boolean
    }
    
    class BrandLike {
        +id: Long
        -userId: Long
        -brandId: Long
        -likedAt: LocalDateTime
        %% 브랜드 좋아요 토글 (멱등성 보장)
        +create(userId: Long, brandId: Long) BrandLike
        +remove()
    }
    
    class Product {
        +id: Long
        +name: String
        +brandId: Long
        +price: int
        +stock: int
        +description: String
        +imageUrl: String
        +status: ProductStatus
        +likeCount: int
        +createdAt: LocalDateTime
        +decreaseStock(qty: int)
        +restoreStock(qty: int)
        +incrementLikeCount()
        +decrementLikeCount()
        +isAvailable() boolean
        +hasEnoughStock(qty: int) boolean
    }
    
    class ProductOption {
        +id: Long
        -productId: Long
        +name: String
        +value: String
        +additionalPrice: int
        +calculateTotalPrice(basePrice: int) int
        +isValid() boolean
    }
    
    class ProductLike {
        +id: Long
        -userId: Long
        -productId: Long
        -likedAt: LocalDateTime
        %% 상품 좋아요 토글 (멱등성 보장)
        +create(userId: Long, productId: Long) ProductLike
        +remove()
    }
    class Order {
        +id: Long
        +orderNumber: String
        -userId: Long
        +status: OrderStatus
        +totalPrice: int
        +createdAt: LocalDateTime
        +updatedAt: LocalDateTime
        +addItem(productId: Long, optionId: Long, qty: int, price: int)
        +calculateTotal() int
        +cancel()
        +updateStatus(status: OrderStatus)
        +canBeCancelled() boolean
        +isPendingPayment() boolean
        +belongsToUser(userId: Long) boolean
    }
    
    class OrderItem {
        +id: Long
        -orderId: Long
        -productId: Long
        -optionId: Long
        -quantity: int
        -pricePerUnit: int
        +productName: String
        +optionName: String
        +imageUrl: String
        %% 주문 시점 정보 스냅샷
        +subtotal() int
        +getProductSnapshot() ProductSnapshot
    }
    
    class ProductSnapshot {
        +productName: String
        +optionName: String
        +imageUrl: String
        +priceAtOrder: int
    }
    
    class Payment {
        +id: Long
        -orderId: Long
        +status: PaymentStatus
        +amount: int
        +method: PaymentMethod
        +pointsUsed: int
        +transactionId: String
        +createdAt: LocalDateTime
        +completedAt: LocalDateTime
        +complete(transactionId: String)
        +fail(reason: String)
        +isCompleted() boolean
        +isPending() boolean
        +validateCallback(amount: int, orderNumber: String) boolean
    }
    
    class PaymentCallback {
        +id: Long
        -paymentId: Long
        +orderNumber: String
        +amount: int
        +success: boolean
        +transactionId: String
        +signature: String
        +receivedAt: LocalDateTime
        %% PG사 콜백 검증 및 처리
        +validateSignature() boolean
        +matchesPayment(payment: Payment) boolean
        +isSuccess() boolean
    }

    %% Enums
    class OrderStatus {
        <<enumeration>>
        PENDING_PAYMENT
        PAYMENT_COMPLETED
        PAYMENT_FAILED
        CANCELLED
        EXPIRED
    }
    
    class PaymentStatus {
        <<enumeration>>
        INITIATED
        PROCESSING
        COMPLETED
        FAILED
        CANCELLED
    }
    
    class ProductStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        OUT_OF_STOCK
        DISCONTINUED
    }
    
    class PaymentMethod {
        <<enumeration>>
        CARD
        POINT
        MIXED
    }
    
    class UserRole {
        <<enumeration>>
        CUSTOMER
        SELLER
        ADMIN
    }
     class Coupon {
        <<enumeration>>
        FIXED
        RATE 

    }
    

    %% Core Relationships
    User --> "1" Point : 소유
    User --> "N" BrandLike : 등록
    User --> "N" ProductLike : 등록
    User --> "N" Order : 주문
    
    Brand --> "N" BrandLike : 받음
    Brand --> "N" Product : 포함
    
    Product --> "1" Brand : 속함
    Product --> "N" ProductOption : 가짐
    Product --> "N" ProductLike : 받음
    Product --> "N" OrderItem : 주문됨
    
    Order --> "N" OrderItem : 포함
    Order --> "1" Payment : 결제됨
    
    OrderItem --> Product : 참조
    OrderItem --> ProductOption : 옵션선택
    OrderItem --> ProductSnapshot : 스냅샷보관
    
    Payment --> "N" PaymentCallback : 콜백받음
    
    %% Status relationships
    Order --> OrderStatus : 상태
    Payment --> PaymentStatus : 상태
    Product --> ProductStatus : 상태
    Payment --> PaymentMethod : 결제수단
    User --> UserRole : 역할
```

---

## 설계 원칙

### 1. 시퀀스 다이어그램 기반 설계
- **OrderAndPaymentFlow**: 주문/결제 분리에 따른 상태 관리
- **PaymentCallback**: 콜백 검증 및 실패 처리 로직
- **Like**: 상품/브랜드 좋아요 토글 기능 (멱등성)
- **Cancel**: 주문 취소 시 리소스 복구 메서드

### 2. 도메인 규칙 반영
- **Point**: 결제 완료 시점 차감, 취소 시 복구
- **Product**: 재고 관리 (차감/복구), 상태별 주문 가능 여부
- **Order**: 사용자 권한 검증, 상태별 취소 가능 여부
- **Payment**: 콜백 검증, 중복 처리 방지

### 3. 멱등성 보장
- **Like 엔티티**: 중복 등록/취소 무시
- **PaymentCallback**: 중복 콜백 처리 방지
- **Order 취소**: 이미 취소된 주문 재처리 방지

### 4. 데이터 일관성
- **ProductSnapshot**: 주문 시점 상품 정보 보존
- **Point 복구**: 주문 취소 시 사용된 포인트 원복
- **Stock 복구**: 주문 취소 시 차감된 재고 원복
