## 04-ERD
### 작업 내용 
> 유저, 브랜드, 상품, 주문, 주문_상품, 좋아요 테이블을 구성
### ERD 설계 고민사항 
- 좋아요의 멱등성을 보장하기 위해, {이용자ID, 상품_id} 에 대해 INSERT, DELETE 되도록 설계 
- N:M 관계 해소, 과제를 위한 핵심적인 필드만 담으려고 하였습니다.
- Audit 관련 컬럼 추가

#### Order (주문)
- 여러 주문상품에 대한 하나의 주문id로
- 여러 주문상품에 대한 총 건수, 총 금액, 정상 처리 금액, 처리가 안된건은 에러 금액으로 관리

#### order_item (주문 상품)
- 상품과 N:1 관계를 맺으며 주문한 상품에 대한 참조키와 기본적인 상품 이름, 결제금액을 관리

#### Brand / Product
- soft delete 을 고려하였습니다.
- Product의 경우 재고량, 원가 정보등을 담습니다.

```mermaid
erDiagram

    USER {
        VARCHAR(16) user_id PK
        VARCHAR(64) user_name 
        VARCHAR(45) description 
        VARCHAR(45) email
        CHAR(8) birthdate
        CHAR(1) gender
        INT point
        TIMESTAMP updated_at
        TIMESTAMP created_at
        TIMESTAMP deleted_at
    }

    PRODUCT {
        BIGINT id PK
        VARCHAR(50) product_name
        VARCHAR(32) category
        INT price
        INT stock
        CHAR(1) status
        BIGINT brand_id FK
        TIMESTAMP created_at
        TIMESTAMP updated_at
        TIMESTAMP deleted_at
    }

    BRAND {
        BIGINT id PK
        VARCHAR(50) name
        VARCHAR(255) description
        CHAR(1) status
        TIMESTAMP created_at
        TIMESTAMP updated_at
        TIMESTAMP deleted_at
    }

    PRODUCT_LIKE {
        VARCHAR(16) user_id PK, FK
        BIGINT product_id PK, FK
        TIMESTAMP created_at
    }

    ORDER {
        BIGINT id PK
        VARCHAR(50) name
        VARCHAR(45) order_cnt
        CHAR(1) order_status
        INT total_price
        INT normal_price
        INT error_price
        VARCHAR(16) user_id FK
    }

    ORDER_ITEM {
        BIGINT id PK
        VARCHAR(50) name
        INT price
        BIGINT order_id FK
        BIGINT product_id FK
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    %% Relationships
    USER ||--o{ ORDER : "주문"
    ORDER ||--|{ ORDER_ITEM : "주문 내용"
    PRODUCT ||--o{ ORDER_ITEM : "주문된 제품"
    USER ||--o{ PRODUCT_LIKE : "좋아요"
    PRODUCT ||--o{ PRODUCT_LIKE : "좋아요된 상품"
    BRAND ||--o{ PRODUCT : "자사 상품"

```