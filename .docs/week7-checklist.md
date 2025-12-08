# 📝 Round 7 Checklist

> **애플리케이션 이벤트 기반 트랜잭션 분리**
>
> 느슨하게, 유연하게, 확장 가능하게!

---

## 💻 Implementation Quest

> `ApplicationEvent`를 활용해 트랜잭션이나 기능을 무조건 분리하는 것이 아니라, **느슨해도 되는 경계**를 잘 구분 지어 분리하는 실습을 진행합니다.
> 적절한 판단 기준을 근거로 **주요 로직**과 **부가 로직**을 잘 구분지어 보세요.

### 🎯 목표

**Must-Have (이번 주에 무조건 가져가야 좋을 것 - 무조건 하세요)**
- Event vs Command 개념 이해
- Application Event 구조 이해 및 적용
- 트랜잭션 경계 설정 (@TransactionalEventListener)

**Nice-To-Have (부가적으로 가져가면 좋을 것 - 시간이 허락하면 꼭 해보세요)**
- 놓친 코드 리팩토링
- Redis 기반 실패 로깅 시스템
- 유저 행동 추적 시스템

### 📋 과제 정보

- **무조건 이벤트로 분리**가 아니라, *경계에 따라 동기/비동기를 나누는 감각*을 익힌다.
- 주문–결제 플로우에서 외부 I/O를 이벤트로 분리한다.
- 좋아요–집계 플로우에서 **eventual consistency**를 적용한다.
- 트랜잭션 결과와의 상관관계에 따라 적절한 리스너를 활용해 메인 트랜잭션과 느슨하게 연결한다.

---

## ✅ Checklist

### 🧾 주문 ↔ 결제

#### 기본 요구사항
- [ ] **이벤트 기반**으로 주문 트랜잭션과 쿠폰 사용 처리를 분리한다.
  - [ ] `OrderCreatedEvent` 정의 및 발행
  - [ ] `OrderEventHandler`에서 쿠폰 사용 처리
  - [ ] 쿠폰 처리 실패 시에도 주문은 유지됨

- [ ] **이벤트 기반**으로 결제 결과에 따른 주문 처리를 분리한다.
  - [ ] `PaymentRequestedEvent` 정의 및 발행
  - [ ] `PaymentSuccessEvent`, `PaymentFailedEvent` 정의
  - [ ] `PaymentEventHandler`에서 결제 요청 처리
  - [ ] 결제 결과에 따른 주문 상태 업데이트

- [ ] **이벤트 기반**으로 주문, 결제의 결과에 대한 데이터 플랫폼에 전송하는 후속처리를 진행한다.
  - [ ] 데이터 플랫폼 클라이언트 구현 (Mock 가능)
  - [ ] 비동기 이벤트 리스너로 전송 처리
  - [ ] 전송 실패 시 로깅 및 재시도 전략

#### 상세 구현 항목
- [ ] `ApplicationEventPublisher` 의존성 주입
- [ ] `@TransactionalEventListener(phase = AFTER_COMMIT)` 적용
- [ ] 이벤트 클래스에 `eventId` 포함 (멱등성 보장)
- [ ] 이벤트 핸들러 예외 처리 (부모 트랜잭션에 영향 없음)

### ❤️ 좋아요 ↔ 집계

#### 기본 요구사항
- [ ] **이벤트 기반**으로 좋아요 처리와 집계를 분리한다.
  - [ ] `LikeCreatedEvent`, `LikeDeletedEvent` 정의
  - [ ] `LikeEventHandler`에서 상품 좋아요 수 집계
  - [ ] 좋아요 저장과 집계를 별도 트랜잭션으로 분리

- [ ] 집계 로직의 성공/실패와 상관 없이, 좋아요 처리는 정상적으로 완료되어야 한다.
  - [ ] 집계 실패 시에도 좋아요는 유지됨 (Eventual Consistency)
  - [ ] 집계 실패 로그 기록
  - [ ] 재집계 메커니즘 고려

#### 상세 구현 항목
- [ ] `LikeService`에서 비관적 락 제거 (집계는 이벤트 핸들러에서)
- [ ] `LikeEventHandler`에서 비관적 락으로 집계
- [ ] 동기 이벤트 리스너 사용 (순서 보장)
- [ ] 캐시 무효화 전략 유지

### 📽️ 공통

#### 유저 행동 추적
- [ ] 이벤트 기반으로 `유저의 행동`에 대해 서버 레벨에서 로깅하고, 추적할 방법을 고민해 봅니다.
  - [ ] `UserActionEvent` 정의
  - [ ] 상품 조회, 클릭, 좋아요, 주문 등 행동별 이벤트 발행
  - [ ] `UserActionTracker` 구현 (비동기 로깅)
  - [ ] Redis 또는 별도 저장소에 행동 데이터 저장

#### 트랜잭션 설계
- [ ] 동작의 주체를 적절하게 분리하고, 트랜잭션 간의 연관관계를 고민해 봅니다.
  - [ ] 핵심 트랜잭션: 주문 생성, 재고 차감, 포인트 차감
  - [ ] 후속 트랜잭션: 쿠폰 사용, 결제 요청, 데이터 전송
  - [ ] 각 트랜잭션의 실패 영향 범위 정의
  - [ ] 보상 트랜잭션 전략 수립

---

## 🏗️ 아키텍처 체크리스트

### 비동기 설정
- [ ] `AsyncConfig` 작성
  - [ ] `@EnableAsync` 적용
  - [ ] `ThreadPoolTaskExecutor` 빈 정의
  - [ ] Core/Max Pool Size 설정 (5/10 권장)
  - [ ] Queue Capacity 설정 (100 권장)
  - [ ] RejectedExecutionHandler 설정 (CallerRunsPolicy)

### 이벤트 구조
- [ ] 도메인 이벤트 패키지 구조 (`domain/{domain}/event`)
- [ ] 이벤트 핸들러 패키지 구조 (`application/{domain}/event`)
- [ ] 이벤트 클래스는 불변 객체 (record 또는 final class)
- [ ] 이벤트 ID 포함 (멱등성 보장)

### 트랜잭션 경계
- [ ] `@TransactionalEventListener` 사용
- [ ] `phase = AFTER_COMMIT` 적용 (기본값)
- [ ] 필요 시 `AFTER_ROLLBACK`, `AFTER_COMPLETION` 활용
- [ ] 새로운 트랜잭션 필요 시 `@Transactional(propagation = REQUIRES_NEW)`

### 예외 처리
- [ ] 이벤트 핸들러 내 try-catch 구현
- [ ] 예외 로깅 및 모니터링
- [ ] 실패 이벤트 저장 (Redis 또는 DB)
- [ ] 재처리 메커니즘 고려

---

## 🧪 테스트 체크리스트

### 단위 테스트
- [ ] 이벤트 발행 테스트
  ```java
  verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
  ```
- [ ] 이벤트 핸들러 단위 테스트
  ```java
  orderEventHandler.handleOrderCreated(event);
  verify(couponService).useCoupon(event.userCouponId());
  ```

### 통합 테스트
- [ ] 주문 생성 → 이벤트 발행 → 쿠폰 사용 전체 흐름
- [ ] 좋아요 생성 → 이벤트 발행 → 집계 업데이트 전체 흐름
- [ ] 트랜잭션 커밋 후 이벤트 실행 확인
- [ ] 비동기 이벤트 완료 대기 (`await().atMost()`)

### 시나리오 테스트
- [ ] 이벤트 핸들러 실패 시 부모 트랜잭션에 영향 없음
- [ ] 이벤트 중복 발행 시 멱등성 보장
- [ ] 동시성 테스트 (좋아요 집계)
- [ ] 외부 시스템 장애 시 핵심 로직은 정상 처리

---

## ⚠️ 주의사항

### 트랜잭션 경계 설정
```java
// ❌ 잘못된 예: @EventListener (트랜잭션 커밋 전 실행)
@EventListener
public void handle(OrderCreatedEvent event) {
    // 부모 트랜잭션이 롤백되면 이 처리도 무의미해짐
}

// ✅ 올바른 예: @TransactionalEventListener (트랜잭션 커밋 후 실행)
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handle(OrderCreatedEvent event) {
    // 주문이 확실히 커밋된 후 실행
}
```

### 비동기 사용 시 순서 보장 불가
```java
// ⚠️ @Async 사용 시 순서 보장 안 됨
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handle(OrderCreatedEvent event) {
    // 여러 핸들러가 있으면 순서가 보장되지 않음
}

// ✅ 순서가 중요하면 동기 처리
@TransactionalEventListener(phase = AFTER_COMMIT)
@Order(1)  // 순서 지정
public void handleFirst(OrderCreatedEvent event) { }

@TransactionalEventListener(phase = AFTER_COMMIT)
@Order(2)
public void handleSecond(OrderCreatedEvent event) { }
```

### 데드락 주의
```java
// ⚠️ 비관적 락 + 비동기 = 데드락 위험
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handle(LikeCreatedEvent event) {
    Product product = productRepository.findByIdWithLock(...);  // 위험!
}

// ✅ 비관적 락 사용 시 동기 처리
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handle(LikeCreatedEvent event) {
    Product product = productRepository.findByIdWithLock(...);  // 안전
}
```

### 예외 처리 필수
```java
// ✅ 이벤트 핸들러는 항상 예외 처리
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handle(OrderCreatedEvent event) {
    try {
        externalApiCall();
    } catch (Exception e) {
        log.error("외부 API 호출 실패", e);
        // 재시도 큐에 넣거나 알림 발송
        failureQueue.add(event);
    }
}
```

---

## 📊 검증 기준

### 기능 검증
- [ ] 주문 생성 후 쿠폰이 사용되었는가?
- [ ] 결제 요청이 비동기로 처리되었는가?
- [ ] 좋아요 생성 후 집계가 업데이트되었는가?
- [ ] 집계 실패 시에도 좋아요는 유지되는가?

### 성능 검증
- [ ] 주문 생성 API 응답 시간이 개선되었는가?
- [ ] 외부 API 지연이 주문 생성에 영향을 주지 않는가?
- [ ] ThreadPool이 효율적으로 동작하는가?

### 안정성 검증
- [ ] 이벤트 핸들러 실패가 부모 트랜잭션에 영향을 주지 않는가?
- [ ] 시스템 재시작 후에도 이벤트가 누락되지 않는가? (필요 시 EventStore 구현)
- [ ] 동시성 상황에서도 집계가 정확한가?

---

## 📚 참고 문서

### Spring Documentation
- [Spring Application Events](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/application-events.html)
- [Spring @Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Transaction Event Listeners](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)

### 외부 자료
- [Baeldung - Event in Spring](https://www.baeldung.com/spring-events)
- [Event vs Command 차이](https://littlemobs.com/blog/difference-between-event-and-command/)
- [Eventual Consistency Pattern](https://martinfowler.com/articles/201701-event-driven.html)

### 프로젝트 내부 문서
- `.claude/round-7/README.md`: 핵심 개념 및 아키텍처
- `.claude/round-7/implementation-guide.md`: 단계별 구현 가이드
- `.claude/round-6/feedback-analysis.md`: Round 6 피드백 분석

---

## 🎯 다음 라운드 미리보기

Round 7에서는 **단일 JVM 내부**의 이벤트 기반 구조를 구현했습니다.

**Round 8 예상 주제:**
- Kafka를 활용한 마이크로서비스 간 이벤트 전파
- 이벤트 소싱 (Event Sourcing)
- CQRS (Command Query Responsibility Segregation)
- 분산 트랜잭션 관리 (Saga Pattern)

**Round 7에서 배운 내용이 Round 8의 기초가 됩니다!**
