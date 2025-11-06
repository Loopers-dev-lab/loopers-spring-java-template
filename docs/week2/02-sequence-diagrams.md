# 02-시퀀스 다이어그램

## 작업 목록
- [x] 1. 상품 좋아요 등록/취소 (멱등 동작)
- [] 2. 주문 생성 및 결제 흐름 (재고 차감, 포인트 차감, 외부 시스템 연동)


## 1. 상품 좋아요 등록/취소 시퀀스다이어그램
- 퍼사드 클래스와 그 하위 서비스들의 협력으로 풀어보려 하였습니다.

```mermaid
sequenceDiagram
    participant U as Client
    participant F as UserLikeProductFacade
    participant S as LikeService
    participant P as ProductService
    participant L as ProductLikeService

    Note over U: 유저가 상품 좋아요 요청
    U->>F: likeProduct(userId, productId)

    Note over F: 상품 유효성 확인
    F->>P: getProductDetail(productId)
    P->>F: Product

    Note over F: 좋아요 처리 시작
    F->>S: likeProduct(userId, productId)
    S->>L: insertIgnore(userId, productId)

    alt 첫 좋아요(INSERT 성공)
        L-->>S: true (새로운 row 삽입)
        S-->>F: LikeResult(LIKED)
        F-->>U: "좋아요 완료 / HTTP 200"
    else 이미 좋아요 되어 있음(멱등)
        L-->>S: false (영향 없음)
        S-->>F: LikeResult(ALREADY_LIKED)
        F-->>U: "이미 좋아요 되어 있음 HTTP 200"
    end

    Note over U: 유저가 좋아요 취소 요청
    U->>F: unlikeProduct(userId, productId)

    Note over F: 상품 유효성 확인
    F->>P: getProductDetail(productId)
    P->>F: Product

    Note over F: 좋아요 취소 처리 시작
    F->>S: unlikeProduct(userId, productId)
    S->>L: delete(userId, productId)

    alt 기존 좋아요 존재(삭제됨)
        L-->>S: 1 row deleted
        S-->>F: LikeResult(UNLIKED)
        F-->>U: "좋아요 취소 완료 / HTTP 200"
    else 이미 취소 상태(멱등)
        L-->>S: 0 row deleted
        S-->>F: LikeResult(ALREADY_UNLIKED, likeCountSame)
        F-->>U: "이미 좋아요 취소 상태 / HTTP 200"
    end


```


## 2. 주문 생성 및 결제 흐름 (재고 차감, 포인트 차감, 외부 시스템 연동)


