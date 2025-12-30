## 💻 Implementation Quest

> 이번에는 카프카 기반의 **이벤트 파이프라인**을 구현합니다.
> 각 이벤트를 외부 시스템과 적절하게 주고 받을 수 있는 구조를 직접 체험해봅니다.
>

<aside>
🎯

**Must-Have (이번 주에 무조건 가져가야 좋을 것-**무조건****하세요**)**

- Kafka
- Event Pipeline
- **At Least Once** producer
- **At Most Once** consumer

**Nice-To-Have (부가적으로 가져가면 좋을 것-**시간이****허락하면****꼭****해보세요**)**

- 상품별 유저 이벤트 집계 테이블 만들기
- 이외의 이벤트 기반 처리를 고민해보기
- 메세지 단건 처리 → 배치 처리도 알아보기
- DLQ 도 고민해보기

</aside>

### 📋 과제 정보

**Kafka 기반 이벤트 파이프라인을 구현합니다.** (최소 기준)

- `commerce-api` → Kafka 의 방향으로 소통합니다.
- **Producer** 는 **At Least Once** 보장을 위해 이벤트를 반드시 발행합니다.
    - **Transactional Outbox Pattern** 을 구현해 보고, 동작을 확인해 봅니다.
- **Consumer** 는 이벤트를 수취해 아래 기능을 수행합니다.
    - **집계(Metrics)** : 좋아요 수 / 판매량 / 상세 페이지 조회 수 등을 `product_metrics` 테이블에 upsert

**토픽 설계** (예시)

- `catalog-events` (상품/재고/좋아요 이벤트, key=productId)
- `order-events` (주문/결제 이벤트, key=orderId)
- *각 세부 이벤트 별로 분리하고 싶다면, 분리해도 좋습니다.*

**Producer, Consumer 필수 처리**

- **Producer**
    - acks=all, idempotence=true 설정
- **Consumer**
    - **manual Ack** 처리
    - `event_handled(event_id PK)` (DB or Redis) 기반의 멱등 처리
    - `version` 또는 `updated_at` 기준으로 최신 이벤트만 반영

> *왜 이벤트 핸들링 테이블과 로그 테이블을 분리하는 걸까? 에 대해 고민하고 리뷰 포인트에 작성해주세요*
>

---

## ✅ Checklist

### 🎾 Producer

- [x]  도메인(애플리케이션) 이벤트 설계
- [x]  Producer 앱에서 도메인 이벤트 발행 (catalog-events, order-events, 등)
- [x]  **PartitionKey** 기반의 이벤트 순서 보장
- [x]  메세지 발행이 실패했을 경우에 대해 고민해보기

> Outbox → Kafka 퍼블리셔에서 `kafkaTemplate.send(...).get()` 호출이 예외를 던지면 해당 event_outbox 레코드를 `markFailed`로 PENDING 상태로 되돌리고 `last_error`, `attempt_count`를 업데이트합니다. 이후 스케줄러가 다시 조회해 재시도하고, 장애가 누적되면 last_error를 기반으로 원인 분석/알람을 할 수 있도록 설계했습니다.

### ⚾ Consumer

- [x]  Consumer 가 Metrics 집계 처리
- [x]  `event_handled` 테이블을 통한 멱등 처리 구현
- [x]  재고 소진 시 상품 캐시 갱신
- [x]  중복 메세지 재전송 테스트 → 최종 결과가 한 번만 반영되는지 확인

> `catalog-events`, `order-events` 모두 event_handled 테이블을 조회 후 처리하기 때문에 동일 event_id를 재전송해도 두 번째부터는 skip 로그만 남고 metrics/caches는 변하지 않습니다. (로컬에서 동일 페이로드를 두 번 publish해 manual 확인)
