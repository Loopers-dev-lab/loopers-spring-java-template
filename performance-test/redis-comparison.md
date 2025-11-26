# RedisTemplate vs Spring Cache 비교

## 📋 개요
Redis를 사용하는 두 가지 방법의 장단점과 사용 시나리오를 비교합니다.

---

## 🎯 방법 1: Spring Cache (`@Cacheable`)

### 현재 프로젝트 적용 방식

```java
// ProductService.java
@Cacheable(value = "product", key = "#id")
public Product getProduct(Long id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
}

@CacheEvict(value = "product", key = "#id")
public Product updateProduct(Long id, ...) {
    // 상품 수정 시 캐시 무효화
}
```

### 장점 ✅
1. **간단함**: 어노테이션만 추가하면 끝
2. **선언적**: 비즈니스 로직과 캐시 로직 분리
3. **유지보수 쉬움**: 캐시 전략 변경 시 코드 수정 최소화
4. **추상화**: Redis 외 다른 캐시 구현체로 쉽게 변경 가능

### 단점 ❌
1. **유연성 부족**: 복잡한 캐시 로직 구현 어려움
2. **세밀한 제어 불가**: 캐시 히트/미스 로깅 어려움
3. **Redis 고급 기능 사용 불가**: Sorted Set, Pub/Sub 등

### 적합한 경우 ✅
- ✅ 단순 조회 API (현재 상품 상세/목록)
- ✅ CRUD 중심 애플리케이션
- ✅ 빠른 개발 속도가 중요한 경우

---

## 🛠️ 방법 2: RedisTemplate

### 예시 구현

```java
// ProductCacheService.java
@Service
@RequiredArgsConstructor
public class ProductCacheService {
    private final RedisTemplate<String, String> redisTemplate;

    public Long incrementViewCount(Long productId) {
        String key = "product:view:" + productId;
        return redisTemplate.opsForValue().increment(key);
    }

    public Long getViewCount(Long productId) {
        String key = "product:view:" + productId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
```

### 장점 ✅
1. **유연성**: 복잡한 캐시 로직 자유롭게 구현
2. **세밀한 제어**: 캐시 히트/미스 로깅, 통계 수집 가능
3. **Redis 고급 기능**: Sorted Set (랭킹), Pub/Sub (실시간 알림) 등 사용 가능
4. **실시간 데이터**: 조회수, 좋아요 수 등 실시간 카운팅

### 단점 ❌
1. **복잡함**: 직접 캐시 로직 구현 필요
2. **코드 중복**: 키 생성, 직렬화, TTL 설정 등 보일러플레이트
3. **유지보수**: 캐시 전략 변경 시 코드 수정 많음
4. **추상화 부족**: Redis에 강하게 의존

### 적합한 경우 ✅
- ✅ 실시간 카운팅 (조회수, 좋아요 수)
- ✅ 랭킹 시스템 (인기 상품 Top 10)
- ✅ 실시간 알림 (Pub/Sub)
- ✅ 분산 락 (동시성 제어)
- ✅ 세션 관리

---

## 🎯 추천 전략: **하이브리드 방식**

### 현재 프로젝트 최적 구성

| 기능 | 방식 | 이유 |
|------|------|------|
| **상품 상세 조회** | Spring Cache | 단순 조회, 캐시 히트율 높음 |
| **상품 목록 조회** | Spring Cache | 단순 조회, TTL 1분으로 충분 |
| **조회수 카운팅** | RedisTemplate | 실시간 반영 필요 |
| **좋아요 수** | DB (likeCount) | 정합성 중요, 캐시 불필요 |
| **인기 상품 랭킹** | RedisTemplate (Sorted Set) | 실시간 순위 변경 필요 |

---

## 📊 성능 비교

### Spring Cache
```java
// 첫 요청: DB 조회 (100ms)
Product product = productService.getProduct(1L);

// 두 번째 요청: 캐시 히트 (1ms)
Product product = productService.getProduct(1L);

// TTL 5분 후: 캐시 미스, DB 재조회
```

**장점**: 자동으로 캐시 관리, 코드 간결

### RedisTemplate
```java
// 조회수 증가 (1ms)
Long viewCount = productCacheService.incrementViewCount(1L);

// 조회수 조회 (1ms)
Long viewCount = productCacheService.getViewCount(1L);

// TTL 직접 관리 필요
```

**장점**: 실시간 반영, 원자성 보장 (INCR)

---

## 🚀 실전 코드 예시

### 시나리오 1: 단순 조회 (Spring Cache 사용) ⭐ 추천
```java
@Service
@RequiredArgsConstructor
public class ProductService {

    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id)...;
    }
}
```

**코드 라인**: 3줄
**복잡도**: 매우 낮음
**유지보수**: 쉬움

---

### 시나리오 2: 조회수 카운팅 (RedisTemplate 사용)
```java
@Service
@RequiredArgsConstructor
public class ProductCacheService {
    private final RedisTemplate<String, String> redisTemplate;

    public Long incrementViewCount(Long productId) {
        String key = "product:view:" + productId;
        return redisTemplate.opsForValue().increment(key);
    }
}

@Service
@RequiredArgsConstructor
public class ProductServiceWithViewCount {
    private final ProductService productService;
    private final ProductCacheService productCacheService;

    public ProductWithViewCount getProductWithViewCount(Long id) {
        Product product = productService.getProduct(id);
        Long viewCount = productCacheService.incrementViewCount(id);
        return new ProductWithViewCount(product, viewCount);
    }
}
```

**코드 라인**: 15줄
**복잡도**: 중간
**유지보수**: 중간

---

### 시나리오 3: 인기 상품 랭킹 (RedisTemplate Sorted Set)
```java
@Service
@RequiredArgsConstructor
public class ProductRankingService {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String RANKING_KEY = "product:ranking:view";

    // 조회수 증가 + 랭킹 업데이트
    public void incrementViewAndUpdateRanking(Long productId) {
        redisTemplate.opsForZSet()
            .incrementScore(RANKING_KEY, String.valueOf(productId), 1);
    }

    // Top 10 인기 상품 조회
    public List<Long> getTop10Products() {
        Set<String> productIds = redisTemplate.opsForZSet()
            .reverseRange(RANKING_KEY, 0, 9);
        return productIds.stream()
            .map(Long::parseLong)
            .toList();
    }
}
```

**사용 케이스**: 실시간 인기 상품 순위
**Spring Cache로 불가능**: Sorted Set 기능 필요

---

## 📝 블로그 포스팅 시 추천 구성

### 기본 내용 (현재 구현)
```markdown
## Redis 캐시 적용

### Spring Cache 사용
- 상품 상세 조회: @Cacheable
- TTL 5분
- 캐시 무효화: @CacheEvict

### 성능 개선 결과
- 첫 요청: 100ms
- 캐시 히트: 1ms
- **100배 향상**
```

### Nice-to-have (선택적 추가)
```markdown
## RedisTemplate 활용 (추가 개선)

### 조회수 실시간 카운팅
- RedisTemplate 사용
- 원자성 보장 (INCR)
- DB 부하 없음

### 인기 상품 랭킹 (Sorted Set)
- 실시간 순위 업데이트
- Top 10 조회 O(log N)
```

---

## ⚠️ 주의사항

### Spring Cache
1. **Serialization**: 엔티티 직렬화 가능하도록 설정 필요
2. **TTL 관리**: CacheManager에서 중앙 관리
3. **캐시 일관성**: @CacheEvict로 무효화 필수

### RedisTemplate
1. **키 관리**: 네이밍 컨벤션 중요 (`product:view:123`)
2. **TTL 직접 설정**: `redisTemplate.expire()` 호출 필요
3. **에러 처리**: Redis 장애 시 fallback 로직 필요
4. **직렬화**: String, JSON 등 명시적 변환 필요

---

## 🎯 결론

### 현재 프로젝트 (Round 5)
**Spring Cache만으로 충분합니다!**

이유:
- ✅ 요구사항: 단순 조회 성능 개선
- ✅ 구현 난이도: 낮음 (어노테이션만)
- ✅ 유지보수: 쉬움
- ✅ 블로그 임팩트: 충분함

### 향후 확장 시
**RedisTemplate 추가 고려**

사용 케이스:
- 조회수 실시간 카운팅
- 인기 상품 랭킹 (Sorted Set)
- 실시간 알림 (Pub/Sub)
- 분산 락 (동시성 제어)

---

## 📚 참고 자료
- Spring Cache: https://docs.spring.io/spring-framework/reference/integration/cache.html
- RedisTemplate: https://docs.spring.io/spring-data/redis/reference/redis/template.html
- Redis Data Structures: https://redis.io/docs/data-types/
