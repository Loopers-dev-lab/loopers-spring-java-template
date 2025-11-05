# 도메인 객체 설계 (클래스 다이어그램 or 설명 중심)

classDiagram  
class Member {  
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
Member member  
Product product  
}  
class Order {  
Member member  
OrderItem orderItem  가
Long id  
String name  
}  
class OrderItem {  
Product product  
Long id  
}

Product --> Brand  
Like --> Member  
Like --> Product

Order --> Member  
Order --> OrderItem  
OrderItem --> Product
