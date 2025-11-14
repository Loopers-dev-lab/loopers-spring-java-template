---
title: LikeProduct API Sequence Diagram
---
sequenceDiagram
    autonumber
    actor Client
    participant C as LikeController
    participant F as LikeFacade
    participant S as LikeService
    participant R as LikeRepository

    %% --- "좋아요" 상품 목록 가져오기 로직 시작 ---
    Client->>+C: GET /api/v1/like/products (Header: X-USER-ID, ProductDto.SearchConditionRequest req)
    Note left of C: "좋아요" 상품 목록 조회 API 호출 (유저ID, 검색 조건 포함)
    Note left of C: Query Parameters<br/>--------------------<br/>"categoryId":123 or null<br/>"brandId": 123 or null,<br/>"sort": "latest",<br/>"pageIndex": 0,<br/>"pageSize": 20<br/>---------------------

    C->>+F: getLikedProducts(String userId, ProductInfo info)
    F->>+S: getLikedProducts(String userId, ProductInfo info)

    S->>+R: findLikedProductsByConditions(<br/>Long userId,<br/>LikeSearchCondition condition,<br/> Pageable pageable)<br/>)
    Note right of S: QueryDSL 의 JPAQueryFactory 를 통해 동적 쿼리 활용<br/>상품 필터 및 페이징 조건을 적용하여 조회

    R-->>-S: List<Product> products
    
    S->>S: List<Product> -> List<ProductInfo>
    S-->>-F: List<ProductInfo>
    F-->>-C: List<ProductInfo>

    C-->>-Client: 200 OK (List<ProductDto.InfoResponse> JSON 응답)
    Note left of C: "meta": { "result": "SUCCESS", "errorCode": null, "message": null },<br/>"data": {<br/>"content": [<br/>{<br/>"id": 10,<br/>"brand": { "id": 1, "name": "Loopers" },<br/>"name": "에센셜 코튼 티셔츠",<br/>"description": "부드러운 촉감의 데일리 티셔츠",<br/>"status": "ACTIVE",<br/>"price": 19900,<br/>"isVisible": true,<br/> "isSellable": true,<br/>"stockQuantity": 42,<br/>"likesCount": 128<br/>},<br/>{<br/>"id": 11,<br/>"brand": { "id": 1, "name": "Loopers" },<br/>"name": "라이트 웨이트 조거 팬츠",<br/>"description": "가벼운 착용감의 조거 팬츠",<br/>"status": "ACTIVE",<br/>"price": 39900,<br/>"isVisible": true,<br/>"isSellable": true,<br/>"stockQuantity": 17,<br/>"likesCount": 64<br/>}<br/>],<br/>"page": {<br/>"page": 0,<br/>"size": 20,<br/>"totalElements": 312,<br/>"totalPages": 16,<br/>"sort": "latest"<br/>}
