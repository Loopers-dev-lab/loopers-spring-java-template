## 테스트 데이터 생성

### 1. 1만 건, https://www.mockaroo.com/

```
INSERT INTO product_list_view (product_id, brand_id, name, price, stock_quantity, like_count, created_at, updated_at) VALUES
(1001, 7, 'Noise Reduction Headphones', 48000, 82168, 891, '2025-09-20 03:14:51', '2025-06-10 04:28:37'),
(1002, 11, 'Wireless Security Camera', 132000, 98812, 768, '2025-04-13 04:00:19', '2025-09-02 19:45:40'),
(1003, 20, 'Casual Cropped Sweater', 48000, 92766, 617, '2025-10-28 21:49:30', '2025-09-18 07:28:50');
```

### 2. 9만 건, python sql 생성

resources/generate_product_data.py

```
python3 generate_product_data.py
```

```
INSERT INTO product_list_view (product_id, brand_id, name, price, stock_quantity, like_count, created_at, updated_at, deleted_at) VALUES
(10001, 2, 'Product 10001', 26800, 105, 8283, '2025-11-26 00:28:17', '2025-11-26 00:28:17', NULL),
(10002, 3, 'Product 10002', 45500, 71, 159, '2025-11-26 00:28:17', '2025-11-26 00:28:17', NULL),
(10003, 4, 'Product 10003', 24700, 84, 8725, '2025-11-26 00:28:17', '2025-11-26 00:28:17', NULL);
```

### 3. 그외, 추가 생성

```
INSERT INTO product_list_view (product_id, brand_id, name, like_count, created_at, updated_at)
SELECT product_id+1000000 ,9, CONCAT(name, '_copy'), like_count, NOW(),NOW()  
FROM product_list_view
where product_id <= 100000;
```

### 4. 테스트 데이터 직후

#### 1) Primary, Unique 컬럼(product_id)  인덱스 확인

-- product_list_view 0 PRIMARY 1 id A 92499 BTREE YES -- product_list_view 0 UK30iej7fsim4i1t5k9oxiq3xd5 1 product_id A
94106 BTREE YES SHOW INDEX FROM product_list_view;

#### 2) 전체 데이터 건수, 100_000

SELECT count(*)
FROM product_list_view ;

#### 3) 전체 조회

##### 기본 (cost=10118,actual time=12.3..67.2)

-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=12.3..67.2 rows=100000 loops=1)
EXPLAIN ANALYZE SELECT *
FROM product_list_view;

##### 최신순 (cost=10118,actual time=325..356)

-- -> Sort: product_list_view.created_at DESC  (cost=10118 rows=99499) (actual time=325..356 rows=100000 loops=1)
-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=2.72..117 rows=100000 loops=1)
EXPLAIN ANALYZE SELECT *
FROM product_list_view ORDER BY created_at DESC ;

##### 좋아요순 (cost=10118,actual time=266..286)

-- -> Sort: product_list_view.like_count DESC  (cost=10118 rows=99499) (actual time=266..286 rows=100000 loops=1)
-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=1.51..111 rows=100000 loops=1)
EXPLAIN ANALYZE SELECT *
FROM product_list_view ORDER BY like_count DESC ;

##### 가격순 (cost=10118,actual time=242..252)

-- -> Sort: product_list_view.price DESC  (cost=10118 rows=99499) (actual time=242..252 rows=100000 loops=1)
-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=1.31..78.8 rows=100000 loops=1)
EXPLAIN ANALYZE SELECT *
FROM product_list_view ORDER BY price DESC ;

#### 4) 브랜드 조회

(상품이 많은 브랜드 - 9,16,15 좋아요 수가 많은 브랜드 - 4,16,17 )

##### 기본 (cost=10118,actual time=4.77..114)

-- -> Filter: (product_list_view.brand_id = 9)  (cost=10118 rows=9950) (actual time=4.77..114 rows=2019 loops=1)
-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=4.74..110 rows=100000 loops=1)
EXPLAIN ANALYZE SELECT *
FROM product_list_view WHERE brand_id = '9';

##### 최신순 (cost=10118,actual time=102..102)

-- -> Sort: product_list_view.created_at DESC  (cost=10118 rows=99499) (actual time=102..102 rows=2019 loops=1)
-- -> Filter: (product_list_view.brand_id = 9)  (cost=10118 rows=99499) (actual time=0.573..96.7 rows=2019 loops=1)
-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=0.56..91.6 rows=100000 loops=1)

EXPLAIN ANALYZE SELECT *
FROM product_list_view WHERE brand_id = '9' ORDER BY created_at DESC ;

##### 좋아요순 (cost=10118,actual time=75.4..75.5)

-- -> Sort: product_list_view.like_count DESC  (cost=10118 rows=99499) (actual time=75.4..75.5 rows=2019 loops=1)
-- -> Filter: (product_list_view.brand_id = 9)  (cost=10118 rows=99499) (actual time=0.899..71.9 rows=2019 loops=1)
-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=0.889..67.8 rows=100000 loops=1)

EXPLAIN ANALYZE SELECT *
FROM product_list_view WHERE brand_id = '9' ORDER BY like_count DESC ;

##### 가격순 (cost=10118,actual time=141..141)

-- -> Sort: product_list_view.price DESC  (cost=10118 rows=99499) (actual time=141..141 rows=2019 loops=1)
-- -> Filter: (product_list_view.brand_id = 9)  (cost=10118 rows=99499) (actual time=5.04..134 rows=2019 loops=1)
-- -> Table scan on product_list_view  (cost=10118 rows=99499) (actual time=4.97..124 rows=100000 loops=1)
EXPLAIN ANALYZE SELECT *
FROM product_list_view WHERE brand_id = '9' ORDER BY price DESC ; 

