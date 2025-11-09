## 03 - 클래스 다이어그램

### 클래스 다이어그램

```mermaid
classDiagram
direction LR

%% =======================
%% Value Objects / Enums
%% =======================
class CategoryCode {
  -String code
  +CategoryCode(String)
  +String code()
}

%% ============
%% Entities
%% ============
class UserModel {
  -String userId
  -String userName
  -String description
  -String email
  -String birthdate
  -Character gender
  -Integer point
  -LocalDateTime createdAt
  -LocalDateTime updatedAt
  -LocalDateTime deletedAt
  +void updatePoint(int)
  +void decreasePoint(int)
}

class BrandModel{
  +Long id
  +String name
  +String description
  +Character status
  -LocalDateTime createdAt
  -LocalDateTime updatedAt
  -LocalDateTime deletedAt
  +void chageBrandStatus(int status)
}

class ProductModel {
  -Long id
  -String productName
  -CategoryCode category
  -Integer price
  -Integer stock
  -Charactor status
  -Long brandId
  -LocalDateTime createdAt
  -LocalDateTime updatedAt
  -LocalDateTime deletedAt
  
  
  +boolean canBePurchased(int qty)
  +void decreaseStock(int qty)
  +void increaseStock(int qty)
}

class ProductLikeModel {
  -String userId
  -Long productId
  -LocalDateTime createdAt
  +static ProductLike of(userId, productId)
}

class OrderModel {
  -Long id
  -Integer orderCnt;
  -Character status
  -Integer totalPrice
  -Integer normalPrice
  -Integer errorPrice
  -String userId;
  -LocalDateTime createdAt
  -LocalDateTime updatedAt
  -LocalDateTime deletedAt
  +void addItem(OrderItemModel)
  +void purchase()
  +void cancel()
}

class OrderItemModel {
  -Long id
  -String itemName
  -Integer price
  -Long orderId
  -Long productId
  void setItemName(Long productId)
  void setPrice(Long productId);
}

%% =================
%% Application layer
%% =================
class ProductService {
  +Page<Product> getProductsList(sort, cursor)
  +Page<Product> getBrandProductsList(brandId, sort, cursor)
  +Product getProductDetail(id)
  +boolean hasEnoughStock(id)
}

class LikeService {
  +LikeResult likeProduct(userId, productId)   // 멱등
  +LikeResult unlikeProduct(userId, productId) // 멱등
}

class OrderService {
  +Order placeOrder(userId, List<OrderLineRequest>)
  +void pay(orderId, PaymentInfo)
  +void cancel(orderId)
}

class BrandService {
    +BrandModel changeBrandStatus(brandId, status)
}

%% =================
%% Application layer (Facade)
%% =================

class UserOrderProductFacade {
  +Order order(userId, List<OrderLineRequest>)
  + void pay(userId, orderId, PaymentInfo)
  + void cancel(userId, orderId)
    
}

class UserLikeProductFacade {
    +LikeResult likeProduct(userId, productId) 
    +LikeResult unlikeProduct(userId, productId) 
}

%% ===============
%% Repositories
%% ===============
class UserRepository {
  <<interface>>
    +boolean existsUserId(String userId);
    +Optional<UserModel> findByUserId(String userId);
    +UserModel save(UserModel user);
    +boolean deleteUser(String userId);
}

class BrandRepository {
  <<interface>>
  +Optional~BrandModel~ findById(Long)
  +BrandModel save(BrandModel)
}

class ProductRepository {
  <<interface>>
  +Optional~ProductModel~ findById(Long)
  +int increaseStockAtomically(id, qty)
  +int decreaseStockAtomically(id, qty)
}

class ProductLikeRepository {
  <<interface>>
  +boolean insertIgnore(userId, productId)
  +int delete(userId, productId)
  +Page~ProductModel~ findLikedProducts(userId, sort, cursor)
}

class OrderRepository {
  <<interface>>
  +Optional~OrderModel~ findById(Long)
  +void save(OrderModel)
  +Page~OrderModel~ findByUser(userId, cursor)
}

%% ============
%% Relations
%% ============
UserModel "1" --> "0..*" OrderModel : 주문한다
OrderModel "1" --> "1..*" OrderItemModel : 포함한다
BrandModel "1" --> "0..*" ProductModel : 소유한다
UserModel "1" --> "0..*" ProductLikeModel : 좋아요
ProductModel "1" --> "0..*" ProductLikeModel : 좋아요됨

ProductService ..> ProductRepository
BrandService ..> BrandRepository

LikeService ..> ProductLikeRepository
%% LikeService ..> ProductRepository
OrderService ..> OrderRepository
%% OrderService ..> ProductService
UserService ..> UserRepository

%% Facade
UserOrderProductFacade ..> UserService
UserOrderProductFacade ..> OrderService
UserOrderProductFacade ..> ProductService

UserLikeProductFacade ..>UserService
UserLikeProductFacade ..>ProductService
UserLikeProductFacade ..>LikeService


%% etc.
ProductModel --> CategoryCode
```

### 기타
#### update로 메소드를 퉁치기 vs increase()/decrease() 역할 나눠주기
> 입력값 검증 등이 다를 수 있기도 하고 명확하게 추적할 수 있으면 좋을 것 같아서 메소드 역할은 하나의 동작만한다는 원칙을 가져가는게 좋아보였습니다.

e.g) UserModel에선 포인트는 충전은 updatePoint로 하고 있는데, 현재 포인트 차감은 없음. 클래스 다이어그램엔 그리고 3주차에 수정
```
public void updatePoint(Integer newPoint) {
        validateChargePoint(newPoint);
        this.point +=  newPoint;
    }
```

#### 기본적인 엔티티, 레파지토리, 서비스 객체간 역할과 책임에 대해 고민

#### 퍼사드 계층으로, 핵심 로직인 유저의 상품 주문 / 유저의 상품 좋아요를 구분하려함

```mermaid
classDiagram
    direction LR
%% Facade
UserOrderProductFacade ..> UserService
UserOrderProductFacade ..> OrderService
UserOrderProductFacade ..> ProductService

UserLikeProductFacade ..>UserService
UserLikeProductFacade ..>ProductService
UserLikeProductFacade ..>LikeService
```