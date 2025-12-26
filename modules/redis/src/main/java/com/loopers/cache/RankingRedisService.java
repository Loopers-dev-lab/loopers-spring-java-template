package com.loopers.cache;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;


import com.loopers.cache.dto.CachePayloads;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis ZSET을 이용한 랭킹 시스템 서비스
 *
 * @author hyunjikoh
 * @since 2025.12.23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RankingRedisService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;
    
    private static final Duration RANKING_TTL = Duration.ofDays(2); // 2일 TTL
    
    /**
     * 배치로 랭킹 점수 업데이트 (여러 날짜의 점수 포함 가능)
     * 
     * @param scores 랭킹 점수 리스트
     */
    public void updateRankingScoresBatch(List<CachePayloads.RankingScore> scores) {
        if (scores.isEmpty()) {
            return;
        }
        
        try {
            // 날짜별로 그룹화
            Map<LocalDate, List<CachePayloads.RankingScore>> scoresByDate = scores.stream()
                .collect(Collectors.groupingBy(CachePayloads.RankingScore::getEventDate));
            
            for (Map.Entry<LocalDate, List<CachePayloads.RankingScore>> entry : scoresByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<CachePayloads.RankingScore> dateScores = entry.getValue();
                String rankingKey = cacheKeyGenerator.generateDailyRankingKey(date);
                
                // 해당 날짜의 상품별 점수 집계
                Map<Long, Double> productScores = dateScores.stream()
                    .collect(Collectors.groupingBy(
                        CachePayloads.RankingScore::productId,
                        Collectors.summingDouble(CachePayloads.RankingScore::getWeightedScore)
                    ));
                
                // Redis Pipeline 사용
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    productScores.forEach((productId, totalScore) -> {
                        redisTemplate.opsForZSet().incrementScore(rankingKey, productId.toString(), totalScore);
                    });
                    return null;
                });
                
                // TTL 설정
                redisTemplate.expire(rankingKey, RANKING_TTL);
                
                log.debug("랭킹 점수 배치 업데이트 완료: key={}, products={}", 
                    rankingKey, productScores.size());
            }
                
        } catch (Exception e) {
            log.error("랭킹 점수 배치 업데이트 실패", e);
            throw new RuntimeException("랭킹 업데이트 실패", e);
        }
    }
    
    /**
     * 특정 날짜에 대해 배치로 랭킹 점수 업데이트 (하위 호환성 유지)
     * 
     * @param scores 랭킹 점수 리스트
     * @param targetDate 대상 날짜
     */
    public void updateRankingScoresBatch(List<CachePayloads.RankingScore> scores, LocalDate targetDate) {
        if (scores.isEmpty()) {
            return;
        }
        
        if (targetDate == null) {
            updateRankingScoresBatch(scores);
            return;
        }
        
        String rankingKey = cacheKeyGenerator.generateDailyRankingKey(targetDate);
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        try {
            // 상품별로 점수 집계 (가중치 적용)
            Map<Long, Double> productScores = scores.stream()
                .collect(Collectors.groupingBy(
                    CachePayloads.RankingScore::productId,
                    Collectors.summingDouble(CachePayloads.RankingScore::getWeightedScore)
                ));
            
            // Redis Pipeline을 사용한 배치 업데이트
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                productScores.forEach((productId, totalScore) -> {
                    zSetOps.incrementScore(rankingKey, productId.toString(), totalScore);
                });
                return null;
            });
            
            // TTL 설정
            redisTemplate.expire(rankingKey, RANKING_TTL);
            
            log.debug("랭킹 점수 배치 업데이트 완료: key={}, products={}", 
                rankingKey, productScores.size());
                
        } catch (Exception e) {
            log.error("랭킹 점수 배치 업데이트 실패: key={}", rankingKey, e);
            throw new RuntimeException("랭킹 업데이트 실패", e);
        }
    }
    
    /**
     * 랭킹 조회 (페이징)
     * 
     * @param date 날짜
     * @param page 페이지 (1부터 시작)
     * @param size 페이지 크기
     * @return 랭킹 리스트 (상위부터)
     */
    public List<CachePayloads.RankingItem> getRanking(LocalDate date, int page, int size) {
        // 1-based index (API)를 0-based index (Redis)로 변환하는 로직을 서비스 내부로 캡슐화
        int pageForRedis = Math.max(1, page); 
        
        String rankingKey = cacheKeyGenerator.generateDailyRankingKey(date);
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        try {
            // 페이징 계산 (Redis는 0부터 시작)
            long start = (long) (pageForRedis - 1) * size;
            long end = start + size - 1;
            
            // 점수 높은 순으로 조회 (ZREVRANGE)
            Set<ZSetOperations.TypedTuple<String>> rankingData = 
                zSetOps.reverseRangeWithScores(rankingKey, start, end);
            
            if (rankingData == null || rankingData.isEmpty()) {
                log.debug("랭킹 데이터 없음: key={}, page={}, size={}", rankingKey, page, size);
                return List.of();
            }
            
            // 순위 계산 (페이징 고려)
            List<CachePayloads.RankingItem> result = new java.util.ArrayList<>();
            long currentRank = start + 1; // 1부터 시작하는 순위
            
            for (ZSetOperations.TypedTuple<String> tuple : rankingData) {
                try {
                    Long productId = Long.parseLong(tuple.getValue());
                    Double score = tuple.getScore();
                    result.add(new CachePayloads.RankingItem(currentRank, productId, score));
                    currentRank++;
                } catch (NumberFormatException e) {
                    log.warn("잘못된 상품 ID 형식: {}", tuple.getValue(), e);
                }
            }
            
            log.debug("랭킹 조회 완료: key={}, page={}, size={}, results={}", 
                rankingKey, page, size, result.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("랭킹 조회 실패: key={}, page={}, size={}", rankingKey, page, size, e);
            return List.of();
        }
    }
    
    /**
     * 특정 상품의 랭킹 조회
     * 
     * @param date 날짜
     * @param productId 상품 ID
     * @return 랭킹 정보 (없으면 null)
     */
    public CachePayloads.RankingItem getProductRanking(LocalDate date, Long productId) {
        String rankingKey = cacheKeyGenerator.generateDailyRankingKey(date);
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        try {
            // 점수 조회
            Double score = zSetOps.score(rankingKey, productId.toString());
            if (score == null) {
                log.debug("상품 랭킹 없음: key={}, productId={}", rankingKey, productId);
                return null;
            }
            
            // 순위 조회 (높은 점수부터 순위 매김)
            Long rank = zSetOps.reverseRank(rankingKey, productId.toString());
            if (rank == null) {
                log.warn("점수는 있지만 순위 조회 실패: key={}, productId={}", rankingKey, productId);
                return null;
            }
            
            CachePayloads.RankingItem result = new CachePayloads.RankingItem(rank + 1, productId, score); // Redis rank는 0부터 시작
            log.debug("상품 랭킹 조회 완료: {}", result);
            
            return result;
            
        } catch (Exception e) {
            log.error("상품 랭킹 조회 실패: key={}, productId={}", rankingKey, productId, e);
            return null;
        }
    }
    
    /**
     * 랭킹 전체 개수 조회
     */
    public long getRankingCount(LocalDate date) {
        String rankingKey = cacheKeyGenerator.generateDailyRankingKey(date);
        Long count = redisTemplate.opsForZSet().zCard(rankingKey);
        return count != null ? count : 0L;
    }
    
    /**
     * 랭킹 데이터 존재 여부 확인
     */
    public boolean hasRankingData(LocalDate date) {
        return getRankingCount(date) > 0;
    }

    /**
     * Score Carry-Over: 전날 점수의 일부를 다음 날로 이월
     * <p>
     * 전날 점수에 가중치를 곱해 다음 날 키에 복사
     *
     * @param sourceDate 원본 날짜 (전날)
     * @param targetDate 대상 날짜 (다음 날)
     * @param carryOverWeight 이월 가중치 (0.0 ~ 1.0, 권장: 0.1)
     * @return 이월된 상품 수
     */
    public long carryOverScores(LocalDate sourceDate, LocalDate targetDate, double carryOverWeight) {
        String sourceKey = cacheKeyGenerator.generateDailyRankingKey(sourceDate);
        String targetKey = cacheKeyGenerator.generateDailyRankingKey(targetDate);

        try {
            // 원본 키에서 모든 데이터 조회
            Set<ZSetOperations.TypedTuple<String>> sourceData = 
                    redisTemplate.opsForZSet().rangeWithScores(sourceKey, 0, -1);
            
            if (sourceData == null || sourceData.isEmpty()) {
                log.info("Carry-Over 스킵: 원본 랭킹 데이터 없음 - sourceKey={}", sourceKey);
                return 0;
            }

            // 가중치를 적용하여 대상 키에 추가 (기존 점수에 합산)
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            
            for (ZSetOperations.TypedTuple<String> tuple : sourceData) {
                String member = tuple.getValue();
                Double score = tuple.getScore();
                
                if (member != null && score != null) {
                    double weightedScore = score * carryOverWeight;
                    zSetOps.incrementScore(targetKey, member, weightedScore);
                }
            }

            // TTL 설정
            redisTemplate.expire(targetKey, RANKING_TTL);

            long resultCount = sourceData.size();
            log.info("Score Carry-Over 완료: {} → {} (weight={}, count={})",
                    sourceKey, targetKey, carryOverWeight, resultCount);

            return resultCount;

        } catch (Exception e) {
            log.error("Score Carry-Over 실패: {} → {}", sourceKey, targetKey, e);
            throw new RuntimeException("Score Carry-Over 실패", e);
        }
    }
}
