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
    participant L as ProductLikeRepository

    Note over U: 유저가 상품 좋아요 요청
    U->>F: likeProduct(userId, productId)
    
    activate F
    Note over F: 상품 유효성 확인
    F->>P: getProductDetail(productId)
    activate P
        P->>L:findByProductId(productId)
        activate L
        L-->>P: Optional<Product>
        deactivate L
        P-->>F: ProductInfo
    deactivate P
    
    opt [상품 없음]
        F-->>U: HTTP 404 / ERR_상품없음
    end    
    opt [상품 판매 중지]
        F-->>U: HTTP 400 / ERR_상품이 판매 중지 되었습니다.
    end    
    
    %% 상품 정보 정상, 좋아요 처리 수행
    Note over F: 좋아요 처리 시작
    F->>S: likeProduct(userId, productId)
    activate S
    S->>L: insertIgnore(userId, productId)

    activate L
    alt 첫 좋아요(INSERT 성공)
        L-->>S: true (새로운 row 삽입)
        S-->>F: LikeResult(LIKED)
        F-->>U: "좋아요 완료 / HTTP 200"
    else 이미 좋아요 되어 있음(멱등)
        L-->>S: false (영향 없음)
        deactivate L
        S-->>F: LikeResult(ALREADY_LIKED)
        F-->>U: "이미 좋아요 되어 있음 HTTP 200"
    end
    deactivate S
    deactivate F
```


## 2. 상품 좋아요 취소 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant U as Client
    participant F as UserLikeProductFacade
    participant S as LikeService
    participant P as ProductService
    participant L as ProductLikeRepository
    
    Note over U: 유저가 좋아요 취소 요청
    U->>F: unlikeProduct(userId, productId)
    activate F
        Note over F: 상품 유효성 확인
        F->>P: getProductDetail(productId)
        activate P
            P->>L:findByProductId(productId)
            activate L
            L-->>P: Optional<Product>
            deactivate L
            P-->>F: ProductInfo
        deactivate P
    %% 상품정보 유효성 확인 결과 리턴
    opt [상품 없음]
        F-->>U: HTTP 404 / ERR_상품없음
    end
    opt [상품 판매 중지]
        F-->>U: HTTP 400 / ERR_상품이 판매 중지 되었습니다.
    end


    Note over F: 좋아요 취소 처리 시작
    F->>S: unlikeProduct(userId, productId)
    activate S
        S->>L: delete(userId, productId)

        activate L
        alt 기존 좋아요 존재(삭제됨)
            L-->>S: 1 row deleted
            S-->>F: LikeResult(UNLIKED)
            F-->>U: "좋아요 취소 완료 / HTTP 200"
        else 이미 취소 상태(멱등)
            L-->>S: 0 row deleted
            deactivate L
            S-->>F: LikeResult(ALREADY_UNLIKED, likeCountSame)
            deactivate S
            F-->>U: "이미 좋아요 취소 상태 / HTTP 200"
        end
    
    deactivate F

```

## 3. 주문 생성 전 재고 관련 선조회
- 1) 유저가 장바구니 이동
- 2) 선택한 상품 목록 조회시 재고 관련 선조회
- 3) 반복하며 재고 확인 결과 응답
```mermaid
sequenceDiagram
    participant U as User (Client)
    participant F as UserOrderProductFacade
    # participant US as UserService
    participant PS as ProductService
    # participant OS as OrderService
    # participant PG as PaymentGateway (외부 결제 시스템)

%% [1] 유저가 주문을 위해 상품목록 재고 조회
    U->>F: preOrder(orderLineList)
    activate F
%% [2] 상품 재고 검증
    loop 상품별
        F->>PS: hasEnoughStock(productId)
        activate PS
        alt 재고 부족
            PS-->>F: false
            F-->>F: orderLine 재고 부족으로 주문 불가 (in-memory) 상태 기록
        else 재고 충분
            PS-->>F: true
        end
    end
    deactivate PS
    deactivate F
    
    F-->>U: orderLineList
```


## 4. 주문 생성 및 결제 흐름 (재고 차감, 외부 결제 시스템 연동)
- **유저는 확인한 재고 있는 상품에 대해 결제 요청 주문 송신**

```mermaid
sequenceDiagram
    participant U as User (Client)
    participant F as UserOrderProductFacade
    participant US as UserService
    participant PS as ProductService
    participant OS as OrderService
    participant PG as PaymentGateway (외부 결제 시스템)

    U->>F: order(userId, orderLineList)
    activate F
%% [4] 주문 생성 및 저장
    F->>OS: placeOrder(userId, orderLineList)
    activate OS
    OS->>OS: OrderModel 생성 (status=CREATED)
    OS->>OS: OrderItemModel 추가 및 합산
    OS->>OS: save(OrderModel)
    OS-->>F: OrderModel(orderId, status=CREATED)
    deactivate OS   
    
%% [5] 재고 차감
    loop 상품별 재고차감
        F->>PS: decreaseStockAtomically(productId, qty)
        activate PS
        alt 차감 성공
            PS-->>F: OK
        else 재고 부족
            PS-->>F: 실패
            %% F-->>U: 주문 실패 - 일부 상품 품절
            F->>OS: 해당 OrderItem 실패로 update 및 청구 금액 제외
            activate OS
            OS->>OS: OrderItem 실패로 update
            OS->>OS: Order 실패금액 갱신
            OS-->>F: OK
            deactivate OS
        end
        deactivate PS
    end

%% [6] 외부 결제 호출
    Note over F: 재고 정상 합산 금액에 대해 외부결제 연동
    %% U->>F: pay(userId, orderId)
    F->>PG: payWithPG(orderId, totalAmount)
    activate PG
    alt 결제 성공
        PG-->>F: SUCCESS
        F->>OS: pay(orderId, paymentInfo)
        OS-->>F: status=PAID
        F-->>U: 결제 성공, 주문 완료
    else 결제 실패
        PG-->>F: FAIL
        deactivate PG
        
        Note over F: 보상 처리 시작
        %% F->>US: increasePoint(userId, usedPoint)
        F->>PS: increaseStock(productId, qty)
        F->>OS: cancel(orderId)
        OS-->>F: status=CANCELLED
        F-->>U: 결제 실패, 재고 복원 완료
    end
    deactivate F
```

## 5. 주문 생성 및 결제 흐름(내부 포인트로 결제)
- 내부결제 방식
```mermaid
sequenceDiagram
    participant U as User (Client)
    participant F as UserOrderProductFacade
    participant US as UserService
    participant PS as ProductService
    participant OS as OrderService
    %% participant PG as PaymentGateway (외부 결제 시스템)

    U->>F: order(userId, orderLineList)
    activate F
%% [4] 주문 생성 및 저장
    F->>OS: placeOrder(userId, orderLineList)
    activate OS
    OS->>OS: OrderModel 생성 (status=CREATED)
    OS->>OS: OrderItemModel 추가 및 합산
    OS->>OS: save(OrderModel)
    OS-->>F: OrderModel(orderId, status=CREATED)
    deactivate OS   
    
%% [5] 재고 차감
    loop 상품별 재고차감
        F->>PS: decreaseStockAtomically(productId, qty)
        activate PS
        alt 차감 성공
            PS-->>F: OK
        else 재고 부족
            PS-->>F: 실패
            %% F-->>U: 주문 실패 - 일부 상품 품절
            F->>OS: 해당 OrderItem 실패로 update 및 청구 금액 제외
            activate OS
            OS->>OS: OrderItem 실패로 update
            OS->>OS: Order 실패금액 갱신
            OS-->>F: OK
            deactivate OS
        end
        deactivate PS
    end

%% [6] 내부 결제 호출
    Note over F: 재고 정상 합산 금액에 대해 유저포인트 차감
    F->>US: pay(userId, orderId)
    activate US
    alt 결제 성공
        US-->>F: SUCCESS
        deactivate US
        F->>OS: pay(orderId, paymentInfo)
        activate OS
        OS-->>F: status=PAID
        deactivate OS
        F-->>U: 결제 성공, 주문 완료
    else 결제 실패
        activate US
        US-->>F: FAIL
        deactivate US
        Note over F: 보상 처리 시작
        F->>US: increasePoint(userId, usedPoint)
        activate US
        US-->>F: SUCCESS
        deactivate US
        F->>PS: increaseStock(productId, qty)
        activate PS
        PS-->>F: SUCCESS
        deactivate PS 
        F->>OS: cancel(orderId)
        activate OS
        OS-->>F: status=CANCELLED
        deactivate OS
        F-->>U: 결제 실패, 유저포인트, 재고 복원 완료
        
    end
    deactivate F
```
