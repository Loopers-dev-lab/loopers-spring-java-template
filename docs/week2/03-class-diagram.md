# 도메인 객체 설계 (클래스 다이어그램 or 설명 중심)
classDiagram  
class Member {  
Long id  
String userId  
String name  
BigDecimal point
}  
class Product {  
Brand brand  
Long id  
String name  
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
OrderItem orderItem  
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