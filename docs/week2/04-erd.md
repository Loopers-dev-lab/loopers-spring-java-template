1. 모든 엔티티 테이블은 `BASE_ENTITY`의 필드를 상속 받습니다.
2. `ORDER.status`를 `VARCHAR`로 저장합니다.
3. `Point`는 별도 엔티티로 관리합니다.
4. `Stock` 클래스는 Value Object(VO)로 구현합니다.

```mermaid
---
config:
  theme: default
---
erDiagram
    BASE_ENTITY {
        BIGINT id PK 
        DATETIME created_at
        DATETIME updated_at 
        DATETIME deleted_at
    }
    
    USER {
        BIGINT id PK 
        STRING user_id UK
        STRING gender
        STRING email
        STRING birth_date
    }
    
    POINT {
        BIGINT id PK 
        STRING user_id FK 
        BIGINT point_amount
    }
    
    BRAND {
        BIGINT id PK 
        STRING name
    }
    
    PRODUCT {
        BIGINT id PK 
        BIGINT brand_id FK 
        STRING name
        INT stock_quantity "Stock VO"
    }
    
    LIKE {
        BIGINT id PK 
        BIGINT user_id FK 
        bigint product_id FK 
    }
    
    ORDER {
        BIGINT id PK 
        BIGINT user_id FK 
        STRING status "주문 상태"
    }
    
    ORDER_ITEM {
        BIGINT id PK 
        BIGINT order_id FK 
        BIGINT product_id FK 
        INT quantity
    }

    USER ||--|| POINT : "1:1로 대응한다"
    USER ||--o{ LIKE : "여러 개의 좋아요를 누른다"
    USER ||--o{ ORDER : "여러 개의 주문을 함"
    
    BRAND ||--o{ PRODUCT : "한 브랜드는 여러 개의 제품을 가지고 있음"
    
    PRODUCT ||--o{ LIKE : "한 상품은 여러 개의 좋아요를 받음"
    PRODUCT ||--o{ ORDER_ITEM : "한 상품은 여러 주문 항목에 포함됨"
    
    ORDER ||--o{ ORDER_ITEM : "한 주문은 여러 주문 항목을 가짐"

```
![Untitled diagram-2025-11-07-025701.png](..%2F..%2F..%2FUsers%2Fsentinel%2FDownloads%2FUntitled%20diagram-2025-11-07-025701.png)