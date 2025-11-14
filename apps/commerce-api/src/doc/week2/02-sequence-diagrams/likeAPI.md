---
title: Like API Sequence Diagram
---
sequenceDiagram
    autonumber
    actor Client
    participant C as LikeController
    participant F as LikeFacade
    participant S as LikeService
    participant R as LikeRepository

    %% --- 상품 좋아요 등록 로직 시작 ---
    Client->>+C: POST /api/v1/like/products/{productId} (Header: X-USER-ID)
    Note left of C: Product 좋아요 등록 API 호출 (유저ID, 상품ID)
    Note left of C: Path Variable: "productId":123<br/>Header:"userId":123

    C->>+F: saveProductLike(String userId, Long productId)
    F->>+S: saveProductLike(String userId, Long productId)

    S->>+R: save(Like likeEntity)
    Note right of S: 중복 등록 시 덮어씀으로 데이터 무결점 확보, 멱등성 실천
    
    S-->>-F:
    F-->>-C:
    
    C-->>-Client: 200 OK
    Note left of C: "meta": { "result": "SUCCESS", "errorCode": null, "message": null }

    %% --- 상품 좋아요 취소 로직 시작 ---
    Client->>+C: DELETE /api/v1/like/products/{productId} (Header: X-USER-ID)
    Note left of C: Product 좋아요 취소 API 호출 (유저ID, 상품ID)
    Note left of C: Path Variable: "productId":123<br/>Header:"userId":123

    C->>+F: deleteProductLike(String userId, Long productId)
    F->>+S: deleteProductLike(String userId, Long productId)

    S->>+R: deleteByUserIdAndTargetId(String userId, Long productId)
    Note right of S: 회원과 상품 ID에 해당하는 Like 만 삭제함으로<br/>중복 삭제 시 아무런 일도 일어나지 않음, 멱등성 실천

    S-->>-F:
    F-->>-C:
    
    C-->>-Client: 200 OK
    Note left of C: "meta": { "result": "SUCCESS", "errorCode": null, "message": null }