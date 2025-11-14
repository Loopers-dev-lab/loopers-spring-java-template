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
        -User user
        -BigDecimal amount
        -Enum grade
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        +charge()
    }
    class Product {
        -Long id
        -ProductCategory productCategory
        -Stock stock
        -Brand brand
        -String name
        -String description
        -Enum status
        -BigDecimal price
        -Boolean isVisible
        -Boolean isSellable
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class ProductCategory {
        -Long id
        -Long parentId
        -List<Product> products
        -String name
        -String description
        -Boolean isVisible
        -Boolean isSellable
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Stock {
        -Long id
        -Product product
        -Long quantity
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Brand {
        -Long id
        -List<Product> products
        -String name
        -String description
        -Enum status
        -Boolean isVisible
        -Boolean isSellable
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Like {
        -Long id
        -User user
        -Product product
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Order {
        -Long id
        -User user
        -List<OrderItem> orderItems
        -Enum status
        -BigDecimal finalAmount
        -BigDecimal totalPrice
        -BigDecimal discountAmount
        -BigDecimal shippingFee
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class OrderItem {
        -Long id
        -Order order
        -Product product
        -Integer quantity
        -BigDecimal unitPrice
        -BigDecimal totalAmount
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
    }
    class Payment {
        -Long id
        -Order order
        -String pgTransactionId
        -Enum method
        -BigDecimal amount
        -Enum status
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
    ProductCategory <-- Product
    Order <-- OrderItem
    Order <-- Payment
    