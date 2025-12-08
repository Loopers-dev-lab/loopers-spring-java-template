# Resilience4j 기반 PG 통신 안정성 강화

## 📋 개요

PG(Payment Gateway) 모듈과의 통신 안정성을 확보하기 위해 Resilience4j를 도입하여 Circuit Breaker, Retry, Fallback 패턴을 구현했습니다.

### 목적

- **장애 전파 방지**: PG 장애 시 전체 시스템으로 장애가 전파되는 것을 차단
- **빠른 실패(Fast Fail)**: 장애 감지 시 즉시 Fallback으로 전환하여 응답 시간 단축
- **자동 복구**: 장애 해소 시 자동으로 정상 상태로 복구
- **재시도 전략**: 일시적 장애에 대한 스마트한 재시도 로직

---

## 🏗️ 아키텍처

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│         PaymentFacade                   │
│  (Application Layer)                    │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│      PaymentProcessor                   │
│  (Domain Layer)                         │
│  @CircuitBreaker(name = "pgClient")     │
│  @Retry(name = "pgClient")              │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│         PgClient (Feign)                │
│  (Infrastructure Layer)                 │
│  + PgClientFallbackFactory              │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│       PG Simulator                      │
│  (External System)                      │
└─────────────────────────────────────────┘
```

---

## 🔧 구현 상세

### 1. Circuit Breaker 설정

#### 목적
PG 장애 시 빠른 실패로 시스템 보호

#### 설정 (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pgClient:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10              # 최근 10번 호출 기준
        minimum-number-of-calls: 5           # 최소 5번 호출 후 실패율 계산
        failure-rate-threshold: 50           # 실패율 50% 초과 시 OPEN
        wait-duration-in-open-state: 60s     # OPEN 상태 60초 유지
        permitted-number-of-calls-in-half-open-state: 3  # HALF_OPEN에서 3번 테스트
        automatic-transition-from-open-to-half-open-enabled: true
        
        # 실패로 간주할 예외
        record-exceptions:
          - feign.FeignException.ServiceUnavailable
          - feign.FeignException.InternalServerError
          - java.util.concurrent.TimeoutException
          - feign.RetryableException
```

#### 동작 방식

| 상태 | 설명 | 조건 |
|------|------|------|
| **CLOSED** (정상) | 모든 요청 통과 | 실패율 < 50% |
| **OPEN** (차단) | 모든 요청 차단 → Fallback 실행 | 실패율 ≥ 50% |
| **HALF_OPEN** (복구 시도) | 3번 테스트 호출 → 성공 시 CLOSED로 복구 | OPEN 상태 60초 경과 |

#### 상태 전환 다이어그램

```
     실패율 50% 초과
CLOSED ──────────────→ OPEN
  ↑                      │
  │                      │ 60초 경과
  │                      ↓
  └──────────────── HALF_OPEN
     3번 연속 성공
```

---

### 2. Retry 설정

#### 목적
일시적 장애에 대한 자동 재시도

#### 설정 (`application.yml`)

```yaml
resilience4j:
  retry:
    instances:
      pgClient:
        max-attempts: 3          # 최대 3회 시도 (원본 1회 + 재시도 2회)
        wait-duration: 1s        # 재시도 간격 1초
        retry-exceptions:
          - feign.FeignException.ServiceUnavailable  # 503
          - feign.FeignException.InternalServerError # 500
          - java.lang.RuntimeException               # PG API FAIL 응답
```

#### 재시도 전략

| 상황 | 재시도 여부 | 이유 |
|------|------------|------|
| `meta.result = "FAIL"` | ✅ 재시도 O | 일시적 PG 장애 |
| 타임아웃 (500ms 초과) | ❌ 재시도 X | 재시도해도 실패 가능성 높음 |
| 503 Service Unavailable | ✅ 재시도 O | 일시적 서버 과부하 |
| 500 Internal Server Error | ✅ 재시도 O | 일시적 서버 오류 |

#### 재시도 흐름 예시

```
호출 1: FAIL → 1초 대기 → 재시도
호출 2: FAIL → 1초 대기 → 재시도
호출 3: FAIL → 최종 실패 → Fallback 실행
```

---

### 3. Feign Client 타임아웃 설정

#### 목적
빠른 실패로 응답 시간 보장

#### 설정 (`application.yml`)

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          pgClient:
            connect-timeout: 500  # 연결 타임아웃 500ms
            read-timeout: 500     # 읽기 타임아웃 500ms
            logger-level: full
```

#### 타임아웃 발생 시 처리 흐름

```
1. PG 호출 (500ms 초과)
   ↓
2. feign.RetryableException 발생
   ↓
3. Circuit Breaker가 실패로 기록
   ↓
4. Retry 하지 않음 (타임아웃은 재시도 제외)
   ↓
5. Fallback 실행
```

---

### 4. Fallback 구현

#### 목적
장애 시 안전한 대체 응답 제공


**PgClientFallbackFactory.java**

**PaymentFacade.java**


---

## 📊 동작 시나리오

### 시나리오 1: 정상 처리

```
요청 → PG 호출 → 성공 (200ms) → PENDING 상태 저장
```

**결과**: 정상적으로 결제 처리

---

### 시나리오 2: 일시적 장애 (Retry 성공)

```
요청 → PG 호출 → FAIL (meta.result = "FAIL")
     ↓
     1초 대기
     ↓
     재시도 → 성공 → PENDING 상태 저장
```

**결과**: 재시도로 복구, 사용자는 지연을 느끼지 못함

---

### 시나리오 3: 타임아웃 (Fallback)

```
요청 → PG 호출 → 타임아웃 (500ms 초과)
     ↓
     Fallback 실행
     ↓
     FAILED 상태 저장
```

**결과**: 빠른 실패, 사용자에게 즉시 오류 응답

---

### 시나리오 4: Circuit OPEN (빠른 실패)

```
[실패율 50% 초과 감지]
요청 → Circuit OPEN → PG 호출 차단
     ↓
     즉시 Fallback 실행
     ↓
     FAILED 상태 저장
     
[60초 후 자동 복구 시도]
요청 → Circuit HALF_OPEN → 3번 테스트 호출
     ↓
     성공 시 CLOSED로 복구
```

**결과**: 장애 전파 차단, 시스템 보호

---

## 🧪 테스트 전략

### 1. 통합 테스트 (Mock 기반)

**파일**: `PaymentCircuitIntegrationTest.java`

#### 테스트 구조

```java
@SpringBootTest
@DisplayName("Circuit Breaker 통합 테스트")
class PaymentCircuitIntegrationTest {
    
    @MockitoBean
    private PgClient pgClient;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
}
```

#### 주요 테스트 케이스

##### 1) 최소 호출 횟수 검증

```java
@Test
@DisplayName("minimumNumberOfCalls(5) 미만에서는 실패율과 관계없이 Circuit이 CLOSED 유지")
void minimumNumberOfCalls_미만에서는_Circuit이_CLOSED_유지() {
    // Given: PG 호출 시 항상 실패
    given(pgClient.requestPayment(...)).willThrow(createPgException());
    
    // When: 4회 연속 실패
    for (int i = 0; i < 4; i++) {
        paymentFacade.processPayment(command);
    }
    
    // Then: Circuit 상태 = CLOSED (최소 호출 수 미만)
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
}
```

##### 2) 실패율 임계값 검증

```java
@Test
@DisplayName("5회 호출 중 3회 실패 시 실패율 60%로 Circuit이 OPEN으로 전환")
void 실패율_60퍼센트_초과_시_Circuit이_OPEN_전환() {
    // Given
    given(pgClient.requestPayment(...))
        .willReturn(success)  // 1회 성공
        .willReturn(success)  // 2회 성공
        .willThrow(exception) // 3회 실패
        .willThrow(exception) // 4회 실패
        .willThrow(exception); // 5회 실패
    
    // When: 5회 호출
    for (int i = 0; i < 5; i++) {
        paymentFacade.processPayment(command);
    }
    
    // Then: Circuit 상태 = OPEN (실패율 60% > 50%)
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(metrics.getFailureRate()).isEqualTo(60.0f);
}
```

##### 3) HALF_OPEN 상태 전환 및 복구

```java
@Test
@DisplayName("실패율 초과로 OPEN → 대기 시간 후 HALF_OPEN → 성공 시 CLOSED로 복구")
void 실패율_초과로_OPEN_상태_후_대기시간_경과하면_HALF_OPEN으로_전환되고_성공_시_CLOSED로_복구() {
    // Given: Circuit을 OPEN 상태로 만들기
    // ... (5회 호출, 실패율 60%)
    
    // When: HALF_OPEN으로 전환
    circuitBreaker.transitionToHalfOpenState();
    
    // When: 3번 연속 성공 호출
    for (int i = 0; i < 3; i++) {
        PaymentInfo result = paymentFacade.processPayment(command);
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    }
    
    // Then: CLOSED로 복구
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
}
```

#### 검증 항목

- ✅ 최소 호출 횟수(minimumNumberOfCalls) 로직
- ✅ 실패율 임계값(failureRateThreshold) 계산
- ✅ Circuit 상태 전환 (CLOSED → OPEN → HALF_OPEN → CLOSED)
- ✅ OPEN 상태에서 호출 차단
- ✅ Fallback 메서드 실행
- ✅ Metrics 정확성

---

### 2. E2E 테스트 (실제 PG 통신)

**파일**: `PaymentV1ApiE2ETest.java`

#### 테스트 구조

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Payment API E2E 테스트 - 실제 PG 통신")
class PaymentV1ApiE2ETest {
    
    @Autowired
    private TestRestTemplate testRestTemplate;
    
    // 실제 PG Simulator와 통신
}
```

#### 주요 테스트 케이스

##### 1) 정상 결제 처리

```java
@Test
@DisplayName("카드 결제 주문 생성 시 PG 모듈과 통신하고 PENDING 상태로 저장된다")
void create_order_with_card_payment_success() {
    // Given
    OrderV1Dtos.CardOrderCreateRequest request = ...;
    
    // When: 실제 HTTP 요청
    ResponseEntity<ApiResponse<OrderCreateResponse>> response = 
        testRestTemplate.exchange(Uris.Order.CREATE_CARD, ...);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.PENDING);
    assertThat(response.getBody().data().paymentInfo().transactionKey()).isNotNull();
}
```

##### 2) 타임아웃 처리

```java
@Test
@DisplayName("PG 타임아웃 발생 시 Fallback이 실행되고 FAILED 상태로 저장된다")
void pg_timeout_triggers_fallback() {
    // Given: 타임아웃 유발 카드 번호
    OrderV1Dtos.CardOrderCreateRequest request = 
        new OrderV1Dtos.CardOrderCreateRequest(..., 
            new CardPaymentInfo("SAMSUNG", "0000-0000-0000-0000", ...));
    
    // When: 결제 요청
    ResponseEntity<ApiResponse<OrderCreateResponse>> response = 
        testRestTemplate.exchange(...);
    
    // Then: Fallback으로 처리
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    
    PaymentEntity savedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
    assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(savedPayment.getTransactionKey()).isNull();
    assertThat(savedPayment.getFailureReason()).contains("일시적으로 사용 불가능");
}
```

##### 3) 콜백 처리

```java
@Test
@DisplayName("PG 콜백으로 결제 성공 시 주문이 CONFIRMED 상태로 변경된다")
void payment_callback_success_updates_order_to_confirmed() {
    // Given: 결제 요청 완료
    Long orderId = ...;
    String transactionKey = ...;
    
    // When: PG 콜백 수신 (SUCCESS)
    PaymentV1Dtos.PgCallbackRequest callbackRequest = 
        new PaymentV1Dtos.PgCallbackRequest(transactionKey, orderId, ..., "SUCCESS", ...);
    
    testRestTemplate.exchange(Uris.Payment.CALLBACK, ...);
    
    // Then: 비동기 처리 대기 후 확인
    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
            PaymentEntity payment = paymentRepository.findByOrderId(orderId).orElseThrow();
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            
            OrderInfo order = orderFacade.getOrderById(username, orderId);
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        });
}
```

#### 검증 항목

- ✅ 실제 PG 통신 성공
- ✅ 타임아웃 시 Fallback 동작
- ✅ 콜백 처리 (SUCCESS/FAILED)
- ✅ 멱등성 보장
- ✅ 비동기 처리 검증
