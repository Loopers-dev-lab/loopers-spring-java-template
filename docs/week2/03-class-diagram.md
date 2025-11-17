### E-Commerce 플랫폼 도메인 클래스 다이어그램

```mermaid
classDiagram

    class Money {
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
        - int quantity
        + of(int quantity) Stock
        + increase(int amount) Stock
        + decrease(int amount) Stock
        + isSufficient(int required) boolean
        - validateQuantity(int quantity)
    }

    class User {
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
        -Long id
        -User user
        -Product product
        +addLike()
        +isSameUserAndProduct()
        -validateUser(User user)
        -validateProduct(Product product)
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

```

### 클래스 다이어그램 설명

*   **Product**: 상품의 기본 정보와 재고, 좋아요 수를 포함합니다.
*   **Brand**: 상품이 속한 브랜드 정보를 나타냅니다.
*   **User**: 시스템을 이용하는 사용자의 기본 정보와 포인트를 포함합니다.
*   **Order**: 사용자의 주문 정보를 나타냅니다.
*   **OrderItem**: 주문 내역에 포함된 개별 상품의 정보를 나타냅니다. `Order`와 `Product` 간의 N:M 관계를 해소하는 중간 엔티티 역할을 합니다.
*   **ProductLike**: 사용자가 특정 상품에 '좋아요'를 표시한 관계를 나타냅니다. `User`와 `Product` 간의 N:M 관계를 해소하는 중간 엔티티 역할을 합니다.