# 10주차 - Spring Batch 기반 주간/월간 랭킹 시스템

## 1. 개요

### 1.1 목표
Spring Batch를 활용해 주간, 월간 랭킹 시스템 구축

### 1.2 배경
- 9주차에 Redis ZSET 기반 실시간 일간 랭킹 시스템 구축
- 이번 주차: `product_metrics` 일간 집계 데이터를 기반으로 주간/월간 랭킹 생성
- Materialized View 패턴으로 조회 성능 최적화

### 1.3 키워드
- Spring Batch (Job / Step / Chunk / Tasklet)
- ItemReader / ItemProcessor / ItemWriter
- Materialized View (사전 집계)
- 실시간 처리 vs 배치 처리

---

## 2. 기능 요구사항

### 2.1 Spring Batch Job

**요구사항**
- product_metrics 테이블의 일간 집계 데이터를 읽어서 처리
- Chunk-Oriented Processing (Reader/Processor/Writer) 방식 권장 (Tasklet도 가능)
- 파라미터 기반 동작 (날짜 파라미터로 특정 기간 처리)

**주간 집계 Job**
- 7일치 데이터 합산
- 상위 100개 상품 추출
- mv_product_rank_weekly 테이블에 저장

**월간 집계 Job**
- 30일치 데이터 합산
- 상위 100개 상품 추출
- mv_product_rank_monthly 테이블에 저장

**제약사항**
- TOP 100 상품만 저장

---

### 2.2 Materialized View 테이블

**테이블 설계**

| 테이블명 | 용도 | 비고 |
|----------|------|------|
| mv_product_rank_weekly | 주간 TOP 100 | 배치 결과 저장 |
| mv_product_rank_monthly | 월간 TOP 100 | 배치 결과 저장 |

**주간 랭킹 스키마**

```sql
CREATE TABLE mv_product_rank_weekly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255),
    like_count INT DEFAULT 0,
    order_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    total_score INT DEFAULT 0,
    ranking INT NOT NULL,
    year_week VARCHAR(10) NOT NULL,  -- 예: "2025-W01"
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_year_week_ranking (year_week, ranking),
    INDEX idx_product_id (product_id)
);
```

**월간 랭킹 스키마**

```sql
CREATE TABLE mv_product_rank_monthly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255),
    like_count INT DEFAULT 0,
    order_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    total_score INT DEFAULT 0,
    ranking INT NOT NULL,
    year_month VARCHAR(7) NOT NULL,  -- 예: "2025-01"
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_year_month_ranking (year_month, ranking),
    INDEX idx_product_id (product_id)
);
```

---

### 2.3 Ranking API 확장

**기존 API**
```
GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1
```

**확장된 API**
```
GET /api/v1/rankings?period={period}&date=yyyyMMdd&size=20&page=1
```

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| period | String | N | daily/weekly/monthly | daily |
| date | String | N | 조회 날짜 (yyyyMMdd) | 오늘 |
| page | Integer | N | 페이지 번호 (1-based) | 1 |
| size | Integer | N | 페이지 크기 | 20 |

**period별 동작**

| period | 데이터 소스 | 설명 |
|--------|------------|------|
| daily | Redis ZSET | 기존 방식 유지 |
| weekly | mv_product_rank_weekly | 해당 주차 데이터 조회 |
| monthly | mv_product_rank_monthly | 해당 월 데이터 조회 |

**제약사항**
- weekly/monthly는 배치가 실행된 데이터만 조회 가능
- 데이터가 없으면 빈 배열 반환

---

## 3. 기술 스펙

### 3.1 product_metrics 테이블 수정

**현재 구조 (누적 데이터)**
```sql
CREATE TABLE product_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    like_count BIGINT,
    order_count BIGINT,
    total_quantity BIGINT,
    last_like_event_at DATETIME
);
```

**변경된 구조 (일간 데이터)**
```sql
CREATE TABLE product_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    date DATE NOT NULL,
    like_count BIGINT DEFAULT 0,
    order_count BIGINT DEFAULT 0,
    total_quantity BIGINT DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    last_like_event_at DATETIME,
    UNIQUE KEY uk_product_date (product_id, date)
);
```

### 3.2 Spring Batch 구성

**의존성**
```groovy
implementation 'org.springframework.boot:spring-boot-starter-batch'
```

**Job 구성**

| Job | Step | Reader | Processor | Writer |
|-----|------|--------|-----------|--------|
| weeklyRankingJob | aggregateStep | JpaPagingItemReader | RankingProcessor | JpaItemWriter |
| monthlyRankingJob | aggregateStep | JpaPagingItemReader | RankingProcessor | JpaItemWriter |

**파라미터**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| startDate | String | 집계 시작 날짜 |
| endDate | String | 집계 종료 날짜 |
| timestamp | Long | Job Instance 구분용 |

### 3.3 프로젝트 구조 (예시)

```
src/main/java/com/loopers/
├── infrastructure/
│   └── batch/
│       ├── config/
│       │   └── BatchConfig.java
│       ├── job/
│       │   ├── WeeklyRankingJobConfig.java
│       │   └── MonthlyRankingJobConfig.java
│       ├── reader/
│       │   └── ProductMetricReader.java
│       ├── processor/
│       │   └── RankingProcessor.java
│       └── writer/
│           └── RankingWriter.java
└── domain/
    └── ranking/
        ├── WeeklyRanking.java
        └── MonthlyRanking.java
```

---

## 4. 체크리스트

### 4.1 Spring Batch
- [ ] Spring Batch Job을 작성하고, 파라미터 기반으로 동작시킬 수 있다
- [ ] Chunk Oriented Processing (Reader/Processor/Writer or Tasklet) 기반의 배치 처리를 구현했다
- [ ] 집계 결과를 저장할 Materialized View의 구조를 설계하고 올바르게 적재했다

### 4.2 Ranking API
- [ ] API가 일간, 주간, 월간 랭킹을 제공하며 조회해야 하는 형태에 따라 적절한 데이터를 기반으로 랭킹을 제공한다

---

## 5. 구현 순서

```
1. product_metrics 테이블 수정 (date 컬럼 추가)
   │
   ↓
2. MV 테이블 생성 (mv_product_rank_weekly, mv_product_rank_monthly)
   │
   ↓
3. Spring Batch 설정 및 Job 구현
   │
   ↓
4. Ranking API 확장 (period 파라미터)
```

---

## 6. 선택 사항 (Nice-to-Have)

| 기능 | 설명 |
|------|------|
| 스케줄링 | 주간: 매주 월요일 새벽, 월간: 매월 1일 새벽 |
| Job 파라미터 검증 | 잘못된 날짜 입력 시 예외 처리 |
| 배치 모니터링 | JobExecution 상태 조회 API |
| 재실행 전략 | 실패 시 해당 청크만 재처리 |
