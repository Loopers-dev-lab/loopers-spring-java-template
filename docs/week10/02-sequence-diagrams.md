# 시퀀스 다이어그램

## 1. Spring Batch 주간 랭킹 집계 흐름

~~~mermaid
sequenceDiagram
    participant Scheduler as Scheduler/Admin
    participant JobLauncher as JobLauncher
    participant Job as WeeklyRankingJob
    participant Reader as JpaPagingItemReader
    participant Processor as RankingProcessor
    participant Writer as JpaItemWriter
    participant MetricsDB as product_metrics
    participant MVDB as mv_product_rank_weekly

    Scheduler->>+JobLauncher: run(job, parameters)
    Note right of JobLauncher: targetDate=2025-12-29

    JobLauncher->>+Job: execute
    Job->>Job: 기간 계산 (7일)
    Note over Job: startDate: 2025-12-22<br/>endDate: 2025-12-28

    loop Chunk Processing (chunkSize=100)
        Job->>+Reader: read()
        Reader->>+MetricsDB: SELECT ... WHERE date BETWEEN startDate AND endDate<br/>GROUP BY product_id
        MetricsDB-->>-Reader: List<ProductMetrics>
        Reader-->>-Job: chunk of items

        Job->>+Processor: process(items)
        Processor->>Processor: 점수 계산<br/>(view*0.1 + like*0.2 + order*0.6)
        Processor->>Processor: TOP 100 추출
        Processor-->>-Job: List<RankingEntry>

        Job->>+Writer: write(items)
        Writer->>+MVDB: DELETE WHERE period_start = startDate
        MVDB-->>-Writer: OK
        Writer->>+MVDB: INSERT ranking entries
        MVDB-->>-Writer: OK
        Writer-->>-Job: done
    end

    Job-->>-JobLauncher: JobExecution (COMPLETED)
    JobLauncher-->>-Scheduler: 200 OK
~~~

---

## 2. Spring Batch 월간 랭킹 집계 흐름

~~~mermaid
sequenceDiagram
    participant Scheduler as Scheduler/Admin
    participant JobLauncher as JobLauncher
    participant Job as MonthlyRankingJob
    participant Reader as JpaPagingItemReader
    participant Processor as RankingProcessor
    participant Writer as JpaItemWriter
    participant MetricsDB as product_metrics
    participant MVDB as mv_product_rank_monthly

    Scheduler->>+JobLauncher: run(job, parameters)
    Note right of JobLauncher: targetDate=2025-12-01

    JobLauncher->>+Job: execute
    Job->>Job: 기간 계산 (30일)
    Note over Job: startDate: 2025-11-01<br/>endDate: 2025-11-30

    loop Chunk Processing (chunkSize=100)
        Job->>+Reader: read()
        Reader->>+MetricsDB: SELECT ... WHERE date BETWEEN startDate AND endDate<br/>GROUP BY product_id
        MetricsDB-->>-Reader: aggregated data
        Reader-->>-Job: chunk of items

        Job->>+Processor: process(items)
        Processor->>Processor: 점수 계산
        Processor->>Processor: TOP 100 추출
        Processor-->>-Job: List<RankingEntry>

        Job->>+Writer: write(items)
        Writer->>+MVDB: REPLACE INTO
        MVDB-->>-Writer: OK
        Writer-->>-Job: done
    end

    Job-->>-JobLauncher: JobExecution (COMPLETED)
    JobLauncher-->>-Scheduler: 200 OK
~~~

---

## 3. 일간 랭킹 조회 (기존 - Redis)

~~~mermaid
sequenceDiagram
    participant C as Client
    participant API as RankingAPI
    participant Facade as RankingFacade
    participant Redis as Redis ZSET
    participant DB as ProductDB

    C->>+API: GET /api/v1/rankings?period=daily&date=20251230
    API->>+Facade: getRanking(period=DAILY, date, page, size)

    Facade->>Facade: period == DAILY
    Facade->>+Redis: ZREVRANGE ranking:all:20251230
    Redis-->>-Facade: List<productId, score>

    Facade->>+DB: findAllById(productIds)
    DB-->>-Facade: List<Product>

    Facade->>Facade: 랭킹 + 상품정보 조합
    Facade-->>-API: RankingResponse
    API-->>-C: 200 OK
~~~

---

## 4. 주간/월간 랭킹 조회 (신규 - MV)

~~~mermaid
sequenceDiagram
    participant C as Client
    participant API as RankingAPI
    participant Facade as RankingFacade
    participant MVRepo as MVRankingRepository
    participant DB as ProductDB

    C->>+API: GET /api/v1/rankings?period=weekly&date=20251230
    API->>+Facade: getRanking(period=WEEKLY, date, page, size)

    Facade->>Facade: period == WEEKLY
    Facade->>Facade: 해당 주차 기간 계산
    Note over Facade: date가 속한 주의<br/>월요일~일요일 계산

    Facade->>+MVRepo: findByPeriod(startDate, endDate, pageable)
    MVRepo-->>-Facade: List<MVProductRank>

    alt 데이터 있음
        Facade->>+DB: findAllById(productIds)
        DB-->>-Facade: List<Product>
        Facade->>Facade: 랭킹 + 상품정보 조합
    else 데이터 없음
        Facade->>Facade: 빈 배열 반환
    end

    Facade-->>-API: RankingResponse
    API-->>-C: 200 OK
~~~

---

## 5. period 파라미터에 따른 분기 처리

~~~mermaid
flowchart TD
    A[GET /api/v1/rankings] --> B{period?}

    B -->|daily| C[Redis ZSET]
    B -->|weekly| D[mv_product_rank_weekly]
    B -->|monthly| E[mv_product_rank_monthly]

    C --> F[ZREVRANGE ranking:all:date]
    D --> G[SELECT ... WHERE period_start/end]
    E --> H[SELECT ... WHERE period_start/end]

    F --> I[상품 정보 조회]
    G --> I
    H --> I

    I --> J[Response 조합]
    J --> K[200 OK]
~~~

---

## 6. 배치 실행 트리거 옵션

~~~mermaid
flowchart LR
    subgraph "트리거 방식"
        A[수동 실행<br/>Admin API]
        B[스케줄러<br/>@Scheduled]
        C[외부 스케줄러<br/>Jenkins/Airflow]
    end

    A --> D[JobLauncher.run]
    B --> D
    C --> D

    D --> E[Spring Batch Job]
    E --> F[product_metrics]
    F --> G[MV 테이블]
~~~

---

## 7. 데이터 흐름 전체 아키텍처

~~~mermaid
flowchart TB
    subgraph "실시간 (9주차)"
        E1[이벤트 발생] --> K[Kafka]
        K --> C1[Consumer]
        C1 --> R[Redis ZSET<br/>일간 랭킹]
        C1 --> PM[product_metrics<br/>일간 집계]
    end

    subgraph "배치 (10주차)"
        PM --> B[Spring Batch]
        B --> MW[mv_product_rank_weekly]
        B --> MM[mv_product_rank_monthly]
    end

    subgraph "API"
        API[RankingAPI]
        API -->|daily| R
        API -->|weekly| MW
        API -->|monthly| MM
    end
~~~

---

## 8. Chunk-Oriented Processing 상세

~~~mermaid
sequenceDiagram
    participant Step
    participant Reader
    participant Processor
    participant Writer
    participant TX as Transaction

    loop until no more data
        TX->>TX: beginTransaction

        loop chunkSize times (100)
            Step->>+Reader: read()
            Reader-->>-Step: item or null
        end

        Step->>+Processor: process(items)
        Processor-->>-Step: processedItems

        Step->>+Writer: write(processedItems)
        Writer-->>-Step: done

        TX->>TX: commit
    end

    Note over Step,TX: 청크 단위 트랜잭션 관리<br/>실패 시 해당 청크만 롤백
~~~
