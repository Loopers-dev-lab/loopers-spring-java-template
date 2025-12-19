## 🧭 루프팩 BE L2 - Round 8

> 누가 기침을 하였는가 ~
>
>
> Kafka 기반으로 **서비스 경계 밖으로 이벤트를 발행**하고, 별도 Consumer 앱이 **후속 처리와 운영 책임**을 담당합니다.
>
> 메시지 전달 보장(At Least Once, Idempotency, DLQ)을 학습하며 **신뢰 가능한 이벤트 파이프라인**을 구현합니다.
>

<aside>
🎯

**Summary**

</aside>

지난 라운드에서는 **메세지에 대한 이해**와 애플리케이션 **이벤트를 통한 결합**을 느슨하게 만들어보았습니다. 하지만 애플리케이션 안에서만 이벤트가 머물러 있어, **서비스 경계를 넘는 확장성은 부족**했습니다.

이번 라운드에서는 이를 해결하기 위해 **Kafka 를 통해 이벤트를 외부로 발행**하고, **별도의 Consumer 앱이 후속 처리를 담당하는 구조**를 학습합니다.

<aside>
📌

**Keywords**

</aside>

- Kafka Producer & Consumer
- At Most Once / At Least Once / Exactly Once
- Idempotency & 멱등 처리
- Dead Letter Queue (DLQ)

<aside>
🧠

**Learning**

</aside>

## 📮 Kafka Overview

<aside>
❓

Kafka 는 흔히 **고성능 메시지 큐**라고 생각하지만, 근본적으로는 **분산 로그 저장소(Distributed Log Store)** 입니다.

Kafka 는 메세지가 흘러가는 통로보다는 `로그가 쌓이는 거대한 시스템` 에 좀 더 가깝습니다.

- 일반적인 Message Queue 와는 다르게 **디스크에 Log 를 지속적으로 append** 함
- Consumer 는 **각자의 Offset 을 기억하고 필요한 시점부터 읽는 것이** 가능함 (재처리 가능)
</aside>

### Kafka 의 주요 특징

- **고가용성** - Partition + Replica 구조로 브로커 장애 시에도 데이터 유실을 최소화
- **확장성** - Broker, Partion 의 수평 확장으로 처리량의 선형 증가
- **범용성** - 단순 메세징 뿐이 아닌 다음의 용도로도 사용
    - 1️⃣ 로그 수집
    - 2️⃣ 이벤트 소싱
    - 3️⃣ 스트리밍 처리의 기반

### Kafka Components

1. **Broker**
    - 카프카 서버 Unit
    - Producer 의 메세지를 받아 Offset 지정 후 디스크에 저장
    - Consumer 의 파티션 Read 에 응답해 디스크의 메세지 전송
    - `Cluster` 내에서 각 1개씩 존재하는 Role Broker
        - **Controller**

          다른 브로커를 모니터링하고 장애가 발생한 Broker 에 특정 토픽의 Leader 파티션이 존재한다면, 다른 브로커의 파티션 중 Leader 를 재분배하는 역할을 수행

        - **Coordinator**

          컨슈머 그룹을 모니터링하고 해당 그룹 내의 특정 컨슈머가 장애가 발생해 매칭된 파티션의 메세지를 Consume 할 수 없는 경우, 해당 파티션을 다른 컨슈머에게 매칭해주는 역할 수행 (`Rebalance`)

2. **Cluster**
    - 고가용성 (HA) 를 위해 여러 서버를 묶어 특정 서버의 장애를 극복할 수 있도록 구성
    - Broker 가 증가할 수록 메시지 수신, 전달 처리량을 분산시킬 수 있으므로 확장에 유리

      > 동작중인 다른 Broker 에 영향 없이 확장이 가능하므로, 트래픽 양의 증가에 따른 브로커 증설이 손쉽게 가능
>
3. **Topic & Partition**
    - `Topic` 은 메세지를 분류하는 기준이며 N 개의 `Partition` 으로 구성
        - 1개의 `Leader` 와 0..N 개의 `Follower` 파티션으로 구성해 가용성을 높일 수 있음
    - `Partition` 은 서로 다른 서버에 분산시킬 수 있기 때문에 수평 확장이 가능
    - 각 `Topic` 의 메세지 처리순서는 `Partition` 별로 관리됨
4. **Message**
    - 카프카에서 취급하는 데이터의 Unit **( ByteArray )**
5. **Producer & Consumer**
    - `Producer` - 메세지를 특정 Topic 에 생성
        - 저장될 파티션을 결정하기 위해 메세지의 Key 해시를 활용하며 Key 가 존재하지 않을 경우, 균형 제어를 위해 Round-Robin 방식으로 메세지를 기록
        - **Partitioner**

          메세지를 수신할 때, 토픽의 어떤 파티션에 저장될 지 결정하며 Producer 측에서 결정. 메세지에 key 가 없다면 특정  메세지에 key 가 존재한다면 key 의 해시값에 매칭되는 파티션에 데이터를 전송함으로써 항상 같은 파티션에 메세지를 적재해 **순서 보장** 이 가능하도록 처리할 수 있음.

    - `Consumer` - 1개 이상의 Topic 을 구독하며 메세지를 순서대로 읽음
        - 메세지를 읽을 때마다 파티션 별로 Offset 을 유지해 읽는 메세지의 위치를 추적할 수 있으며 오프셋은 두가지 종류가 존재
        - `CURRENT-OFFSET`

          컨슈머가 어디까지 처리했는지를 나타내는 offset 이며 메세지를 소비하는 컨슈머가 이를 기록하고 후에 장애가 발생했을 시에 그 뒤부터 이어 처리할 수 있도록 하며 장애 복구 상황을 위해 메세지가 처리된 이후에 반드시 커밋하여야 함

        - 만약 오류가 발생하거나 문제가 발생할 경우, 컨슈머 그룹 차원에서 `--reset-offsets`  옵션을 통해 실패한 시점으로 오프셋을 되돌릴 수 있음
6. **Consumer Group**
    - 메세지를 소비할 때, 토픽의 파티션을 매칭하는 그룹 단위이며 N 개의 컨슈머를 포함
    - 각 파티션은 그룹 내 하나의 컨슈머만 소비할 수 있음
    - 보통 소비 주체인 Application 단위로 Consumer Group 을 생성, 관리함
    - 같은 토픽에 대한 소비주체를 늘리고 싶다면, 별도의 컨슈머 그룹을 만들어 토픽을 구독

      ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/0e86a30d-2307-454b-9469-59a3c805b1b1/0453d381-6abe-4bfa-a4ab-5ef2e4a7ef98/Untitled.png)


    > 파티션의 개수가 그룹 내 컨슈머 개수보다 많다면 잉여 파티션의 경우 메세지가 소비될 수 없음을 의미함
    > 
    - **( 참고 )** 토픽의 Partition 개수와 Consumer 개수에 따른 소비
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/0e86a30d-2307-454b-9469-59a3c805b1b1/b6f3777a-44b7-4e85-a0b0-3b708ede0291/Untitled.png)
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/0e86a30d-2307-454b-9469-59a3c805b1b1/ba2876f1-df33-45fc-86b5-85eb462e17ba/Untitled.png)
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/0e86a30d-2307-454b-9469-59a3c805b1b1/74cc946f-e4b3-4f1b-9611-374cd526b410/Untitled.png)

7. **Rebalancing**
    - Consmuer Group 의 **가용성과 확장성**을 확보해주는 개념
    - 특정 컨슈머로부터 다른 컨슈머로 파티션의 소유권을 이전시키는 행위

      e.g. `Consumer Group` 내에 Consumer 가 추가될 경우, 특정 파티션의 소유권을 이전시키거나 오류가 생긴 Consumer 로부터 소유권을 회수해 다른 Consumer 에 배정함


    <aside>
    🚫 **( 주의 )** 리밸런싱 중에는 컨슈머가 메세지를 읽을 수 없음.
    
    </aside>
    
    `Rebalancing Case`
    
    1. Consumer Group 내에 새로운 Consumer 추가
    2. Consumer Group 내의 특정 Consumer 장애로 소비 중단
    3. Topic 내에 새로운 Partition 추가
8. **Replication**
    - Cluster 의 가용성을 보장하는 개념
    - 각 Partition 의 Replica 를 만들어 백업 및 장애 극복
        - Leader Replica

          각 파티션은 1개의 리더 Replica를 가진다. 모든 Producer, Consumer 요청은 리더를 통해 처리되게 하여 일관성을 보장한다.

        - Follower Replica

          각 파티션의 리더를 제외한 Replica 이며 단순히 리더의 메세지를 복제해 백업한다. 만일, 파티션의 리더가 중단되는 경우 팔로워 중 하나를 새로운 리더로 선출한다.

          > Leader 의 메세지가 동기화되지 않은 Replica 는 Leader 로 선출될 수 없다.
          >
            - `In-Sync Replica (ISR)`

              Leader 의 최신 메세지를 계속 요청하는 Follower

            - `Out-Sync Replica (OSR)`

              특정 기준에 의해 Leader 의 메세지를 백업하는 Follower


### Messaging Systems

| 구분 | **Redis (Pub/Sub)** | **RabbitMQ (AMQP)** | **Kafka (Distributed Log)** |
| --- | --- | --- | --- |
| **기반 모델** | Pub/Sub | 메시지 큐 (AMQP 프로토콜) | Pub/Sub + 분산 로그 |
| **메시지 저장** | ❌ 저장 안 함
(채널 자체에 보관 X) | ✅ 일시 저장
Queue 에 보관(서버/Queue 종료 시 삭제) | ✅ 저장
디스크(Log)에 영속 저장(보존 기간까지 유지) |
| **구독 방식** | 채널 기반, subscriber 없으면 메시지 소실 | Exchange → Queue 매핑 후 Consumer 수신 | Topic → Partition, Consumer Group 으로 분배 |
| **순서 보장** | 없음 | Queue 단위 순서 보장 | Partition 단위 순서 보장(Key 로 동일 Partition 강제 가능) |
| **확장성** | 제한적 (단일 서버 메모리 한계) | 브로커 클러스터 구성 가능하지만 Scale-out 제한적 | 고수준 확장성 (Broker/Partition 수평 확장) |
| **재처리 (Replay)** | ❌ 불가
(실시간 전달 전용) | ❌ 불가 
(소비하면 Queue 에서 제거) | ✅ 가능 
(Consumer Offset 조정으로 과거 이벤트 재소비 가능) |
| **메시지 유실** | Subscriber 없으면 유실 | Queue 보관 중 서버 장애 시 유실 가능 | 설정(`acks=all`, Replica)으로 내구성 보장 |
| **활용 사례** | 실시간 알림, 단순 신호 전달 | 트랜잭션 메시징, 업무 프로세스 큐잉 | 로그 수집, 이벤트 소싱, 스트리밍 처리, 대규모 이벤트 파이프라인 |

---

## 🚀 Kafka Essentials

> 카프카 활용의 핵심은 **메세지를 잃지 않고, 단 한번만 처리되게 보장할 수 있는가** 입니다.
**Producer → Broker → Consumer** 전 경로에 걸친 설정과 처리 방식의 조합은 메세지 전달 방식을 결정하는 주요 요소입니다.
다양한 운영 노하우가 필요하지만, 아래 내용들은 꼭 지켜질 수 있도록 해보세요.
>

### 📦 Message Delivery Semantics

**1️⃣ Producer → Broker**

- 🎯 **어떻게든 발행 (At Least Once)**
- `Producer` 는 네트워크 지연, 장애가 있어도 메세지를 최소 한 번은 `Broker` 에 기록되도록 보장해야 합니다.

### 📌 Producer 측 패턴: **Transactional Outbox**

- 도메인 데이터 변경(DB write)과 아웃박스 메시지 기록

  ➡ 두 작업을 **하나의 DB 트랜잭션**으로 묶음

- Outbox 테이블에 쌓인 메시지를 별도의 메시지 릴레이/데몬이 Broker로 전달
- 실패 시 계속 재시도 → 결과적으로 **At Least Once 발행 보장**

<aside>
💡  **Producer 예시**

- 주문 생성 시
    1. 주문 DB insert
    2. Outbox 테이블에 “OrderCreatedEvent” 기록

       ➡ 하나의 트랜잭션

- 백그라운드 워커가 Kafka로 publish
- 네트워크 불안해도 워커가 다시 재시도 → **`Broker에는 반드시 1번 이상 기록됨`** (최소 1회)
</aside>

**2️⃣ Consumer ← Broker**

- 🎯 **어떻게든 한 번만 처리 (At Most Once)**
- `Consumer` 는 같은 메세지가 여러 번 오더라도, 멱등하게 처리하여 최종 결과는 단 한번만 반영되도록 보장해야 합니다.

### 📌 Consumer 측 패턴: **Transactional Inbox / Idempotent Consumer**

- 메시지를 받을 때 **Inbox 테이블에 메시지 ID를 먼저 기록**
- 이미 처리된 메시지인지 검사
- 처리한 뒤 Inbox 상태를 완료로 업데이트

  → 메시지가 중복 오더라도 같은 ID는 무시

  → "어떻게 오든 단 한 번만 처리"


<aside>
💡 **Consumer 예시**

- Kafka 메시지 `orderId=12345` 처리
- Inbox 테이블에서 메시지 ID(또는 aggregateKey) 확인
    - 있음 → 이미 처리된 메시지 → skip
    - 없음 → Insert 후 정상 처리
- 이메일 발송/포인트 적립/주문 상태 변경 등에 안전

> 멱등성이 필요한 작업(마일리지 적립 같은 것)에 매우 중요
>
</aside>

---

### 🛡️ Idempotency (멱등성)

- **왜 필요한가?**
    - At Least Once 전략에 의해 중복 메시지가 발생할 수 있음
    - 중복이 오더라도 결과가 변하지 않아야 함
- **구현 전략**
    1. `eventId` PK 테이블 → 중복 메시지 무시
    2. `version` / `updatedAt` 비교 → 최신만 반영
    3. Upsert → Insert or Update : 중복 메시지에도 동일 결과 유지

---

### 🚨 Operation Tips

- **Retry & Backoff**: 일시 장애는 재시도로 복구, 즉시 무한재시도는 금물
- **DLQ (Dead Letter Queue)**: 반복 실패 메시지는 DLQ로 격리, 운영자가 후처리
- **Lag 모니터링**: Consumer가 얼마나 뒤쳐져 있는지 체크 (지연·병목 지표)
- **Partition 순서 보장**: Partition 단위로만 순서가 보장되므로 `partition.key=aggregateId` 설정 필수

---

## 🤔 오해

1️⃣ **Kafka는 MQ다**

- ❌ MQ는 메시지를 전달하고 삭제하는 방식
- ✅ Kafka는 데이터를 삭제하지 않고, 설정된 보존 기간 동안 **모든 메시지를 유지**

2️⃣ **Consumer가 메시지를 소유한다**

- ❌ MQ에서는 메시지를 소비하면 큐에서 제거됨
- ✅ Kafka에서는 메시지는 여전히 남아 있고, **Consumer Group 단위 Offset**만 이동

3️⃣ **Kafka는 순서를 보장하지 않는다**

- ❌ 전체 Topic 차원의 순서는 보장하지 않음
- ✅ **Partition 단위**로는 엄격하게 순서를 보장

4️⃣ **Kafka는 유실이 없다**

- ❌ 설정에 따라 다름 (acks=0/1이면 유실 가능)
- ✅ `acks=all` + `min.insync.replicas` 설정 시 **강력한 내구성 보장**

---

<aside>
📚

**References**

</aside>

| 구분 | 링크 |
| --- | --- |
| 🔍 Kafka | [Kafka docs](https://kafka.apache.org/documentation/) |
| ⚙ Spring Kafka | [Spring for Apache Kafka](https://spring.io/projects/spring-kafka) |
| 📖 우아콘2023 - 카프카 | [Kafka를 활용한 이벤트 기반 아키텍처 구축](https://www.youtube.com/watch?v=DY3sUeGu74M) |
| 🌟 라인 - 카프카 활용 | [LINE에서 Kafka를 사용하는 방법](https://engineering.linecorp.com/ko/blog/how-to-use-kafka-in-line-1) |

<aside>
🌟

**Next Week Preview**

</aside>

> **가장 인기 있는 상품을 보여주고 싶은데, 어떻게 알 수 있을까?**
>
>
>
> 이번 주차에는 카프카 메세지를 기반으로 안정적으로 이벤트를 발행하고, 처리하기 위한 방안들을 고민해 보았습니다. 이제 우리 **커머스 서비스**에 필요한 기본기는 많이 갖춘 것 같아요.
>
> 유저가 정말 좋아할 만한 상품은 뭘까요? 앞서 마련한 기반들을 이용해 우리는 실시간으로 상품 랭킹을 만들어 볼 거예요. 차주에는 **랭킹 파이프라인**을 통해 유저가 정말 좋아할 만한 상품들을 진열해 봅시다.
>