# 테이블 구조

~~~mermaid
erDiagram
    USERS ||--|| POINT_ACCOUNTS: "포인트보유"
    USERS ||--o{ PRODUCT_LIKES: "등록"
    USERS ||--o{ ORDERS: "생성"
    BRANDS ||--o{ PRODUCTS: "포함"
    PRODUCTS ||--o{ PRODUCT_LIKES: "받음"
    PRODUCTS ||--o{ ORDER_ITEMS: "참조됨"
    ORDERS ||--|{ ORDER_ITEMS: "포함"

    USERS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR user_id UK "계정ID"
        VARCHAR email "이메일"
        VARCHAR birth_date "생년월일(yyyy-MM-dd)"
        VARCHAR gender "성별(MALE/FEMALE)"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    POINT_ACCOUNTS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR user_id "사용자ID"
        BIGINT amount "잔액(Point)"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    BRANDS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR name "브랜드명"
        BOOLEAN active "활성여부"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    PRODUCTS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT brand_id FK "브랜드ID"
        VARCHAR name "상품명"
        TEXT description "상품설명"
        BIGINT price "판매가격"
        BIGINT stock "현재재고"
        BIGINT total_likes "총좋아요수"
        BIGINT version "낙관적락버전"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    PRODUCT_LIKES {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id "사용자ID"
        BIGINT product_id "상품ID"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    ORDERS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR user_id "주문자ID"
        BIGINT total_amount "총결제금액"
        VARCHAR status "주문상태(PENDING/CONFIRMED/CANCELLED)"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    ORDER_ITEMS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT order_id FK "주문ID"
        BIGINT product_id "상품ID"
        VARCHAR product_name "주문당시상품명"
        BIGINT price "주문당시가격"
        BIGINT quantity "주문수량"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }
~~~

## 인덱스

| 테이블 | 인덱스 | 컬럼 | 설명 |
|-------|-------|------|------|
| USERS | uk_users_user_id | user_id | 계정ID 유니크 |
| PRODUCT_LIKES | uk_product_like_user_product | user_id, product_id | 사용자-상품 좋아요 유니크 |
| PRODUCTS | idx_products_brand_id | brand_id | 브랜드별 상품 조회 |
| PRODUCTS | idx_products_created_at | created_at | 최신순 정렬 |
| PRODUCTS | idx_products_price | price | 가격순 정렬 |
| PRODUCTS | idx_products_total_likes | total_likes | 좋아요순 정렬 |
