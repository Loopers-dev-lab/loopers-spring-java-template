
### 1. 상품 목록/상세 조회 및 브랜드 조회
```mermaid
sequenceDiagram
    participant User
    participant ProductController
    participant ProductReader

    %% 상품 목록 조회 %%
    User->>ProductController: GET /api/v1/products?sort=likes_desc
    ProductController->>ProductReader: getProducts(sort)
    ProductReader-->>ProductController: productList
    ProductController-->>User: productList

    %% 상품 상세 조회 %%
    User->>ProductController: GET /api/v1/products/{productId}
    ProductController->>ProductReader: getProduct(productId)
    ProductReader-->>ProductController: product
    ProductController-->>User: product

    participant BrandController
    participant BrandReader

    %% 브랜드 조회 %%
    User->>BrandController: GET /api/v1/brands/{brandId}
    BrandController->>BrandReader: getBrand({brandId})
    BrandReader-->>BrandController: brand
    BrandController-->>User: brand
```
### 2. 주문 생성

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderService
    participant ProductReader
    participant PointReader
    participant ProductService
    participant PointService
    participant OrderRepository

    User->>OrderController: POST /api/v1/orders (body: {productId, quantity})
    OrderController->>OrderService: setOrder(userId, {productId, quantity})

    %% 조회 및 검증 %%
    OrderService->>ProductReader: getProduct({productId})
    ProductReader -->>OrderService: product(현재가격, 재고)

    OrderService->>PointReader: getPoint(userId)
    PointReader-->>OrderService: point (현재 잔여 포인트)

    %% 재고 및 포인트 차감  %%
    critical Transaction Block
        OrderService ->>ProductService: decreaseStock(productId, quantity)
        OrderService ->>PointService: deductPoint(productPrice * quantity)
        OrderService->> OrderRepository: save(new Order(...))
        OrderRepository-->>OrderService: orderInfo
    end

    %% 응답 %%
    OrderService -->> OrderController:orderInfo
    OrderController-->> User:orderInfo
    ```