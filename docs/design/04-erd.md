# ERD (Entity Relationship Diagram)

> 클래스 다이어그램을 기반으로 설계된 데이터베이스 스키마

```mermaid
erDiagram
    %% 사용자 관리
    USER {
        bigint id PK "사용자 ID"
        varchar login_id UK "로그인 ID"
        varchar name "사용자명"
        varchar email UK "이메일"
        enum role "사용자 역할 (CUSTOMER, SELLER, ADMIN)"
        boolean is_active "활성 상태"
        timestamp created_at "생성일시"
        timestamp updated_at "수정일시"
    }
    
    POINT {
        bigint id PK "포인트 ID"
        bigint user_id FK "사용자 ID"
        int amount "포인트 잔액"
        timestamp updated_at "최종 수정일시"
    }
    
    %% 브랜드 관리
    BRAND {
        bigint id PK "브랜드 ID"
        varchar name UK "브랜드명"
        text description "브랜드 설명"
        varchar sns_links "SNS 링크"
        int like_count "좋아요 수"
        boolean is_active "활성 상태"
        timestamp created_at "생성일시"
        timestamp updated_at "수정일시"
    }
    
    BRAND_LIKE {
        bigint id PK "브랜드 좋아요 ID"
        bigint user_id FK "사용자 ID"
        bigint brand_id FK "브랜드 ID"
        timestamp liked_at "좋아요 등록일시"
    }
    
    %% 상품 관리
    PRODUCT {
        bigint id PK "상품 ID"
        varchar name "상품명"
        bigint brand_id FK "브랜드 ID"
        int price "상품 가격"
        int stock "재고 수량"
        text description "상품 설명"
        varchar image_url "대표 이미지 URL"
        enum status "상품 상태 (ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED)"
        int like_count "좋아요 수"
        timestamp created_at "생성일시"
        timestamp updated_at "수정일시"
    }
    
    PRODUCT_OPTION {
        bigint id PK "상품 옵션 ID"
        bigint product_id FK "상품 ID"
        varchar name "옵션명"
        varchar value "옵션값"
        int additional_price "추가 가격"
        boolean is_active "활성 상태"
        timestamp created_at "생성일시"
        timestamp updated_at "수정일시"
    }
    
    PRODUCT_LIKE {
        bigint id PK "상품 좋아요 ID"
        bigint user_id FK "사용자 ID"
        bigint product_id FK "상품 ID"
        timestamp liked_at "좋아요 등록일시"
    }
    
    %% 주문 관리
    ORDER {
        bigint id PK "주문 ID"
        varchar order_number UK "주문번호"
        bigint user_id FK "사용자 ID"
        enum status "주문 상태 (PENDING_PAYMENT, PAYMENT_COMPLETED, PAYMENT_FAILED, CANCELLED, EXPIRED)"
        int total_price "총 주문 금액"
        timestamp created_at "주문 생성일시"
        timestamp updated_at "주문 수정일시"
    }
    
    ORDER_ITEM {
        bigint id PK "주문 항목 ID"
        bigint order_id FK "주문 ID"
        bigint product_id FK "상품 ID"
        bigint product_option_id FK "상품 옵션 ID"
        int quantity "주문 수량"
        int price_per_unit "단가"
        varchar product_name "주문 시점 상품명"
        varchar option_name "주문 시점 옵션명"
        varchar image_url "주문 시점 이미지 URL"
        timestamp created_at "생성일시"
    }
    
    %% 결제 관리
    PAYMENT {
        bigint id PK "결제 ID"
        bigint order_id FK "주문 ID"
        enum status "결제 상태 (INITIATED, PROCESSING, COMPLETED, FAILED, CANCELLED)"
        int amount "결제 금액"
        enum method "결제 방법 (CARD, POINT, MIXED)"
        int points_used "사용된 포인트"
        varchar transaction_id "거래 고유 ID"
        timestamp created_at "결제 생성일시"
        timestamp completed_at "결제 완료일시"
    }
    
    PAYMENT_CALLBACK {
        bigint id PK "결제 콜백 ID"
        bigint payment_id FK "결제 ID"
        varchar order_number "주문번호"
        int amount "콜백 금액"
        boolean success "결제 성공 여부"
        varchar transaction_id "거래 고유 ID"
        varchar signature "서명"
        timestamp received_at "콜백 수신일시"
    }
    
    %% 관계 정의
    USER ||--|| POINT : "1:1 사용자-포인트"
    USER ||--o{ BRAND_LIKE : "1:N 사용자-브랜드좋아요"
    USER ||--o{ PRODUCT_LIKE : "1:N 사용자-상품좋아요"
    USER ||--o{ ORDER : "1:N 사용자-주문"
    
    BRAND ||--o{ BRAND_LIKE : "1:N 브랜드-좋아요"
    BRAND ||--o{ PRODUCT : "1:N 브랜드-상품"
    
    
    PRODUCT ||--o{ PRODUCT_OPTION : "1:N 상품-옵션"
    PRODUCT ||--o{ PRODUCT_LIKE : "1:N 상품-좋아요"
    PRODUCT ||--o{ ORDER_ITEM : "1:N 상품-주문항목"
    
    PRODUCT_OPTION ||--o{ ORDER_ITEM : "1:N 옵션-주문항목"
    
    ORDER ||--o{ ORDER_ITEM : "1:N 주문-주문항목"
    ORDER ||--|| PAYMENT : "1:1 주문-결제"
    
    PAYMENT ||--o{ PAYMENT_CALLBACK : "1:N 결제-콜백"
```

---

## 데이터베이스 제약조건

### 1. Primary Keys
- 모든 테이블은 `id` 컬럼을 Primary Key로 사용
- `bigint` 타입의 자동 증가 값

### 2. Unique Constraints
- **USER**: `login_id`, `email` (중복 불가)
- **BRAND**: `name` (브랜드명 중복 불가)
- **ORDER**: `order_number` (주문번호 중복 불가)
- **BRAND_LIKE**: `(user_id, brand_id)` 복합 유니크
- **PRODUCT_LIKE**: `(user_id, product_id)` 복합 유니크

### 3. Foreign Key Constraints
- **CASCADE DELETE**: 사용자 삭제 시 관련 데이터 함께 삭제
- **RESTRICT DELETE**: 참조되는 데이터 삭제 시 오류 발생
- 모든 FK는 NOT NULL (참조 무결성 보장)

### 4. Check Constraints
- **POINT.amount**: `>= 0` (음수 포인트 불가)
- **PRODUCT.price**: `> 0` (가격은 양수)
- **PRODUCT.stock**: `>= 0` (재고는 0 이상)
- **ORDER.total_price**: `> 0` (주문 금액은 양수)
- **ORDER_ITEM.quantity**: `> 0` (수량은 양수)
- **PAYMENT.amount**: `> 0` (결제 금액은 양수)

### 5. Indexes
- **성능 최적화를 위한 인덱스**:
  - `USER(login_id)`, `USER(email)`
  - `PRODUCT(brand_id)`, `PRODUCT(status)`
  - `ORDER(user_id)`, `ORDER(status)`, `ORDER(created_at)`
  - `ORDER_ITEM(order_id)`, `ORDER_ITEM(product_id)`
  - `PAYMENT(order_id)`, `PAYMENT(status)`
  - `BRAND_LIKE(user_id, brand_id)`, `PRODUCT_LIKE(user_id, product_id)`

---

## 특수 설계 고려사항

### 1. 멱등성 보장
- **BRAND_LIKE**, **PRODUCT_LIKE**: 복합 유니크 키로 중복 방지
- **PAYMENT_CALLBACK**: 동일한 `transaction_id`로 중복 처리 방지

### 2. 데이터 일관성
- **ORDER_ITEM**: 주문 시점의 상품 정보 스냅샷 저장
- **POINT**: 결제 완료 시점에만 차감, 주문 취소 시 복구
- **PRODUCT.stock**: 결제 완료 시점에 차감, 주문 취소 시 복구

### 3. 성능 고려사항
- **like_count**: 비정규화로 조회 성능 향상
- **ORDER_ITEM**: 상품 정보 중복 저장으로 조회 성능 향상
- **적절한 인덱스**: 자주 사용되는 조회 패턴에 맞춘 인덱스 설계

### 4. 확장성 고려사항
- **PRODUCT_OPTION**: 다양한 옵션 타입 지원
- **PAYMENT_METHOD**: ENUM으로 새로운 결제 수단 추가 용이