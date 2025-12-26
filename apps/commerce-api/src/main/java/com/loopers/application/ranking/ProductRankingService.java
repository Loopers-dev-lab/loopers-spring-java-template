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
import java.time.format.DateTimeFormatter;
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
    
    private static final String RANKING_KEY_PREFIX_HOURLY = "ranking:hourly:";
    private static final String RANKING_KEY_PREFIX_DAILY = "ranking:daily:";
    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    /**
     * 특정 날짜의 랭킹 키 생성
     */
    private String getKeyForDate(LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        return RANKING_KEY_PREFIX + dateStr;
    }

    /**
     * Top-N 랭킹 조회 (상품 정보 포함)
     */
    public Page<RankingItem> getTopRankings(LocalDate date, Pageable pageable) {
        String key = getKeyForDate(date);
        
        // 페이지네이션 계산
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;
        
        // Redis에서 상위 ID 조회 (점수 포함)
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, start, end);
        
        if (tuples == null || tuples.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        // ID 리스트 추출
        List<Long> productIds = tuples.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .collect(Collectors.toList());
        
        // ProductView 조회
        List<ProductView> productViews = productViewRepository.findByIds(productIds);
        Map<Long, ProductView> productViewMap = productViews.stream()
                .collect(Collectors.toMap(ProductView::getId, pv -> pv));
        
        // 순위 순서 유지하며 RankingItem 생성
        List<RankingItem> rankingItems = new ArrayList<>();
        long rank = start + 1; // 1-based rank
        for (Long productId : productIds) {
            ProductView productView = productViewMap.get(productId);
            if (productView != null) {
                rankingItems.add(new RankingItem(rank++, productId, productView));
            }
        }
        
        // 전체 개수 조회
        Long totalCount = redisTemplate.opsForZSet().zCard(key);
        if (totalCount == null) {
            totalCount = 0L;
        }
        
        return new PageImpl<>(rankingItems, pageable, totalCount);
    }

    /**
     * 특정 상품의 순위 조회 (1-based)
     */
    public Long getProductRank(Long productId, LocalDate date) {
        String key = getKeyForDate(date);
        Long rank = redisTemplate.opsForZSet().reverseRank(key, productId.toString());
        
        if (rank == null) {
            return null;
        }
        
        // 0-based를 1-based로 변환
        return rank + 1;
    }

    /**
     * 시간 단위 랭킹 조회 (Redis 우선, 실패 시 Hourly 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsHourly(LocalDateTime hour, Pageable pageable) {
        String key = RANKING_KEY_PREFIX_HOURLY + hour.format(HOUR_FORMATTER);
        
        try {
            // Redis에서 조회 시도
            Page<RankingItem> result = getRankingsFromRedis(key, pageable);
            if (result != null && !result.getContent().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get rankings from Redis for hourly: {}, falling back to snapshot", hour, e);
        }
        
        // Fallback: Hourly 스냅샷에서 조회
        LocalDateTime normalizedHour = hour.withMinute(0).withSecond(0).withNano(0);
        return getRankingsFromHourlySnapshot(normalizedHour, pageable);
    }

    /**
     * 일 단위 랭킹 조회 (Redis 우선, 실패 시 Daily 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsDaily(LocalDate date, Pageable pageable) {
        String key = RANKING_KEY_PREFIX_DAILY + date.format(DATE_FORMATTER);
        
        try {
            // Redis에서 조회 시도
            Page<RankingItem> result = getRankingsFromRedis(key, pageable);
            if (result != null && !result.getContent().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get rankings from Redis for daily: {}, falling back to snapshot", date, e);
        }
        
        // Fallback: Daily 스냅샷에서 조회
        LocalDateTime snapshotTime = date.atStartOfDay();
        return getRankingsFromDailySnapshot(snapshotTime, pageable);
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
     * Hourly 스냅샷에서 랭킹 조회
     */
    private Page<RankingItem> getRankingsFromHourlySnapshot(LocalDateTime snapshotTime, Pageable pageable) {
        List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyRepository
                .findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);
        
        List<SnapshotItem> snapshotItems = snapshots.stream()
                .map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore()))
                .collect(Collectors.toList());
        
        return convertSnapshotsToRankingPageInternal(snapshotItems, pageable);
    }

    /**
     * Daily 스냅샷에서 랭킹 조회
     */
    private Page<RankingItem> getRankingsFromDailySnapshot(LocalDateTime snapshotTime, Pageable pageable) {
        List<RankingSnapshotDaily> snapshots = rankingSnapshotDailyRepository
                .findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);
        
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

