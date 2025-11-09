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
    participant LikeService
    participant UserService
    participant ProductService
    participant LikeRepository

    User->>LikeController: POST /api/v1/like/products/1<br/>Header: X-USER-ID=123
    LikeController->>LikeService: addLike(userId=123, productId=1)
    
    LikeService->>UserService: validateUserExists(userId=123)
    alt 사용자가 존재하지 않는 경우
        UserService-->>LikeService: UserNotFoundException
        LikeService-->>LikeController: UserNotFoundException
        LikeController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>LikeService: User validated
        LikeService->>ProductService: validateProductExists(productId=1)
        alt 상품이 존재하지 않는 경우
            ProductService-->>LikeService: ProductNotFoundException
            LikeService-->>LikeController: ProductNotFoundException
            LikeController-->>User: 404 Not Found
        else 상품이 존재하는 경우
            ProductService-->>LikeService: Product validated
            LikeService->>LikeRepository: upsert(userId=123, productId=1)
            LikeRepository-->>LikeService: LikeResult()
            LikeService->>ProductService: increaseLikeCount()
            ProductService-->>LikeService: LikeResult()
            LikeService-->>LikeController: LikeResponse(action)
        end
    end
    
    LikeController-->>User: 200 OK
```

## 6. 좋아요 취소

```mermaid
sequenceDiagram
    participant User
    participant LikeController
    participant LikeService
    participant UserService
    participant LikeRepository

    User->>LikeController: DELETE /api/v1/like/products/1<br/>Header: X-USER-ID=123
    LikeController->>LikeService: removeLike(userId=123, productId=1)
    
    LikeService->>UserService: validateUserExists(userId=123)
    alt 사용자가 존재하지 않는 경우
        UserService-->>LikeService: UserNotFoundException
        LikeService-->>LikeController: UserNotFoundException
        LikeController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>LikeService: User validated
        LikeService->>LikeRepository: deleteIfExists(userId=123, productId=1)
        LikeRepository-->>LikeService: UnlikeResult(action=REMOVED|ALREADY_REMOVED)
        LikeService->>ProductService: decreaseLikeCount()
        ProductService-->>LikeService: LikeResult()
        LikeService-->>LikeController: UnlikeResponse(action)
    end
    
    LikeController-->>User: 200 OK
```

## 7. 내가 좋아요한 상품 목록 조회

```mermaid
sequenceDiagram
    participant User
    participant LikeController
    participant LikeService
    participant UserService
    participant LikeRepository

    User->>LikeController: GET /api/v1/like/products?page=0&size=20<br/>Header: X-USER-ID=123
    LikeController->>LikeService: getUserLikedProducts(userId=123, pageable)
    
    LikeService->>UserService: validateUserExists(userId=123)
    alt 사용자가 존재하지 않는 경우
        UserService-->>LikeService: UserNotFoundException
        LikeService-->>LikeController: UserNotFoundException
        LikeController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>LikeService: User validated
        LikeService->>LikeRepository: findUserLikedProductsWithDetails(userId=123, pageable)
        
        alt 좋아요한 상품이 있는 경우
            LikeRepository-->>LikeService: Page<LikedProductInfo>
            LikeService-->>LikeController: LikedProductListResponse(totalElements=12, content=[...])
            LikeController-->>User: 200 OK
        else 좋아요한 상품이 없는 경우
            LikeRepository-->>LikeService: Page.empty()
            LikeService-->>LikeController: LikedProductListResponse(totalElements=0, content=[])
            LikeController-->>User: 200 OK
        end
    end
```

## 8. 주문 요청

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderService
    participant UserService
    participant ProductService
    participant PointService
    participant OrderRepository
    

    User->>OrderController: POST /api/v1/orders<br/>Header: X-USER-ID=123<br/>Body: {"items":[{"productId":1,"quantity":2}]}
    OrderController->>OrderService: createOrder(userId=123, orderRequest)
    
    OrderService->>UserService: validateUserExists(userId=123)
    alt 사용자가 존재하지 않는 경우
        UserService-->>OrderService: UserNotFoundException
        OrderService-->>OrderController: UserNotFoundException
        OrderController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>OrderService: User validated
        
        loop 각 주문 상품 검증 및 재고 예약
            OrderService->>ProductService: validateAndReserveStock(productId, quantity)
            alt 상품이 존재하지 않거나 재고 부족
                ProductService-->>OrderService: ProductException
                Note over OrderService: 이미 예약된 재고 해제 (보상 트랜잭션)
                OrderService-->>OrderController: ProductException
                OrderController-->>User: 400 Bad Request
            else 재고 예약 성공
                ProductService-->>OrderService: Stock reserved
            end
        end
        
        OrderService->>PointService: deductPoints(userId=123, totalAmount)
        alt 포인트가 부족한 경우
            PointService-->>OrderService: InsufficientPointsException
            Note over OrderService: 예약된 모든 재고 해제 (보상 트랜잭션)
            OrderService-->>OrderController: InsufficientPointsException
            OrderController-->>User: 400 Bad Request
        else 포인트 차감 성공
            PointService-->>OrderService: Points deducted
            OrderService->>OrderRepository: createOrderWithItems(orderData)
            alt 주문 생성 실패
                OrderRepository-->>OrderService: DataIntegrityException
                Note over OrderService: 포인트 복구 + 재고 해제 (보상 트랜잭션)
                OrderService-->>OrderController: OrderCreationException
                OrderController-->>User: 500 Internal Server Error
            else 주문 생성 성공
                OrderRepository-->>OrderService: OrderEntity created
                OrderService-->>OrderController: OrderResponse
                OrderController-->>User: 201 Created
            end
        end
    end
```

### 🔒 주문 처리 원자성 보장

#### **트랜잭션 전략**
- **보상 트랜잭션**: 실패 시점에 따른 롤백 전략
  - 재고 예약 실패 → 이미 예약된 재고 즉시 해제
  - 포인트 차감 실패 → 예약된 모든 재고 해제  
  - 주문 생성 실패 → 포인트 복구 + 재고 해제

## 9. 사용자의 주문 목록 조회

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderService
    participant UserService
    participant OrderRepository

    User->>OrderController: GET /api/v1/orders?page=0&size=20<br/>Header: X-USER-ID=123
    OrderController->>OrderService: getUserOrders(userId=123, pageable)
    
    OrderService->>UserService: validateUserExists(userId=123)
    alt 사용자가 존재하지 않는 경우
        UserService-->>OrderService: UserNotFoundException
        OrderService-->>OrderController: UserNotFoundException
        OrderController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>OrderService: User validated
        OrderService->>OrderRepository: findByUserId(userId=123, pageable)
        
        alt 주문 내역이 존재하는 경우
            OrderRepository-->>OrderService: Page<OrderEntity>
            OrderService-->>OrderController: OrderListResponse(totalElements=8, content=[...])
            OrderController-->>User: 200 OK
        else 주문 내역이 없는 경우
            OrderRepository-->>OrderService: Page.empty()
            OrderService-->>OrderController: OrderListResponse(totalElements=0, content=[])
            OrderController-->>User: 200 OK
        end
    end
```

## 10. 주문 상세 조회

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderService
    participant UserService
    participant OrderRepository

    User->>OrderController: GET /api/v1/orders/1<br/>Header: X-USER-ID=123
    OrderController->>OrderService: getOrderDetail(orderId=1, userId=123)
    
    OrderService->>UserService: validateUserExists(userId=123)
    alt 사용자가 존재하지 않는 경우
        UserService-->>OrderService: UserNotFoundException
        OrderService-->>OrderController: UserNotFoundException
        OrderController-->>User: 404 Not Found
    else 사용자가 존재하는 경우
        UserService-->>OrderService: User validated
        OrderService->>OrderRepository: findOrderDetailByIdAndUserId(orderId=1, userId=123)
        
        alt 주문이 존재하는 경우
            OrderRepository-->>OrderService: OrderDetailInfo(order, orderItems, products)
            OrderService-->>OrderController: OrderDetailResponse
            OrderController-->>User: 200 OK
        else 주문이 존재하지 않는 경우
            OrderRepository-->>OrderService: Optional.empty()
            OrderService-->>OrderController: OrderNotFoundException
            OrderController-->>User: 404 Not Found
        end
    end
```
