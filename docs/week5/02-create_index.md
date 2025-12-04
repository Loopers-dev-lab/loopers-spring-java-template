# 📌 ProductListView 인덱스 최적화 분석 정리

상품 목록 조회 성능 개선을 위해 **브랜드 ID + 좋아요수 DESC** 복합 인덱스(`idx_brand_like`)를 생성하고, 브랜드 ID 단일 인덱스와 실제 쿼리 성능 차이를 비교했다.

아래는 각 정렬 기준별로 **EXPLAIN ANALYZE** 결과를 정리한 내용이다.

---

## 1) 브랜드 ID 단일 조건 조회

### 기존 (인덱스 없음)

```
-> Filter: (brand_id = 9)  (cost=10118 rows=9950) (actual time=4.77..114)
-> Table scan on product_list_view (actual rows=100000)
```

* **풀 스캔**, 시간도 4.7ms → 114ms까지 증가
* 전체 10만건을 스캔하면서 브랜드 2019건 필터링

### 복합 인덱스 적용 후

```
-> Index lookup using idx_brand_like (brand_id=9)  
   (cost=743 rows=2122) (actual time=2.2..65.3)
```

* 인덱스 탐색으로 전환
* **cost 10118 → 743**, 실행 시간 약 50% 이상 감소

---

## 2) 최신순 정렬 (`ORDER BY created_at DESC`)

### 기존 (인덱스 없음)

```
-> Sort: created_at DESC (cost=10118) (actual time=102..102)
-> Table scan (actual time=0.56..91.6)
```

* 풀스캔 + 정렬 → 전체 정렬 비용 발생

### 복합 인덱스 적용 후

```
-> Index lookup using idx_brand_like (brand_id=9) (cost=707)
-> Sort: created_at DESC (actual time=2.22..2.36)
```

* 정렬 대상이 2019건(브랜드 필터링 결과)으로 감소
* **정렬 비용 102ms → 2ms**, 극적인 성능 개선

---

## 3) 좋아요순 정렬 (`ORDER BY like_count DESC`)

### 기존

```
-> Sort: like_count DESC (actual time=75.4..75.5)
-> Table scan (actual time=0.889..67.8)
```

### 복합 인덱스 적용 후

```
-> Index lookup using idx_brand_like (brand_id=9)  
   (cost=707) (actual time=0.263..5.65)
```

* 필터링 + 정렬이 인덱스 덕분에 훨씬 가벼워짐
* **정렬 75ms → 5ms 수준**으로 감소

---

## 4) 가격순 정렬 (`ORDER BY price DESC`)

### 기존

```
-> Sort: price DESC (actual time=141..141)
-> Table scan (actual time=4.97..124)
```

### 복합 인덱스 사용 후

```
-> Sort: price DESC (cost=707 rows=2019) (actual time=1.97..1.98)
-> Index lookup using idx_brand_like (actual time=1.71..1.91)
```

* 인덱스 목록 → 가격 정렬(본문에서는 비정규화한 price 컬럼)
* **141ms → 2ms대로 감소**

---

# 📌 Q. 좋아요수는 숫자 타입 + 자주 변경되는데 인덱스에 넣어도 괜찮은가?

일반적인 DB 설계 상,

> **자주 변경되는 숫자(좋아요수, 조회수, 재고 등)를 인덱스에 넣는 것은 좋지 않다**
> 인덱스 재조정 비용이 크기 때문이다.

이 때문에 초기에 **brand_id 단일 인덱스만 생성하려고 했었다.**

하지만 실제 환경을 고려해 수치를 검증해보았다.

---

# 📌 좋아요수 10만 건 반복 업데이트 성능 테스트

다음 쿼리로 brand_id = 9 의 상품(10만건)의 좋아요수를 지속적으로 갱신:

```sql
WITH RECURSIVE nums AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM nums WHERE n < 100
)
UPDATE product_list_view p
JOIN nums n ON TRUE
SET p.like_count = p.like_count + FLOOR(RAND() * 100 - 50)
WHERE brand_id = 9;
```

### 결과

```
-> Index lookup using idx_brand_like (brand_id=9)
   (cost=10942 rows=99560) 
   (actual time=0.149..109)
```

* 좋아요수 반복 갱신에도 **성능 저하가 체감되지 않음**
* 브랜드별 10만 건이라는 상황 자체가 현실적으로 거의 발생하지 않음
* 읽기 비중이 압도적으로 높은 서비스 구조에서는
  **정렬 성능 개선 효과 > 인덱스 유지 비용**

따라서 종합적으로

> **brand_id + like_count DESC 인덱스를 사용하는 것이 더 현실적이고 유리한 선택**
> 이라는 결론을 얻었다.

---

# 📌 최종 결론

| 항목           | 단일 인덱스 (brand_id) | 복합 인덱스 (brand_id, like_count desc) |
| ------------ | ----------------- | ---------------------------------- |
| 최신순          | 중간                | 매우 빠름                              |
| 좋아요순         | 보통                | 매우 빠름                              |
| 가격순          | 보통                | 빠름                                 |
| 좋아요수 변경 부하   | 낮음                | 낮음(테스트 결과)                         |
| 브랜드별 대량 데이터  | 없음                | 없음                                 |
| 전체 조회 성능 최적화 | 보통                | **우수**                             |

👉 결과적으로, **복합 인덱스가 전반적으로 더 큰 이점을 제공**한다. 👉 좋아요수 업데이트 부하는 현실적으로 문제되지 않는 수준.