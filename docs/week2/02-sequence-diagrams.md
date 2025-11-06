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

- 현재는 유저가 장바구니 이동 / 선택한 상품 조회 / 결제 누르고 
- 반복하며 재고 확인 후 다시 결제 요청을 보내도록 함
- (고민) 유저가 장바구니로 이동 / 선택한 상품들 조회하려고 할때 재고상태를 미리 체크할 수도 있지 않을까?

```mermaid
sequenceDiagram
    participant U as User (Client)
    participant F as UserOrderProductFacade
    participant US as UserService
    participant PS as ProductService
    participant OS as OrderService
    participant PG as PaymentGateway (외부 결제 시스템)

%% [1] 유저가 주문 요청
    U->>F: order(userId, orderLineList)

%% [2] 유저 검증 및 포인트 확인
    F->>US: findByUserId(userId)
    US-->>F: UserModel(userId, point)
    F->>US: decreasePoint(userId, usedPoint)
    US-->>F: OK

%% [3] 상품 재고 검증
    loop 상품별
        F->>PS: hasEnoughStock(productId)
        alt 재고 부족
            PS-->>F: false
            F-->>U: 재고 부족으로 주문 불가
        else 재고 충분
            PS-->>F: true
        end
    end

%% [4] 주문 생성 및 저장
    F->>OS: placeOrder(userId, orderLineList)
    OS->>OS: OrderModel 생성 (status=CREATED)
    OS->>OS: OrderItemModel 추가 및 합산
    OS->>OS: save(OrderModel)
    OS-->>F: OrderModel(orderId, status=CREATED)

%% [5] 재고 차감
    loop 상품별
        F->>PS: decreaseStockAtomically(productId, qty)
        alt 차감 성공
            PS-->>F: OK
        else 재고 부족
            PS-->>F: 실패
            F-->>U: 주문 실패 - 일부 상품 품절
        end
    end

%% [6] 외부 결제 호출
    Note over U:유저가 구매가능한 상품에 대해 주문 결제 요청
    U->>F: pay(userId, orderId)
    F->>PG: pay(orderId, totalAmount)
    alt 결제 성공
        PG-->>F: SUCCESS
        F->>OS: pay(orderId, paymentInfo)
        OS-->>F: status=PAID
        F-->>U: 결제 성공, 주문 완료
    else 결제 실패
        PG-->>F: FAIL
        Note over F: 보상 처리 시작
        F->>US: increasePoint(userId, usedPoint)
        F->>PS: increaseStock(productId, qty)
        F->>OS: cancel(orderId)
        OS-->>F: status=CANCELLED
        F-->>U: 결제 실패, 재고/포인트 복원
    end
```
