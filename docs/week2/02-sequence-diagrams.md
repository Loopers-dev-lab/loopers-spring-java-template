# 시퀀스 다이어그램

## 상품 목록 조회
~~~mermaid
sequenceDiagram
    participant C as 클라이언트
    participant AG as API Gateway
    participant PS as Product Service
    participant BS as Brand Service
    participant LS as Like Service

    Note over C,LS: 1. 상품 목록 조회 (GET /api/v1/products)

    C->>AG: GET /api/v1/products?brandId=1&sort=latest&page=0&size=20
    AG->>AG: 요청 파라미터 검증

    alt 유효하지 않은 정렬 키
        AG-->>C: 400 유효하지 않은 정렬 기준입니다.
    else 정렬 키 정상
        AG->>PS: 상품 목록 조회 요청(brandId, sort, page, size)

        alt brandId 파라미터 있음
            PS->>BS: 브랜드 존재 및 활성 상태 확인(brandId)
            alt 브랜드 없음 또는 비활성
                BS-->>PS: 브랜드 없음
                PS-->>AG: 404 해당 브랜드를 찾을 수 없습니다.
                AG-->>C: 404 해당 브랜드를 찾을 수 없습니다.
            else 브랜드 존재
                BS-->>PS: 브랜드 확인 완료
                PS->>PS: 브랜드 조건 포함 조회
            end
        else brandId 파라미터 없음
            PS->>PS: 브랜드 필터 없이 조회 
        end

        alt X-USER-ID 헤더 있음
            PS->>LS: 사용자의 좋아요 정보 조회(상품 ID 목록 기준)
            LS-->>PS: 좋아요 여부 정보
            PS->>PS: 상품 목록에 isLiked 매핑
        end

        PS-->>AG: 상품 목록 결과(상품 정보, totalLikes, isLiked 포함)
        AG-->>C: 200 OK, 상품 목록 응답
    end

    Note over C,LS: 빈 페이지인 경우에도 200 OK와 빈 배열 반환
~~~


## 좋아요 등록
~~~mermaid
sequenceDiagram
    participant C as 클라이언트
    participant AG as API Gateway
    participant LS as Like Service
    participant US as User Service
    participant PS as Product Service

    Note over C,PS: 1. 좋아요 등록 (POST /api/v1/like/products/{productId})<br/>Header: X-USER-ID

    C->>AG: POST /api/v1/like/products/{productId}<br/>Header: X-USER-ID
    AG->>AG: 헤더(X-USER-ID) 검증

    alt X-USER-ID 누락
        AG-->>C: 404 해당 사용자를 찾을 수 없습니다
    else 헤더 OK
        AG->>LS: 좋아요 등록 요청(userId, productId)

        LS->>US: 사용자 존재 확인(userId)
        alt 사용자 없음/유효하지 않음
            US-->>LS: 사용자 없음
            LS-->>AG: 404 해당 사용자를 찾을 수 없습니다
            AG-->>C: 404 해당 사용자를 찾을 수 없습니다
        else 사용자 존재
            US-->>LS: 사용자 확인 완료

            LS->>PS: 상품 존재/활성 확인(productId)
            alt 상품 없음/비활성
                PS-->>LS: 상품 없음/비활성
                LS-->>AG: 404 해당 상품을 찾을 수 없습니다
                AG-->>C: 404 해당 상품을 찾을 수 없습니다
            else 상품 존재
                PS-->>LS: 상품 확인 완료

                LS->>LS: 이미 좋아요 여부 확인(userId, productId)
                alt 이미 좋아요 상태 (멱등)
                    LS-->>AG: 200 OK { liked: true, totalLikes: unchanged }
                    AG-->>C: 200 OK
                else 신규 좋아요
                    LS->>LS: 좋아요 생성
                    LS->>PS: totalLikes 증가 요청(productId)
                    PS-->>LS: 증가 완료
                    LS-->>AG: 200 OK { liked: true, totalLikes: updated }
                    AG-->>C: 200 OK
                end
            end
        end
    end
~~~

---

## 주문 생성 - 포인트 결제 (동기)
~~~mermaid
sequenceDiagram
    participant C as 클라이언트
    participant OC as OrderController
    participant OF as OrderFacade
    participant PDS as ProductDomainService
    participant ODS as OrderDomainService
    participant PayDS as PaymentDomainService
    participant PSF as PaymentStrategyFactory
    participant PtS as PointPaymentStrategy
    participant PtAcc as PointAccountDomainService

    Note over C,PtAcc: 포인트 결제 (POST /api/v1/orders, cardInfo 없음)

    C->>OC: POST /api/v1/orders (items만, cardInfo 없음)
    OC->>OF: createOrder(userId, items, null, null)

    rect rgb(240, 248, 255)
        Note over OF: Transaction 시작

        OF->>OF: determinePaymentType() -> POINT_ONLY
        OF->>OF: productId 순 정렬 (데드락 방지)

        loop 각 상품별
            OF->>PDS: decreaseStock(productId, quantity)
            PDS-->>OF: Product 반환 (재고 차감됨)
        end

        OF->>ODS: createOrder(userId, orderItems, totalAmount)
        ODS-->>OF: Order 생성 (PENDING)

        OF->>PayDS: createPayment(orderId, userId, amount, POINT_ONLY)
        PayDS-->>OF: Payment 생성 (PENDING)

        OF->>OF: order.startPayment() -> PAYING

        OF->>PSF: create(POINT_ONLY)
        PSF-->>OF: PointPaymentStrategy

        OF->>PtS: executePayment(context)
        PtS->>PtAcc: deduct(userId, amount)
        PtAcc-->>PtS: 포인트 차감 완료
        PtS-->>OF: 완료

        OF->>OF: order.confirm() -> CONFIRMED
        OF->>PayDS: markAsSuccess(paymentId)

        Note over OF: Transaction 커밋
    end

    OF-->>OC: OrderInfo
    OC-->>C: 200 OK (status: CONFIRMED)
~~~

---

## 주문 생성 - 카드 결제 (비동기)
~~~mermaid
sequenceDiagram
    participant C as 클라이언트
    participant OC as OrderController
    participant OF as OrderFacade
    participant PDS as ProductDomainService
    participant ODS as OrderDomainService
    participant PayDS as PaymentDomainService
    participant PSF as PaymentStrategyFactory
    participant CS as CardPaymentStrategy
    participant PG as PG Simulator

    Note over C,PG: 카드 결제 (POST /api/v1/orders, cardInfo 있음)

    C->>OC: POST /api/v1/orders (items + cardInfo)
    OC->>OF: createOrder(userId, items, cardType, cardNo)

    rect rgb(240, 248, 255)
        Note over OF: Transaction 시작

        OF->>OF: determinePaymentType() -> CARD_ONLY
        OF->>OF: productId 순 정렬 (데드락 방지)

        loop 각 상품별
            OF->>PDS: decreaseStock(productId, quantity)
            PDS-->>OF: Product 반환 (재고 차감됨)
        end

        OF->>ODS: createOrder(userId, orderItems, totalAmount)
        ODS-->>OF: Order 생성 (PENDING)

        OF->>PayDS: createPayment(orderId, userId, amount, CARD_ONLY)
        PayDS-->>OF: Payment 생성 (PENDING)

        OF->>OF: order.startPayment() -> PAYING

        OF->>OF: registerSynchronization(afterCommit)

        Note over OF: Transaction 커밋
    end

    OF-->>OC: OrderInfo
    OC-->>C: 200 OK (status: PAYING)

    rect rgb(255, 248, 240)
        Note over OF,PG: afterCommit() - 트랜잭션 커밋 후 비동기 실행

        OF->>PSF: create(CARD_ONLY)
        PSF-->>OF: CardPaymentStrategy

        OF->>CS: executePayment(context)

        rect rgb(255, 240, 240)
            Note over CS,PG: Resilience4j 적용 (Retry + CircuitBreaker)
            CS->>PG: POST /api/v1/payments (결제 요청)
            PG-->>CS: transactionKey 반환
        end

        CS->>PayDS: updatePgTransactionId(paymentId, transactionKey)
        Note over CS: PG 콜백 대기 상태
    end
~~~

---

## PG 콜백 처리 - 결제 성공
~~~mermaid
sequenceDiagram
    participant PG as PG Simulator
    participant PC as PaymentCallbackController
    participant PF as PaymentFacade
    participant PayDS as PaymentDomainService
    participant ODS as OrderDomainService

    Note over PG,ODS: PG 결제 완료 콜백 (SUCCESS)

    PG->>PC: POST /api/v1/payments/callback
    Note right of PG: { transactionKey, status: SUCCESS }

    PC->>PF: completePaymentByCallback(transactionKey)

    rect rgb(240, 248, 255)
        Note over PF: Transaction 시작

        PF->>PayDS: getPaymentByPgTransactionId(transactionKey)
        PayDS-->>PF: Payment

        PF->>PayDS: markAsSuccess(paymentId, transactionKey)
        Note over PayDS: Payment: PENDING -> SUCCESS

        PF->>ODS: confirmOrder(userId, orderId)
        Note over ODS: Order: PAYING -> CONFIRMED (멱등성 보장)

        Note over PF: Transaction 커밋
    end

    PF-->>PC: 완료
    PC-->>PG: 200 OK
~~~

---

## PG 콜백 처리 - 결제 실패 (재고 복구)
~~~mermaid
sequenceDiagram
    participant PG as PG Simulator
    participant PC as PaymentCallbackController
    participant PF as PaymentFacade
    participant PayDS as PaymentDomainService
    participant OF as OrderFacade
    participant ODS as OrderDomainService
    participant PDS as ProductDomainService

    Note over PG,PDS: PG 결제 실패 콜백 (FAILED)

    PG->>PC: POST /api/v1/payments/callback
    Note right of PG: { transactionKey, status: FAILED, reason }

    PC->>PF: failPaymentByCallback(transactionKey, reason)

    rect rgb(255, 240, 240)
        Note over PF: Transaction 시작

        PF->>PayDS: getPaymentByPgTransactionId(transactionKey)
        PayDS-->>PF: Payment

        PF->>PayDS: markAsFailed(paymentId, reason)
        Note over PayDS: Payment: PENDING -> FAILED

        PF->>OF: handlePaymentFailure(userId, orderId)

        OF->>ODS: getOrder(userId, orderId)
        ODS-->>OF: Order (with OrderItems)

        OF->>OF: orderItems 역순 정렬 (productId DESC)

        loop 각 상품별 (역순)
            OF->>PDS: increaseStock(productId, quantity)
            Note over PDS: 재고 복구
        end

        OF->>OF: order.fail() -> FAILED

        Note over PF: Transaction 커밋
    end

    PF-->>PC: 완료
    PC-->>PG: 200 OK
~~~

---

## 결제 상태 조회 (PG 동기화)
~~~mermaid
sequenceDiagram
    participant C as 클라이언트
    participant OC as OrderController
    participant PF as PaymentFacade
    participant PayDS as PaymentDomainService
    participant PG as PG Simulator
    participant ODS as OrderDomainService

    Note over C,ODS: GET /api/v1/orders/{orderId}/payment

    C->>OC: GET /api/v1/orders/{orderId}/payment
    OC->>PF: syncPaymentStatusWithPG(userId, orderId)

    PF->>PayDS: getPaymentByOrderId(orderId)
    PayDS-->>PF: Payment

    alt Payment.status == PENDING
        rect rgb(240, 248, 255)
            Note over PF,PG: PG와 상태 동기화

            PF->>PG: GET /api/v1/payments/{transactionKey}
            Note over PF,PG: Retry 정책 적용
            PG-->>PF: PgTransactionDetail (status, reason)

            alt PG status == SUCCESS
                PF->>PayDS: markAsSuccess(paymentId, transactionKey)
                PF->>ODS: confirmOrder(userId, orderId)
            else PG status == FAILED
                PF->>PayDS: markAsFailed(paymentId, reason)
                Note over PF: handlePaymentFailure 호출 (재고 복구)
            else PG status == PENDING
                Note over PF: 상태 유지, 다음 조회 시 재확인
            end
        end
    end

    PF-->>OC: Payment (최신 상태)
    OC-->>C: 200 OK (PaymentStatusResponse)
~~~