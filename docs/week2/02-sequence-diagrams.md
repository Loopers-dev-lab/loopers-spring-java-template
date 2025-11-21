# 🔄 02. 시퀀스 다이어그램

## 1. 상품 목록 조회

```mermaid
sequenceDiagram
    participant User
    participant ProductController
    participant ProductService
    participant BrandService
    participant ProductRepository

    User->>ProductController: GET /api/v1/products?brandId=1&sort=latest&page=0&size=20
    ProductController->>ProductService: getProducts(brandId=1, sort=latest, pageable)
    
    alt 브랜드 필터링이 있는 경우
        ProductService->>BrandService: validateBrandExists(brandId=1)
        alt 브랜드가 존재하지 않는 경우
            BrandService-->>ProductService: BrandNotFoundException
            ProductService-->>ProductController: BrandNotFoundException
            ProductController-->>User: 404 Not Found
        else 브랜드가 존재하는 경우
            BrandService-->>ProductService: Brand validated
            ProductService->>ProductRepository: findByBrandIdWithSort(brandId, sort, pageable)
        end
    else 전체 상품 조회
        ProductService->>ProductRepository: findAllWithSort(sort, pageable)
    end
    
    ProductRepository-->>ProductService: Page<ProductEntity>
    
    alt 상품이 존재하는 경우
        ProductService-->>ProductController: ProductListResponse(totalElements=25, content=[...])
        ProductController-->>User: 200 OK
    else 상품이 없는 경우
        ProductService-->>ProductController: ProductListResponse(totalElements=0, content=[])
        ProductController-->>User: 200 OK
    end
```

## 2. 상품 상세 조회

```mermaid
sequenceDiagram
    participant User
    participant ProductController
    participant ProductService
    participant ProductRepository

    User->>ProductController: GET /api/v1/products/1<br/>Header: X-USER-ID=123 (optional)
    ProductController->>ProductService: getProductDetail(productId=1, userId=123)
    
    ProductService->>ProductRepository: findProductDetail(productId=1, userId=123)
    
    alt 상품이 존재하는 경우
        ProductRepository-->>ProductService: ProductDetailInfo(product, brand, totalLikes, isLiked)
        ProductService-->>ProductController: ProductDetailResponse
        ProductController-->>User: 200 OK
    else 상품이 존재하지 않는 경우
        ProductRepository-->>ProductService: Optional.empty()
        ProductService-->>ProductController: ProductNotFoundException
        ProductController-->>User: 404 Not Found
    end
```

## 3. 브랜드 목록 조회

```mermaid
sequenceDiagram
    participant User
    participant BrandController
    participant BrandService
    participant BrandRepository

    User->>BrandController: GET /api/v1/brands?page=0&size=20
    BrandController->>BrandService: getBrands(pageable)
    BrandService->>BrandRepository: findAll(pageable)
    BrandRepository-->>BrandService: Page<BrandEntity>
    
    alt 브랜드가 존재하는 경우
        BrandService-->>BrandController: BrandListResponse(totalElements=15, content=[...])
        BrandController-->>User: 200 OK
    else 브랜드가 없는 경우
        BrandService-->>BrandController: BrandListResponse(totalElements=0, content=[])
        BrandController-->>User: 200 OK
    end
```

## 4. 브랜드 상세 조회

```mermaid
sequenceDiagram
    participant User
    participant BrandController
    participant BrandService
    participant BrandRepository

    User->>BrandController: GET /api/v1/brands/1
    BrandController->>BrandService: getBrandById(brandId=1)
    BrandService->>BrandRepository: findById(brandId=1)
    
    alt 브랜드가 존재하는 경우
        BrandRepository-->>BrandService: BrandEntity
        BrandService-->>BrandController: BrandDetailResponse
        BrandController-->>User: 200 OK
    else 브랜드가 존재하지 않는 경우
        BrandRepository-->>BrandService: Optional.empty()
        BrandService-->>BrandController: BrandNotFoundException
        BrandController-->>User: 404 Not Found
    end
```

## 5. 좋아요 등록

```mermaid
sequenceDiagram
    participant User
    participant LikeController
    participant LikeFacade
    participant UserService
    participant ProductService
    participant LikeService
    participant LikeRepository
    participant ProductRepository

    User->>LikeController: POST /api/v1/like/products/1<br/>Header: X-USER-ID=testuser
    LikeController->>LikeFacade: upsertLike(username="testuser", productId=1)
    
    LikeFacade->>UserService: getUserByUsername("testuser")
    alt 사용자가 존재하지 않는 경우
        UserService-->>LikeFacade: CoreException(NOT_FOUND_USER)
        LikeFacade-->>LikeController: CoreException
        LikeController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>LikeFacade: UserEntity
        
        LikeFacade->>ProductService: getProductDetail(productId=1)
        alt 상품이 존재하지 않는 경우
            ProductService-->>LikeFacade: CoreException(NOT_FOUND_PRODUCT)
            LikeFacade-->>LikeController: CoreException
            LikeController-->>User: 404 Not Found
        else 상품이 존재하는 경우
            ProductService-->>LikeFacade: ProductEntity
            
            LikeFacade->>LikeService: upsertLike(user, product)
            LikeService->>LikeRepository: findByUserIdAndProductId(userId, productId)
            
            alt 좋아요가 없는 경우
                LikeRepository-->>LikeService: Optional.empty()
                LikeService->>ProductRepository: save(product) [좋아요 카운트 +1]
                LikeService->>LikeRepository: save(newLike)
                LikeRepository-->>LikeService: LikeEntity (신규)
            else 삭제된 좋아요가 있는 경우
                LikeRepository-->>LikeService: LikeEntity (deleted)
                Note over LikeService: like.restore()
                LikeService->>ProductRepository: save(product) [좋아요 카운트 +1]
                LikeRepository-->>LikeService: LikeEntity (복원)
            else 활성 좋아요가 있는 경우
                LikeRepository-->>LikeService: LikeEntity (active)
                Note over LikeService: 카운트 변경 없음 (중복 방지)
                LikeRepository-->>LikeService: LikeEntity (기존)
            end
            
            LikeService-->>LikeFacade: LikeEntity
            LikeFacade-->>LikeController: LikeInfo
            LikeController-->>User: 200 OK
        end
    end
```

## 6. 좋아요 취소

```mermaid
sequenceDiagram
    participant User
    participant LikeController
    participant LikeFacade
    participant UserService
    participant ProductService
    participant LikeService
    participant LikeRepository
    participant ProductRepository

    User->>LikeController: DELETE /api/v1/like/products/1<br/>Header: X-USER-ID=testuser
    LikeController->>LikeFacade: unlikeProduct(username="testuser", productId=1)
    
    LikeFacade->>UserService: getUserByUsername("testuser")
    alt 사용자가 존재하지 않는 경우
        UserService-->>LikeFacade: CoreException(NOT_FOUND_USER)
        LikeFacade-->>LikeController: CoreException
        LikeController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>LikeFacade: UserEntity
        
        LikeFacade->>ProductService: getProductDetail(productId=1)
        alt 상품이 존재하지 않는 경우
            ProductService-->>LikeFacade: CoreException(NOT_FOUND_PRODUCT)
            LikeFacade-->>LikeController: CoreException
            LikeController-->>User: 404 Not Found
        else 상품이 존재하는 경우
            ProductService-->>LikeFacade: ProductEntity
            
            LikeFacade->>LikeService: unlikeProduct(user, product)
            LikeService->>LikeRepository: findByUserIdAndProductId(userId, productId)
            
            alt 좋아요가 없는 경우
                LikeRepository-->>LikeService: Optional.empty()
                Note over LikeService: 아무 작업 없음 (멱등성 보장)
            else 이미 삭제된 좋아요인 경우
                LikeRepository-->>LikeService: LikeEntity (deleted)
                Note over LikeService: 아무 작업 없음 (멱등성 보장)
            else 활성 좋아요인 경우
                LikeRepository-->>LikeService: LikeEntity (active)
                Note over LikeService: like.delete() (소프트 삭제)
                LikeService->>ProductRepository: save(product) [좋아요 카운트 -1]
            end
            
            LikeService-->>LikeFacade: void
            LikeFacade-->>LikeController: void
            LikeController-->>User: 200 OK
        end
    end
```

## 7. 포인트 충전

```mermaid
sequenceDiagram
    participant User
    participant PointController
    participant PointFacade
    participant PointService
    participant UserService
    participant UserRepository
    participant PointHistoryRepository

    User->>PointController: POST /api/v1/points/charge<br/>Header: X-USER-ID=testuser<br/>Body: {"amount": 10000}
    PointController->>PointFacade: chargePoint(username="testuser", request)
    
    PointFacade->>PointService: charge(username, amount)
    PointService->>UserRepository: findByUsername("testuser")
    
    alt 사용자가 존재하지 않는 경우
        UserRepository-->>PointService: Optional.empty()
        PointService-->>PointFacade: CoreException(NOT_FOUND_USER)
        PointFacade-->>PointController: CoreException
        PointController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserRepository-->>PointService: UserEntity
        
        Note over PointService: user.chargePoint(amount)<br/>[포인트 잔액 증가]
        
        PointService->>PointHistoryRepository: save(chargeHistory)
        Note over PointService: PointHistoryEntity.createChargeHistory()<br/>(userId, amount, balanceAfter)
        PointHistoryRepository-->>PointService: PointHistoryEntity
        
        PointService->>UserRepository: save(user)
        UserRepository-->>PointService: UserEntity
        
        PointService-->>PointFacade: 충전 후 잔액
        PointFacade-->>PointController: PointChargeResponse
        PointController-->>User: 200 OK
    end
```

## 8. 포인트 조회

```mermaid
sequenceDiagram
    participant User
    participant PointController
    participant PointFacade
    participant PointService
    participant UserService
    participant UserRepository
    participant PointHistoryRepository

    User->>PointController: GET /api/v1/points<br/>Header: X-USER-ID=testuser
    PointController->>PointFacade: getPointInfo(username="testuser")
    
    PointFacade->>UserService: getUserByUsername("testuser")
    alt 사용자가 존재하지 않는 경우
        UserService-->>PointFacade: CoreException(NOT_FOUND_USER)
        PointFacade-->>PointController: CoreException
        PointController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>PointFacade: UserEntity
        
        PointFacade->>PointService: getPointHistories(username)
        PointService->>UserRepository: findByUsername("testuser")
        UserRepository-->>PointService: UserEntity
        
        PointService->>PointHistoryRepository: findByUserOrderByCreatedAtDesc(user)
        PointHistoryRepository-->>PointService: List<PointHistoryEntity>
        PointService-->>PointFacade: List<PointHistoryEntity>
        
        Note over PointFacade: PointInfo 생성<br/>(잔액, 이력 목록)
        PointFacade-->>PointController: PointInfo
        PointController-->>User: 200 OK
    end
```

## 10. 주문 요청

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderFacade
    participant UserService
    participant ProductService
    participant PointService
    participant OrderService
    participant OrderRepository
    participant ProductRepository

    User->>OrderController: POST /api/v1/orders<br/>Header: X-USER-ID=testuser<br/>Body: {"items":[{"productId":1,"quantity":2}]}
    OrderController->>OrderFacade: createOrder(command)
    
    OrderFacade->>UserService: getUserByUsername("testuser")
    alt 사용자가 존재하지 않는 경우
        UserService-->>OrderFacade: CoreException(NOT_FOUND_USER)
        OrderFacade-->>OrderController: CoreException
        OrderController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>OrderFacade: UserEntity
        
        Note over OrderFacade: 주문 항목을 productId로 정렬 (데드락 방지)
        
        loop 각 주문 상품 검증 (정렬된 순서)
            OrderFacade->>ProductService: getProductDetailLock(productId) [비관적 락]
            alt 상품이 존재하지 않는 경우
                ProductService-->>OrderFacade: CoreException(NOT_FOUND_PRODUCT)
                OrderFacade-->>OrderController: CoreException
                OrderController-->>User: 404 Not Found
            else 재고가 부족한 경우
                ProductService-->>OrderFacade: ProductEntity
                Note over OrderFacade: product.canOrder(quantity) = false
                OrderFacade-->>OrderController: IllegalArgumentException
                OrderController-->>User: 400 Bad Request
            else 재고 충분
                ProductService-->>OrderFacade: ProductEntity (locked)
                Note over OrderFacade: 주문 가능 상품 목록에 추가<br/>총 주문 금액 계산
            end
        end
        
        OrderFacade->>PointService: use(user, totalAmount)
        alt 포인트가 부족한 경우
            PointService-->>OrderFacade: CoreException(INSUFFICIENT_POINTS)
            Note over OrderFacade: 트랜잭션 롤백 (재고 락 자동 해제)
            OrderFacade-->>OrderController: CoreException
            OrderController-->>User: 400 Bad Request
        else 포인트 차감 성공
            PointService-->>OrderFacade: 차감 후 잔액
            
            OrderFacade->>OrderService: createOrder(request)
            OrderService->>OrderRepository: save(order)
            OrderRepository-->>OrderService: OrderEntity
            OrderService-->>OrderFacade: OrderEntity
            
            loop 각 주문 항목 생성 및 재고 차감
                OrderFacade->>ProductService: deductStock(product, quantity)
                ProductService->>ProductRepository: save(product) [재고 차감]
                ProductRepository-->>ProductService: ProductEntity
                ProductService-->>OrderFacade: ProductEntity
                
                OrderFacade->>OrderService: createOrderItem(request)
                OrderService->>OrderRepository: save(orderItem)
                OrderRepository-->>OrderService: OrderItemEntity
                OrderService-->>OrderFacade: OrderItemEntity
            end
            
            OrderFacade-->>OrderController: OrderInfo
            OrderController-->>User: 201 Created
        end
    end
```

### 🔒 주문 처리 원자성 보장

#### **트랜잭션 전략**
- **@Transactional 기반 원자성**: OrderFacade의 createOrder 메서드 전체가 하나의 트랜잭션
  - 모든 작업이 성공하면 커밋
  - 중간에 예외 발생 시 자동 롤백 (재고 락 해제, 포인트 복구, 주문 취소)
  
#### **데드락 방지 전략**
- **정렬된 락 획득**: 주문 항목을 productId 기준으로 정렬하여 처리
  - 스레드 A: [상품1, 상품2] 순서로 락 획득
  - 스레드 B: [상품1, 상품2] 순서로 락 획득 (동일 순서)
  - 결과: 원형 대기(circular wait) 방지
  
#### **동시성 제어**
- **비관적 락(Pessimistic Lock)**: getProductDetailLock()에서 SELECT ... FOR UPDATE 사용
  - 재고 조회 시점에 행 레벨 락 획득
  - 트랜잭션 종료 시까지 다른 트랜잭션의 접근 차단

## 11. 사용자의 주문 목록 조회

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderFacade
    participant OrderService
    participant OrderRepository

    User->>OrderController: GET /api/v1/orders?page=0&size=20<br/>Header: X-USER-ID=testuser
    OrderController->>OrderFacade: getOrderSummariesByUserId(userId, pageable)
    
    OrderFacade->>OrderService: getOrdersByUserId(userId, pageable)
    OrderService->>OrderRepository: findByUserId(userId, pageable)
    
    alt 주문 내역이 존재하는 경우
        OrderRepository-->>OrderService: Page<OrderEntity>
        OrderService-->>OrderFacade: Page<OrderEntity>
        
        loop 각 주문에 대해
            OrderFacade->>OrderService: countOrderItems(orderId)
            OrderService->>OrderRepository: countByOrderId(orderId)
            OrderRepository-->>OrderService: itemCount
            OrderService-->>OrderFacade: itemCount
            Note over OrderFacade: OrderSummary.from(order, itemCount)
        end
        
        OrderFacade-->>OrderController: Page<OrderSummary>
        OrderController-->>User: 200 OK
    else 주문 내역이 없는 경우
        OrderRepository-->>OrderService: Page.empty()
        OrderService-->>OrderFacade: Page.empty()
        OrderFacade-->>OrderController: Page.empty()
        OrderController-->>User: 200 OK
    end
```

## 12. 주문 상세 조회

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderFacade
    participant OrderService
    participant OrderRepository

    User->>OrderController: GET /api/v1/orders/1<br/>Header: X-USER-ID=testuser
    OrderController->>OrderFacade: getOrderById(orderId=1)
    
    OrderFacade->>OrderService: getOrderById(orderId=1)
    OrderService->>OrderRepository: findById(orderId=1)
    
    alt 주문이 존재하지 않는 경우
        OrderRepository-->>OrderService: Optional.empty()
        OrderService-->>OrderFacade: CoreException(NOT_FOUND)
        OrderFacade-->>OrderController: CoreException
        OrderController-->>User: 404 Not Found
    else 주문이 존재하는 경우
        OrderRepository-->>OrderService: OrderEntity
        OrderService-->>OrderFacade: OrderEntity
        
        OrderFacade->>OrderService: getOrderItemsByOrderId(orderId=1)
        OrderService->>OrderRepository: findByOrderId(orderId=1)
        OrderRepository-->>OrderService: List<OrderItemEntity>
        OrderService-->>OrderFacade: List<OrderItemEntity>
        
        Note over OrderFacade: OrderInfo.from(order, orderItems)
        OrderFacade-->>OrderController: OrderInfo
        OrderController-->>User: 200 OK
    end
```

## 13. 주문 취소

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderFacade
    participant OrderService
    participant ProductService
    participant PointService
    participant OrderRepository
    participant ProductRepository

    User->>OrderController: DELETE /api/v1/orders/1<br/>Header: X-USER-ID=testuser
    OrderController->>OrderFacade: cancelOrder(orderId=1, username="testuser")
    
    OrderFacade->>OrderService: getOrderById(orderId=1)
    OrderService->>OrderRepository: findById(orderId=1)
    
    alt 주문이 존재하지 않는 경우
        OrderRepository-->>OrderService: Optional.empty()
        OrderService-->>OrderFacade: CoreException(NOT_FOUND)
        OrderFacade-->>OrderController: CoreException
        OrderController-->>User: 404 Not Found
    else 주문이 존재하는 경우
        OrderRepository-->>OrderService: OrderEntity
        OrderService-->>OrderFacade: OrderEntity
        
        Note over OrderFacade: order.cancelOrder() [상태 변경]
        
        OrderFacade->>OrderService: getOrderItemsByOrderId(orderId=1)
        OrderService->>OrderRepository: findByOrderId(orderId=1)
        OrderRepository-->>OrderService: List<OrderItemEntity>
        OrderService-->>OrderFacade: List<OrderItemEntity>
        
        Note over OrderFacade: 주문 항목을 productId로 정렬 (데드락 방지)
        
        loop 각 주문 항목에 대해 재고 원복 (정렬된 순서)
            OrderFacade->>ProductService: restoreStock(productId, quantity) [비관적 락]
            ProductService->>ProductRepository: findByIdWithLock(productId)
            ProductRepository-->>ProductService: ProductEntity (locked)
            Note over ProductService: product.restoreStock(quantity)
            ProductService->>ProductRepository: save(product) [재고 복구]
            ProductRepository-->>ProductService: ProductEntity
            ProductService-->>OrderFacade: ProductEntity
        end
        
        OrderFacade->>PointService: charge(username, totalAmount)
        Note over PointService: user.chargePoint(amount)<br/>포인트 이력 생성 (CHARGE)
        PointService-->>OrderFacade: 환불 후 잔액
        
        OrderFacade-->>OrderController: OrderInfo (취소됨)
        OrderController-->>User: 200 OK
    end
```

### 🔒 주문 취소 원자성 보장

#### **트랜잭션 전략**
- **@Transactional 기반 원자성**: OrderFacade의 cancelOrder 메서드 전체가 하나의 트랜잭션
  - 주문 상태 변경, 재고 복구, 포인트 환불이 모두 성공하면 커밋
  - 중간에 예외 발생 시 자동 롤백
  
#### **데드락 방지 전략**
- **정렬된 락 획득**: 주문 항목을 productId 기준으로 정렬하여 재고 복구
  - 주문 생성 시와 동일한 순서로 락 획득
  - 원형 대기(circular wait) 방지
