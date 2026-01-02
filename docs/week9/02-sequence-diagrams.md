# 시퀀스 다이어그램

## 1. 상품 상세 조회 (순위 + View 이벤트)

~~~mermaid
sequenceDiagram
    participant C as Client
    participant API as ProductAPI
    participant DB as ProductDB
    participant Redis as Redis ZSET
    participant Kafka as Kafka
    participant Consumer as CatalogEventConsumer

    C->>+API: GET /api/v1/products/{id}
    API->>+DB: findById(productId)
    DB-->>-API: Product

    API->>+Redis: ZREVRANK ranking:all:{today} {productId}
    alt 랭킹에 존재
        Redis-->>API: rank (0-based)
        API->>API: rank + 1 (1-based 변환)
    else 랭킹에 없음
        Redis-->>API: null
    end
    Redis-->>-API: rank

    API->>Kafka: ProductViewedEvent 발행
    API-->>-C: 200 OK { ..., rank: 5 }

    Kafka->>+Consumer: consume
    Consumer->>Consumer: 멱등성 체크
    Consumer->>+Redis: ZINCRBY (weight: 0.1)
    Redis-->>-Consumer: OK
    Consumer-->>-Kafka: ack
~~~

---

## 2. Like 이벤트 흐름

~~~mermaid
sequenceDiagram
    participant C as Client
    participant API as LikeAPI
    participant Kafka as Kafka
    participant Consumer as CatalogEventConsumer
    participant Redis as Redis ZSET

    C->>+API: POST /api/v1/like/products/{id}
    API->>API: 좋아요 등록 (멱등)
    API->>Kafka: ProductLikedEvent 발행
    API-->>-C: 200 OK

    Kafka->>+Consumer: consume
    Consumer->>Consumer: 멱등성 체크
    alt liked == true
        Consumer->>+Redis: ZINCRBY (weight: 0.2)
        Redis-->>-Consumer: OK
    else liked == false
        Note over Consumer: ZSET 반영 안 함
    end
    Consumer-->>-Kafka: ack
~~~

---

## 3. Order 이벤트 흐름

~~~mermaid
sequenceDiagram
    participant API as OrderAPI
    participant Kafka as Kafka
    participant Consumer as RankingOrderConsumer
    participant Redis as Redis ZSET

    API->>API: order.confirm()
    API->>Kafka: OrderCompletedEvent 발행
    Note right of Kafka: items 포함 (productId, quantity)

    Kafka->>+Consumer: consume (@Payload)
    Consumer->>Consumer: 멱등성 체크
    loop 각 OrderItem
        Consumer->>+Redis: ZINCRBY (weight: 0.6 * quantity)
        Redis-->>-Consumer: OK
    end
    Consumer-->>-Kafka: ack
~~~

---

## 4. 랭킹 조회 API

~~~mermaid
sequenceDiagram
    participant C as Client
    participant API as RankingAPI
    participant Redis as Redis ZSET
    participant DB as ProductDB

    C->>+API: GET /api/v1/rankings?date=20251222&page=0&size=20
    API->>+Redis: ZREVRANGE ranking:all:{date}
    Redis-->>-API: List<productId, score>
    API->>+DB: 상품 정보 조회 (productIds)
    DB-->>-API: List<Product>
    API->>API: 랭킹 + 상품정보 조합
    API-->>-C: 200 OK (RankingResponse)
~~~

---

## 5. ZSET 키 전략

~~~mermaid
sequenceDiagram
    participant Service as RankingRedisService
    participant Redis as Redis ZSET

    Note over Service,Redis: KEY: ranking:all:{yyyyMMdd}

    Service->>+Redis: ZINCRBY ranking:all:20251222 {score} {productId}
    Redis-->>-Service: newScore

    alt 키가 새로 생성된 경우
        Service->>+Redis: EXPIRE ranking:all:20251222 172800
        Note over Redis: TTL: 2일 (키 생성 시 한 번만)
        Redis-->>-Service: OK
    end
~~~
