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
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Point {
        -Long id
        -BigDecimal amount
        -Enum grade
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -User user
        +charge(BigDecimal pointAmount)
        +deduct(BigDecimal pointAmount)
    }
    class Product {
        -Long id
        -String name
        -String description
        -BigDecimal price
        -Long likeCount
        -Boolean isVisible
        -Boolean isSellable
        -Enum status
        -Stock stock
        -Brand brand
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Stock {
        -Long id
        -Long quantity
        -Product product
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        +deduct(Long quantity)
    }
    class Brand {
        -Long id
        -String name
        -String description
        -Enum status
        -Boolean isVisible
        -Boolean isSellable
        -List<Product> products
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Like {
        -Long id
        -Long userId
        -Long targetId
        -Enum targetType
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Order {
        -Long id
        -BigDecimal finalAmount
        -BigDecimal totalPrice
        -BigDecimal discountAmount
        -BigDecimal shippingFee
        -Enum orderStatus
        -User user
        -List<OrderItem> orderItems
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
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
    }
    class Payment {
        -Long id
        -String pgTransactionId
        -Enum method
        -BigDecimal amount
        -Enum paymentStatus
        -Order order
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class PageDto {
        -List<T> elements
        -Integer index
        -Integer number
        -Integer size
        -Integer totalElements
        -Integer totalPages
        -String sort
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }

    User <-- Point
    User <-- Like
    User <-- Order
    Product <-- Like
    Product <-- Stock
    Product <-- OrderItem
    Brand <-- Product
    Order <-- OrderItem
    Order <-- Payment
    