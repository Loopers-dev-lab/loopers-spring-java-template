sequenceDiagram
    autonumber
    actor Client
    participant C as OrderController
    participant F as OrderFacade
    participant S as OrderService
    participant R as Repository

    Client->>+C: POST /api/v1/orders (X-USER-ID, OrderDto.OrderRequest)
    Note left of C: 주문 요청 (유저ID, 상품ID, 개수)
    Note left of C: JSON<br/>"items": [<br/>{ "productId": 1, "quantity": 2 },<br/>{ "productId": 3, "quantity": 1 }<br/>]<br/>

    C->>+F: createOrderAndPayment(String userId, OrderInfo orderInfo)
    F->>+S: createOrder(String userId, Order order)
    S->>+R: orderRepository.saveOrder(Order order)
    Note right of S: 1. 주문 객체 및 주문 상품 객체 저장
    R-->>-S: Order savedOrder

    %% 2. 재고 차감 로직 시작

    rect rgb(20,20,0)
        opt 재고 부족하면 (Stock Check)
            S->>+R: checkAndDeductStock(Long productId, Long quantity)
            Note right of S: 재고 확인 및 차감 트랜잭션 시작
            R-->>-S: false
            S-->>-F: throw CoreException(ErrorType.BAD_REQUEST)
            F-->>-C: throw CoreException(ErrorType.BAD_REQUEST)
            C-->>-Client: 400 Bad Request ("재고 없음")
        end
    end

    %% 3. 결제 (포인트 차감) 로직 시작

    S->>+R: getPoint(String userId)
    Note right of S: 회원의 포인트 확인
    R-->>-S: Point point
    Note right of S: 결제 금액과 포인트 비교

    rect rgb(20,20,0)
        opt 포인트 부족하면 (Long point)
            S-->>F: throw CoreException(ErrorType.BAD_REQUEST)
            F-->>C: throw CoreException(ErrorType.BAD_REQUEST)
            C-->>Client: 400 Bad Request ("재고 없음")
        end
    end
    
    S->>+R: calculateAndDeductPoints(userId, totalAmount)
    Note right of S: 총 결제 금액 계산 및 포인트 차감 시도
    R-->>-S: isPaymentSuccessful