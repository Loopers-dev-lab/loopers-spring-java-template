-- 주간 랭킹 Materialized View
CREATE TABLE IF NOT EXISTS mv_product_rank_weekly (
    id BIGINT AUTO_INCREMENT,
    rank_position INT NOT NULL,
    product_id BIGINT NOT NULL,
    total_score DOUBLE NOT NULL,
    week_start_date DATE NOT NULL COMMENT '주간 시작일 (월요일)',
    week_end_date DATE NOT NULL COMMENT '주간 종료일 (일요일)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_weekly_ranking_week_position (week_start_date, rank_position),
    INDEX idx_week_product (week_start_date, product_id),
    INDEX idx_product_week (product_id, week_start_date)
) COMMENT '주간 상품 랭킹 (TOP 100)';

-- 월간 랭킹 Materialized View
CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
    id BIGINT AUTO_INCREMENT,
    rank_position INT NOT NULL,
    product_id BIGINT NOT NULL,
    total_score DOUBLE NOT NULL,
    month_year VARCHAR(7) NOT NULL COMMENT '월간 키 (YYYY-MM)',
    month_start_date DATE NOT NULL COMMENT '월간 시작일',
    month_end_date DATE NOT NULL COMMENT '월간 종료일',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_ranking_month_position (month_start_date, rank_position),
    INDEX idx_month_product (month_year, product_id),
    INDEX idx_product_month (product_id, month_year)
) COMMENT '월간 상품 랭킹 (TOP 100)';

-- Product Metrics 테이블 (배치 소스)
CREATE TABLE IF NOT EXISTS product_metrics (
    product_id BIGINT NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    sales_count BIGINT NOT NULL DEFAULT 0,
    sales_amount BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (product_id)
) COMMENT '상품 메트릭 집계 테이블';
