-- Seed brands (IDs fixed to match CSV references)
INSERT INTO brand (id, name, created_at, updated_at) VALUES
  (1, '브랜드A', NOW(), NOW()),
  (2, '브랜드B', NOW(), NOW()),
  (3, '브랜드C', NOW(), NOW());

-- Seed products (align with ProductList.csv)
INSERT INTO product (brand_id, name, price, stock_quantity, like_count, created_at, updated_at, version) VALUES
  (1, '티셔츠 기본',        9900, 100, 5,  NOW(), NOW(), 0),
  (1, '셔츠 옥스포드',      29900, 50,  12, NOW(), NOW(), 0),
  (2, '데님 팬츠',          39900, 40,  8,  NOW(), NOW(), 0),
  (2, '코튼 팬츠',          24900, 80,  0,  NOW(), NOW(), 0),
  (3, '러닝 슈즈',          79900, 30,  20, NOW(), NOW(), 0),
  (3, '슬립온',             49900, 60,  7,  NOW(), NOW(), 0),
  (1, '후드 집업',          45900, 70,  3,  NOW(), NOW(), 0),
  (2, '야상 자켓',          89900, 20,  15, NOW(), NOW(), 0),
  (3, '니트 스웨터',        35900, 55,  0,  NOW(), NOW(), 0),
  (1, '볼캡 모자',          15900, 120, 2,  NOW(), NOW(), 0);


















