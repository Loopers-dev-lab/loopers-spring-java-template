## 1. 클래스 다이어그램램

```mermaid
classDiagram
    class User {
        Long id
        String name
        int point
    }
    class Product {
        Long id
        String name
        int price
        int quantity
    }
    class Brand {
        Long id
        String name
    }
    class Like {
        User user
        Product product
    }
    class Order {
        Long id
        User user
        int totalPrice
        Timestamp orderDate
    }
    class OrderItem {
        Order order
        Product product
        int quantity
        int orderPrice
    }

    %% --- 관계 정의 ---
    Product --> Brand : (상품은 브랜드를 가짐)
    
    Order --> User : (주문은 유저를 가짐)
    
    OrderItem --> Order : (주문 항목은 주문에 속함)
    OrderItem --> Product : (주문 항목은 상품을 가짐)
      
    Like --> User : (좋아요는 유저를 가짐)
    Like -- > Product : (좋아요는 상품을 가짐)