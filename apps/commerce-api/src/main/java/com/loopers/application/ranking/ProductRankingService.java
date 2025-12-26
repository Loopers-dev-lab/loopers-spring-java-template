package com.loopers.application.ranking;

import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.domain.ranking.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final StringRedisTemplate redisTemplate;
    private final ProductViewRepository productViewRepository;
    private final RankingSnapshotHourlyRepository rankingSnapshotHourlyRepository;
    private final RankingSnapshotDailyRepository rankingSnapshotDailyRepository;
    

    /**
     * 시간 단위 랭킹 조회 (Redis 우선, 실패 시 최신 Hourly 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsHourly(LocalDateTime hour, Pageable pageable) {
        // Redis는 슬라이딩 윈도우 방식이므로 ranking:hourly 키 사용
        String key = "ranking:hourly";
        
        try {
            // Redis에서 조회 시도
            Page<RankingItem> result = getRankingsFromRedis(key, pageable);
            if (result != null && !result.getContent().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get rankings from Redis for hourly, falling back to latest snapshot", e);
        }
        
        // Fallback: 최신 Hourly 스냅샷에서 조회
        return getRankingsFromLatestHourlySnapshot(pageable);
    }

    /**
     * 일 단위 랭킹 조회 (Redis 우선, 실패 시 최신 Daily 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsDaily(LocalDate date, Pageable pageable) {
        // Redis는 슬라이딩 윈도우 방식이므로 ranking:daily 키 사용
        String key = "ranking:daily";
        
        try {
            // Redis에서 조회 시도
            Page<RankingItem> result = getRankingsFromRedis(key, pageable);
            if (result != null && !result.getContent().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get rankings from Redis for daily, falling back to latest snapshot", e);
        }
        
        // Fallback: 최신 Daily 스냅샷에서 조회
        return getRankingsFromLatestDailySnapshot(pageable);
    }

    /**
     * Redis에서 랭킹 조회
     */
    private Page<RankingItem> getRankingsFromRedis(String key, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;
        
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, start, end);
        
        if (tuples == null || tuples.isEmpty()) {
            return null;
        }
        
        List<Long> productIds = tuples.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .collect(Collectors.toList());
        
        List<ProductView> productViews = productViewRepository.findByIds(productIds);
        Map<Long, ProductView> productViewMap = productViews.stream()
                .collect(Collectors.toMap(ProductView::getId, pv -> pv));
        
        List<RankingItem> rankingItems = new ArrayList<>();
        long rank = start + 1;
        for (Long productId : productIds) {
            ProductView productView = productViewMap.get(productId);
            if (productView != null) {
                rankingItems.add(new RankingItem(rank++, productId, productView));
            }
        }
        
        Long totalCount = redisTemplate.opsForZSet().zCard(key);
        if (totalCount == null) {
            totalCount = 0L;
        }
        
        return new PageImpl<>(rankingItems, pageable, totalCount);
    }

    /**
     * 최신 Hourly 스냅샷에서 랭킹 조회
     */
    private Page<RankingItem> getRankingsFromLatestHourlySnapshot(Pageable pageable) {
        // 최신 Hourly 스냅샷의 시간 찾기
        LocalDateTime latestSnapshotTime = rankingSnapshotHourlyRepository
                .findTopByOrderBySnapshotTimeDesc()
                .map(RankingSnapshotHourly::getSnapshotTime)
                .orElse(null);
        
        if (latestSnapshotTime == null) {
            log.warn("No hourly snapshot found, returning empty result");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        log.debug("Using latest hourly snapshot at time: {}", latestSnapshotTime);
        
        // 해당 시간의 모든 스냅샷 조회
        List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyRepository
                .findBySnapshotTimeOrderByTotalScoreDesc(latestSnapshotTime);
        
        List<SnapshotItem> snapshotItems = snapshots.stream()
                .map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore()))
                .collect(Collectors.toList());
        
        return convertSnapshotsToRankingPageInternal(snapshotItems, pageable);
    }

    /**
     * 최신 Daily 스냅샷에서 랭킹 조회
     */
    private Page<RankingItem> getRankingsFromLatestDailySnapshot(Pageable pageable) {
        // 최신 Daily 스냅샷의 시간 찾기
        LocalDateTime latestSnapshotTime = rankingSnapshotDailyRepository
                .findTopByOrderBySnapshotTimeDesc()
                .map(RankingSnapshotDaily::getSnapshotTime)
                .orElse(null);
        
        if (latestSnapshotTime == null) {
            log.warn("No daily snapshot found, returning empty result");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        log.debug("Using latest daily snapshot at time: {}", latestSnapshotTime);
        
        // 해당 시간의 모든 스냅샷 조회
        List<RankingSnapshotDaily> snapshots = rankingSnapshotDailyRepository
                .findBySnapshotTimeOrderByTotalScoreDesc(latestSnapshotTime);
        
        List<SnapshotItem> snapshotItems = snapshots.stream()
                .map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore()))
                .collect(Collectors.toList());
        
        return convertSnapshotsToRankingPageInternal(snapshotItems, pageable);
    }

    /**
     * 스냅샷 데이터를 RankingItem Page로 변환 (내부 구현)
     */
    private Page<RankingItem> convertSnapshotsToRankingPageInternal(
            List<SnapshotItem> snapshotItems, Pageable pageable) {
        
        if (snapshotItems.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        // 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), snapshotItems.size());
        List<SnapshotItem> pagedSnapshots = snapshotItems.subList(start, end);
        
        // 상품 ID 리스트 추출
        List<Long> productIds = pagedSnapshots.stream()
                .map(SnapshotItem::productId)
                .collect(Collectors.toList());
        
        // ProductView 조회
        List<ProductView> productViews = productViewRepository.findByIds(productIds);
        Map<Long, ProductView> productViewMap = productViews.stream()
                .collect(Collectors.toMap(ProductView::getId, pv -> pv));
        
        // RankingItem 생성
        List<RankingItem> rankingItems = new ArrayList<>();
        long rank = start + 1;
        for (SnapshotItem snapshot : pagedSnapshots) {
            ProductView productView = productViewMap.get(snapshot.productId());
            if (productView != null) {
                rankingItems.add(new RankingItem(rank++, snapshot.productId(), productView));
            }
        }
        
        return new PageImpl<>(rankingItems, pageable, snapshotItems.size());
    }

    /**
     * 스냅샷 데이터 임시 저장 (내부 사용)
     */
    private record SnapshotItem(Long productId, Double totalScore) {}

    /**
     * 랭킹 아이템 DTO
     */
    public record RankingItem(
        Long rank,
        Long productId,
        ProductView productView
    ) {}
}

