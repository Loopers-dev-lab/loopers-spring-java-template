# 전체 테이블 구조 및 관계 정리 (ERD Mermaid 작성 가능)

erDiagram  
users {  
bigint id PK  
varchar userId PK    
varchar email  
date birthday  
varchar gender  
bigint points  
}  
products {  
bigint id PK  
bigint ref_brand_id FK  
varchar name  
decimal price  
bigint stock  
bigint hold_stock  
}  
brands {  
bigint id PK  
varchar name  
varchar story  
}  
likes {  
bigint id PK  
bigint ref_user_id PK, FK  
bigint ref_product_id PK, FK  
timestamp created_at  
}  
orders {    
bigint id PK  
bigint ref_user_id FK    
varchar status  
decimal payment_price  
decimal total_price  
timestamp order_at   
}  
order_items {    
bigint id PK    
bigint ref_order_id FK   
bigint ref_product_id FK  
bigint quantity  
decimal unit_price  
decimal total_price  
}  
products ||--o{ likes : ""  
users ||--o{ likes : ""  
brands ||--o{ products : ""  
users ||--o{ orders : "places"  
orders ||--|{ order_items : contains  
products ||--o{ order_items :""
