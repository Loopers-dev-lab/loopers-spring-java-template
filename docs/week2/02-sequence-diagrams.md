# 시퀀스 다이어그램 최소 2개 이상 (Mermaid 기반 작성 권장)

[1.상품목록 조회]
sequenceDiagram  
actor U as User  
participant C as ProductController  
participant S as ProductService  
participant R as ProductRepository  
U->>C: GET /api/v1/products  
C->>S: getProducts(condition, pageRequest)
alt 검색 조건이 있을 경우  
S->>R : findByCondition(condition, pageRequest)
else 검색 조건 없을 경우  
S->>R : findAll(pageRequest)
end alt 데이터가 없을 경우  
R-->>S: empty  
S-->>U: "등록된 상품이 없습니다"
end

[2.상품상세 조회]
sequenceDiagram  
actor U as User  
participant C as ProductController  
participant S as ProductService  
participant R as ProductRepository  
U->>C: GET /api/v1/products/{productId}  
C->>S: getProduct(productId)
S->>R : findPoduct(productId)
alt 데이터가 없을 경우  
R-->>S: empty  
S-->>U: "등록된 상품정보를 찾을수 없습니다."
end
[3.브랜드 조회]
sequenceDiagram  
actor U as User  
participant C as BrandController  
participant S as BrandService  
participant R as BrandRepository  
U->>C: GET /api/v1/brands/{brandId}  
C->>S: getBrand(brandId)
S->>R : findBrand(brandId)
alt 데이터가 없을 경우  
R-->>S: empty  
S-->>U: "등록된 브랜드정보를 찾을수 없습니다."
end

[4-1.좋아요 등록]
sequenceDiagram  
actor U as User  
participant C as LikeController  
participant S as LikeService  
participant R as LikeRepository  
U->>C: POST /api/v1/like/products/{productId}  
alt X-USER-ID 가 없을 경우  
C-->>U: "로그인이 필요합니다."
end  
C->>S: saveLike(userId,productId)
S->>R: findLike(userId, productId)
alt 좋아요가 없을 경우  
S->>R : saveLike(like)
end

[4-2.좋아요 취소]
sequenceDiagram  
actor U as User  
participant C as LikeController  
participant S as LikeService  
participant R as LikeRepository  
U->>C: DELETE /api/v1/like/products/{productId}  
alt X-USER-ID 가 없을 경우  
C-->>U: "로그인이 필요합니다."
end  
C->>S: deleteLike(userId,productId)
S->>R : deleteLike(like)

[5. 주문 생성 및 결재]
sequenceDiagram  
actor U as User  
participant C as OrderController  
participant S as OrderService  
participant R as OrderRepository  
U->>C: POST /api/v1/orders  
alt X-USER-ID 가 없을 경우  
C-->>U: "로그인이 필요합니다."
end  
C->>S: saveOrder(order)
loop  
S->>R : findProduct(product)
end  
alt 재고가 없을 경우  
S-->>U: "품절된 상품입니다."
else 재고가 부족할 경우  
S-->>U: "{재고 수량} 이상 주문 불가한 상품입니다."
else 재고가 있을 경우  
S-->>R: allocateProducts(products)
end  
S->>R : findUser(user)
alt 포인트가 부족할 경우  
S-->>R: releaseProducts(products)
S-->>U: "포인트가 부족합니다."
end  
S->>R : payPoint(user)
S->>R : commitStock(products)
S->>R : saveOrder(order)
S->>R : saveOrderItem(orderItem)
