# 클래스 다이어그램

~~~mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        -Long id PK
        -ZonedDateTime createdAt
        -ZonedDateTime updatedAt
        -ZonedDateTime deletedAt
        +delete() 삭제
        +restore() 복원
    }

    class User {
        -String userId 계정ID
        -String email 이메일
        -String birthDate 생년월일
        -Gender gender 성별
        +create() 회원가입
    }

    class PointAccount {
        -String userId 사용자ID
        -Point balance 잔액
        +create(userId) 생성
        +charge(amount) 충전
        +deduct(amount) 차감
        +getBalance(): Point 잔액조회
    }

    class Point {
        <<Embeddable>>
        -long amount 금액
        +zero(): Point 0원생성
        +of(amount): Point 생성
        +amount(): long 금액조회
    }

    class Brand {
        -String name 브랜드명
        -boolean active 활성여부
        +create(name) 생성
        +createInactive(name) 비활성생성
        +activate() 활성화
        +deactivate() 비활성화
        +isActive(): boolean 활성상태인지
    }

    class Product {
        -String name 상품명
        -String description 상품설명
        -long price 판매가격
        -Long brandId 브랜드ID
        -Long stock 현재재고
        -Long totalLikes 총좋아요수
        -Long version 낙관적락버전
        +create() 생성
        +hasStock(): boolean 재고있는지
        +hasEnoughStock(quantity): boolean 충분한재고인지
        +decreaseStock(quantity) 재고차감
        +increaseLikes() 좋아요증가
        +decreaseLikes() 좋아요감소
    }

    class ProductLike {
        -Long userId 사용자ID
        -Long productId 상품ID
        +create(userId, productId) 생성
    }

    class Order {
        -String userId 주문자ID
        -List~OrderItem~ orderItems 주문아이템목록
        -long totalAmount 총결제금액
        -OrderStatus status 주문상태
        +create() 주문생성
        +confirm() 주문확정
    }

    class OrderItem {
        -Order order 주문
        -Long productId 상품ID
        -String productName 주문당시상품명
        -Long quantity 주문수량
        -long price 주문당시가격
        +create() 생성
    }

%% ======================= Enums =======================
    class Gender {
        <<enumeration>>
        MALE 남성
        FEMALE 여성
    }

    class OrderStatus {
        <<enumeration>>
        PENDING 주문요청
        CONFIRMED 주문완료(재고/포인트차감완료)
        CANCELLED 주문취소
    }

%% ======================= Relations =======================
    User --|> BaseEntity
    PointAccount --|> BaseEntity
    Brand --|> BaseEntity
    Product --|> BaseEntity
    ProductLike --|> BaseEntity
    Order --|> BaseEntity
    OrderItem --|> BaseEntity

    User "1" -- "1" PointAccount : 포인트 보유
    User "1" -- "0..*" ProductLike : 좋아요 등록
    User "1" -- "0..*" Order : 주문 생성

    PointAccount *-- Point : 잔액(값 객체)

    Brand "1" -- "0..*" Product : 상품 포함
    Product "1" -- "0..*" ProductLike : 좋아요 받음
    Product "1" -- "0..*" OrderItem : 참조됨

    Order "1" *-- "1..*" OrderItem : 주문 아이템 포함(강한 소유)
    Order --> OrderStatus
    User --> Gender
~~~
