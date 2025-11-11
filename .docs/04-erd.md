# 엔티티 관계도 (ERD)

## ERD

![ERD](./image/erd.png)

```mermaid
erDiagram
    BRAND ||--o{ PRODUCT : contains
    USER ||--o{ PRODUCT_LIKE : creates
    USER ||--o{ "ORDER" : places
    USER ||--o{ PAYMENT : makes
    PRODUCT ||--o{ PRODUCT_LIKE : receives
    PRODUCT ||--o{ ORDER_ITEMS : contains
    "ORDER" ||--o{ ORDER_ITEMS : has
    "ORDER" ||--|| PAYMENT : triggers

    BRAND {
        bigint id PK
        varchar name
        varchar description
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    PRODUCT {
        bigint id PK
        bigint brand_id FK
        varchar name
        decimal price
        bigint stock
        bigint like_count
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    USER {
        bigint id PK
        varchar identifier
        varchar email
        date birth_day
        varchar gender
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    PRODUCT_LIKE {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    "ORDER" {
        bigint id PK
        bigint user_id FK
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        bigint quantity
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    PAYMENT {
        bigint id PK
        bigint order_id FK
        bigint user_id FK
        decimal amount
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }
```

## 관계 설명

| 관계                       | 카디널리티 | 설명                                |
|---------------------------|-------|-----------------------------------|
| BRAND → PRODUCT           | 1:N   | 하나의 브랜드는 여러 상품을 가짐              |
| USER → PRODUCT_LIKE       | 1:N   | 사용자는 여러 상품에 좋아요 가능              |
| PRODUCT → PRODUCT_LIKE    | 1:N   | 상품은 여러 사용자로부터 좋아요 받을 수 있음     |
| USER → ORDER              | 1:N   | 사용자는 여러 주문 가능                   |
| ORDER → ORDER_ITEMS       | 1:N   | 하나의 주문은 여러 주문 항목을 가짐            |
| PRODUCT → ORDER_ITEMS     | 1:N   | 상품은 여러 주문 항목에 포함될 수 있음         |
| ORDER → PAYMENT           | 1:1   | 각 주문은 하나의 결제 정보를 가짐             |
| USER → PAYMENT            | 1:N   | 사용자는 여러 결제를 할 수 있음              |

