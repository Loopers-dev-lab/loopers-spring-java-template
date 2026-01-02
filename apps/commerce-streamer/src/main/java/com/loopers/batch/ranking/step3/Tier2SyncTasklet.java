package com.loopers.batch.ranking.step3;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Step 3-2: Tier 2 Redis Hash 동기화 Tasklet
 * 
 * 모든 랭킹 타입의 Tier 1 ZSET에서 productId를 수집하여
 * ProductView를 조회하고 Redis Hash에 캐싱
 */
@Slf4j
public class Tier2SyncTasklet implements Tasklet {

    private final StringRedisTemplate redisTemplate;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 500;
    private static final long CACHE_TTL_SECONDS = 3600; // 1시간
    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final String[] RANKING_KEYS = {
        "ranking:hourly",
        "ranking:daily",
        "ranking:weekly",
        "ranking:monthly"
    };

    public Tier2SyncTasklet(StringRedisTemplate redisTemplate,
                            EntityManager entityManager,
                            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Tier2SyncTasklet 시작");

        try {
            // 1. Redis 연결 확인
            if (!isRedisAvailable()) {
                log.warn("Redis 연결 실패. Step을 SKIP합니다.");
                contribution.setExitStatus(ExitStatus.COMPLETED);
                return RepeatStatus.FINISHED;
            }

            // 2. 모든 랭킹 타입의 Tier 1 ZSET에서 productId 수집
            Set<Long> allProductIds = collectProductIdsFromRankings();
            
            if (allProductIds.isEmpty()) {
                log.debug("수집된 productId가 없습니다. Step을 SKIP합니다.");
                return RepeatStatus.FINISHED;
            }

            log.debug("수집된 productId: {}건", allProductIds.size());

            // 3. Redis EXISTS 체크 및 TTL 연장 (배치, 500건씩)
            List<Long> uncachedProductIds = checkExistsAndExtendTtl(allProductIds);
            
            log.debug("캐싱되지 않은 productId: {}건", uncachedProductIds.size());

            // 4. ProductView 배치 조회 (500건씩)
            if (!uncachedProductIds.isEmpty()) {
                cacheProductViews(uncachedProductIds);
            }

            log.info("Tier2SyncTasklet 완료: total={}건, cached={}건, new={}건", 
                allProductIds.size(), allProductIds.size() - uncachedProductIds.size(), uncachedProductIds.size());
            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            log.error("Tier2SyncTasklet 실패", e);
            // Redis 장애 시 Step을 SKIP하고 배치는 성공 상태로 완료
            contribution.setExitStatus(ExitStatus.COMPLETED);
            return RepeatStatus.FINISHED;
        }
    }

    /**
     * 모든 랭킹 타입의 Tier 1 ZSET에서 productId 수집 및 중복 제거
     */
    private Set<Long> collectProductIdsFromRankings() {
        Set<Long> productIds = new HashSet<>();
        
        for (String rankingKey : RANKING_KEYS) {
            try {
                // 상위 500개 productId 조회 (ZREVRANGE)
                Set<String> memberIds = redisTemplate.opsForZSet()
                    .reverseRange(rankingKey, 0, 499);
                
                if (memberIds != null) {
                    for (String memberId : memberIds) {
                        try {
                            productIds.add(Long.parseLong(memberId));
                        } catch (NumberFormatException e) {
                            log.warn("Invalid productId format: {}", memberId);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("랭킹 키에서 productId 수집 실패: key={}", rankingKey, e);
            }
        }
        
        return productIds;
    }

    /**
     * Redis EXISTS 체크 및 TTL 연장 (배치, 500건씩)
     * 이미 캐싱된 상품은 TTL 연장, 캐싱되지 않은 상품은 목록 반환
     */
    private List<Long> checkExistsAndExtendTtl(Set<Long> productIds) {
        List<Long> uncachedProductIds = new ArrayList<>();
        List<Long> productIdList = new ArrayList<>(productIds);
        
        // 배치 단위로 처리 (500건씩)
        for (int i = 0; i < productIdList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, productIdList.size());
            List<Long> batch = productIdList.subList(i, end);
            
            // Pipeline을 사용한 배치 EXISTS 체크
            List<Object> results = redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    var keyCommands = connection.keyCommands();
                    for (Long productId : batch) {
                        keyCommands.exists(getProductDetailKey(productId).getBytes());
                    }
                    return null;
                }
            );
            
            // EXISTS 결과 확인 및 TTL 연장
            for (int j = 0; j < batch.size(); j++) {
                Long productId = batch.get(j);
                Boolean exists = (Boolean) results.get(j);
                
                if (Boolean.TRUE.equals(exists)) {
                    // 이미 캐싱된 상품: TTL 연장
                    redisTemplate.expire(getProductDetailKey(productId), Duration.ofSeconds(CACHE_TTL_SECONDS));
                } else {
                    // 캐싱되지 않은 상품: 목록에 추가
                    uncachedProductIds.add(productId);
                }
            }
        }
        
        return uncachedProductIds;
    }

    /**
     * ProductView 배치 조회 및 Redis Hash 캐싱 (500건씩)
     */
    private void cacheProductViews(List<Long> productIds) {
        // 배치 단위로 처리 (500건씩)
        for (int i = 0; i < productIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, productIds.size());
            List<Long> batch = productIds.subList(i, end);
            
            // 1. ProductView 배치 조회 (QueryDSL 사용)
            // Note: ProductView는 commerce-api 모듈에 있으므로 엔티티 클래스를 직접 참조할 수 없음
            // 대신 네이티브 쿼리나 JPA Query를 사용하여 조회
            List<Map<String, Object>> productViews = findProductViewsByIds(batch);
            
            if (productViews.isEmpty()) {
                continue;
            }
            
            // 2. Redis Hash 일괄 캐싱 (Pipeline)
            redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    var keyCommands = connection.keyCommands();
                    var hashCommands = connection.hashCommands();
                    for (Map<String, Object> productView : productViews) {
                        Long productId = (Long) productView.get("id");
                        String key = getProductDetailKey(productId);
                        
                        try {
                            // ProductView를 JSON으로 직렬화하여 Hash에 저장
                            String json = objectMapper.writeValueAsString(productView);
                            
                            // Hash 필드 설정 (단일 필드로 저장)
                            hashCommands.hSet(key.getBytes(), "data".getBytes(), json.getBytes());
                            
                            // TTL 설정
                            keyCommands.expire(key.getBytes(), CACHE_TTL_SECONDS);
                        } catch (Exception e) {
                            log.warn("ProductView 캐싱 실패: productId={}", productId, e);
                        }
                    }
                    return null;
                }
            );
            
            log.debug("ProductView 캐싱 완료: {}건", productViews.size());
        }
    }

    /**
     * ProductView 배치 조회 (네이티브 쿼리 사용)
     */
    private List<Map<String, Object>> findProductViewsByIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        
        // IN 절을 위한 파라미터 바인딩 (간단한 방식)
        String placeholders = String.join(",", Collections.nCopies(productIds.size(), "?"));
        String sql = "SELECT id, name, price, like_count, brand_id, brand_name, status, created_at " +
                     "FROM product_view WHERE id IN (" + placeholders + ")";
        
        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < productIds.size(); i++) {
            query.setParameter(i + 1, productIds.get(i));
        }
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        
        String[] fieldNames = {"id", "name", "price", "likeCount", "brandId", "brandName", "status", "createdAt"};
        
        return rows.stream()
            .map(row -> {
                Map<String, Object> productView = new HashMap<>();
                for (int i = 0; i < fieldNames.length && i < row.length; i++) {
                    productView.put(fieldNames[i], row[i]);
                }
                return productView;
            })
            .collect(Collectors.toList());
    }

    /**
     * ProductDetail Redis 키 생성
     */
    private String getProductDetailKey(Long productId) {
        return PRODUCT_DETAIL_KEY_PREFIX + productId;
    }

    /**
     * Redis 연결 상태 확인
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.hasKey("ping");
            return true;
        } catch (Exception e) {
            log.debug("Redis 연결 확인 실패", e);
            return false;
        }
    }
}

