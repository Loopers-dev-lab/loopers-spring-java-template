-- product_ranking_daily: 롤링 집계를 위한 일간 스냅샷 소스
CREATE TABLE IF NOT EXISTS product_ranking_daily (
  stat_date     DATE        NOT NULL,
  product_id    BIGINT      NOT NULL,
  like_count    INT         NOT NULL DEFAULT 0,
  view_count    INT         NOT NULL DEFAULT 0,
  order_count   INT         NOT NULL DEFAULT 0,
  sales_amount  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (stat_date, product_id),
  INDEX idx_prd_daily_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- mv_product_rank_weekly: period_start(anchorDate) 기준 주간 MV
CREATE TABLE IF NOT EXISTS mv_product_rank_weekly (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  product_id     BIGINT       NOT NULL,
  period_start   DATE         NOT NULL,
  rank_position  INT          NOT NULL,
  total_score    DOUBLE       NOT NULL,
  like_count     INT          NOT NULL DEFAULT 0,
  view_count     INT          NOT NULL DEFAULT 0,
  order_count    INT          NOT NULL DEFAULT 0,
  sales_amount   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_weekly_product_period (product_id, period_start),
  INDEX idx_weekly_period_rank (period_start, rank_position),
  INDEX idx_weekly_period_score (period_start, total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- mv_product_rank_monthly: period_start(anchorDate) 기준 월간 MV
CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  product_id     BIGINT       NOT NULL,
  period_start   DATE         NOT NULL,
  rank_position  INT          NOT NULL,
  total_score    DOUBLE       NOT NULL,
  like_count     INT          NOT NULL DEFAULT 0,
  view_count     INT          NOT NULL DEFAULT 0,
  order_count    INT          NOT NULL DEFAULT 0,
  sales_amount   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_monthly_product_period (product_id, period_start),
  INDEX idx_monthly_period_rank (period_start, rank_position),
  INDEX idx_monthly_period_score (period_start, total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


