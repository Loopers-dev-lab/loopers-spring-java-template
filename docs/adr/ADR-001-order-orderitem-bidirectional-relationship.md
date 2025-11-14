# ADR-001: Order-OrderItem은 양방향 관계로 구현

## 결정사항
**Order-OrderItem 관계는 양방향 객체 참조(cascade + orphanRemoval)로 구현한다.**

```java
@Entity
public class Order {
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  public void addItem(OrderItem item) {
    this.items.add(item);
    item.assignOrder(this);
  }
}

@Entity
public class OrderItem {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ref_order_id", nullable = false)
  private Order order;
}
```

## 상황
Order-OrderItem은 다른 엔티티 관계와 다른 특성을 가짐:
- OrderItem은 Order 없이 존재할 수 없음
- 주문 생성 시 items와 totalAmount가 정합성을 유지해야 함
- Order가 OrderItem의 생명주기를 완전히 제어해야 함

## 선택 근거

### 1. DDD Aggregate Root 패턴
Order가 OrderItem의 생명주기를 완전히 제어:
- Order 생성 → OrderItem 생성
- Order 삭제 → OrderItem 자동 삭제
- OrderItem 독립 생성 불가

### 2. 트랜잭션 일관성 보장
```java
Order order = Order.create(userId, totalAmount);
order.addItem(OrderItem.of(productId, quantity, price));
orderRepository.save(order); // items도 함께 저장
```

### 3. 비즈니스 로직 캡슐화
Order 도메인이 items 관리 로직 포함:
- items 추가/제거 검증
- totalAmount 자동 재계산
- 양방향 관계 설정

## 페이징 처리 전략

양방향 관계에서 `JOIN FETCH` + 페이징은 메모리 페이징 문제가 발생한다.

### ✅ 해결 방안

**목록 조회**: DTO 프로젝션 (DB 레벨 페이징)

```java
@Query("SELECT new OrderListDto(o.id, o.orderedAt, o.totalAmount, o.status, SIZE(o.items)) " +
       "FROM Order o WHERE o.userId = :userId")
Page<OrderListDto> findOrderList(Long userId, Pageable pageable);
```

**상세 조회**: @EntityGraph (단건은 페이징 불필요)

```java
@EntityGraph(attributePaths = "items")
Optional<Order> findWithItemsById(Long orderId);
```

**원칙**: 목록은 DTO, 상세는 Entity

## 트레이드오프

### ✅ 장점
- 트랜잭션 일관성 보장 (cascade)
- 비즈니스 로직 캡슐화
- DDD Aggregate Root 패턴 구현
- 명확한 생명주기 관리

### ⚠️ 단점
- 목록/상세 쿼리 패턴 분리 필요
- DTO 프로젝션 필요
- 양방향 관계 설정 필수

## 참고
- 요구사항: `docs/design/01-requirements.md`
- ERD: `docs/design/04-erd.md`