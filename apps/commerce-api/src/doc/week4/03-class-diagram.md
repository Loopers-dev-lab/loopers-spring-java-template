---
title: E-Commerce Class Diagram
---
classDiagram
    class User {
        -Long id
        -String loginId
        -String email
        -String birthday
        -Enum gender
        -Point point
        -List~Like~ likes
        -List~Coupon~ coupons
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
    }
    class Point {
        -Long id
        -BigDecimal amount
        -User user
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
    }
    class Product {
        -Long id
        -String name
        -String description
        -BigDecimal price
        -Long likeCount
        -Enum status
        -Boolean isVisible
        -Boolean isSellable
        -Brand brand
        -Stock stock
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
        +setBrand(Brand brand)
    }
    class Stock {
        -Long id
        -Long quantity
        -Product product
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
    }
    class Brand {
        -Long id
        -String name
        -String description
        -Enum status
        -Boolean isVisible
        -Boolean isSellable
        -List~Product~ products
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
        +setPoint(List~Product~ products)
    }
    class Like {
        -LikeId likeId
        -User user
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class LikeId {
        -Long userId
        -Long likeTargetId
        -Enum likeTargetType
    }
    class Order {
        -Long id
        -BigDecimal finalAmount
        -BigDecimal totalPrice
        -BigDecimal discountAmount
        -BigDecimal shippingFee
        -Enum orderStatus
        -User user
        -List~OrderItem~ orderItems
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
        +addOrderItem(Product product, Integer quantity)
        +applyDiscount(BigDecimal discountAmount)
        +confirm()
    }
    class Coupon {
        -Long id
        -Enum couponType
        -BigDecimal discountValue
        -Boolean isUsed
        -User user
        -Order order
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
        +calculateDiscount(BigDecimal totalPrice) BigDecimal
    }
    class OrderItem {
        -Long id
        -Integer quantity
        -BigDecimal unitPrice
        -BigDecimal totalAmount
        -Order order
        -Product product
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
    }
    class Payment {
        -Long id
        -Enum method
        -BigDecimal amount
        -Enum paymentStatus
        -Order order
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
    }
    class UserService {
        +saveUser(User user) Optional~User~
        +findUserByLoginId(String loginId) Optional~User~
    }
    class PointService {
        +findByUserLoginId(String loginId) Optional~Point~
        +savePoint(Point point) Optional~Point~
        +deduct(String loginId, BigDecimal deductAmount)
        +charge(String loginId, BigDecimal chargeAmount)
    }
    class ProductService {
        +saveProduct(Product product) Optional~Product~
        +findById(Long productId) Optional~Product~
        +findProducts(ProductCondition condition, Pageable pageable) Page~Product~
        +getProductDetailWithBrand(Long productId) ProductWithBrand
    }
    class StockService {
        +findByProductId(Long productId) Optional~Stock~
        +saveStock(Stock stock) Optional~Stock~
        +decreaseQuantity(Long productId, Long decreaseQuantity)
        +increaseQuantity(Long productId, Long increaseQuantity)
    }
    class BrandService {
        +findById(Long brandId) Brand
    }
    class LikeService {
        +saveProductLike(User user, Long productId)
        +deleteProductLike(User user, Long productId)
    }
    class OrderService {
        +saveOrder(Order order) Optional~Order~
        +findOrderById(Long orderId) Order
        +findOrdersByUser(User user) List~Order~
    }
    class CouponService {
        +saveCoupon(Coupon coupon) Optional~Coupon~
        +findById(Long couponId) Coupon
        +findByUser(User user) List~Coupon~
        +findAvailableCouponsByUser(User user) List~Coupon~
        +useCoupon(Order order, Long couponId)
    }

    User "1" --> "0..1" Point
    User "1" --> "0..*" Like
    User "1" --> "0..*" Order
    User "1" --> "0..*" Coupon
    Product "1" --> "0..*" Like
    Product "1" --> "0..1" Stock
    Product "1" --> "0..*" OrderItem
    Brand "1" --> "0..*" Product
    Order "1" --> "1..*" OrderItem
    Order "1" --> "0..1" Payment
    Coupon "0..1" --> "0..1" Order
    Like "*--" "1" LikeId
    
    UserService ..> User : uses
    PointService ..> Point : uses
    ProductService ..> Product : uses
    StockService ..> Stock : uses
    BrandService ..> Brand : uses
    LikeService ..> Like : uses
    OrderService ..> Order : uses
    CouponService ..> Coupon : uses
