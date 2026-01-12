erDiagram
    users {
        %% BaseEntity가 id, created_at 등을 제공합니다.
        varchar name
    }
    points {
        bigint id PK
        bigint user_id FK
        int point
    }
    products {
        %% BaseEntity가 id, created_at 등을 제공합니다.
        varchar name
        int price
        int stock_quantity
        bigint brand_id FK
    }
    brands {
        %% BaseEntity가 id, created_at 등을 제공합니다.
        varchar name
    }
    likes {
        bigint user_id PK, FK
        bigint product_id PK, FK
    }
    orders {
        %% BaseEntity가 id, created_at 등을 제공합니다.
        bigint user_id FK
        int total_price
        %% Timestamp created_at 제거
    }
    orderitems {
        %% BaseEntity가 id, created_at 등을 제공합니다.
        bigint order_id FK
        bigint product_id FK
        int quantity
        int order_price
    }

    %% --- 관계 정의 (1:N) ---
    users ||--o{ likes : "likes"
    users ||--o{ orders : "places"
     users ||--o{ points : "has"
    products ||--o{ likes : "is_liked"
    products ||--o{ orderitems : "is_in"
    brands ||--o{ products : "has"
    orders ||--o{ orderitems : "contains"