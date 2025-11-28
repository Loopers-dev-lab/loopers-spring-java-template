# 빠른 조회를 위한 선택: 성능과 정합성 사이의 트레이드오프

## 들어가며

"조회 성능을 개선하려면 어떻게 해야 할까?"

답은 간단합니다. **빠르게 찾으면 됩니다.** 하지만 현실에서 빠르게 찾는다는 것은 생각보다 복잡한 문제입니다. 이번 Round 5에서는 100,000개 이상의 상품 데이터에서 빠른 조회를 구현하면서 마주한 트레이드오프와 그 해결 과정을 공유합니다.

## 빠른 조회의 두 가지 방법

멘토님께서 비유로 설명해주신 내용이 인상 깊었습니다:

### 1. 인덱스: 책갈피를 꽂아두기

학창시절을 떠올려보세요. 중요한 내용에 포스트잇을 붙여두면 나중에 빠르게 찾을 수 있습니다. 데이터베이스의 **인덱스**도 같은 원리입니다.

**장점:**
- 조회 속도가 극적으로 향상
- 자주 사용하는 쿼리 패턴을 최적화

**하지만 트레이드오프가 있습니다:**

```sql
-- 인덱스 없이 전체 테이블 스캔
SELECT * FROM products WHERE brand_id = 1 ORDER BY like_count DESC;
-- 실행 시간: ~150ms (100,000개 행 스캔)

-- 인덱스 사용
CREATE INDEX idx_products_brand_deleted ON products(brand_id, deleted_at);
CREATE INDEX idx_products_like_count ON products(like_count DESC);
-- 실행 시간: ~15ms (인덱스로 바로 접근)
```

**책갈피가 너무 많아지면?**

멘토님이 지적하신 핵심 문제입니다:

1. **쓰기 성능 저하**: 데이터를 INSERT/UPDATE할 때마다 인덱스도 갱신해야 합니다
2. **인덱스 선택 오류**: RDBMS가 잘못된 인덱스를 선택해서 오히려 느려질 수 있습니다
3. **테이블 변경 어려움**: 인덱스에 포함된 컬럼을 수정하려면 전체 데이터를 재반영해야 합니다

**결론**: 모든 조회 패턴에 인덱스를 거는 것이 아니라, **정말 최적화가 필요한 조회 패턴에만** 거는 것이 유효합니다.

### 2. 캐시: 메모지에 적어두기

한번 조회한 내용을 어딘가에 메모해두면, 다음번엔 DB를 거치지 않고 바로 사용할 수 있습니다.

**캐시의 두 가지 선택**

멘토님이 비유로 설명해주신 내용:

| 구분 | 로컬 캐시 | 글로벌 캐시 (Redis) |
|------|-----------|---------------------|
| 비유 | 나의 개인 메모지 | 우리 팀 공용 메모지 |
| 공유 범위 | 내 인스턴스만 | 모든 인스턴스 |
| 통신 비용 | 없음 (내부 메모리) | 있음 (네트워크) |
| 일관성 | 인스턴스 간 이격 발생 | 모든 서버가 동일한 데이터 |
| 응답 속도 | 매우 빠름 (~1ms) | 빠름 (~2-3ms) |

현재 프로젝트는 **글로벌 캐시(Redis)**를 선택했습니다. 이유는 다음과 같습니다.
- 다중 서버 환경에서 데이터 일관성 유지
- 상품 데이터는 모든 사용자에게 동일해야 함
- 약간의 네트워크 비용(1-2ms)은 허용 가능

## 실전 적용: 상품 조회 최적화

### 요구사항 분석

- 100,000개 이상의 상품 데이터
- 브랜드별 필터링
- 좋아요순, 가격순, 최신순 정렬
- 목표: 100 동시 사용자, 99% 성공률, 평균 응답 시간 50ms 이하

### 1단계: 인덱스 설계

**조회 패턴 분석**

```sql
-- 가장 빈번한 쿼리 패턴
SELECT p.* FROM products p
WHERE p.brand_id = ? AND p.deleted_at IS NULL
ORDER BY p.like_count DESC;
```

**인덱스 전략**

```sql
-- 1. 브랜드 필터 + Soft Delete 체크
CREATE INDEX idx_products_brand_deleted
ON products(brand_id, deleted_at);

-- 2. 좋아요순 정렬
CREATE INDEX idx_products_like_count
ON products(like_count DESC);

-- 3. 생성일순 정렬
CREATE INDEX idx_products_created_at
ON products(created_at DESC);

-- 4. 가격순 정렬
CREATE INDEX idx_products_price
ON products(price ASC);
```

**왜 복합 인덱스를 만들지 않았나?**

처음엔 `(brand_id, deleted_at, like_count)` 같은 복합 인덱스를 고려했습니다. 하지만:

1. **정렬 기준이 3가지**(좋아요/가격/최신순): 각각에 복합 인덱스를 만들면 총 3개
2. **쓰기 성능 저하**: 좋아요가 추가될 때마다 3개의 복합 인덱스 갱신
3. **유지보수 비용**: 향후 정렬 조건이 추가되면 인덱스도 추가

결국 **단순한 단일 컬럼 인덱스** 여러 개가 더 유연하다고 판단했습니다.

### 2단계: 비정규화로 조회 성능 개선

**문제점: COUNT() 집계의 비용**

```sql
-- 기존: 매번 JOIN + GROUP BY + COUNT
SELECT p.*, COUNT(l.id) as like_count
FROM products p
LEFT JOIN likes l ON p.id = l.product_id AND l.deleted_at IS NULL
WHERE p.deleted_at IS NULL
GROUP BY p.id
ORDER BY like_count DESC;
-- 실행 시간: ~200ms (100,000개 상품 + 500,000개 좋아요)
```

**해결: likeCount 컬럼 추가**

```java
@Entity
public class Product extends BaseEntity {
    // ...
    @Column(nullable = false)
    private Integer likeCount = 0;  // 비정규화!

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
```

```sql
-- 개선: 단순 정렬만
SELECT p.* FROM products p
WHERE p.deleted_at IS NULL
ORDER BY p.like_count DESC;
-- 실행 시간: ~15ms (인덱스 사용)
```

**트레이드오프:**
- ✅ 조회 성능: 200ms → 15ms (13배 향상)
- ❌ 쓰기 복잡도: 좋아요 추가/삭제 시 Product도 업데이트
- ❌ 데이터 정합성: likeCount와 실제 likes 테이블이 불일치할 위험

**정합성 보장 전략:**

```java
@Transactional
public void like(String userId, Long productId) {
    // 비관적 락으로 동시성 제어
    Product product = productRepository.findByIdWithLock(productId)
        .orElseThrow(...);

    Like like = Like.builder()
        .userId(userId)
        .productId(productId)
        .build();
    likeRepository.save(like);

    // 같은 트랜잭션 내에서 likeCount 증가
    product.incrementLikeCount();
}
```

### 3단계: Redis 캐시 전략

**실시간성 vs 정합성의 균형**

멘토님이 강조하신 핵심 트레이드오프:

> "메모지가 더 이상 유효하지 않을 때... 최신화되지 않는 것"

**우리 서비스의 특성:**
```
실시간성 (조회 속도) >>> 정합성 (데이터 정확성)
```

상품 정보는 초 단위로 변경되지 않습니다. 1-5분 정도의 지연은 허용 가능합니다.

**TTL 전략 설계**

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 상품 상세: 5분 TTL
        // 이유: 상품 정보는 자주 변경되지 않음, 조회 빈도 높음
        cacheConfigurations.put("product",
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 상품 목록: 1분 TTL
        // 이유: 정렬 순서가 자주 바뀜 (좋아요 증가), 페이지마다 다른 결과
        cacheConfigurations.put("products",
            defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

**왜 상품 목록은 1분, 상품 상세는 5분?**

1. **상품 상세**
   - 변경 빈도: 낮음 (상품 정보 수정은 드묾)
   - 조회 빈도: 매우 높음 (사용자가 특정 상품을 자주 봄)
   - 캐시 히트율: 높음
   - → **5분 TTL**로 조회 성능 극대화

2. **상품 목록**
   - 변경 빈도: 높음 (좋아요가 계속 증가하면 순서 변경)
   - 조회 빈도: 높음
   - 캐시 키 분산: 페이지마다 다름
   - → **1분 TTL**로 실시간성과 성능 균형

**캐시 무효화 전략**

```java
@Service
public class ProductService {
    // 조회: 캐시 사용
    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id)...;
    }

    // 수정: 캐시 무효화
    @CacheEvict(value = "product", key = "#id")
    public Product updateProduct(Long id, ...) {
        Product product = getProduct(id);
        product.updateInfo(...);
        return product;
    }
}
```

**멘토님이 언급하신 대안들:**

| 전략 | 장점 | 단점 | 우리의 선택 |
|------|------|------|-------------|
| **긴 TTL (10분)** | 안정적, DB 부하 최소 | 최신 데이터 반영 느림 | ❌ |
| **짧은 TTL (10초)** | 실시간성 좋음 | 캐시 효과 낮음 | ❌ |
| **배치 갱신** | 일관된 업데이트 | 배치 실행 주기만큼 지연 | ❌ |
| **Write-Through** | 항상 최신 데이터 | 쓰기 성능 저하, 복잡도 증가 | ❌ |
| **TTL + CacheEvict** | 균형있는 접근 | 완벽한 실시간성은 아님 | ✅ |

**최종 선택: TTL + CacheEvict**

- 읽기 > 쓰기 비율이 높은 서비스 (상품 조회가 상품 수정보다 훨씬 많음)
- 1-5분 정도의 데이터 지연은 비즈니스적으로 허용
- 상품 수정 시 명시적으로 캐시 무효화하여 보완

## N+1 문제와 Lazy Loading

**예상치 못한 문제**

캐시를 적용했는데 오히려 에러가 발생했습니다:

```
LazyInitializationException: Could not initialize proxy [Brand#1] - no session
```

**원인:**

```java
@Cacheable("product")
public Product getProduct(Long id) {
    return productRepository.findById(id);  // Brand는 LAZY
}

// 캐시에 저장하려고 Product를 직렬화할 때
// Brand의 Lazy 프록시를 읽으려고 하지만 Session이 이미 종료됨
```

**해결: DTO 변환 방식**

엔티티를 직접 캐싱하는 대신, **트랜잭션 내에서 DTO로 변환 후 캐싱**하는 방식을 선택했습니다.

```java
@Transactional(readOnly = true)
@Cacheable(value = "product", key = "#id")
public ProductInfo getProduct(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    // 트랜잭션 내에서 Brand를 명시적으로 로딩
    product.getBrand().getName();

    // DTO로 변환하여 반환 (캐시에는 DTO가 저장됨)
    return ProductInfo.from(product);
}
```

**왜 DTO를 캐싱하는가?**

1. **프록시 문제 회피**: DTO는 일반 객체라 Lazy 프록시가 없음
2. **직렬화 안전**: Redis 직렬화 시 예외 발생 없음
3. **불필요한 데이터 제거**: 엔티티의 모든 필드가 아닌 필요한 것만 캐싱
4. **레이어 분리**: Controller는 DTO만 다루므로 레이어 간 책임 명확

**트레이드오프:**
- ✅ Lazy Loading 문제 완전 해결
- ✅ 캐시 직렬화 안전
- ✅ Controller에서 추가 변환 불필요
- ❌ Service에서 엔티티가 아닌 DTO를 반환 (일부 메서드는 엔티티 필요)
- ❌ DTO 변환 코드 추가 필요

**대안으로 고려했던 방법:**

| 방법 | 장점 | 단점 | 선택 여부 |
|------|------|------|-----------|
| **Fetch Join** | 쿼리만 수정, 엔티티 반환 | 항상 JOIN 발생 | ❌ |
| **@EntityGraph** | 동적 페치 전략 | 복잡도 증가 | ❌ |
| **DTO 변환** | 안전한 직렬화, 레이어 분리 | DTO 변환 코드 필요 | ✅ |
| **Eager Fetch** | 간단함 | 모든 조회에 JOIN 발생 | ❌ |

상품 정보는 항상 브랜드 정보가 필요하고, Controller에서 어차피 DTO로 변환하므로 **Service에서 미리 DTO로 변환하는 것이 더 효율적**이라고 판단했습니다.

## 성능 측정과 검증

### k6 부하 테스트

**시나리오 설계**

```javascript
export let options = {
  stages: [
    { duration: '30s', target: 10 },   // Warm-up
    { duration: '1m', target: 50 },    // Ramp-up
    { duration: '2m', target: 50 },    // Steady
    { duration: '1m', target: 100 },   // Peak
    { duration: '1m', target: 100 },   // Peak hold
    { duration: '30s', target: 0 },    // Ramp-down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'],
    'errors': ['rate<0.1'],
  },
};
```

**테스트 환경**
- 데이터: 브랜드 100개, 상품 100,000개, 좋아요 500,000개
- 부하: 6분간 최대 20명 동시 접속 (VUs)
- 시나리오: 1분 Ramp-up → 3분 Steady → 1분 Peak → 1분 Ramp-down

**TO-BE 테스트 결과 (인덱스 + 캐시)**

| 지표 | 목표 | 실제 결과 | 평가 |
|------|------|-----------|------|
| **성공률** | 99% 이상 | 99.73% | ✅ |
| **총 요청** | - | 5,268개 | ✅ |
| **에러율** | 1% 미만 | 0.54% | ✅ |
| **평균 응답 시간** | < 50ms | 19.22ms | ✅ |
| **p(95) 응답 시간** | < 500ms | 31.68ms | ✅ |
| **p(90) 응답 시간** | - | 29.71ms | ✅ |
| **처리량 (RPS)** | > 10 req/s | 14.59 req/s | ✅ |

**AS-IS vs TO-BE 성능 비교**

실제 k6 부하 테스트 결과:

| 지표 | AS-IS<br/>(인덱스 없음) | TO-BE<br/>(인덱스 + 캐시) | 개선율 |
|------|------------------------|---------------------------|--------|
| **평균 응답 시간** | 32.53ms | 19.22ms | **41% 개선** ⬇️ |
| **p(95) 응답 시간** | 74.77ms | 31.68ms | **58% 개선** ⬇️ |
| **p(90) 응답 시간** | 71.22ms | 29.71ms | **58% 개선** ⬇️ |
| **에러율** | 0.55% | 0.54% | 거의 동일 |
| **처리량 (RPS)** | 14.28 req/s | 14.59 req/s | 약간 향상 |

**핵심 인사이트:**
- p(95) 기준 43ms 단축 (74.77ms → 31.68ms)
- 인덱스와 캐시 최적화로 **1.7배~2.4배 성능 향상**
- 95%의 요청이 32ms 이내 처리 (목표 500ms 대비 15배 빠름)

## 배운 점과 회고

### 1. 모든 최적화는 트레이드오프다

**인덱스**
- 읽기 ↑ vs 쓰기 ↓
- 유연성 ↓ vs 성능 ↑

**비정규화**
- 조회 성능 ↑ vs 데이터 정합성 위험 ↑
- 단순성 ↓ vs 속도 ↑

**캐시**
- 응답 속도 ↑ vs 실시간성 ↓
- 메모리 사용 ↑ vs DB 부하 ↓

### 2. 비즈니스 요구사항이 기술 선택을 결정한다

"상품 조회 서비스"의 특성:
- 읽기 >> 쓰기 (읽기가 쓰기의 100배 이상)
- 약간의 데이터 지연은 허용 (좋아요 수가 1분 늦게 반영되어도 OK)
- 높은 트래픽 (100+ 동시 사용자)

→ **캐시 + 비정규화 + 인덱스** 조합이 최적

### 3. 측정 가능해야 개선 가능하다

k6 부하 테스트로:
- 실제 사용자 환경 시뮬레이션
- 병목 지점 정확히 파악
- 개선 효과 정량적 검증

### 4. 완벽한 실시간성은 환상이다

멘토님이 지적하신 것처럼:
> "나는 최신성도 포기할 수 없어... 1분마다 배치? 10초? 2초?"

결국 **어느 정도의 지연은 허용**해야 합니다. 우리는 1-5분을 선택했고, 이는 비즈니스적으로 충분히 허용 가능한 수준입니다.

### 5. 동시성 제어의 중요성

비정규화를 도입하면서 가장 신경 쓴 부분:

```java
@Transactional
public void like(String userId, Long productId) {
    // 비관적 락으로 동시성 제어
    Product product = productRepository.findByIdWithLock(productId);
    // ...
    product.incrementLikeCount();
}
```

락 없이는 likeCount와 실제 좋아요 수가 불일치할 수 있습니다.

## AS-IS vs TO-BE 성능 비교

### 자동화 테스트 스크립트

인덱스 적용 전후를 공정하게 비교하기 위해 자동화 스크립트를 작성했습니다:

```bash
#!/bin/bash
# performance-test/run-comparison-test.sh

# AS-IS 테스트 (인덱스 제거)
echo "AS-IS 테스트 (인덱스 없음)"
mysql ... < performance-test/sql/08-remove-indexes.sql
docker exec redis-master redis-cli FLUSHDB
k6 run performance-test/k6/product-load-test-fixed.js \
  2>&1 | tee performance-test/results/as-is-result.txt

# TO-BE 테스트 (인덱스 적용)
echo "TO-BE 테스트 (인덱스 + 비정규화 + 캐시)"
mysql ... < performance-test/sql/04-create-indexes.sql
docker exec redis-master redis-cli FLUSHDB
k6 run performance-test/k6/product-load-test-fixed.js \
  2>&1 | tee performance-test/results/to-be-result.txt

# 결과 비교
echo "========== AS-IS (인덱스 없음) =========="
grep -A 15 "k6 Performance Test Summary" as-is-result.txt

echo "========== TO-BE (인덱스 + 캐시) =========="
grep -A 15 "k6 Performance Test Summary" to-be-result.txt
```

### 성능 비교 결과

| 지표 | AS-IS<br/>(인덱스 없음) | TO-BE<br/>(인덱스 + 캐시) | 개선율 |
|------|-------------------------|---------------------------|--------|
| **평균 응답 시간** | 32.53ms | 19.22ms | **41% 개선** ⬇️ |
| **p(95) 응답 시간** | 74.77ms | 31.68ms | **58% 개선** ⬇️ |
| **p(90) 응답 시간** | 71.22ms | 29.71ms | **58% 개선** ⬇️ |
| **에러율** | 0.55% | 0.54% | 거의 동일 |
| **성공률** | 99.78% | 99.73% | 거의 동일 |
| **처리량 (RPS)** | 14.28 req/s | 14.59 req/s | 약간 향상 |
| **총 요청 (6분)** | 5,172개 | 5,268개 | 약간 향상 |

### EXPLAIN으로 확인하는 차이

**AS-IS (인덱스 없음):**
```sql
EXPLAIN SELECT p.* FROM products p
WHERE p.brand_id = 1 AND p.deleted_at IS NULL
ORDER BY p.like_count DESC LIMIT 20;

+------+-------+------+---------+------+-------------+
| type | rows  | key  | Extra                         |
+------+-------+------+---------+------+-------------+
| ALL  | 99847 | NULL | Using where; Using filesort   |
+------+-------+------+---------+------+-------------+
```
- 10만건 전체 스캔
- 메모리에서 정렬 (filesort)
- 실행 시간: ~200ms

**TO-BE (인덱스 적용):**
```sql
EXPLAIN SELECT p.* FROM products p
WHERE p.brand_id = 1 AND p.deleted_at IS NULL
ORDER BY p.like_count DESC LIMIT 20;

+------+------+---------------------------+-------------+
| type | rows | key                       | Extra       |
+------+------+---------------------------+-------------+
| ref  |   42 | idx_products_brand_deleted| Using index |
+------+------+---------------------------+-------------+
```
- 42건만 스캔
- 인덱스로 정렬 해결
- 실행 시간: ~5ms

### 다음 단계로 고려할 수 있는 최적화

현재 달성한 성능(p95 31.68ms, 성공률 99.73%)은 충분히 만족스럽지만, 더 높은 트래픽이나 복잡한 요구사항이 생긴다면 다음과 같은 최적화를 고려할 수 있습니다:

1. **부하 테스트 확장**
   - 더 높은 동시 사용자 (100, 500, 1000)로 병목 지점 파악
   - 다양한 쿼리 패턴 시뮬레이션 (검색, 필터 조합)

2. **캐시 워밍 전략**
   - 애플리케이션 시작 시 주요 데이터 미리 캐싱
   - Cold start 문제 해결로 초기 응답 시간 개선

3. **Read Replica 도입**
   - 읽기 요청을 Replica로 분산
   - 쓰기/읽기 분리로 Master DB 부하 감소

4. **ElasticSearch 도입**
   - 복잡한 검색 쿼리 (브랜드 + 가격대 + 키워드 조합)
   - 전문 검색 엔진의 전문 검색, 자동완성, 필터링 기능 활용

5. **CDN 적용**
   - 정적 상품 이미지 CDN 캐싱
   - Edge Location에서 제공하여 지연 시간 최소화

## 마치며

"빠른 조회"라는 간단해 보이는 요구사항 뒤에는 수많은 선택과 트레이드오프가 있었습니다.

**핵심은**:
1. 비즈니스 요구사항을 정확히 이해하기
2. 각 기술의 장단점 파악하기
3. 측정 가능한 지표로 검증하기
4. 완벽함보다 실용성 추구하기

이번 최적화를 통해 **99.73% 성공률, p(95) 31.68ms**를 달성했습니다. 인덱스 없는 상태 대비 p(95) 기준 58% 성능 향상(74.77ms → 31.68ms)을 달성했으며, 이는 현재 서비스 규모와 요구사항에 충분히 만족스러운 결과입니다.

---

**참고 자료:**
- [Spring Cache 공식 문서](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [MySQL 인덱스 최적화 가이드](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html)
- [k6 부하 테스트 문서](https://k6.io/docs/)
- [Redis 캐싱 전략](https://redis.io/docs/manual/patterns/)

---

## 📋 Round 5 실행 가이드

### 사전 준비

**1. 인프라 실행**
```bash
# MySQL + Redis 실행
docker-compose -f docker/infra-compose.yml up -d mysql redis-master

# MySQL 연결 확인
mysql -h 127.0.0.1 -P 3306 -u application -papplication -e "SELECT 1"
```

**2. 대량 데이터 생성**
```bash
# 브랜드 100개
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/01-insert-brands.sql

# 상품 100,000개
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/02-insert-products.sql

# 좋아요 500,000개
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/03-insert-likes.sql

# 인덱스 생성
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/04-create-indexes.sql

# likeCount 마이그레이션
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/06-migrate-like-count.sql
```

**3. 애플리케이션 실행**
```bash
./gradlew :apps:commerce-api:bootRun
```

### AS-IS vs TO-BE 성능 테스트

**k6 설치 (macOS)**
```bash
brew install k6
```

**AS-IS 테스트 (인덱스 없음)**
```bash
# 1. 인덱스 제거
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/08-remove-indexes.sql

# 2. Redis 캐시 비우기
docker exec redis-master redis-cli FLUSHALL

# 3. k6 테스트 실행
k6 run performance-test/k6/product-load-test-fixed.js
```

**TO-BE 테스트 (인덱스 + 캐시)**
```bash
# 1. 인덱스 생성
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/04-create-indexes.sql

# 2. Redis 캐시 비우기
docker exec redis-master redis-cli FLUSHALL

# 3. k6 테스트 실행
k6 run performance-test/k6/product-load-test-fixed.js
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew :apps:commerce-api:test

# 특정 테스트만 실행
./gradlew :apps:commerce-api:test --tests "*LikeService*"
./gradlew :apps:commerce-api:test --tests "*ProductService*"
```

### EXPLAIN 분석으로 인덱스 효과 확인

```bash
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/05-explain-queries.sql
```

### 주의사항

1. **데이터 생성 시간**: 100,000개 상품 삽입은 약 1-2분 소요
2. **likeCount 마이그레이션**: 500,000개 좋아요 집계는 약 10-20초 소요
3. **캐시 워밍업**: 첫 요청은 캐시 미스로 약간 느릴 수 있음
4. **Redis 필수**: 애플리케이션 실행 시 Redis가 반드시 실행 중이어야 함

### 문제 해결

**Port 8080 already in use**
```bash
lsof -ti :8080 | xargs kill -9
```

**Redis connection refused**
```bash
docker start redis-master
```

**MySQL connection refused**
```bash
docker start mysql
```
