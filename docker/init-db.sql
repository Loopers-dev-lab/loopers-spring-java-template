-- PG Simulator를 위한 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS paymentgateway CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
GRANT ALL PRIVILEGES ON paymentgateway.* TO 'application'@'%';
FLUSH PRIVILEGES;

-- Round 8: Kafka Event Pipeline 테이블 생성
USE loopers;

-- Event Inbox 테이블 (Consumer 멱등성 보장)
CREATE TABLE IF NOT EXISTS event_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(50) NOT NULL COMMENT 'Outbox의 ID (멱등키)',
    aggregate_type VARCHAR(50) NOT NULL COMMENT 'ORDER, PRODUCT, LIKE, PAYMENT',
    aggregate_id VARCHAR(50) NOT NULL COMMENT 'orderId, productId 등',
    event_type VARCHAR(100) NOT NULL COMMENT 'OrderCreatedEvent, LikeCreatedEvent 등',
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '처리 시각',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    UNIQUE INDEX idx_event_id (event_id),
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Consumer 멱등성 보장을 위한 Inbox';

-- Product Metrics 집계 테이블
CREATE TABLE IF NOT EXISTS product_metrics (
    product_id BIGINT PRIMARY KEY COMMENT '상품 ID',
    like_count INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    view_count INT NOT NULL DEFAULT 0 COMMENT '조회 수',
    order_count INT NOT NULL DEFAULT 0 COMMENT '주문 수',
    sales_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '판매 금액',
    version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic Lock',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_like_count (like_count DESC),
    INDEX idx_view_count (view_count DESC),
    INDEX idx_order_count (order_count DESC),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='상품별 집계 데이터';

-- Dead Letter Queue (DLQ)
CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_topic VARCHAR(100) NOT NULL COMMENT '원본 Topic',
    partition_key VARCHAR(100) COMMENT 'Partition Key',
    event_id VARCHAR(50) COMMENT '이벤트 ID',
    payload TEXT NOT NULL COMMENT '원본 메시지',
    error_message TEXT COMMENT '에러 메시지',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    failed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '실패 시각',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_failed_at (failed_at),
    INDEX idx_event_id (event_id),
    INDEX idx_topic (original_topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='처리 실패한 메시지 저장';
