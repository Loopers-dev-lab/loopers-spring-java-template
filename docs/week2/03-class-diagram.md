### E-Commerce 플랫폼 도메인 클래스 다이어그램

```mermaid
classDiagram

    class Money {
        <<VO>>
        - BigDecimal amount
        + of(BigDecimal amount) Money
        + of(long amount) Money
        + zero() Money
        + add(Money other) Money
        + subtract(Money other) Money
        + multiply(int quantity) Money
        + multiply(long quantity) Money
        + isGreaterThan(Money other) boolean
        + isGreaterThanOrEqual(Money other) boolean
        + isLessThan(Money other) boolean
        + isLessThanOrEqual(Money other) boolean
        - validateAmount(BigDecimal amount)
    }

    class Stock {
        <<VO>>
        - int quantity
        + of(int quantity) Stock
        + increase(int amount) Stock
        + decrease(int amount) Stock
        + isSufficient(int required) boolean
        - validateQuantity(int quantity)
    }

    class User {
        <<Entity>>
        -Long id
        -String userId
        -String name
        -String email
        -Gender gender
        -Money point
        -String birthdate
        + createUser()
        - validationuserId(String userId)
        - validationUserEmail(String email)
        - validationUserBirthdate(String birthdate)
        - validateGender(Gender gender)
        - validateAmount(Money amount)
        + chargePoint(Money amount)
        + usePoint(Money amount)
    }

    class Brand {
        <<Entity>>
        -Long id
        -String brandName
        -boolean isActive
        +createBrand(String brandName)
        -validationBrandName(String brandName)
        +activate()
        +deactivate()
        +isAvailable()
    }

    class Product {
        <<Entity>>
        -Long id
        -String productCode
        -String productName
        -Stock stock
        -Money price
        -int likeCount
        -List<ProductLike> productLikes
        -Brand brand
        +createProduct()
        -validationProductCode(String productCode)
        -validationProductName(String productName)
        -validationProductPrice(Money price)
        -validationProductStock(Stock stock)
        -validationBrand(Brand brand)
        +increaseStock(Stock increase)
        +decreaseStock(Stock decrease)
        +incrementLikeCount(ProductLike productLike)
        +decrementLikeCount(ProductLike productLike)
    }

    class OrderItem {
        <<Entity>>
        -Long id
        -Order order
        -Product product
        -int quantity
        -Money price
        -Money totalPrice
        -validateOrder(Order order)
        -validateProduct(Product product)
        -validateQuantity(int quantity)
    }

    class Order {
        <<Entity>>
        -Long id
        -User user
        -String status
        -Money totalPrice
        -createOrder()
        -validateUser(User user)
        -validateProductQuantities(Map<Product, Integer> productQuantities)
        -validateStatusUpdate(OrderStatus status)
        -validateUserPoint(User user, Money totalPrice)
        -validateProductStock(Map<Product, Integer> productQuantities)
        +updateStatus(OrderStatus status)
        -calculateTotalPrice(Map<Product, Integer> productQuantities)
    }

    class ProductLike {
        <<Entity>>
        -Long id
        -User user
        -Product product
        +addLike()
        +isSameUserAndProduct()
        -validateUser(User user)
        -validateProduct(Product product)
    }
    
    class Coupon {
        <<Entity>>
        -Long id
        -String code
        -String name
        -String description
        -LocaleDate validStartDate
        -LocaleDate validEndDate
        -Discount discount
        -boolean isActive
        -Integer maxIssuanceLimit
        -Integer currentIssuanceCount
        -validateCouponCode(String code)
        -validateCouponName(String name)
        -validateDiscount(Discount discount)
        -validateCouponDates(LocaleDate validStartDate, LocaleDate validEndDate)
        +increaseIssuanceCount()
        +isValidAt(LocalDate date)
        +isValidNow()
        +canIssue()
        +setMaxIssuanceLimit(Integer limit)
        +activate()
        +deactivate()
    }

    class IssuedCoupon {
        <<Entity>>
        +User user
        +Coupon coupon
        +CouponStatus status
        +LocalDateTime usedAt
        +IssuedCoupon(User, Coupon, CouponStatus)
        +static IssuedCoupon issue(User, Coupon)
        +void useCoupon()
        -static void validateIssuedCouponFields(User, Coupon, CouponStatus)
        -static void validateCanIssue(Coupon)
        -void validateCanUseCoupon()
    }

    class Payment {
        <<Entity>>
        -String paymentId
        -Order order
        -String pgTransactionId
        -Money amount
        -PaymentStatus status
        -PaymentType paymentType
        -CardType cardType
        -String cardNo
        -LocalDateTime createdAt
        -LocalDateTime completedAt
        -Integer statusCheckCount
        -LocalDateTime lastStatusCheckAt
        +createPaymentForPoint(Order, Money, PaymentType)
        +createPaymentForCard(Order, Money, PaymentType, CardType, String) 
        +startProcessing(String pgTransactionId)
        +completePointPayment()
        +completePayment()
        +failPayment(String reason)
        +incrementStatusCheckCount()
        +canCheckStatus(int maxCheckCount) boolean
        +isProcessingOverMinutes(int minutes) boolean
        -validatePaymentType(PaymentType, CardType, String)
        -validateCardNo(String)
        -validateCardType(CardType)
    }

    class PaymentStatus {
        <<Enum>>
        PENDING
        PROCESSING
        SUCCESS
        FAILED
    }

    class PaymentType {
        <<Enum>>
        CARD
        POINT
    }

    class CardType {
        <<Enum>>
        SAMSUNG
        KB
        HYUNDAI
    }

    User "1" -- "0..*" Order : 
    User "1" -- "0..*" ProductLike : 
    Order "1" *-- "1..*" OrderItem : 
    Product "1" -- "0..*" OrderItem : 
    Product "1" -- "0..*" ProductLike : 
    Brand "1" -- "0..*" Product : 
    
    Product "1" *-- "1" Money : price
    Product "1" *-- "1" Stock : stock
    User "1" *-- "1" Money : point
    OrderItem "1" *-- "1" Money : price
    OrderItem "1" *-- "1" Money : totalPrice
    Order "1" *-- "1" Money : totalPrice

    IssuedCoupon "0..*" *-- "1" User : user
    IssuedCoupon "0..*" *-- "1" Coupon : coupon

    Order "1" -- "0..1" Payment : 
    Payment "1" *-- "1" Money : amount
    Payment "1" --> "1" PaymentStatus : status
    Payment "1" --> "1" PaymentType : paymentType
    Payment "1" --> "0..1" CardType : cardType

```

### 클래스 다이어그램 설명

*   **Product**: 상품의 기본 정보와 재고, 좋아요 수를 포함합니다.
*   **Brand**: 상품이 속한 브랜드 정보를 나타냅니다.
*   **User**: 시스템을 이용하는 사용자의 기본 정보와 포인트를 포함합니다.
*   **Order**: 사용자의 주문 정보를 나타냅니다.
*   **OrderItem**: 주문 내역에 포함된 개별 상품의 정보를 나타냅니다. `Order`와 `Product` 간의 N:M 관계를 해소하는 중간 엔티티 역할을 합니다.
*   **ProductLike**: 사용자가 특정 상품에 '좋아요'를 표시한 관계를 나타냅니다. `User`와 `Product` 간의 N:M 관계를 해소하는 중간 엔티티 역할을 합니다.
*   **Coupon**: 쿠폰 정보를 나타냅니다.
*   **IssuedCoupon**: 유저에게 발행된 쿠폰 정보를 나타냅니다.
*   **Payment**: 주문에 대한 결제 정보를 나타냅니다. PG사와의 연동을 통해 결제 처리 상태를 추적하며, 카드 결제와 포인트 결제를 지원합니다.
*   **PaymentStatus**: 결제의 상태를 나타내는 Enum입니다. PENDING(대기), PROCESSING(처리중), SUCCESS(성공), FAILED(실패) 상태를 가집니다.
*   **PaymentType**: 결제 수단을 나타내는 Enum입니다. CARD(카드 결제), POINT(포인트 결제)를 지원합니다.
*   **CardType**: 카드사 정보를 나타내는 Enum입니다. SAMSUNG, KB, HYUNDAI 카드를 지원합니다.