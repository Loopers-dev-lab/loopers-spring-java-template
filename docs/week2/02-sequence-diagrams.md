### E-Commerce 플랫폼 도메인 시퀀스 다이어그램

### 1. 상품 목록 조회

```mermaid
sequenceDiagram
    participant User
    participant ProductController
    participant ProductService
    participant ProductRepository

    User->>ProductController: GET /api/v1/products?keyword=...&sortBy=...
    Note right of ProductController: 검색/필터링/정렬 파라미터 수신

    ProductController->>ProductService: findProducts(SearchCriteria criteria)
    Note right of ProductService: 검색 조건으로 상품 목록 조회 요청

    ProductService->>ProductRepository: findAll(SearchCriteria criteria, Pageable pageable)
    Note right of ProductRepository: 동적 쿼리 실행

    ProductRepository-->>ProductService: Page<Product>
    Note left of ProductService: 상품 엔티티 목록(페이지) 수신

    ProductService-->>ProductController: Page<ProductDto>
    Note left of ProductController: DTO 리스트로 변환하여 반환

    alt 검색 결과가 존재할 경우
        ProductController-->>User: 200 OK (product list)
    else 검색 결과가 없을 경우
        ProductController-->>User: 200 OK (empty list [])
    end
```

### 2. 상품 상세 정보 조회

```mermaid
sequenceDiagram
    participant User
    participant ProductController
    participant ProductService
    participant ProductRepository

    User->>ProductController: GET /api/v1/products/{id}
    Note right of ProductController: HTTP 요청 수신 및 파라미터 추출

    ProductController->>ProductService: findProduct(Long id)
    Note right of ProductService: 상품 조회 비즈니스 로직 호출

    ProductService->>ProductRepository: findById(Long id)
    Note right of ProductRepository: SELECT 쿼리 실행

    ProductRepository-->>ProductService: Optional<Product>
    Note left of ProductService: Product 엔티티 또는 empty Optional 수신

    alt 상품 정보가 존재할 경우
        ProductService-->>ProductController: ProductDetailResponseDto
        Note left of ProductController: DTO로 변환하여 반환
        ProductController-->>User: 200 OK (JSON)
        Note right of User: 상품 상세 정보 수신
    else 상품 정보가 없을 경우 (Not Found)
        ProductService-->>ProductController: null
        ProductController-->>User: 404 NOT_FOUND
    end
```

### 3. 상품 '좋아요' 등록

```mermaid
sequenceDiagram
    participant User
    participant ProductLikeController
    participant ProductLikeService
    participant ProductService
    participant ProductLikeRepository
    participant ProductRepository

    User->>ProductLikeController: POST /api/v1/likes?productId={id}<br>Header: X-USER-ID={userId}
    
    alt X-USER-ID 헤더가 없는 경우 (비회원)
        ProductLikeController-->>User: 404 NOT_FOUND
    else X-USER-ID 헤더가 있는 경우 (회원)
        Note right of ProductLikeController: X-USER-ID 헤더에서 userId 획득
        ProductLikeController->>ProductLikeService: addLike(Long userId, Long productId)
        Note right of ProductLikeService: @Transactional 시작

        ProductLikeService->>ProductLikeRepository: findLikeProduct(userId, productId)
        ProductLikeRepository-->>ProductLikeService: Optional<Like>

        alt '좋아요' 정보가 없을 경우 (신규 등록)
            ProductLikeService->>ProductLikeRepository: save(new Like(user, product))
            Note right of ProductLikeRepository: INSERT 쿼리 실행
            
            ProductLikeService->>ProductService: incrementLikeCount(productId)
            Note right of ProductService: Product 도메인 로직 호출
            ProductService->>ProductRepository: findById(productId)
            ProductRepository-->>ProductService: Product
            Note left of ProductService: likeCount++
            ProductService->>ProductRepository: save(product)
            ProductRepository-->>ProductService: void
            ProductService-->>ProductLikeService: void

        else '좋아요' 정보가 이미 존재할 경우
            Note right of ProductLikeService: 아무 작업도 수행하지 않음 (멱등성 보장)
        end

        ProductLikeService-->>ProductLikeController: LikeResponseDto(liked: true, count: int)
        Note right of ProductLikeService: @Transactional 종료 (커밋)

        ProductLikeController-->>User: 200 OK (JSON)
    end
```

### 4. 상품 '좋아요' 취소

```mermaid
sequenceDiagram
    participant User
    participant ProductLikeController
    participant ProductLikeService
    participant ProductService
    participant ProductLikeRepository
    participant ProductRepository

    User->>ProductLikeController: DELETE /api/v1/likes?productId={id}<br>Header: X-USER-ID={userId}
    
    alt X-USER-ID 헤더가 없는 경우 (비회원)
        ProductLikeController-->>User: 404 NOT_FOUND
    else X-USER-ID 헤더가 있는 경우 (회원)
        Note right of ProductLikeController: X-USER-ID 헤더에서 userId 획득
        ProductLikeController->>ProductLikeService: removeLike(Long userId, Long productId)
        Note right of ProductLikeService: @Transactional 시작

        ProductLikeService->>ProductLikeRepository: findLikeProduct(userId, productId)
        ProductLikeRepository-->>ProductLikeService: Optional<Like>

        alt '좋아요' 정보가 존재할 경우 (삭제)
            ProductLikeService->>ProductLikeRepository: delete(Like entity)
            Note right of ProductLikeRepository: DELETE 쿼리 실행

            ProductLikeService->>ProductService: decrementLikeCount(productId)
            Note right of ProductService: Product 도메인 로직 호출
            ProductService->>ProductRepository: findById(productId)
            ProductRepository-->>ProductService: Product
            Note left of ProductService: likeCount--
            ProductService->>ProductRepository: save(product)
            ProductRepository-->>ProductService: void
            ProductService-->>ProductLikeService: void

        else '좋아요' 정보가 없을 경우
            Note right of ProductLikeService: 아무 작업도 수행하지 않음 (멱등성 보장)
        end

        ProductLikeService-->>ProductLikeController: LikeResponseDto(liked: false, count: int)
        Note right of ProductLikeService: @Transactional 종료 (커밋)

        ProductLikeController-->>User: 200 OK (JSON)
    end
```

### 5. 주문 생성 및 결제

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Controller as OrderV1Controller
    participant Facade as OrderFacade
    participant Domain as Domain Layer<br/>(Order, Payment, Product, User)
    participant PaymentGateway as PaymentGateway
    participant PG as PG Simulator

    User->>Controller: POST /api/v1/orders/new<br/>paymentType: "CARD"

    Controller->>Facade: createOrder(OrderCommand)

    activate Facade
    Note over Facade: 1. 주문 검증 및 생성
    Facade->>Domain: - User, Product 조회<br/>- 재고 확인 및 차감<br/>- 포인트 차감<br/>- 쿠폰 사용 (Optional)
    Domain-->>Facade: Order (할인 적용)

    Note over Facade: 2. Payment 생성 (PENDING)
    Facade->>Domain: Payment.createPaymentForCard(...)
    Domain-->>Facade: Payment (PENDING)

    Note over Facade: 3. PG 결제 요청
    Facade->>PaymentGateway: processPayment(payment, callbackUrl)

    activate PaymentGateway
    PaymentGateway->>PG: POST /api/v1/payments<br/>(Feign Client)

    Note over PG: 비동기 결제 처리 시작
    PG-->>PaymentGateway: transactionId, status: "PROCESSING"
    deactivate PaymentGateway

    PaymentGateway-->>Facade: PaymentResult

    Note over Facade: 4. Transaction ID 저장 및 Order 저장
    Facade->>Domain: payment.startProcessing(transactionId)<br/>order.save()

    Facade-->>Controller: OrderInfo
    deactivate Facade

    Controller-->>User: 200 OK<br/>{orderId, status: "INIT"}

    Note over PG: 결제 완료 후<br/>콜백 준비
```

## 6. 결제 콜백 처리 플로우

```mermaid
sequenceDiagram
    participant PG as PG Simulator
    participant Controller as PaymentCallbackController
    participant Facade as PaymentFacade
    participant Domain as Domain Layer<br/>(Payment, Order)

    Note over PG: 결제 처리 완료

    PG->>Controller: POST /api/v1/payments/callback<br/>Body: {transactionId, status, message}

    activate Controller
    Controller->>Facade: handlePaymentCallback(command)

    activate Facade
    Note over Facade: 1. Payment 조회
    Facade->>Domain: findByPgTransactionId(transactionId)
    Domain-->>Facade: Payment

    Note over Facade: 2. 결제 상태별 처리

    alt 결제 성공 (status == "SUCCESS")
        Facade->>Domain: payment.completePayment()
        Note over Domain: - Payment: PROCESSING → SUCCESS<br/>- Order: INIT → COMPLETED
        Domain-->>Facade: 완료

    else 결제 실패 (status == "FAILED")
        Facade->>Domain: payment.failPayment(message)
        Note over Domain: - Payment: PROCESSING → FAILED<br/>- Order: INIT → CANCELLED<br/>- 보상 트랜잭션 실행:<br/>  재고 복구, 포인트 환불, 쿠폰 복구
        Domain-->>Facade: 실패 처리 완료
    end

    Facade-->>Controller: void
    deactivate Facade

    Controller-->>PG: 200 OK
    deactivate Controller
```

## 7. 스케줄러 기반 결제 상태 확인 플로우 (장애 복구)

콜백이 도착하지 않은 경우를 대비

```mermaid
sequenceDiagram
    participant Scheduler as PaymentStatusCheckScheduler
    participant Domain as Domain Layer<br/>(Payment, Order)
    participant PaymentGateway as PaymentGateway
    participant PG as PG Simulator

    Note over Scheduler: 1분마다 자동 실행<br/>@Scheduled(fixedRate = 60000)

    activate Scheduler
    Note over Scheduler: 1. PROCESSING 상태 결제 조회
    Scheduler->>Domain: findProcessingPayments()<br/>(조건: 1분 이상 경과, 10회 미만)
    Domain-->>Scheduler: List<Payment>

    loop 각 PROCESSING 결제
        Note over Scheduler: 2. PG 상태 확인 요청
        Scheduler->>PaymentGateway: checkPaymentStatus(transactionId)

        Note over PaymentGateway: @Retry, @CircuitBreaker 적용
        PaymentGateway->>PG: GET /api/v1/payments/{transactionId}

        alt PG 응답 성공
            PG-->>PaymentGateway: status: SUCCESS/FAILED/PROCESSING
        else Timeout/장애 (Retry/CircuitBreaker)
            Note over PaymentGateway: Fallback: PROCESSING 유지
        end

        PaymentGateway-->>Scheduler: PaymentResult

        Note over Scheduler: 3. 확인 횟수 증가 및 상태 처리
        Scheduler->>Domain: - incrementStatusCheckCount()<br/>- completePayment() or failPayment()
        Note over Domain: 상태에 따라:<br/>- SUCCESS: Payment/Order 완료<br/>- FAILED: 보상 트랜잭션 실행<br/>- PROCESSING: 다음 주기 대기
        Domain-->>Scheduler: 업데이트 완료
    end

    deactivate Scheduler
```