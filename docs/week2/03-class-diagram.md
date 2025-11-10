### E-Commerce 플랫폼 도메인 클래스 다이어그램

```mermaid
classDiagram

    class User {
        +Long userId
        +String name
        +String email
        +Gender gender
        +int points
        +String birthdate
        + validUserInfo()
        + chargePoint()
    }

    class Brand {
        +Long brandId
        +String brandName
        +boolean isActive
    }

    class Product {
        +Long productId
        +String productCode
        +String productName
        +int stock
        +int price
        +int likeCount
        +List<ProductLike> productLikes
        +Brand brand
        +incrementLikeCount()
        +decrementLikeCount()
        +increaseStock()
        +decreaseStock()
    }

    class OrderProduct {
        +Order order
        +Product product
        +int quantity
        +int price
    }
    
    class Order {
        +Long orderId
        +User user
        +String status
        +int totalPrice
    }

    class ProductLike {
        +User user
        +Product product
    }

    User "1" -- "0..*" Order : 
    User "1" -- "0..*" ProductLike : 
    Order "1" *-- "1..*" OrderProduct : 
    Product "1" -- "0..*" OrderProduct : 
    Product "1" -- "0..*" ProductLike : 
    Brand "1" -- "0..*" Product : 

```

### 클래스 다이어그램 설명

*   **Product**: 상품의 기본 정보와 재고, 좋아요 수를 포함합니다.
*   **Brand**: 상품이 속한 브랜드 정보를 나타냅니다.
*   **User**: 시스템을 이용하는 사용자의 기본 정보와 포인트를 포함합니다.
*   **Order**: 사용자의 주문 정보를 나타냅니다.
*   **OrderProduct**: 주문 내역에 포함된 개별 상품의 정보를 나타냅니다. `Order`와 `Product` 간의 N:M 관계를 해소하는 중간 엔티티 역할을 합니다.
*   **ProductLike**: 사용자가 특정 상품에 '좋아요'를 표시한 관계를 나타냅니다. `User`와 `Product` 간의 N:M 관계를 해소하는 중간 엔티티 역할을 합니다.