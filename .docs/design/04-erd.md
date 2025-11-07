## 1. ERD

```mermaid
erDiagram
    users {
        bigint id PK
        varchar name
        int point
    }
    products {
        bigint id PK
        varchar name
        int price
        int stock_quantity
        bigint brand_id FK
    }
    brands {
        bigint id PK
        varchar name
    }
    likes {
        bigint user_id PK, FK
        bigint product_id PK, FK
    }
    orders {
        bigint id PK
        bigint user_id FK
        int total_price
        Timestamp created_at
    }
    orderitems {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        int order_price
    }

    %% --- 관계 정의 (1:N) ---
    users ||--o{ likes : "likes"
    users ||--o{ orders : "places"
    
    products ||--o{ likes : "is_liked"
    products ||--o{ orderitems : "is_in"
    
    brands ||--o{ products : "has"
    
    orders ||--o{ orderitems : "contains"