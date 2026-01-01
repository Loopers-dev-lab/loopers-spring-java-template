# ERD (Entity Relationship Diagram)

> 비즈니스 규칙이 **데이터로 어떻게 저장**되는지를 시각화한 문서입니다.

## 🏗️ 전체 ERD

```mermaid
erDiagram
    users ||--o| points : "포인트 계좌"
    points ||--o{ point_histories : "거래 내역"
    brands ||--o{ products : "소속 상품"
    users ||--o{ likes : "좋아요"
    products ||--o{ likes : "좋아요 받음"
    users ||--o{ orders : "주문"
    orders ||--|{ order_items : "주문 항목"
    coupons ||--o{ user_coupons : "쿠폰 발급"
    users ||--o{ user_coupons : "보유 쿠폰"
    products ||--o{ product_metrics : "메트릭 집계"
    products ||--o{ mv_product_rank_weekly : "주간 랭킹"
    products ||--o{ mv_product_rank_monthly : "월간 랭킹"
    orders ||--o| payments : "결제 정보"
    event_outbox }o--|| orders : "주문 이벤트"
    event_outbox }o--|| payments : "결제 이벤트"
    event_outbox }o--|| likes : "좋아요 이벤트"
    event_inbox }o--|| catalog_events : "카탈로그 이벤트"
    event_inbox }o--|| order_events : "주문 이벤트"

    users {
        bigint id PK
        varchar(10) user_id UK
        varchar(10) gender
        varchar(10) birthdate
        varchar(100) email
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    points {
        bigint id PK
        varchar(10) user_id UK
        decimal(19_2) balance
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    point_histories {
        bigint id PK
        varchar(10) user_id
        varchar(20) transaction_type
        decimal(19_2) amount
        decimal(19_2) balance_after
        varchar(500) description
        timestamp created_at
    }

    brands {
        bigint id PK
        varchar(100) name UK
        text description
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    products {
        bigint id PK
        bigint brand_id FK
        varchar(200) name
        decimal(19_2) price
        int stock
        text description
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    likes {
        bigint id PK
        varchar(10) user_id
        bigint product_id FK
        timestamp created_at
        timestamp deleted_at
    }

    orders {
        bigint id PK
        varchar(10) user_id
        varchar(20) status
        decimal(19_2) total_amount
        timestamp created_at
        timestamp updated_at
        timestamp canceled_at
        timestamp deleted_at
    }

    order_items {
        bigint id PK
        bigint order_id FK
        bigint product_id
        varchar(200) product_name
        varchar(100) brand_name
        int quantity
        decimal(19_2) price
        timestamp created_at
    }

    coupons {
        bigint id PK
        varchar(100) name
        varchar(20) type
        decimal(19_2) discount_value
        varchar(500) description
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    user_coupons {
        bigint id PK
        varchar(10) user_id
        bigint coupon_id FK
        boolean is_used
        timestamp used_at
        bigint version
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    product_metrics {
        bigint product_id PK
        int like_count
        int view_count
        int order_count
        decimal(15_2) sales_amount
        int version
        timestamp created_at
        timestamp updated_at
    }

    mv_product_rank_weekly {
        bigint id PK
        bigint product_id
        varchar(10) year_week
        int rank_position
        double total_score
        int like_count
        int view_count
        int order_count
        decimal(15_2) sales_amount
        timestamp created_at
        timestamp updated_at
    }

    mv_product_rank_monthly {
        bigint id PK
        bigint product_id
        varchar(7) year_month
        int rank_position
        double total_score
        int like_count
        int view_count
        int order_count
        decimal(15_2) sales_amount
        timestamp created_at
        timestamp updated_at
    }

    payments {
        bigint id PK
        varchar(100) transaction_key UK
        varchar(20) order_id
        varchar(10) user_id
        decimal(19_2) amount
        varchar(20) status
        varchar(500) failure_reason
        varchar(50) card_type
        varchar(50) card_no
        timestamp created_at
        timestamp updated_at
    }

    event_outbox {
        bigint id PK
        varchar(50) aggregate_type
        varchar(100) aggregate_id
        varchar(100) event_type
        text payload
        varchar(20) status
        int retry_count
        text error_message
        timestamp created_at
        timestamp updated_at
    }

    event_inbox {
        bigint id PK
        varchar(100) event_id UK
        varchar(50) aggregate_type
        varchar(100) aggregate_id
        varchar(100) event_type
        text payload
        timestamp processed_at
        timestamp created_at
    }

    dead_letter_queue {
        bigint id PK
        varchar(50) topic
        int partition_number
        bigint offset_value
        varchar(100) event_type
        text payload
        text error_message
        timestamp created_at
    }
```

## 📦 테이블별 상세 설계

---

## 1. users (사용자)

### 비즈니스 규칙

| 규칙 | 설명 | DB 구현 |
|---|---|---|
| 사용자 ID는 중복 불가 | 로그인 ID가 겹치면 안 됨 | `UNIQUE(user_id)` |
| 이메일 형식 검증 | xx@yy.zz 형식 | 애플리케이션에서 검증 |
| 회원 탈퇴 시 복구 가능 | 실수로 탈퇴해도 복구 | `deleted_at` (Soft Delete) |

### CREATE 문

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(10) NOT NULL UNIQUE,
    gender VARCHAR(10) NOT NULL,
    birthdate VARCHAR(10) NOT NULL,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
---

## 2. points (포인트)

### 비즈니스 규칙

| 규칙 | 설명 | DB 구현 |
|---|---|---|
| 한 사용자당 하나의 계좌 | 포인트 계좌는 중복 불가 | `UNIQUE(user_id)` |
| 잔액은 음수 불가 | 빚은 안 됨 | `CHECK (balance >= 0)` |
| 잔액 변경 시 히스토리 기록 | 감사 추적 | point_histories 테이블 |

### CREATE 문

```sql
CREATE TABLE points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(10) NOT NULL UNIQUE,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    
    CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 3. point_histories (포인트 거래 내역)

### 비즈니스 규칙

| 규칙 | 설명 | DB 구현 |
|---|---|---|
| 모든 거래는 기록 | 충전/사용/환불 모두 | INSERT만 가능 (UPDATE/DELETE 불가) |
| 거래 후 잔액도 저장 | 정합성 검증용 | `balance_after` 컬럼 |
| 거래 내역은 수정 불가 | 감사 추적 | 애플리케이션에서 UPDATE 금지 |

### CREATE 문

```sql
CREATE TABLE point_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,  -- CHARGE, USE, REFUND
    amount DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 4. products (상품)

### 비즈니스 규칙

| 규칙 | 설명 | DB 구현 |
|---|---|---|
| 재고는 음수 불가 | 판매 불가능한 상품 | `CHECK (stock >= 0)` |
| 가격은 0원 이상 | 음수 가격 불가 | `CHECK (price >= 0)` |
| 모든 상품은 브랜드 소속 | 브랜드 필수 | `brand_id NOT NULL` + FK |
| 동시 주문 시 재고 차감 안전 | 동시성 제어 | 락(Lock) 또는 낙관적 락 |

### CREATE 문

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    
    FOREIGN KEY (brand_id) REFERENCES brands(id),
    CHECK (price >= 0),
    CHECK (stock >= 0),
    INDEX idx_brand_id (brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 동시성 문제

**문제 상황**:
```
[초기] 재고: 1개

[시간] A 고객            B 고객
10:00  재고 확인(1개)
10:00                  재고 확인(1개)
10:01  재고 차감(0개)
10:01                  재고 차감(-1개) ← 문제!
```

**해결 방법 1: 비관적 락**
```sql
-- A 고객의 트랜잭션
SELECT stock FROM products WHERE id = 1 FOR UPDATE;  -- 행 잠금!
-- B 고객은 여기서 대기
UPDATE products SET stock = stock - 1 WHERE id = 1;
COMMIT;  -- 이제 B 고객 차례
```

**해결 방법 2: 낙관적 락**
```sql
-- version 컬럼 추가
ALTER TABLE products ADD COLUMN version INT NOT NULL DEFAULT 0;

-- A 고객
UPDATE products 
SET stock = stock - 1, version = version + 1
WHERE id = 1 AND version = 10;  -- 성공 (1 row affected)

-- B 고객 (동시 시도)
UPDATE products 
SET stock = stock - 1, version = version + 1
WHERE id = 1 AND version = 10;  -- 실패 (0 rows affected)
→ 재시도 또는 에러
```

---

## 5. likes (좋아요)

### 비즈니스 규칙

| 규칙 | 설명 | DB 구현 |
|---|---|---|
| 중복 좋아요 불가 | 한 사용자는 한 상품에 한 번만 | `UNIQUE(user_id, product_id, deleted_at)` |
| 좋아요 취소 시 복구 가능 | 실수로 취소해도 복구 | Soft Delete |
| 좋아요 수 실시간 집계 | 상품 상세 화면에 표시 | COUNT 쿼리 또는 캐싱 |

### CREATE 문

```sql
CREATE TABLE likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(10) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    
    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE INDEX uk_user_product_active (user_id, product_id, deleted_at),
    INDEX idx_product_active (product_id, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### unique로 가장 간단한 멱등성 구현

**시나리오**
```
고객이 좋아요 버튼을 2번 클릭

[1차 시도]
INSERT INTO likes (user_id, product_id) VALUES ('user1', 5);
→ 성공

[2차 시도]
INSERT INTO likes (user_id, product_id) VALUES ('user1', 5);
→ 에러! UNIQUE 제약 위반

애플리케이션에서:
try {
    INSERT ...
} catch (DuplicateKeyException e) {
    return success();  // 에러를 성공으로 변환 (멱등성)
}
```

---

## 6. orders (주문)

### 비즈니스 규칙

| 규칙 | 설명 | DB 구현 |
|---|---|---|
| 주문 금액은 0원 이상 | 음수 주문 불가 | `CHECK (total_amount >= 0)` |
| 배송 시작 후 취소 불가 | 상태 전이 제약 | 애플리케이션에서 검증 |
| 주문 생성 시 재고+포인트 원자적 처리 | 트랜잭션 | BEGIN ~ COMMIT |

### CREATE 문

```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, COMPLETED, CANCELED
    total_amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    canceled_at TIMESTAMP NULL DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    
    CHECK (total_amount >= 0),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 7. order_items (주문 항목)

### 비즈니스 규칙

| 규칙 | 설명 | DB 구현 |
|---|---|---|
| 주문 삭제 시 항목도 삭제 | 종속 관계 | `ON DELETE CASCADE` |
| 가격은 주문 당시 가격 | 가격 스냅샷 | `price` 컬럼에 저장 |
| 수량은 1개 이상 | 0개 주문 불가 | `CHECK (quantity >= 1)` |

### CREATE 문

```sql
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19, 2) NOT NULL,  -- 주문 당시 가격 (스냅샷)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    CHECK (quantity >= 1),
    CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

---

## 9. coupons (쿠폰 마스터)

**설명**: 쿠폰 마스터 정보를 저장하는 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 쿠폰 고유 번호 |
| name | varchar(100) | NOT NULL | 쿠폰명 |
| type | varchar(20) | NOT NULL | 쿠폰 타입 (FIXED_AMOUNT, PERCENTAGE) |
| discount_value | decimal(19,0) | NOT NULL | 할인 값 (정액: 금액, 정률: 퍼센트) |
| description | varchar(500) | NULL | 쿠폰 설명 |
| created_at | timestamp | NOT NULL | 생성 시간 |
| updated_at | timestamp | NOT NULL | 수정 시간 |
| deleted_at | timestamp | NULL | 삭제 시간 (Soft Delete) |

**비즈니스 규칙**:
- 쿠폰 타입은 `FIXED_AMOUNT`(정액) 또는 `PERCENTAGE`(정률)만 가능
- `discount_value`는 0보다 커야 함
- 정률 쿠폰의 경우 `discount_value`는 100 이하여야 함

**인덱스**:
```sql
INDEX idx_type (type)
```

---

## 10. user_coupons (사용자별 발급 쿠폰)

**설명**: 사용자에게 발급된 쿠폰을 저장하는 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 발급 쿠폰 고유 번호 |
| user_id | varchar(10) | NOT NULL | 사용자 ID |
| coupon_id | bigint | FK (coupons.id) | 쿠폰 마스터 ID |
| is_used | boolean | NOT NULL, DEFAULT false | 사용 여부 |
| used_at | timestamp | NULL | 사용 시간 |
| version | bigint | NOT NULL, DEFAULT 0 | 낙관적 락 버전 |
| created_at | timestamp | NOT NULL | 생성 시간 (발급 시간) |
| updated_at | timestamp | NOT NULL | 수정 시간 |
| deleted_at | timestamp | NULL | 삭제 시간 (Soft Delete) |

**외래키**:
```sql
FOREIGN KEY (coupon_id) REFERENCES coupons(id)
```

**비즈니스 규칙**:
- 한 번 사용된 쿠폰(`is_used = true`)은 재사용 불가
- 삭제된 쿠폰(`deleted_at IS NOT NULL`)은 사용 불가
- `version` 필드를 통한 낙관적 락으로 동시성 제어

**인덱스**:
```sql
INDEX idx_user_id (user_id)
INDEX idx_coupon_id (coupon_id)
INDEX idx_user_id_is_used (user_id, is_used, deleted_at)
```

**동시성 제어**:
- `version` 컬럼: JPA `@Version`을 통한 낙관적 락
- 비관적 락: `SELECT ... FOR UPDATE` 사용 시 row lock

---

## 11. product_metrics (상품 메트릭 집계)

**설명**: 실시간 상품 이벤트 집계를 저장하는 테이블 (Round 9 추가)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| product_id | bigint | PK | 상품 ID |
| like_count | int | NOT NULL, DEFAULT 0 | 좋아요 수 |
| view_count | int | NOT NULL, DEFAULT 0 | 조회 수 |
| order_count | int | NOT NULL, DEFAULT 0 | 주문 수 |
| sales_amount | decimal(15,2) | NOT NULL, DEFAULT 0.00 | 판매 금액 |
| version | int | NOT NULL, DEFAULT 0 | 낙관적 락 버전 |
| created_at | timestamp | NOT NULL | 생성 시간 |
| updated_at | timestamp | NOT NULL | 수정 시간 |

**비즈니스 규칙**:
- Kafka 이벤트를 통해 실시간으로 집계
- `version` 필드를 통한 낙관적 락으로 동시성 제어
- 일별 데이터 집계

**인덱스**:
```sql
INDEX idx_like_count (like_count DESC)
INDEX idx_view_count (view_count DESC)
INDEX idx_order_count (order_count DESC)
INDEX idx_updated_at (updated_at)
```

---

## 12. mv_product_rank_weekly (주간 랭킹)

**설명**: 주간 상품 랭킹 Materialized View (Round 10 추가)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 랭킹 고유 번호 |
| product_id | bigint | NOT NULL | 상품 ID |
| year_week | varchar(10) | NOT NULL | ISO Week 형식 (YYYY-Wnn) |
| rank_position | int | NOT NULL | 순위 (1~100) |
| total_score | double | NOT NULL | 총점 |
| like_count | int | NOT NULL | 좋아요 수 |
| view_count | int | NOT NULL | 조회 수 |
| order_count | int | NOT NULL | 주문 수 |
| sales_amount | decimal(15,2) | NOT NULL | 판매 금액 |
| created_at | timestamp | NOT NULL | 생성 시간 |
| updated_at | timestamp | NOT NULL | 수정 시간 |

**비즈니스 규칙**:
- Spring Batch로 주 1회 집계 (매주 월요일 01:00)
- TOP 100만 저장
- 점수 계산: `(view_count × 0.1) + (like_count × 0.2) + (order_count × 0.6 × log10(sales_amount + 1))`

**인덱스**:
```sql
UNIQUE KEY uk_product_week (product_id, year_week)
INDEX idx_year_week_rank (year_week, rank_position)
INDEX idx_year_week_score (year_week, total_score DESC)
```

---

## 13. mv_product_rank_monthly (월간 랭킹)

**설명**: 월간 상품 랭킹 Materialized View (Round 10 추가)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 랭킹 고유 번호 |
| product_id | bigint | NOT NULL | 상품 ID |
| year_month | varchar(7) | NOT NULL | 년월 형식 (YYYY-MM) |
| rank_position | int | NOT NULL | 순위 (1~100) |
| total_score | double | NOT NULL | 총점 |
| like_count | int | NOT NULL | 좋아요 수 |
| view_count | int | NOT NULL | 조회 수 |
| order_count | int | NOT NULL | 주문 수 |
| sales_amount | decimal(15,2) | NOT NULL | 판매 금액 |
| created_at | timestamp | NOT NULL | 생성 시간 |
| updated_at | timestamp | NOT NULL | 수정 시간 |

**비즈니스 규칙**:
- Spring Batch로 월 1회 집계 (매월 1일 02:00)
- TOP 100만 저장
- 점수 계산: 주간 랭킹과 동일

**인덱스**:
```sql
UNIQUE KEY uk_product_month (product_id, year_month)
INDEX idx_year_month_rank (year_month, rank_position)
INDEX idx_year_month_score (year_month, total_score DESC)
```

---

## 업데이트된 order_items 테이블 (스냅샷 패턴)

**변경 사항**: `product_id`를 FK에서 일반 컬럼으로 변경, 스냅샷 필드 추가

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 주문 항목 고유 번호 |
| order_id | bigint | FK (orders.id) | 주문 ID |
| product_id | bigint | NOT NULL | 상품 ID (FK 아님, 스냅샷) |
| product_name | varchar(200) | NOT NULL | 상품명 (주문 당시) |
| brand_name | varchar(100) | NOT NULL | 브랜드명 (주문 당시) |
| quantity | int | NOT NULL | 주문 수량 |
| price | decimal(19,2) | NOT NULL | 주문 당시 가격 |
| created_at | timestamp | NOT NULL | 생성 시간 |

**스냅샷 패턴 적용 이유**:
- 상품 정보가 변경되어도 주문 내역은 주문 당시 정보를 유지
- Product 테이블과의 강한 결합 제거
- 상품 삭제 시에도 주문 내역 조회 가능

---

## 14. payments (결제)

**설명**: 결제 정보 및 상태 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 결제 고유 번호 |
| transaction_key | varchar(100) | UNIQUE, NOT NULL | PG사 거래 키 (중복 방지) |
| order_id | varchar(20) | NOT NULL | 주문 ID |
| user_id | varchar(10) | NOT NULL | 사용자 ID |
| amount | decimal(19,2) | NOT NULL | 결제 금액 |
| status | varchar(20) | NOT NULL | 결제 상태 (PENDING, SUCCESS, FAILED) |
| failure_reason | varchar(500) | NULL | 실패 사유 (FAILED 시 필수) |
| card_type | varchar(50) | NULL | 카드 타입 (CREDIT, DEBIT) |
| card_no | varchar(50) | NULL | 카드 번호 (마스킹 처리) |
| created_at | timestamp | NOT NULL | 생성 시간 |
| updated_at | timestamp | NOT NULL | 수정 시간 |

**비즈니스 규칙**:
- `transaction_key`는 PG사에서 제공하는 고유 거래 식별자
- 결제 상태는 PENDING → SUCCESS 또는 FAILED로만 변경 가능
- 결제 실패 시 `failure_reason` 필수
- `card_no`는 마스킹 처리 (예: 1234-****-****-5678)

**인덱스**:
```sql
UNIQUE INDEX uk_transaction_key (transaction_key)
INDEX idx_order_id (order_id)
INDEX idx_user_id (user_id)
INDEX idx_status (status)
```

---

## 15. event_outbox (이벤트 아웃박스)

**설명**: Transactional Outbox 패턴 구현 (Round 8 추가)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 아웃박스 고유 번호 |
| aggregate_type | varchar(50) | NOT NULL | 애그리거트 타입 (ORDER, PAYMENT, LIKE) |
| aggregate_id | varchar(100) | NOT NULL | 애그리거트 ID |
| event_type | varchar(100) | NOT NULL | 이벤트 타입 (OrderCreatedEvent 등) |
| payload | text | NOT NULL | 이벤트 페이로드 (JSON) |
| status | varchar(20) | NOT NULL, DEFAULT 'PENDING' | 발행 상태 (PENDING, PUBLISHED, FAILED) |
| retry_count | int | NOT NULL, DEFAULT 0 | 재시도 횟수 |
| error_message | text | NULL | 에러 메시지 (실패 시) |
| created_at | timestamp | NOT NULL | 생성 시간 |
| updated_at | timestamp | NOT NULL | 수정 시간 |

**비즈니스 규칙**:
- 비즈니스 트랜잭션과 동일한 트랜잭션에서 이벤트 저장
- 최대 재시도 횟수는 3회
- 3회 실패 시 status = FAILED (수동 처리 필요)
- OutboxEventPublisher 스케줄러가 주기적으로 PENDING 이벤트 발행

**인덱스**:
```sql
INDEX idx_status_created (status, created_at)
INDEX idx_aggregate (aggregate_type, aggregate_id)
```

**Transactional Outbox 패턴**:
```
[목적]
이벤트 발행 실패 시에도 이벤트 손실 방지

[흐름]
1. 주문 생성 + EventOutbox 저장 (같은 트랜잭션)
2. 트랜잭션 커밋
3. OutboxEventPublisher가 PENDING 이벤트 조회
4. Kafka로 발행
5. 성공 → PUBLISHED / 실패 → 재시도
```

---

## 16. event_inbox (이벤트 인박스)

**설명**: Event Inbox 패턴 구현 - 멱등성 보장 (Round 9 추가)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | 인박스 고유 번호 |
| event_id | varchar(100) | UNIQUE, NOT NULL | 이벤트 고유 ID (중복 방지) |
| aggregate_type | varchar(50) | NOT NULL | 애그리거트 타입 |
| aggregate_id | varchar(100) | NOT NULL | 애그리거트 ID |
| event_type | varchar(100) | NOT NULL | 이벤트 타입 |
| payload | text | NOT NULL | 이벤트 페이로드 (JSON) |
| processed_at | timestamp | NOT NULL | 처리 시간 |
| created_at | timestamp | NOT NULL | 생성 시간 (수신 시간) |

**비즈니스 규칙**:
- Kafka 이벤트 수신 시 중복 처리 방지
- `event_id`가 이미 존재하면 이벤트 스킵 (멱등성 보장)
- 처리 성공 시에만 저장

**인덱스**:
```sql
UNIQUE INDEX uk_event_id (event_id)
INDEX idx_aggregate (aggregate_type, aggregate_id)
INDEX idx_processed_at (processed_at)
```

**Event Inbox 패턴**:
```
[목적]
중복 이벤트 처리 방지 (Exactly-once 보장)

[흐름]
1. Kafka 메시지 수신
2. event_id 추출
3. event_inbox 테이블 조회
4. 중복이면 → 스킵
5. 중복 아니면 → 비즈니스 로직 실행 + event_inbox 저장
```

---

## 17. dead_letter_queue (실패 메시지 저장)

**설명**: 처리 실패한 Kafka 메시지 저장 (Round 9 추가)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | bigint | PK, AUTO_INCREMENT | DLQ 고유 번호 |
| topic | varchar(50) | NOT NULL | Kafka 토픽명 |
| partition_number | int | NOT NULL | 파티션 번호 |
| offset_value | bigint | NOT NULL | 오프셋 값 |
| event_type | varchar(100) | NULL | 이벤트 타입 |
| payload | text | NOT NULL | 메시지 페이로드 |
| error_message | text | NOT NULL | 에러 메시지 |
| created_at | timestamp | NOT NULL | 생성 시간 (실패 시간) |

**비즈니스 규칙**:
- Kafka Consumer에서 처리 실패한 메시지 저장
- 수동으로 재처리 가능
- 에러 분석 및 디버깅 용도

**인덱스**:
```sql
INDEX idx_topic_created (topic, created_at DESC)
INDEX idx_event_type (event_type)
```

**DLQ 패턴**:
```
[목적]
실패한 메시지를 별도 저장소에 보관하여 재처리 가능

[흐름]
1. Kafka 메시지 처리 중 예외 발생
2. dead_letter_queue에 저장
3. 로그로 알림
4. 수동으로 원인 파악 후 재처리
```

---

## 동시성 제어 전략 요약

### Version 필드 (@Version)
- `products.version`
- `points.version`
- `user_coupons.version`

**목적**: 낙관적 락을 통한 동시성 제어
**동작**: UPDATE 시 version 자동 증가, 충돌 시 OptimisticLockException

### 비관적 락 (@Lock PESSIMISTIC_WRITE)
**주문 생성 시 락 획득 순서** (데드락 방지):
1. UserCoupon (쿠폰 ID 기준)
2. Product (상품 ID 기준)
3. Point (사용자 ID 기준)

**SQL**: `SELECT ... FOR UPDATE`
**목적**: 트랜잭션 격리, 동시 수정 방지
