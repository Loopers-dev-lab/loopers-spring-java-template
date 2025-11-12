# 02-sequence-diagrams.md - 시퀀스 다이어그램

## 📑 목차

### 상품 (Products)
- [1. 상품 목록 조회](#1-상품-목록-조회)
- [2. 상품 상세 조회 - 성공 플로우](#2-상품-상세-조회---성공-플로우)
- [3. 상품 상세 조회 - 에러 처리 (404)](#3-상품-상세-조회---에러-처리-404)

### 좋아요 (Likes)
- [4. 좋아요 기능 사용자 여정](#4-좋아요-기능-사용자-여정)
- [5. 상품 좋아요 등록](#5-상품-좋아요-등록)
- [6. 상품 좋아요 취소](#6-상품-좋아요-취소)
- [7. 좋아요 목록 조회](#7-좋아요-목록-조회)
- [8. 좋아요 에러 시나리오](#8-좋아요-에러-시나리오)

### 브랜드 (Brands)
- [9. 브랜드 조회](#9-브랜드-조회)

### 주문 (Orders)
- [10. 주문 생성 - 성공 플로우](#10-주문-생성---성공-플로우)
- [11. 주문 생성 - 실패 플로우](#11-주문-생성---실패-플로우)
- [12. 주문 목록 조회](#12-주문-목록-조회)
- [13. 주문 상세 조회](#13-주문-상세-조회)

---

## 1. 상품 목록 조회

### 플로우 설명

사용자가 상품 목록을 조회할 때의 흐름입니다. 브랜드 필터링, 정렬 조건, 페이지네이션을 지원하며, 각 상품의 좋아요 수와 현재 사용자의 좋아요 여부를 함께 반환합니다.

### 주요 처리 사항

- 상품 조회 및 페이지네이션 처리
- 사용자별 좋아요 상태 조회
- Product + LikeStatus 결합하여 응답 생성

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant ProductController
    participant ProductFacade
    participant ProductRepository
    participant LikeRepository

    Client->>+ProductController: GET /api/v1/products?brandId=1&sort=latest&page=0&size=20
    Note over ProductController: 쿼리 파라미터 검증

    ProductController->>+ProductFacade: getProducts(conditions, userId, pageable)

    ProductFacade->>+ProductRepository: findAll(conditions, pageable)
    ProductRepository-->>-ProductFacade: Page<Product>

    ProductFacade->>+LikeRepository: findLikeStatusByUser(userId, productIds)
    LikeRepository-->>-ProductFacade: Map<ProductId, LikeStatus>

    Note over ProductFacade: Product + LikeStatus → ProductResponse 변환

    ProductFacade-->>-ProductController: Page<ProductResponse>
    ProductController-->>-Client: 200 OK (응답 DTO)
```

---

## 2. 상품 상세 조회 - 성공 플로우

### 플로우 설명

사용자가 특정 상품의 상세 정보를 조회할 때의 성공 흐름입니다. 상품 정보와 함께 현재 사용자의 좋아요 여부를 반환합니다.

### 주요 처리 사항

- 상품 ID로 상품 정보 조회
- 사용자의 좋아요 여부 확인
- 상품 상세 정보 응답 생성

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant ProductController
    participant ProductFacade
    participant ProductRepository
    participant LikeRepository

    Client->>+ProductController: GET /api/v1/products/{productId}

    ProductController->>+ProductFacade: getProduct(productId, userId)

    ProductFacade->>+ProductRepository: findById(productId)
    ProductRepository-->>-ProductFacade: Product

    ProductFacade->>+LikeRepository: existsByUserIdAndProductId(userId, productId)
    LikeRepository-->>-ProductFacade: boolean (좋아요 여부)

    Note over ProductFacade: Product + LikeStatus → ProductDetailResponse 변환

    ProductFacade-->>-ProductController: ProductDetailResponse
    ProductController-->>-Client: 200 OK
```

---

## 3. 상품 상세 조회 - 에러 처리 (404)

### 플로우 설명

존재하지 않는 상품을 조회할 때의 에러 처리 흐름입니다.

### 주요 처리 사항

- 상품 존재 여부 확인
- ProductNotFoundException 발생
- ExceptionHandler에서 404 응답 변환

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant ProductController
    participant ProductFacade
    participant ProductRepository

    Client->>+ProductController: GET /api/v1/products/999

    ProductController->>+ProductFacade: getProduct(999, userId)

    ProductFacade->>+ProductRepository: findById(999)
    ProductRepository-->>-ProductFacade: ProductNotFoundException

    ProductFacade-->>-ProductController: ProductNotFoundException
    Note over ProductController: ExceptionHandler가 처리
    ProductController-->>-Client: 404 Not Found
```

---

## 4. 좋아요 기능 사용자 여정

### 전체 시나리오

김철수가 좋아요 기능을 사용하는 완전한 여정:

1. **첫 좋아요 등록** → 마음에 드는 상품에 좋아요 클릭
2. **중복 등록 시도** → 실수로 다시 클릭해도 멱등성으로 정상 처리
3. **좋아요 취소** → 마음이 바뀌어 취소 버튼 클릭
4. **중복 취소 시도** → 이미 취소된 상태에서 재시도해도 멱등성으로 정상 처리
5. **재등록** → 다시 생각을 바꿔 좋아요 재등록
6. **좋아요 목록 확인** → 좋아요한 상품들을 한눈에 조회

이 여정을 통해 좋아요 기능의 **멱등성**(동일한 요청을 여러 번 해도 결과가 동일)이 어떻게 보장되는지 확인할 수 있습니다.

**관련 시퀀스**:
- [5. 상품 좋아요 등록](#5-상품-좋아요-등록)
- [6. 상품 좋아요 취소](#6-상품-좋아요-취소)
- [7. 좋아요 목록 조회](#7-좋아요-목록-조회)

---

## 5. 상품 좋아요 등록

### 플로우 설명

사용자가 상품에 좋아요를 등록할 때의 흐름입니다. 멱등성을 보장하기 위해 이미 좋아요가 등록되어 있으면 중복 등록하지 않고 정상 응답을 반환합니다.

### 주요 처리 사항

- 상품 존재 여부 확인
- 좋아요 중복 여부 확인
- 신규 등록 시 Like 도메인 객체 생성 및 저장
- **Product 테이블의 좋아요 수 증가 (like_count++)**
- 이미 존재 시 멱등성 보장 (중복 등록 무시)

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant LikeController
    participant LikeFacade
    participant ProductReader
    participant LikeRepository
    participant ProductRepository
    participant Like
    participant Product

    Client->>+LikeController: POST /api/v1/like/products/{productId}
    Note over LikeController: X-USER-ID 헤더 추출

    LikeController->>+LikeFacade: addLike(userId, productId)

    LikeFacade->>+ProductReader: findById(productId)
    ProductReader-->>-LikeFacade: Product (또는 ProductNotFoundException)

    LikeFacade->>+LikeRepository: existsByUserIdAndProductId(userId, productId)
    LikeRepository-->>-LikeFacade: boolean

    alt 좋아요 없음 (신규 등록)
        LikeFacade->>+Like: create(userId, productId)
        Like-->>-LikeFacade: Like 도메인 객체
        LikeFacade->>+LikeRepository: save(Like)
        LikeRepository-->>-LikeFacade: saved Like

        Note over LikeFacade,Product: 좋아요 수 증가
        LikeFacade->>Product: incrementLikeCount()
        LikeFacade->>ProductRepository: save(Product)
    else 좋아요 이미 존재 (멱등성)
        Note over LikeFacade: 중복 등록 무시, 정상 응답
    end

    LikeFacade-->>-LikeController: void (성공)
    LikeController-->>-Client: 200 OK
```

---

## 6. 상품 좋아요 취소

### 플로우 설명

사용자가 상품의 좋아요를 취소할 때의 흐름입니다. 멱등성을 보장하기 위해 이미 취소되어 있어도 정상 응답을 반환합니다.

### 주요 처리 사항

- 좋아요 존재 여부 확인
- 존재 시 삭제 처리
- **Product 테이블의 좋아요 수 감소 (like_count--)**
- 없을 시 멱등성 보장 (이미 취소됨)

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant LikeController
    participant LikeFacade
    participant ProductReader
    participant LikeRepository
    participant ProductRepository
    participant Product

    Client->>+LikeController: DELETE /api/v1/like/products/{productId}
    Note over LikeController: X-USER-ID 헤더 추출

    LikeController->>+LikeFacade: removeLike(userId, productId)

    LikeFacade->>+LikeRepository: findByUserIdAndProductId(userId, productId)
    LikeRepository-->>-LikeFacade: Optional<Like>

    alt 좋아요 존재
        LikeFacade->>+LikeRepository: delete(Like)
        Note over LikeRepository: Soft Delete 또는 Hard Delete
        LikeRepository-->>-LikeFacade: void (삭제 완료)

        Note over LikeFacade,Product: 좋아요 수 감소
        LikeFacade->>+ProductReader: findById(productId)
        ProductReader-->>-LikeFacade: Product
        LikeFacade->>Product: decrementLikeCount()
        LikeFacade->>ProductRepository: save(Product)
    else 좋아요 없음 (멱등성)
        Note over LikeFacade: 이미 삭제됨, 정상 응답
    end

    LikeFacade-->>-LikeController: void (성공)
    LikeController-->>-Client: 200 OK
```

---

## 7. 좋아요 목록 조회

### 플로우 설명

사용자가 자신이 좋아요한 상품 목록을 조회할 때의 흐름입니다. 페이지네이션과 정렬을 지원하며, 각 상품의 기본 정보와 브랜드 정보를 함께 반환합니다.

### 주요 처리 사항

- 사용자의 좋아요 목록 조회
- 각 좋아요에 해당하는 상품 정보 조회
- Like + Product 결합하여 응답 생성

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant LikeController
    participant LikeFacade
    participant LikeRepository
    participant ProductReader

    Client->>+LikeController: GET /api/v1/like/products?page=0&size=20&sort=latest
    Note over LikeController: X-USER-ID 헤더 추출

    LikeController->>+LikeFacade: getLikedProducts(userId, pageable)

    LikeFacade->>+LikeRepository: findByUserId(userId, pageable)
    LikeRepository-->>-LikeFacade: Page<Like>

    Note over LikeFacade: Like 목록에서 productIds 추출
    LikeFacade->>+ProductReader: findByIdIn(productIds)
    ProductReader-->>-LikeFacade: List<Product>

    Note over LikeFacade: Like + Product → LikedProductResponse 변환

    LikeFacade-->>-LikeController: Page<LikedProductResponse>
    LikeController-->>-Client: 200 OK (응답 DTO)
```

---

## 8. 좋아요 에러 시나리오

### 플로우 설명

좋아요 기능에서 발생할 수 있는 주요 에러 케이스입니다.

### 8.1 상품이 존재하지 않는 경우 (404)

```mermaid
sequenceDiagram
    participant Client
    participant LikeController
    participant LikeFacade
    participant ProductReader

    Client->>+LikeController: POST /api/v1/like/products/999

    LikeController->>+LikeFacade: addLike(userId, 999)

    LikeFacade->>+ProductReader: findById(999)
    ProductReader-->>-LikeFacade: ProductNotFoundException

    LikeFacade-->>-LikeController: ProductNotFoundException
    Note over LikeController: ExceptionHandler가 처리
    LikeController-->>-Client: 404 Not Found
```

### 8.2 인증되지 않은 사용자 (401)

```mermaid
sequenceDiagram
    participant Client
    participant LikeController

    Client->>+LikeController: POST /api/v1/like/products/{productId}
    Note over LikeController: X-USER-ID 헤더 없음

    LikeController-->>-Client: 401 Unauthorized
```

### 8.3 중복 좋아요 등록 시도 (멱등성 처리)

**플로우 설명**: 이미 좋아요한 상품에 다시 좋아요를 시도할 때, DB UNIQUE 제약 조건을 활용하여 멱등성을 보장하는 흐름입니다.

**주요 처리 사항**:
- DB UNIQUE(ref_user_id, ref_product_id) 제약으로 중복 방지
- 제약 위반 예외 발생 시 200 OK 응답으로 변환
- 동시 요청에도 데이터 일관성 보장

```mermaid
sequenceDiagram
    participant Client
    participant LikeController
    participant LikeFacade
    participant ProductReader
    participant LikeRepository
    participant Database

    Client->>+LikeController: POST /api/v1/like/products/{productId}
    Note over Client: 이미 좋아요한 상품에 재시도

    LikeController->>+LikeFacade: addLike(userId, productId)

    LikeFacade->>+ProductReader: findById(productId)
    ProductReader-->>-LikeFacade: Product

    LikeFacade->>+LikeRepository: save(Like)
    LikeRepository->>+Database: INSERT INTO product_likes
    Note over Database: UNIQUE(ref_user_id, ref_product_id) 제약 위반

    Database-->>-LikeRepository: IntegrityConstraintViolationException
    LikeRepository-->>-LikeFacade: IntegrityConstraintViolationException

    Note over LikeFacade: 예외 처리: 이미 등록됨으로 판단
    Note over LikeFacade: 멱등성 보장 - 정상 응답 반환

    LikeFacade-->>-LikeController: void (성공)
    LikeController-->>-Client: 200 OK
    Note over Client: 중복 등록 시도이지만 정상 응답
```

---

## 9. 브랜드 조회

### 플로우 설명

사용자가 특정 브랜드의 정보를 조회할 때의 흐름입니다. 브랜드 ID를 통해 브랜드명과 설명을 조회합니다.

### 주요 처리 사항

- 브랜드 ID로 브랜드 정보 조회
- 브랜드 존재 여부 확인
- 브랜드 정보 응답 생성

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant BrandController
    participant BrandFacade
    participant BrandRepository

    Client->>+BrandController: GET /api/v1/brands/{brandId}

    BrandController->>+BrandFacade: getBrand(brandId)

    BrandFacade->>+BrandRepository: findById(brandId)

    alt 브랜드 존재
        BrandRepository-->>BrandFacade: Brand
        Note over BrandFacade: Brand → BrandResponse 변환
        BrandFacade-->>BrandController: BrandResponse
        BrandController-->>Client: 200 OK
    else 브랜드 없음
        BrandRepository-->>BrandFacade: BrandNotFoundException
        BrandFacade-->>BrandController: BrandNotFoundException
        Note over BrandController: ExceptionHandler가 처리
        BrandController-->>Client: 404 Not Found
    end

    BrandRepository-->>-BrandFacade: (완료)
    BrandFacade-->>-BrandController: (완료)
    BrandController-->>-Client: (완료)
```

---

## 10. 주문 생성 - 성공 플로우

### 플로우 설명

사용자가 여러 상품을 주문하고 결제할 때의 성공 흐름입니다. 재고 확인, 포인트 확인, 재고 차감, 포인트 차감이 하나의 트랜잭션으로 처리됩니다.

### 주요 처리 사항

- 주문 상품별 재고 확인
- 총 결제 금액 계산
- 사용자 포인트 잔액 확인
- 재고 차감 및 주문 생성
- 포인트 차감
- 외부 시스템 전송 (Mock)

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant OrderController
    participant OrderFacade
    participant ProductReader
    participant Order
    participant OrderRepository
    participant PointFacade
    participant External

    Client->>+OrderController: POST /api/v1/orders
    Note over OrderController: 요청 DTO 검증

    OrderController->>+OrderFacade: createOrder(userId, orderItems)

    Note over OrderFacade: 트랜잭션 시작

    Note over OrderFacade: orderItems에서 productIds 추출
    OrderFacade->>+ProductReader: findByIdIn(productIds)
    ProductReader-->>-OrderFacade: List<Product>

    Note over OrderFacade: 각 Product 재고 확인 (stock >= quantity)
    Note over OrderFacade: 총 결제 금액 계산

    OrderFacade->>+PointFacade: checkBalance(userId, totalAmount)
    PointFacade-->>-OrderFacade: 포인트 잔액 (충분함)

    loop 각 Product마다
        OrderFacade->>Product: decreaseStock(quantity)
        Note over Product: 재고 차감 처리 (도메인 로직)
    end

    OrderFacade->>ProductRepository: saveAll(products)

    OrderFacade->>+Order: create(userId, orderItems, totalAmount)
    Order-->>-OrderFacade: Order 도메인 객체

    OrderFacade->>+OrderRepository: save(Order)
    OrderRepository-->>-OrderFacade: saved Order

    OrderFacade->>+PointFacade: deductPoints(userId, totalAmount)
    PointFacade-->>-OrderFacade: 포인트 차감 완료

    Note over OrderFacade: 트랜잭션 커밋

    OrderFacade->>+External: sendOrderInfo(order)
    External-->>-OrderFacade: 전송 완료 (Mock)

    Note over OrderFacade: Order → OrderResponse 변환

    OrderFacade-->>-OrderController: OrderResponse
    OrderController-->>-Client: 201 Created
```

---

## 11. 주문 생성 - 실패 플로우

### 플로우 설명

주문 생성 시 재고 부족 또는 포인트 부족으로 실패하는 경우의 흐름입니다. 트랜잭션이 롤백되어 모든 변경사항이 취소됩니다.

### 11.1 재고 부족 케이스

```mermaid
sequenceDiagram
    participant Client
    participant OrderController
    participant OrderFacade
    participant ProductReader

    Client->>+OrderController: POST /api/v1/orders

    OrderController->>+OrderFacade: createOrder(userId, orderItems)

    Note over OrderFacade: 트랜잭션 시작

    OrderFacade->>+ProductReader: findById(productId)
    ProductReader-->>-OrderFacade: Product

    Note over OrderFacade: 재고 확인 (stock < quantity)
    OrderFacade-->>OrderFacade: InsufficientStockException

    Note over OrderFacade: 트랜잭션 롤백

    OrderFacade-->>-OrderController: InsufficientStockException
    Note over OrderController: ExceptionHandler가 처리
    OrderController-->>-Client: 400 Bad Request (재고 부족)
```

### 11.2 포인트 부족 케이스

```mermaid
sequenceDiagram
    participant Client
    participant OrderController
    participant OrderFacade
    participant ProductReader
    participant PointFacade

    Client->>+OrderController: POST /api/v1/orders

    OrderController->>+OrderFacade: createOrder(userId, orderItems)

    Note over OrderFacade: 트랜잭션 시작

    Note over OrderFacade: orderItems에서 productIds 추출
    OrderFacade->>+ProductReader: findByIdIn(productIds)
    ProductReader-->>-OrderFacade: List<Product>

    Note over OrderFacade: 각 Product 재고 확인 (stock >= quantity)
    Note over OrderFacade: 총 결제 금액 계산

    OrderFacade->>+PointFacade: checkBalance(userId, totalAmount)
    PointFacade-->>-OrderFacade: InsufficientPointException

    Note over OrderFacade: 트랜잭션 롤백

    OrderFacade-->>-OrderController: InsufficientPointException
    Note over OrderController: ExceptionHandler가 처리
    OrderController-->>-Client: 400 Bad Request (포인트 부족)
```

### 11.3 결제 처리 실패

**플로우 설명**: 주문 저장과 포인트 차감은 성공했으나, 외부 결제 시스템(Mock) 처리가 실패하는 경우입니다. 트랜잭션은 이미 커밋되었으므로 재시도 정책을 적용합니다.

**주요 처리 사항**:
- 트랜잭션 커밋 후 결제 시스템 호출 (트랜잭션 밖)
- 3회 자동 재시도 (1초, 2초, 4초 간격, 지수 백오프)
- 재시도 실패 시 주문 상태를 '결제 대기'로 마킹 및 500 에러 응답

```mermaid
sequenceDiagram
    participant Client
    participant OrderController
    participant OrderFacade
    participant OrderRepository
    participant Order
    participant PaymentSystem

    Client->>+OrderController: POST /api/v1/orders

    OrderController->>+OrderFacade: createOrder(userId, orderItems)

    Note over OrderFacade: 트랜잭션 시작
    Note over OrderFacade: 재고 확인, 포인트 확인
    Note over OrderFacade: 재고 차감, 포인트 차감

    OrderFacade->>+Order: create(userId, orderItems, totalAmount)
    Order-->>-OrderFacade: Order 도메인 객체

    OrderFacade->>+OrderRepository: save(Order)
    OrderRepository-->>-OrderFacade: saved Order

    Note over OrderFacade: 트랜잭션 커밋 완료
    Note over OrderFacade: (주문 저장, 재고/포인트 차감 완료)

    loop 3회 재시도 (지수 백오프: 1초, 2초, 4초)
        OrderFacade->>+PaymentSystem: processPayment(order)
        PaymentSystem-->>-OrderFacade: Timeout/Error
        Note over OrderFacade: 재시도 대기
    end

    Note over OrderFacade: 재시도 실패 (3회 모두 실패)
    OrderFacade->>Order: updateStatus("결제 대기")
    OrderFacade->>OrderRepository: save(Order)

    Note over OrderFacade: 모니터링 알림 발송

    OrderFacade-->>-OrderController: PaymentFailedException
    Note over OrderController: ExceptionHandler가 처리
    OrderController-->>-Client: 500 Internal Server Error
    Note over Client: 주문은 저장되었으나 결제 처리 실패
```

---

## 12. 주문 목록 조회

### 플로우 설명

사용자가 자신의 주문 목록을 조회할 때의 흐름입니다. 페이지네이션을 지원하며, 각 주문의 기본 정보(주문 ID, 일시, 총 금액, 상품 수)를 반환합니다.

### 주요 처리 사항

- 사용자별 주문 목록 조회
- 페이지네이션 처리
- 주문 기본 정보 응답 생성

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant OrderController
    participant OrderFacade
    participant OrderRepository

    Client->>+OrderController: GET /api/v1/orders?page=0&size=20
    Note over OrderController: X-USER-ID 헤더 추출

    OrderController->>+OrderFacade: getOrders(userId, pageable)

    OrderFacade->>+OrderRepository: findByUserId(userId, pageable)
    OrderRepository-->>-OrderFacade: Page<Order>

    Note over OrderFacade: Order → OrderListResponse 변환

    OrderFacade-->>-OrderController: Page<OrderListResponse>
    OrderController-->>-Client: 200 OK
```

---

## 13. 주문 상세 조회

### 플로우 설명

사용자가 특정 주문의 상세 정보를 조회할 때의 흐름입니다. 주문 항목별 상품 정보와 가격을 포함한 전체 주문 정보를 반환합니다.

### 주요 처리 사항

- 주문 ID로 주문 정보 조회
- 주문 항목(OrderItem) 정보 포함
- 주문 상세 정보 응답 생성

### 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant OrderController
    participant OrderFacade
    participant OrderRepository

    Client->>+OrderController: GET /api/v1/orders/{orderId}
    Note over OrderController: X-USER-ID 헤더 추출

    OrderController->>+OrderFacade: getOrder(orderId, userId)

    OrderFacade->>+OrderRepository: findByIdAndUserId(orderId, userId)

    alt 주문 존재
        OrderRepository-->>OrderFacade: Order (with OrderItems)
        Note over OrderFacade: Order → OrderDetailResponse 변환
        OrderFacade-->>OrderController: OrderDetailResponse
        OrderController-->>Client: 200 OK
    else 주문 없음
        OrderRepository-->>OrderFacade: OrderNotFoundException
        OrderFacade-->>OrderController: OrderNotFoundException
        Note over OrderController: ExceptionHandler가 처리
        OrderController-->>Client: 404 Not Found
    end

    OrderRepository-->>-OrderFacade: (완료)
    OrderFacade-->>-OrderController: (완료)
    OrderController-->>-Client: (완료)
```
