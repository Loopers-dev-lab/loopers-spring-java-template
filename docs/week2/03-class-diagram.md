# 도메인 객체 설계 (클래스 다이어그램 or 설명 중심)

classDiagram  
class User {  
Long id  
String userId  
String name  
Point point  
}  
class Product {  
Brand brand  
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
User user  
Product product  
}  
class Order {  
User user  
OrderItem orderItem  
Long id  
String name  
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
