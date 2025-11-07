# 04-erd.md - ERD 설계

## 📑 목차

- [1. 데이터베이스 테이블 구조](#1-데이터베이스-테이블-구조)
- [2. 테이블 설명](#2-테이블-설명)
  - [2.1 USERS](#21-users)
  - [2.2 POINTS](#22-points)
  - [2.3 BRANDS](#23-brands)
  - [2.4 PRODUCTS](#24-products)
  - [2.5 PRODUCT_LIKES](#25-product_likes)
  - [2.6 ORDERS](#26-orders)
  - [2.7 ORDER_ITEMS](#27-order_items)
- [3. 공통 필드](#3-공통-필드)

---

## 1. 데이터베이스 테이블 구조

```mermaid
erDiagram
    USERS ||--|| POINTS : ""
    USERS ||--o{ PRODUCT_LIKES : ""
    USERS ||--o{ ORDERS : ""

    BRANDS ||--o{ PRODUCTS : ""

    PRODUCTS ||--o{ ORDER_ITEMS : ""
    PRODUCTS ||--o{ PRODUCT_LIKES : ""

    ORDERS ||--o{ ORDER_ITEMS : ""

    USERS {
        bigint id PK "사용자 식별자"
        varchar(50) user_id UK "로그인 ID"
        varchar(100) email UK "이메일 주소"
        varchar(10) gender "성별"
        date birth_date "생년월일"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
        varchar(50) created_by
        varchar(50) updated_by
    }

    POINTS {
        bigint id PK "포인트 식별자"
        bigint ref_user_id UK "사용자 참조"
        bigint amount "포인트 금액"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
        varchar(50) created_by
        varchar(50) updated_by
    }

    BRANDS {
        bigint id PK "브랜드 식별자"
        varchar(100) name "브랜드명"
        text description "브랜드 설명"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
        varchar(50) created_by
        varchar(50) updated_by
    }

    PRODUCTS {
        bigint id PK "상품 식별자"
        varchar(100) name "상품명"
        bigint price "가격"
        text description "상품 설명"
        int stock "재고 수량"
        bigint like_count "좋아요 수 (비정규화)"
        bigint ref_brand_id "브랜드 참조"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
        varchar(50) created_by
        varchar(50) updated_by
    }

    PRODUCT_LIKES {
        bigint id PK "좋아요 식별자"
        bigint ref_user_id UK "사용자 참조"
        bigint ref_product_id UK "상품 참조"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
        varchar(50) created_by
        varchar(50) updated_by
    }

    %% PRODUCT_LIKES: UNIQUE(ref_user_id, ref_product_id) - 중복 좋아요 방지

    ORDERS {
        bigint id PK "주문 식별자"
        bigint ref_user_id "사용자 참조"
        varchar(50) status "주문 상태"
        bigint total_amount "주문 총액"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
        varchar(50) created_by
        varchar(50) updated_by
    }

    ORDER_ITEMS {
        bigint id PK "주문 항목 식별자"
        bigint ref_order_id "주문 참조"
        bigint ref_product_id "상품 참조"
        int quantity "주문 수량"
        bigint price "주문 당시 가격"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
        varchar(50) created_by
        varchar(50) updated_by
    }
```

## 2. 테이블 설명

### 2.1 USERS
- 사용자 계정 정보
- user_id, email 유니크 제약

### 2.2 POINTS
- 포인트 잔액
- User와 1:1 관계

### 2.3 BRANDS
- 브랜드 정보
- 사전 등록 데이터

### 2.4 PRODUCTS
- 판매 상품 정보
- 재고 관리 (stock)
- 좋아요 수 비정규화 (like_count)
  - 성능 최적화: 매번 COUNT(*) 대신 미리 계산된 값 저장
  - 좋아요 추가/삭제 시 트랜잭션으로 업데이트
- 사전 등록 데이터

### 2.5 PRODUCT_LIKES
- 상품 좋아요
- **제약조건**:
  - PRIMARY KEY: id
  - UNIQUE KEY: (ref_user_id, ref_product_id)
    - 한 사용자가 동일 상품에 중복 좋아요 방지
    - 멱등성 보장을 위한 비즈니스 규칙

### 2.6 ORDERS
- 주문 정보
- total_amount는 OrderItem 합계
- **status**: 주문 상태
  - `COMPLETED`: 결제 완료
  - `PAYMENT_PENDING`: 결제 대기 (결제 처리 실패 시)

### 2.7 ORDER_ITEMS
- 주문 상품 상세
- 주문 당시 가격 저장

## 3. 공통 필드

모든 테이블은 다음 공통 필드를 포함합니다:

| 필드명 | 타입 | 설명 |
|--------|------|------|
| created_at | timestamp | 생성 일시 |
| updated_at | timestamp | 수정 일시 |
| deleted_at | timestamp | 삭제 일시 (Soft Delete) |
| created_by | varchar(50) | 생성자 |
| updated_by | varchar(50) | 수정자 |
