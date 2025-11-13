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
    participant User
    participant OrderController
    participant OrderService
    participant ProductService
    participant UserService
    participant OrderRepository
    participant OrderNotificationClient

    User->>OrderController: POST /api/v1/orders (OrderRequest)<br>Header: X-USER-ID={userId}

    alt X-USER-ID 헤더가 없는 경우 (비회원)
        OrderController-->>User: 404 NOT_FOUND
    else X-USER-ID 헤더가 있는 경우 (회원)
        Note right of OrderController: X-USER-ID 헤더에서 userId 획득 및 DTO 수신
        OrderController->>OrderService: placeOrder(Long userId, OrderRequest request)
        Note right of OrderService: @Transactional 시작

        Note over OrderService: 1. 상품 재고 확인 및 차감
        OrderService->>ProductService: decreaseStock(productId, quantity)
        ProductService-->>OrderService: void

        Note over OrderService: 2. 사용자 포인트 확인 및 차감
        OrderService->>UserService: deductPoints(userId, totalAmount)
        UserService-->>OrderService: void

        Note over OrderService: 3. 주문 정보 생성 및 저장
        OrderService->>OrderRepository: save(new Order(..., status='PAID'))
        Note right of OrderRepository: 주문 정보 INSERT (상태: 결제 완료)
        OrderRepository-->>OrderService: savedOrder

        alt 외부 시스템 호출 성공
            Note over OrderService: 4. 외부 시스템에 주문 정보 전송 (Mock)
            OrderService->>OrderNotificationClient: sendOrderConfirmation(savedOrder)
            OrderNotificationClient-->>OrderService: void
        else 외부 시스템 호출 실패
            Note over OrderService: 4. 외부 시스템 호출 실패 처리 (Rollback)
            OrderService->>OrderService: handleNotificationFailure(savedOrder)
        end

        OrderService-->>OrderController: OrderConfirmation
        Note right of OrderService: @Transactional 종료 (커밋)
        Note left of OrderService: 재고 또는 포인트 부족 시 예외 발생 -> @Transactional 롤백

        OrderController-->>User: 200 OK (JSON)
    end
```