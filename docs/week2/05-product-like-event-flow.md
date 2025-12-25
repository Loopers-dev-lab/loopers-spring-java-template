# Product Like Event Flow

상품 좋아요 이벤트 발행부터 집계 처리까지의 전체 플로우

## 전체 아키텍처

```mermaid
graph LR
    A[commerce-api] -->|Kafka Event| B[Kafka Topic: product-like]
    B -->|Consume| C[commerce-collector]

    subgraph commerce-api
        A1[ProductLikeService]
        A2[OutboxEventService]
        A1 --> A2
    end

    subgraph commerce-collector
        C1[ProductLikeEventConsumer]
        C2[ProductLikeEventHandler]
        C3[ProductMetricsService]
        C1 --> C2
        C2 --> C3
    end
```

## 이벤트 플로우

```mermaid
sequenceDiagram
    actor User
    participant API as ProductLikeService
    participant Outbox as OutboxEventService
    participant DB as Database
    participant Kafka as Kafka Topic
    participant Consumer as ProductLikeEventConsumer
    participant Handler as ProductLikeEventHandler

    User->>API: 좋아요 추가/취소

    Note over API: @Transactional
    API->>DB: ProductLike 저장/삭제
    API->>Outbox: createOutboxEvent()
    Outbox->>DB: OutboxEvent 저장
    API-->>User: 200 OK

    Note over Outbox,Kafka: OutboxEventPublisher (스케줄러)
    Outbox->>Kafka: 이벤트 발행

    Kafka->>Consumer: 이벤트 수신
    Consumer->>Handler: handleLikeAdded/Removed()

    Note over Handler: @Transactional
    Handler->>Handler: 멱등성 체크 (EventHandled)
    Handler->>DB: ProductMetrics 업데이트
    Handler->>DB: EventHandled 저장

    Consumer->>Kafka: acknowledgment.acknowledge()
```

## 핵심 설계 포인트

### 1. Outbox 패턴
DB 트랜잭션과 Kafka 이벤트 발행의 원자성 보장
- OutboxEvent를 DB에 저장 후 스케줄러가 Kafka로 발행
- 좋아요 저장과 이벤트 발행이 모두 성공하거나 모두 실패

### 2. 멱등성 보장
EventHandled 테이블로 중복 처리 방지
- Kafka 메시지 중복 수신 시에도 한 번만 처리
- At-Least-Once 전송에서도 Exactly-Once 처리 보장

### 3. 이벤트 발행 실패 시 로그만 기록
- 이벤트 발행 실패가 사용자 경험에 영향 주지 않도록
- 좋아요 기능의 가용성 향상
- Eventual Consistency 허용

### 4. 수동 커밋
- 이벤트 처리 완료 후에만 커밋
- 처리 실패 시 재처리 가능