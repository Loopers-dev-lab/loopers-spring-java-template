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
    }
    point {
        bigint id PK
        bigint user_id FK
        bigint amount
        enum grade
    }
    product {
        bigint id PK
        bigint brand_id FK
        varchar name
        varchar description
        enum status
        bigint price
        boolean is_visible
        boolean is_sellable
    }
    product_category {
        bigint id PK
        bigint parent_id
        varchar name
        varchar description
        boolean is_visible
        boolean is_sellable
    }
    stock {
        bigint id PK
        bigint product_id FK
        bigint quantity
    }
    brand {
        bigint id PK
        varchar name
        varchar description
        Enum status
        boolean is_visible
        boolean is_sellable
    }
    like {
        bigint id PK
        bigint user_id FK
        bigint target_id
        Enum targetType
    }
    order {
        bigint id PK
        bigint user_id FK
        enum status
        bigint final_amount
        bigint total_price
        bigint discount_amount
        bigint shipping_fee
    }
    order_item {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        bigint unit_price
        bigint total_amount
    }
    payment {
        bigint id PK
        bigint order_id FK
        varchar pg_transaction_id
        enum method
        bigint amount
        enum status
    }

    user ||--|| point : ""
    user ||--o{ like : ""
    user ||--o{ order : ""
    product ||--o{ like : ""
    product ||--|| stock : ""
    product ||--o{ order_item : ""
    brand ||--o{ product : ""
    product_category ||--o{ product : ""
    order ||--|{ order_item : ""
    order ||--|| payment : ""