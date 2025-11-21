---
title: E-Commerce ERD
---
erDiagram
    user {
        bigint id PK
        varchar login_id UK
        varchar email UK
        varchar birthday
        enum gender
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    point {
        bigint id PK
        bigint user_id FK
        decimal amount
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    product {
        bigint id PK
        bigint brand_id FK
        varchar name
        varchar description
        enum status
        decimal price
        bigint like_count
        boolean is_visible
        boolean is_sellable
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    stock {
        bigint id PK
        bigint product_id FK
        bigint quantity
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    brand {
        bigint id PK
        varchar name
        varchar description
        enum status
        boolean is_visible
        boolean is_sellable
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    user_like {
        bigint user_id PK,FK
        bigint like_target_id PK
        enum like_target_type PK
        datetime created_at
        datetime updated_at
    }
    orders {
        bigint id PK
        bigint user_id FK
        enum order_status
        decimal final_amount
        decimal total_price
        decimal discount_amount
        decimal shipping_fee
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    order_item {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        decimal unit_price
        decimal total_amount
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    payment {
        bigint id PK
        bigint order_id FK
        enum method
        decimal amount
        enum payment_status
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }
    coupon {
        bigint id PK
        bigint user_id FK
        bigint order_id FK
        enum coupon_type
        decimal discount_value
        boolean is_used
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    user ||--|| point : "has"
    user ||--o{ user_like : "creates"
    user ||--o{ orders : "places"
    user ||--o{ coupon : "owns"
    product ||--o{ user_like : "receives"
    product ||--|| stock : "has"
    product ||--o{ order_item : "included_in"
    brand ||--o{ product : "has"
    orders ||--|{ order_item : "contains"
    orders ||--|| payment : "has"
    coupon ||--o| orders : "applied_to"