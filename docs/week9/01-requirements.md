# 9주차 - 실시간 랭킹 시스템

## 1. 개요

### 1.1 목표
Redis ZSET을 이용한 실시간 랭킹 시스템 구축

### 1.2 배경
- 8주차에 Kafka Consumer로 `product_metrics` 테이블에 집계 정보 적재
- 이번 주차: 이벤트 기반으로 Redis ZSET에 랭킹 점수 반영
- API로 "오늘의 인기상품" 제공

---

## 2. 기능 요구사항

### 2.1 랭킹 조회 API

**유저 스토리**
- 사용자는 오늘의 인기 상품 목록을 조회할 수 있다
- 사용자는 특정 날짜의 랭킹을 조회할 수 있다
- 사용자는 페이지네이션으로 랭킹을 탐색할 수 있다

**기능 흐름**
- `GET /api/v1/rankings?date=yyyyMMdd&size=20&page=0`
- 요청 파라미터
  - date (Optional): 조회할 날짜 (기본값: 오늘)
  - size: 페이지당 상품 수 (기본값: 20)
  - page: 페이지 번호 (기본값: 0)
- 응답: 상품 ID뿐만 아니라 상품 정보가 Aggregation 되어 제공

**제약사항**
- 날짜 형식은 yyyyMMdd (예: 20241222)
- 랭킹 데이터가 없는 날짜는 빈 목록 반환
- 최대 2일 전까지 조회 가능 (TTL 제한)

---

### 2.2 상품 상세에 순위 추가

**유저 스토리**
- 사용자는 상품 상세 조회 시 해당 상품의 오늘 순위를 확인할 수 있다
- 순위에 없는 상품은 null로 표시된다

**기능 흐름**
- 기존 `GET /api/v1/products/{productId}` 응답에 `rank` 필드 추가
- 응답 예시:
```json
{
  "id": 1,
  "name": "상품명",
  "rank": 5
}
```

**제약사항**
- 순위는 오늘 날짜 기준으로 조회
- 랭킹에 없는 상품은 `rank: null` 반환
- 순위는 1부터 시작 (0-based 아님)

---

### 2.3 이벤트 -> ZSET 적재

**유저 스토리**
- 사용자의 행동(조회, 좋아요, 주문)이 실시간으로 랭킹에 반영된다

**이벤트별 처리**

| 이벤트 | 설명 | 토픽 |
|--------|------|------|
| ProductViewedEvent | 상품 조회 | catalog-events |
| ProductLikedEvent | 상품 좋아요 | catalog-events |
| OrderCompletedEvent | 주문 완료 | order-events |

**가중치 (Weight)**

| 이벤트 | Weight | Score | 계산식 |
|--------|--------|-------|--------|
| View (조회) | 0.1 | 1 | 0.1 * 1 = 0.1 |
| Like (좋아요) | 0.2 | 1 | 0.2 * 1 = 0.2 |
| Order (주문) | 0.6 | quantity | 0.6 * quantity |

**제약사항**
- 이벤트 중복 처리 방지 (멱등성 보장)
- 이벤트 발생 시점의 날짜로 ZSET에 적재
- 좋아요 취소(liked=false)는 점수에 반영하지 않음

---

## 3. 8주차 피드백 반영

| 피드백 | 적용 방안 |
|--------|----------|
| JSONConverter 사용 | 새로 만드는 Consumer(RankingOrderConsumer)에 @Payload 방식 적용 |
| Zero-Trust 관점 | Consumer에서 멱등성 처리 유지 (eventId 기반 중복 체크) |

---

## 4. 기술 스펙

### 4.1 Redis ZSET

| 항목 | 값 | 비고 |
|------|-----|------|
| KEY 형식 | `ranking:all:{yyyyMMdd}` | 일별 분리 |
| Member | productId (String) | ZSET member |
| Score | 가중치 합산 점수 | double |
| TTL | 2일 (172,800초) | 키 생성 시 한 번만 설정 |

### 4.2 Consumer 구성

| Consumer | 토픽 | Group ID | 처리 이벤트 |
|----------|------|----------|------------|
| CatalogEventConsumer | catalog-events | catalog-consumer | View, Like, StockDepleted |
| RankingOrderConsumer | order-events | ranking-consumer | OrderCompleted |

### 4.3 신규 이벤트

**ProductViewedEvent**
```java
public class ProductViewedEvent {
    private Long productId;
    private LocalDateTime occurredAt;
}
```

**OrderCompletedEvent 수정**
```java
public class OrderCompletedEvent {
    private Long orderId;
    private String userId;
    private long totalAmount;
    private long discountAmount;
    private long paymentAmount;
    private List<OrderItemInfo> items;  // 추가
    private LocalDateTime occurredAt;
}

public record OrderItemInfo(
    Long productId,
    int quantity,
    long price
) {}
```

---

## 5. 체크리스트

### 5.1 Ranking Consumer
- [ ] 랭킹 ZSET의 TTL, 키 전략을 적절하게 구성하였다
- [ ] 날짜별로 적재할 키를 계산하는 기능을 만들었다
- [ ] 이벤트가 발생한 후, ZSET에 점수가 적절하게 반영된다

### 5.2 Ranking API
- [ ] 랭킹 Page 조회 시 정상적으로 랭킹 정보가 반환된다
- [ ] 랭킹 Page 조회 시 단순히 상품 ID가 아닌 상품정보가 Aggregation 되어 제공된다
- [ ] 상품 상세 조회 시 해당 상품의 순위가 함께 반환된다 (순위에 없다면 null)

---

## 6. 선택 사항 (Nice-to-Have)

| 기능 | 설명 |
|------|------|
| 초실시간 랭킹 | 시간 단위 랭킹 (ranking:all:2024122114) |
| 콜드 스타트 해결 | 23:50에 Score Carry-Over로 다음날 랭킹판 미리 생성 |
| 실시간 Weight 조절 | 가중치를 동적으로 변경할 수 있는 구조 |
