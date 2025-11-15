classDiagram
    class User {
        Long id
        String name
        int point
    }
    class Product {
        Long id
        String name
        Price price       %% int -> Price (VO)
        Quantity quantity %% int -> Quantity (VO)
    }
    class Brand {
        Long id
        String name
    }
    class Like {
        User user
        Product product
        %% boolean liked 제거
    }
    class Order {
        Long id
        User user
        int totalPrice
        Timestamp created_at
    }
    class OrderItem {
        Order order
        Product product
        int quantity
        int orderPrice
    }
    
    %% --- VO 정의 ---
    class Price { <<VO>> }
    class Quantity { <<VO>> }

    %% --- 관계 정의 ---
    Product --> Brand : (상품은 브랜드를 가짐)
    Product --> Price
    Product --> Quantity
    
    Order --> User : (주문은 유저를 가짐)
    
    OrderItem --> Order : (주문 항목은 주문에 속함)
    OrderItem --> Product : (주문 항목은 상품을 가짐)
      
    Like --> User : (좋아요는 유저를 가짐)
    Like --> Product : (좋아요는 상품을 가짐)