# 클래스 다이어그램

~~~mermaid
classDiagram
    class User {
        -Long id 사용자내부ID
        -String userId 계정ID
        -Gender gender 성별
        -String email 이메일
        -LocalDate birthDate 생년월일
        +register() 회원가입
        +getUser() 내정보조회
    }

    class PointAccount {
        -Long id 포인트ID
        -Long userId 사용자ID
        -Point balance 잔액
        -Long version 잠금버전
        +charge(amount: Point) 충전
        +deduct(amount: Point) 차감
        +getBalance(): Point 잔액조회
        +hasEnough(amount: Point): boolean 충분한지확인
    }

    class PointHistory {
        -Long id 이력ID
        -Long userId 사용자ID
        -PointType type 유형
        -Point amount 변경금액
        -Point balanceAfter 변경후잔액
        -String idempotencyKey 멱등키
    }

    class Point {
        -Long amount 금액
    }

    class Brand {
        -Long id 브랜드번호
        -String name 브랜드명
        -String description 브랜드소개
        -BrandStatus status 활성여부
        +getBrand() 정보조회
        +isActive(): boolean 활성상태인지
    }

    class Product {
        -Long id 상품번호
        -Long brandId 브랜드ID
        -String name 상품명
        -String description 상품설명
        -Price price 판매가격
        -Long stock 현재재고
        -Long totalLikes 총좋아요수
        -ProductStatus status 판매상태
        +getDetail() 상세조회
        +isAvailable(): boolean 구매가능여부
        +hasStock(quantity: Long): boolean 재고있는지
        +decreaseStock(quantity: Long) 재고차감
        +increaseLikes() 좋아요증가
        +decreaseLikes() 좋아요감소
    }

    class Price {
        -Long amount 금액
    }

    class ProductLike {
        -Long id 좋아요번호
        -Long userId 사용자ID
        -Long productId 상품ID
        +isLikedBy(userId: Long): boolean 좋아요여부
    }

    class Order {
        -Long id 주문내부ID
        -String orderId 주문번호
        -Long userId 주문자ID
        -Price totalAmount 총결제금액
        -OrderStatus status 주문상태
        -String idempotencyKey 중복방지키
        +create() 주문생성
        +getDetail() 상세조회
        +cancel() 주문취소
        +calculateTotal(): Price 총금액계산
    }

    class OrderItem {
        -Long id 항목번호
        -Long orderId 주문ID
        -Long productId 상품ID
        -String productName 주문당시상품명
        -Price price 주문당시가격
        -Long quantity 주문수량
        -Price subtotal 항목소계
        +calculateSubtotal(): Price 소계계산
    }

    class Payment {
        -Long id 결제번호
        -Long orderId 주문ID
        -Long userId 결제자ID
        -Point amount 결제금액(포인트)
        -PaymentType type 결제수단
        -PaymentStatus status 결제상태
        -String idempotencyKey 멱등키
        +process() 결제처리
        +complete() 결제완료
        +fail() 결제실패
    }

    class ExternalOrderService {
        <<interface>>
        +sendOrder(order주문) 주문전송
    }

%% ======================= Enums =======================
    class Gender {
        <<enumeration>>
        MALE 남성
        FEMALE 여성
    }

    class PointType {
        <<enumeration>>
        CHARGE 충전
        USE 사용
        REFUND 환불
    }

    class BrandStatus {
        <<enumeration>>
        ACTIVE 활성
        INACTIVE 비활성
    }

    class ProductStatus {
        <<enumeration>>
        ACTIVE 판매중
        INACTIVE 판매중지
        OUT_OF_STOCK 품절
    }

    class OrderStatus {
        <<enumeration>>
        PENDING 대기중 (재고확인/차감 완료 전)
        CONFIRMED 확정됨 (재고차감 OK)
        CANCELLED 취소됨
        COMPLETED 완료됨 (포인트차감/결제완료)
    }

    class PaymentType {
        <<enumeration>>
        POINT 포인트결제
    }

    class PaymentStatus {
        <<enumeration>>
        PENDING 대기중
        COMPLETED 완료됨
        FAILED 실패함
    }

%% ======================= Relations =======================
    User "1" *-- "1" PointAccount : 포인트 보유(강한 소유)
    User "1" -- "0..*" PointHistory : 포인트 이력
    Payment "0..1" -- "0..*" PointHistory : 결제-이력(환불 등 다건 가능)

    User "1" -- "0..*" ProductLike : 좋아요 등록
    Product "1" -- "0..*" ProductLike : 좋아요 받음

    Brand "1" -- "0..*" Product : 상품 포함
    User "1" -- "0..*" Order : 주문 생성
    Order "1" *-- "1..*" OrderItem : 주문 아이템 포함(강한 소유)
    Product "1" -- "0..*" OrderItem : 참조됨

    Order "1" -- "0..1" Payment : 결제(단일 결제 가정)

    Product *-- Price : 판매가격(값 객체)
    OrderItem *-- Price : 주문당시가격(스냅샷)

    Brand --> BrandStatus
    Product --> ProductStatus
    Order --> OrderStatus
    Payment --> PaymentType
    Payment --> PaymentStatus
    PointHistory --> PointType
~~~