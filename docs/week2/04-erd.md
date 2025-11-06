# 테이블 구조

~~~mermaid
erDiagram
    USER ||--|| POINT_ACCOUNT: "포인트보유"
    USER ||--o{ POINT_HISTORY: "포인트이력"
    USER ||--o{ PRODUCT_LIKE: "등록"
    USER ||--o{ ORDER: "생성"
    USER ||--o{ PAYMENT: "결제자"
    PAYMENT ||--o{ POINT_HISTORY: "결제내역"
    BRAND ||--o{ PRODUCT: "포함"
    PRODUCT ||--o{ PRODUCT_LIKE: "받음"
    PRODUCT ||--o{ ORDER_ITEM: "참조됨"
    ORDER ||--|o PAYMENT: "발생"
    ORDER ||--|{ ORDER_ITEM: "포함"

    USER {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR user_id UK "계정ID"
        VARCHAR gender "성별(MALE/FEMALE)"
        VARCHAR email "이메일"
        DATE birth_date "생년월일"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    POINT_ACCOUNT {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id "사용자ID"
        BIGINT balance "잔액"
        BIGINT version "낙관적락버전"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    POINT_HISTORY {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id "사용자ID"
        BIGINT payment_id "결제ID(옵션)"
        VARCHAR type "유형(CHARGE/USE/REFUND)"
        BIGINT amount "변경금액"
        BIGINT balance_after "변경후잔액"
        VARCHAR idempotency_key UK "멱등키"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    BRAND {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR name "브랜드명"
        TEXT description "브랜드소개"
        VARCHAR status "활성여부(ACTIVE/INACTIVE)"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    PRODUCT {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT brand_id "브랜드ID"
        VARCHAR name "상품명"
        TEXT description "상품설명"
        BIGINT price "판매가격"
        BIGINT stock "현재재고"
        BIGINT total_likes "총좋아요수"
        VARCHAR status "판매상태(ACTIVE/INACTIVE/OUT_OF_STOCK)"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    PRODUCT_LIKE {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id "사용자ID"
        BIGINT product_id "상품ID"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    ORDER {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR order_id UK "주문번호"
        BIGINT user_id "주문자ID"
        BIGINT total_amount "총결제금액"
        VARCHAR status "주문상태(PENDING/CONFIRMED/CANCELLED/COMPLETED)"
        VARCHAR idempotency_key UK "중복방지키"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    ORDER_ITEM {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT order_id "주문ID"
        BIGINT product_id "상품ID"
        VARCHAR product_name "주문당시상품명"
        BIGINT price "주문당시가격"
        BIGINT quantity "주문수량"
        BIGINT subtotal "항목소계"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }

    PAYMENT {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT order_id "주문ID"
        BIGINT user_id "결제자ID"
        BIGINT amount "결제금액(포인트)"
        VARCHAR type "결제수단(POINT)"
        VARCHAR status "결제상태(PENDING/COMPLETED/FAILED)"
        VARCHAR idempotency_key UK "멱등키"
        TIMESTAMP created_at "생성일시"
        TIMESTAMP updated_at "수정일시"
        TIMESTAMP deleted_at "삭제일시(소프트삭제)"
    }
~~~