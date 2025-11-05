# 도메인 객체 설계 (클래스 다이어그램 or 설명 중심)

classDiagram  
class User {  
Long id  
String userId  
Point point  
}  
class Product {  
Long refBrandId  
Long id  
String name  
BigDicimal price  
long likeCount  
long stock  
long holdStock  
}  
class Brand {  
Long id  
String name  
}  
class Like {  
Long refUserId  
Long refProductId  
}  
class Order {  
Long refUserId  
OrderItem orderItem  
Long id  
OrderStatus status  
BigDicimal paymentPrice  
BigDicimal totalPrice  
ZonedDateTime order_at   
}  
class OrderItem {  
Product product  
Long id  
}

Product --> Brand  
Like --> User  
Like --> Product

Order --> User  
Order --> OrderItem  
OrderItem --> Product
