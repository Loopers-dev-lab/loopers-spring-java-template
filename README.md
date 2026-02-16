# E-Commerce Platform with Event-Driven Architecture

이벤트 기반 아키텍처를 활용한 프로덕션급 이커머스 플랫폼입니다. 트랜잭션 처리와 실시간 분석을 분리하여 일관성과 확장성을 동시에 확보했습니다.

## 📋 목차

- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [아키텍처](#아키텍처)
- [핵심 설계 결정](#핵심-설계-결정)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [프로젝트 구조](#프로젝트-구조)

---

## 프로젝트 개요

### 시스템 구성

```
┌─────────────────┐         ┌──────────────────────┐
│  commerce-api   │────────>│  commerce-collector  │
│ (트랜잭션 처리)   │  Kafka  │   (메트릭 수집/집계)   │
└─────────────────┘         └──────────────────────┘
        │                              │
        │                              │
        ▼                              ▼
   MySQL (OLTP)                 MySQL (OLAP)
   Redis Cache                  Spring Batch
```

### 주요 특징

-  **높은 안정성**: Circuit Breaker + Retry 패턴으로 외부 시스템 장애 격리
-  **확장 가능**: CQRS 패턴으로 읽기/쓰기 독립적 스케일링
-  **데이터 정합성**: 멱등성 보장 및 보상 트랜잭션으로 일관성 유지
-  **고성능**: Redis 캐싱, 배치 처리, 인덱스 최적화

---

## 주요 기능

### commerce-api (트랜잭션 서비스)

- **주문 관리**: 주문 생성, 조회, 취소
- **결제 처리**:
  - 포인트 결제 (동기)
  - 카드 결제 (비동기, PG 연동)
  - 결제 실패 시 자동 보상 트랜잭션
- **재고 관리**: 실시간 재고 차감 및 복구
- **쿠폰 시스템**: 할인 정책 적용 (금액/비율 할인)
- **사용자 포인트**: 적립 및 차감

### commerce-collector (분석 서비스)

- **실시간 메트릭 수집**: Kafka 이벤트 소비 및 배치 처리
- **다층 집계**:
  - 일별 집계 (자정 실행)
  - 주간 집계 (Spring Batch)
  - 월간 집계 (Spring Batch)
- **랭킹 API**: 주간/월간 인기 상품 랭킹 제공
- **이벤트 멱등성**: EventHandled 테이블로 중복 처리 방지

---

## 아키텍처

### 계층형 아키텍처 (DDD)

```
┌─────────────────────────────────────────┐
│  Presentation Layer (interfaces/api)   │  ← Controllers, DTOs
├─────────────────────────────────────────┤
│  Application Layer (application)       │  ← Facades, Orchestration
├─────────────────────────────────────────┤
│  Domain Layer (domain)                  │  ← Entities, Services, VOs
├─────────────────────────────────────────┤
│  Infrastructure Layer (infrastructure) │  ← JPA, Feign, Kafka
└─────────────────────────────────────────┘

의존성 방향: Presentation → Application → Domain ← Infrastructure
```

**핵심 원칙**:
- 각 계층은 하위 계층만 의존
- Domain은 외부 의존성 없음 (순수 비즈니스 로직)
- Infrastructure가 Domain 인터페이스 구현 (Dependency Inversion)

### 도메인 주도 설계 (DDD)

**Aggregate 경계**:
- Order Aggregate: Order + Payment (동일 트랜잭션 보장)
- Product Aggregate: Product + Stock
- User Aggregate: User + Point

**Value Objects**:
- `Money`: 금액 계산 로직 캡슐화, 불변성 보장
- `Stock`: 재고 차감/복구 로직, 음수 방지

**Repository 패턴**:
```java
// Domain Layer
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(String paymentId);
}

// Infrastructure Layer
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository jpaRepository;
    // JPA 구현체에 위임
}
```

---

## 핵심 설계 결정

### 1. 트랜잭션 분리 전략

**문제**: PG 호출 실패 시 결제 기록이 사라져 복구 불가능

**해결**: `@Transactional(propagation = REQUIRES_NEW)` 사용

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Payment processCardPayment(Order order) {
    Payment payment = Payment.createPending(order);
    paymentRepository.save(payment);  // 독립 트랜잭션으로 확실히 저장

    try {
        pgClient.requestPayment(payment);
    } catch (Exception e) {
        // PG 실패해도 Payment는 이미 저장됨 → 스케줄러가 재처리
    }
}
```

**트레이드오프**:
-  결제 기록 누락 방지, 자동 복구 가능
-  트랜잭션 관리 복잡도 증가, 즉시 실패 불가

### 2. Circuit Breaker + Retry 패턴

**문제**: PG 서비스 장애가 전체 시스템에 전파

**해결**: Resilience4j 적용

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50        # 50% 실패율에서 열림
        slidingWindowSize: 10           # 최근 10회 기준
  retry:
    configs:
      default:
        maxAttempts: 3                  # 3회 재시도
        waitDuration: 1s                # 지수 백오프
```

**트레이드오프**:
-  장애 전파 차단, 스레드 풀 보호, 자동 복구
-  Circuit 열린 동안 모든 요청 실패, 사용자 경험 저하 가능

### 3. CQRS-lite: 읽기/쓰기 분리

**문제**: 실시간 메트릭 조회가 트랜잭션 쓰기와 경합

**해결**: Kafka 기반 이벤트 분리

```
commerce-api                    commerce-collector
    │                                   │
    ├─ 주문 생성                        │
    ├─ OrderCreated 발행 ──────>      │
    │                              ├─ 배치 소비 (100개씩)
    │                              ├─ 메트릭 업데이트
    │                              └─ EventHandled 기록
```

**배치 처리로 N+1 방지**:
```java
// 1. 한 번의 쿼리로 처리된 이벤트 조회
Set<String> handledEventIds =
    eventHandledRepository.findAlreadyHandled(eventIds);

// 2. 미처리 이벤트만 필터링
List<Event> unprocessed = events.stream()
    .filter(e -> !handledEventIds.contains(e.getId()))
    .collect(toList());
```

**트레이드오프**:
-  트랜잭션 성능 영향 없음, 독립적 스케일링
-  데이터 지연 (수 초), Kafka 운영 복잡도 증가

### 4. Spring Batch 다층 집계

**문제**: 주간/월간 랭킹 조회 시 일별 데이터 풀스캔

**해결**: 사전 집계 테이블 구축

```
ProductMetrics (실시간)
    ↓ 이벤트마다 업데이트
ProductMetricsDaily (자정 집계)
    ↓ 주간 배치 (매주 월요일)
ProductMetricsWeekly
    ↓ 월간 배치 (매월 1일)
ProductMetricsMonthly
```

**Spring Batch 구성**:
```java
@Bean
public Step aggregateWeeklyMetricsStep() {
    return stepBuilder
        .<DailyMetric, WeeklyMetric>chunk(100)  // 100개씩 처리
        .reader(dailyMetricsReader())
        .processor(weeklyAggregationProcessor())
        .writer(weeklyMetricsWriter())
        .build();
}
```

**성능 개선**:
- 주간 랭킹: O(n*7) → O(1)
- 월간 랭킹: 약 30배 쿼리 감소

**트레이드오프**:
-  조회 성능 밀리초 단위, DB 부하 최소화
-  저장소 비용 3배, 실시간성 부족

### 5. 멱등성 보장

**문제**: Kafka 중복 메시지로 이중 카운팅

**해결**: EventHandled 테이블 + 원자적 처리

```java
@Transactional
public void handleEvents(List<Event> events) {
    // 1. 중복 체크
    Set<String> handledIds = findAlreadyHandled(events);
    List<Event> unprocessed = filterUnprocessed(events, handledIds);

    // 2. 메트릭 업데이트
    updateMetrics(unprocessed);

    // 3. 처리 완료 기록 (동일 트랜잭션)
    markAsHandled(unprocessed);
}
```

**트레이드오프**:
-  Exactly-once 처리, 중복 방지, 감사 로그
-  저장소 증가, 체크 쿼리 오버헤드

### 6. 보상 트랜잭션

**문제**: 결제 실패 시 재고/포인트/쿠폰 원복 필요

**해결**: 멱등성 보장 보상 로직

```java
public void handleFailedPayment(String orderId) {
    Order order = orderRepository.findById(orderId);

    // 멱등성: 이미 취소됐으면 스킵
    if (order.getStatus() == CANCELED) return;

    // 보상 실행
    order.cancelOrder();              // 주문 취소
    inventoryService.restoreStock();  // 재고 복구
    pointService.refund();            // 포인트 환불
    couponService.restore();          // 쿠폰 복구
}
```

**트레이드오프**:
-  데이터 일관성 유지, 안전한 재시도
-  로직 복잡도 증가, 테스트 시나리오 복잡

---

## 기술 스택

### Backend
- **Java 21**: Virtual Threads, Record 등 최신 기능 활용
- **Spring Boot 3.x**: 성숙한 생태계, Observability 기본 지원
- **Spring Data JPA**: Repository 패턴, Query DSL
- **Spring Batch**: 대용량 배치 처리, 재시작 기능

### Infrastructure
- **MySQL 8.0**: 트랜잭션 데이터베이스
- **Redis 7**: L2 캐시, 세션 저장소
- **Kafka**: 이벤트 스트리밍, CQRS 구현
- **Docker Compose**: 로컬 개발 환경

### Resilience & Monitoring
- **Resilience4j**: Circuit Breaker, Retry, Fallback
- **Spring Boot Actuator**: Health Check, Metrics
- **Prometheus**: 메트릭 수집
- **Grafana**: 모니터링 대시보드

### Testing
- **JUnit 5**: 테스트 프레임워크
- **Mockito**: Mock 객체 생성
- **TestContainers**: 실제 DB/Kafka 환경 통합 테스트
- **Embedded Kafka**: Kafka Consumer/Producer 테스트

---

## 시작하기

### 사전 요구사항

- Java 21+
- Docker & Docker Compose
- Gradle 8.x

### 1. 인프라 실행

```bash
# MySQL, Redis, Kafka 실행
docker-compose -f ./docker/infra-compose.yml up -d

# 모니터링 스택 실행 (선택)
docker-compose -f ./docker/monitoring-compose.yml up -d
```

### 2. 애플리케이션 실행

```bash
# commerce-api 실행 (포트 8080)
./gradlew :apps:commerce-api:bootRun

# commerce-collector 실행 (포트 8081)
./gradlew :apps:commerce-collector:bootRun

# PG 시뮬레이터 실행 (포트 8082)
./gradlew :apps:pg-simulator:bootRun
```

### 3. 모니터링

- **Grafana**: http://localhost:3000 (admin/admin)
- **Actuator**: http://localhost:8080/actuator
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :apps:commerce-api:test

# 단일 테스트 클래스
./gradlew :apps:commerce-api:test --tests "PaymentFacadeTest"

# 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

---

## 프로젝트 구조

### Multi-Module 구성

```
Root
├── apps/              # 실행 가능한 Spring Boot 애플리케이션
│   ├── commerce-api       # 메인 API 서버
│   ├── commerce-collector # 메트릭 수집 서버
│   └── pg-simulator       # PG 시뮬레이터
├── modules/           # 재사용 가능한 설정 모듈
│   ├── jpa               # JPA 설정
│   ├── redis             # Redis 설정
│   └── kafka             # Kafka 설정
└── supports/          # 부가 기능 모듈
    ├── jackson           # JSON 직렬화
    ├── logging           # 로깅 설정
    └── monitoring        # 메트릭 설정
```

**중요**: `apps/*` 모듈만 실행 가능한 JAR 생성, 나머지는 라이브러리

### commerce-api 상세 구조

```
src/main/java/com/loopers/
├── interfaces/api/        # Presentation Layer
│   ├── order/
│   ├── payment/
│   └── product/
├── application/           # Application Layer
│   ├── order/            # OrderFacade, OrderInfo
│   ├── payment/          # PaymentFacade, PaymentInfo
│   └── product/          # ProductFacade, ProductInfo
├── domain/               # Domain Layer
│   ├── order/            # Order, OrderRepository
│   ├── payment/          # Payment, PaymentRepository
│   └── product/          # Product, ProductRepository
├── infrastructure/       # Infrastructure Layer
│   ├── order/            # OrderJpaRepository, OrderRepositoryImpl
│   ├── payment/          # PaymentJpaRepository, PgClient
│   └── product/          # ProductJpaRepository
├── config/               # 설정 클래스
└── support/error/        # 예외 처리
```

---

## 성능 최적화

### 1. 인덱스 전략

```sql
-- 일별 메트릭 조회 최적화
CREATE INDEX idx_product_metrics_daily_date_product
ON product_metrics_daily(date, product_id);

-- 주문 조회 최적화
CREATE INDEX idx_order_user_created
ON orders(user_id, created_at DESC);

-- 이벤트 중복 체크 최적화
CREATE UNIQUE INDEX idx_event_handled_event_id
ON event_handled(event_id);
```

### 2. 배치 처리

- 이벤트 소비: 100개씩 배치 처리로 10배 처리량 향상
- Spring Batch: 청크 크기 100으로 메모리 효율성 확보

### 3. 캐시 전략

```java
@Cacheable(value = "product", key = "#productId")
public ProductInfo getProduct(Long productId) {
    // Redis 캐시 히트 시 DB 조회 생략
}

@CacheEvict(value = "product", key = "#productId")
public void updateProduct(Long productId, ProductInfo info) {
    // 업데이트 시 캐시 무효화
}
```

### 4. 커넥션 풀 튜닝

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # 동시 요청 처리량 기반
      minimum-idle: 10
      connection-timeout: 3000
      idle-timeout: 600000
```

---

## 배운 점

### 트랜잭션 경계 설계의 중요성
처음엔 전체 결제 흐름에 단일 트랜잭션을 사용했으나, PG 실패 시 결제 기록까지 롤백되어 복구가 불가능했습니다. `REQUIRES_NEW`를 통한 트랜잭션 분리로 안정성을 크게 향상시켰습니다.

### 최종 일관성 구현의 복잡성
CQRS 패턴은 개념적으로 간단해 보이지만, 실제 구현 시 이벤트 처리 실패, 재처리, 중복 처리 등 수많은 예외 상황을 고려해야 했습니다. EventHandled 테이블과 DLQ가 필수 인프라임을 깨달았습니다.

### 멱등성은 선택이 아닌 필수
초기 테스트에서 중복 Kafka 메시지로 인한 이중 환불 문제를 경험했습니다. 이후 모든 외부 호출, 이벤트 컨슈머, 보상 트랜잭션에 멱등성 설계를 필수로 적용했습니다.

### 메트릭과 모니터링의 차이
Spring Boot Actuator 메트릭(기술 지표)만으로는 부족하고, 비즈니스 메트릭(주문 수, 결제 성공률 등)이 별도로 필요함을 깨달았습니다. 이것이 ProductMetrics 도메인 엔티티 설계의 계기가 되었습니다.

---

## 향후 개선 계획

1. **Event Sourcing**: 현재 방식은 결제 상태 히스토리를 잃어버림
2. **분산 추적**: Spring Cloud Sleuth → OpenTelemetry로 전환
3. **Rate Limiting**: API 레벨 스로틀링 추가
4. **캐시 워밍**: Redis 콜드 스타트 문제 해결

---

## 라이선스

이 프로젝트는 학습 목적으로 작성되었습니다.
