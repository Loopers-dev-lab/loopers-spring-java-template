```mermaid
erDiagram
brands {
    bigint id PK
    varchar name
}
products {
    bigint id PK
    varchar name
    bigint brand_id FK
    Price price
    Stock stock
}
product_likes {
    bigint user_id PK, FK
    bigint product_id PK, FK
    timestamp deleted_at
}
product_like_count {
    bigint product_id PK, FK
    int total_count
}
users {
    bigint id PK
    varchar login_id
    varchar password
    varchar email
    varchar birth_date
    varchar gender
}
points {
    bigint id PK
    bigint user_id FK 
    int balance
}
orders {
    bigint id PK
    bigint user_id FK
    orderStatus status
}
order_items {
    bigint id PK
    bigint order_id FK
    bigint product_id FK
    int quantity
    Price item_price
}

brands ||--o{ products : ""
users ||--o{ product_likes : ""
users ||--|| points : ""
users ||--o{ orders : ""
orders ||--o{ order_items : ""
products ||--o{ product_likes : ""
products ||--|| product_like_count : ""
order_items ||--|| products : ""
```