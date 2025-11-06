유저 정보 조회
``` mermaid
sequenceDiagram
	participant Client
	participant UserController
	participant UserFacade 
	participant UserService
	participant UserRepository

    Client->>UserController: GET /api/users/me Header: X-USER-ID
    alt X-USER-ID 존재
        UserController->>UserFacade: get(userId)
        UserFacade->>UserService: get(userId)
        UserService->>UserRepository: find(userId)
        UserRepository-->>UserService: User or null
        UserService-->>UserFacade: User or NotFound
        UserFacade-->>UserController: UserInfo
        UserController-->>Client: 200 or 404
    else X-USER-ID 없음
        UserController-->>Client: 400 Bad Request
    end
```

브랜드 정보 조회
``` mermaid
sequenceDiagram
	participant Client
	participant BrandController
	participant BrandFacade 
	participant BrandService
	participant BrandRepository
	
	Client->>BrandController: Get /api/brands/{brandId}
	BrandController->>BrandFacade: get(brandId)
	BrandFacade->>BrandService: get(brandId)
	BrandService->>BrandRepository: find(brandId)
	BrandRepository->>BrandService: Brand or NotFound
	BrandService-->>BrandFacade: BrandInfo
    BrandController-->>Client: 200 or 404
```

상품 목록 조회
``` mermaid
sequenceDiagram
	participant Client
	participant ProductController
	participant ProductFacade 
	participant ProductService
	participant ProductRepository
	
	Client->>ProductController: Get /api/products?brandId=1&sort=latest&page=0&size=20
	ProductController->>ProductFacade: getList(searchRequest)
	ProductFacade->>ProductService: getList(searchRequest)
	ProductService->>ProductRepository: findList(searchRequest)
	ProductRepository->>ProductService: ProductList or NotFound
	ProductService-->>ProductFacade: ProductListInfo
    ProductController-->>Client: 200 or 404
```

상품 상세 조회
``` mermaid
sequenceDiagram
	participant Client
	participant ProductController
	participant ProductFacade 
	participant ProductService
	participant ProductRepository
	
	Client->>ProductController: Get /api/products/{productId}
	ProductController->>ProductFacade: get(productId)
	ProductFacade->>ProductService: get(productId)
	ProductService->>ProductRepository: find(productId)
	ProductRepository->>ProductService: Product or NotFound
	ProductService-->>ProductFacade: ProductInfo
    ProductController-->>Client: 200 or 404
```

좋아요 등록
``` mermaid
sequenceDiagram
	participant Client
	participant LikeController
	participant LikeFacade 
	participant LikeService
	participant LikeRepository
	
	Client->>LikeController: Post /api/like/products/{productId} Header: X-USER-ID
	alt X-USER-ID 존재
        LikeController->>LikeFacade: like(productId, userId)
        Note right of LikeFacade: 상품과 유저 정보 조회 없으면 404 Not Found
        LikeFacade->>LikeService: like(productId, userId)
        LikeService->>LikeRepository: exists(productId, userId)
        alt 해당 상품에 유저의 좋아요가 이미 존재하는 경우
            LikeService->>LikeFacade: Success
            LikeFacade->>LikeController: Success
            LikeController->>Client: 200
        else 
            LikeService->>LikeRepository: save(Like)
            LikeService->>LikeFacade: Success
            LikeFacade->>LikeController: Success
            LikeController->>Client: 200
        end
	else X-USER-ID 없음
        LikeController-->>Client: 400 Bad Request
    end
```

좋아요 취소
``` mermaid
sequenceDiagram
	participant Client
	participant LikeController
	participant LikeFacade 
	participant LikeService
	participant LikeRepository
	
	Client->>LikeController: Delete /api/like/products/{productId} Header: X-USER-ID
	alt X-USER-ID 존재
        LikeController->>LikeFacade: unLike(productId, userId)
        Note right of LikeFacade: 상품과 유저 정보 조회 없으면 404 Not Found
        LikeFacade->>LikeService: unLike(productId, userId)
        LikeService->>LikeRepository: find(productId, userId)
        alt 해당 상품에 유저의 좋아요가 존재하는 경우
            LikeService->>LikeRepository: delete(Like)
            LikeService->>LikeRepository: Success
            LikeFacade->>LikeController: Success
            LikeController->>Client: 200
        else 존재하지 않는 경우 
            LikeService->>LikeFacade: Success
            LikeFacade->>LikeController: Success
            LikeController->>Client: 200
        end
	else X-USER-ID 없음
        LikeController-->>Client: 400 Bad Request
    end
```

좋아요 목록 조회
``` mermaid
sequenceDiagram
	participant Client
	participant LikeController
	participant LikeFacade 
	participant LikeService
	participant LikeRepository
	
	Client->>LikeController: Get /api/like/products Header: X-USER-ID
	alt X-USER-ID 존재
        LikeController->>LikeFacade: getList(userId)
        Note right of LikeFacade: 유저 정보 조회 없으면 404 Not Found
        LikeFacade->>LikeService: getList(userId)
        LikeService->>LikeRepository: find(userId)
        LikeRepostitory->>LikeService: LikeProductList or NotFound
        LikeService->>LikeController: LikeProductListInfo
        LikeController->>Client: 200
    else X-USER-ID 없음
        LikeController-->>Client: 400 Bad Request
    end
```

주문 생성
``` mermaid
sequenceDiagram
	participant Client
	participant OrderController
	participant OrderFacade 
	participant StockFacade
	participant PaymentFacade
	participant OrderService
	participant OrderRepository
	participant ExternalSystem
	
	Client->>OrderController: Post /api/orders Header: X-USER-ID body: {productId, quantity}
	alt X-USER-ID 존재
        OrderController->>OrderFacade: createOrder(orderCreateRequest, userId)
        Note right of OrderFacade: 유저, 상품 정보 조회 없으면 404 Not Found
        OrderFacade->>StockFacade: reserveStock(productId, quantity)
        Note right of StockFacade: 재고 감소시 0 미만이면 400 Bad Request
        OrderFacade->>OrderService: createOrder(productId, userId, totalPrice)
        OrderService->>OrderRepository: save(Order)
        OrderRepository->>OrderService: Order
        OrderFacade->>PaymentFacade: createReadyPayment(orderId, productId, userId, totalPrice)
        OrderService->>OrderFacade: Order
        OrderFacade->>ExternalSystem: createOrder(Order)
        OrderFacade->>OrderController: OrderInfo
        OrderController->>Client: 200
    else X-USER-ID 없음
        OrderController-->>Client: 400 Bad Request
    end
```

결제 완료
``` mermaid
sequenceDiagram
	participant Client
	participant PaymentController
	participant PaymentFacade
	participant OrderFacade 
	participant StockFacade
	participant PointFacade
	participant PaymentService
	participant PaymentRepository
	participant ExternalSystem
	
	Client->>PaymentController: Post /api/payments/{paymentId} Header: X-USER-ID
	alt X-USER-ID 존재
        PaymentController->>PaymentFacade: paidPayment(paymentId, userId)
        Note right of PaymentFacade: 유저, 상품, 주문 정보 조회 없으면 404 Not Found
        PaymentFacade->>StockFacade: decreaseStock(productId, quantity)
        PaymentFacade->>PointFacade: deductPoint(userId, totalPrice)
        Note right of PointFacade: 포인트 차감시 0 미만이면 400 Bad Request
        PaymentFacade->>PaymentService: paidPayment(orderId, productId, userId, totalPrice)
        PaymentService->>PaymentRepository: paidPayment(orderId, productId, userId, totalPrice)
        PaymentRepository->>PaymentService: Payment
        PaymentService->>PaymentFacade: Payment
        PaymentFacade->>OrderFacade: paidOrder(orderId)
        PaymentFacade->>ExternalSystem: paidPayment(Order)
        PaymentFacade->>PaymentController: PaymentInfo
        PaymentController->>Client: 200
    else X-USER-ID 없음
        PaymentController-->>Client: 400 Bad Request
    end
```

주문 상세 조회
``` mermaid
sequenceDiagram
	participant Client
	participant OrderController
	participant OrderFacade 
	participant OrderService
	participant OrderRepository
	
	Client->>OrderController: Get /api/orders/{orderId} Header: X-USER-ID
        alt X-USER-ID 존재
        OrderController->>OrderFacade: get(orderId)
        OrderFacade->>OrderService: get(orderId)
        OrderService->>OrderRepository: find(orderId)
        OrderRepository->>OrderService: Order or NotFound
        OrderService-->>OrderFacade: OrderInfo
        OrderController-->>Client: 200 or 404
    else X-USER-ID 없음
        OrderController-->>Client: 400 Bad Request
    end
```
