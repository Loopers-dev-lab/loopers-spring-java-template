``` mermaid
classDiagram
    class User {
        Long id
        String userId
        String email
        Date birthday
        Gender gender
    }
    class Product {
        Long id
        String name
        Long price
        Brand brand
    }
    class Stock {
        Long id
        int quantity
        Product product
        increaseStock()
        decreaseStock()
    }
    class StockHistory {
        Long id
        int beforeQuantity
        int quantity
        int afterQuantity
        StockType stockType
        Stock stock
    }
    class Brand {
        Long id
        String name
    }
    class Like {
        User user
        Product product
        like()
        unLike()
    }
    class Point {
        Long id
        Long point
        User user
        chargePoint()
        deductPoint()
    }
    class PointHistory {
        Long id
        Long beforePoint
        Long amount
        Long afterPoint
        PointType pointType
        Point point
    }
    class Order {
        Long id
        Long totalPrice
        OrderStatus orderStatus
        User user
        createAndPaidOrder()
    }
    class OrderItem {
        Long id
        int quantity
        Long productPrice
        Product product (주문 당시 스냅샷)
        Order order
    }

    Product --> Brand
    Stock --> Product
    StockHistory --> Stock
    Like --> User
    Like --> Product
    Point --> User
    Order --> User
    PointHistory --> Point
    OrderItem --> Order
```