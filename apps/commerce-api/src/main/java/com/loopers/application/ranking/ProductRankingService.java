package com.loopers.application.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final String HASH_DATA_FIELD = "data";
    private static final Duration CACHE_TTL = Duration.ofSeconds(3600); // 1시간

    private final StringRedisTemplate redisTemplate;
    private final ProductViewRepository productViewRepository;
    private final RankingSnapshotHourlyRepository rankingSnapshotHourlyRepository;
    private final RankingSnapshotDailyRepository rankingSnapshotDailyRepository;
    private final RankingSnapshotWeeklyRepository rankingSnapshotWeeklyRepository;
    private final RankingSnapshotMonthlyRepository rankingSnapshotMonthlyRepository;
    private final ObjectMapper objectMapper;
    

    /**
     * 시간 단위 랭킹 조회 (Redis 우선, 실패 시 최신 Hourly 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsHourly(LocalDateTime hour, Pageable pageable) {
        return getRankingsWithFallback("ranking:hourly", "hourly", 
            () -> getRankingsFromLatestHourlySnapshot(pageable), pageable);
    }

    /**
     * 일 단위 랭킹 조회 (Redis 우선, 실패 시 최신 Daily 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsDaily(LocalDate date, Pageable pageable) {
        return getRankingsWithFallback("ranking:daily", "daily", 
            () -> getRankingsFromLatestDailySnapshot(pageable), pageable);
    }

    /**
     * 주 단위 랭킹 조회 (Redis 우선, 실패 시 최신 Weekly 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsWeekly(Pageable pageable) {
        return getRankingsWithFallback("ranking:weekly", "weekly", 
            () -> getRankingsFromLatestWeeklySnapshot(pageable), pageable);
    }

    /**
     * 월 단위 랭킹 조회 (Redis 우선, 실패 시 최신 Monthly 스냅샷 Fallback)
     */
    public Page<RankingItem> getTopRankingsMonthly(Pageable pageable) {
        return getRankingsWithFallback("ranking:monthly", "monthly", 
            () -> getRankingsFromLatestMonthlySnapshot(pageable), pageable);
    }

    /**
     * 특정 datetime과 period를 기반으로 랭킹 조회
     * datetime을 period에 맞게 정규화하여 해당 스냅샷을 조회
     * 
     * @param datetime 조회할 datetime (yyyyMMddHHmmss 형식 문자열)
     * @param period 기간 타입 (hourly, daily, weekly, monthly)
     * @param pageable 페이지 정보
     * @return 랭킹 페이지
     */
    public Page<RankingItem> getTopRankingsByDatetime(String datetime, String period, Pageable pageable) {
        LocalDateTime dateTime = parseDatetime(datetime);
        LocalDateTime normalizedTime = normalizeSnapshotTime(dateTime, period);
        
        return switch (period.toLowerCase()) {
            case "hourly" -> getRankingsFromSnapshotTime("hourly", normalizedTime, pageable);
            case "daily" -> getRankingsFromSnapshotTime("daily", normalizedTime, pageable);
            case "weekly" -> getRankingsFromSnapshotTime("weekly", normalizedTime, pageable);
            case "monthly" -> getRankingsFromSnapshotTime("monthly", normalizedTime, pageable);
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };
    }

    /**
     * datetime 문자열 파싱 (yyyyMMddHHmmss 형식)
     */
    private LocalDateTime parseDatetime(String datetime) {
        if (datetime == null || datetime.length() != 14) {
            throw new IllegalArgumentException("datetime must be in yyyyMMddHHmmss format (14 digits)");
        }
        
        try {
            int year = Integer.parseInt(datetime.substring(0, 4));
            int month = Integer.parseInt(datetime.substring(4, 6));
            int day = Integer.parseInt(datetime.substring(6, 8));
            int hour = Integer.parseInt(datetime.substring(8, 10));
            int minute = Integer.parseInt(datetime.substring(10, 12));
            int second = Integer.parseInt(datetime.substring(12, 14));
            
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid datetime format: " + datetime, e);
        }
    }

    /**
     * period에 따라 snapshotTime 정규화
     * - hourly: 정시로 정규화 (예: 10:15:30 → 10:00:00)
     * - daily: 00:00:00으로 정규화 (예: 2025-12-01 10:15:30 → 2025-12-01 00:00:00)
     * - weekly: 해당 주의 월요일 00:00:00으로 정규화 (예: 2025-12-01(월) → 2025-11-24(월) 00:00:00)
     * - monthly: 해당 월의 1일 00:00:00으로 정규화 (예: 2025-12-15 → 2025-12-01 00:00:00)
     */
    private LocalDateTime normalizeSnapshotTime(LocalDateTime dateTime, String period) {
        return switch (period.toLowerCase()) {
            case "hourly" -> dateTime.withMinute(0).withSecond(0).withNano(0);
            case "daily" -> dateTime.toLocalDate().atStartOfDay();
            case "weekly" -> {
                // 해당 주의 월요일 00:00:00으로 정규화
                DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
                int daysToSubtract = (dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7;
                LocalDateTime monday = dateTime.toLocalDate().minusDays(daysToSubtract).atStartOfDay();
                yield monday;
            }
            case "monthly" -> dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };
    }

    /**
     * 특정 snapshotTime으로 스냅샷 조회
     */
    private Page<RankingItem> getRankingsFromSnapshotTime(String period, LocalDateTime snapshotTime, Pageable pageable) {
        return switch (period.toLowerCase()) {
            case "hourly" -> {
                List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyRepository
                    .findBySnapshotTimeOrderByProductRank(snapshotTime);
                yield convertSnapshotsToRankingPage(
                    snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
                    snapshotTime,
                    "hourly",
                    pageable
                );
            }
            case "daily" -> {
                List<RankingSnapshotDaily> snapshots = rankingSnapshotDailyRepository
                    .findBySnapshotTimeOrderByProductRank(snapshotTime);
                yield convertSnapshotsToRankingPage(
                    snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
                    snapshotTime,
                    "daily",
                    pageable
                );
            }
            case "weekly" -> {
                List<RankingSnapshotWeekly> snapshots = rankingSnapshotWeeklyRepository
                    .findBySnapshotTimeOrderByProductRank(snapshotTime);
                yield convertSnapshotsToRankingPage(
                    snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
                    snapshotTime,
                    "weekly",
                    pageable
                );
            }
            case "monthly" -> {
                List<RankingSnapshotMonthly> snapshots = rankingSnapshotMonthlyRepository
                    .findBySnapshotTimeOrderByProductRank(snapshotTime);
                yield convertSnapshotsToRankingPage(
                    snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
                    snapshotTime,
                    "monthly",
                    pageable
                );
            }
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };
    }

    /**
     * Redis에서 랭킹 조회 시도, 실패 시 Fallback 실행
     */
    private Page<RankingItem> getRankingsWithFallback(String redisKey, String type, 
                                                      Supplier<Page<RankingItem>> fallback, Pageable pageable) {
        try {
            Page<RankingItem> result = getRankingsFromRedis(redisKey, pageable, null);
            if (result != null && !result.getContent().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get rankings from Redis for {}, falling back to latest snapshot", type, e);
        }
        
        return fallback.get();
    }

    /**
     * 특정 상품의 랭킹 순위 조회 (Redis 우선, 실패 시 최신 Daily 스냅샷 Fallback)
     * 
     * @param productId 상품 ID
     * @param date 조회할 날짜 (현재는 사용하지 않지만 향후 확장 가능)
     * @return 상품의 랭킹 순위 (1부터 시작), 랭킹에 없으면 null
     */
    public Long getProductRank(Long productId, LocalDate date) {
        String key = "ranking:daily";
        String productIdStr = productId.toString();
        
        try {
            // Redis에서 순위 조회 시도
            Long rank = redisTemplate.opsForZSet().reverseRank(key, productIdStr);
            if (rank != null) {
                // Redis의 rank는 0부터 시작하므로 1부터 시작하는 순위로 변환
                return rank + 1;
            }
        } catch (Exception e) {
            log.warn("Failed to get product rank from Redis for productId: {}, falling back to snapshot", productId, e);
        }
        
        // Fallback: 최신 Daily 스냅샷에서 조회
        return getProductRankFromSnapshot(productId);
    }

    /**
     * 스냅샷에서 상품의 랭킹 순위 조회
     */
    private Long getProductRankFromSnapshot(Long productId) {
        // 최신 Daily 스냅샷의 시간 찾기
        LocalDateTime latestSnapshotTime = rankingSnapshotDailyRepository
                .findTopByOrderBySnapshotTimeDesc()
                .map(RankingSnapshotDaily::getSnapshotTime)
                .orElse(null);
        
        if (latestSnapshotTime == null) {
            log.debug("No daily snapshot found for productId: {}", productId);
            return null;
        }
        
        // 해당 시간의 모든 스냅샷 조회 (점수 내림차순)
        List<RankingSnapshotDaily> snapshots = rankingSnapshotDailyRepository
                .findBySnapshotTimeOrderByTotalScoreDesc(latestSnapshotTime);
        
        // 상품의 순위 찾기
        for (int i = 0; i < snapshots.size(); i++) {
            if (Objects.equals(snapshots.get(i).getProductId(), productId)) {
                // 순위는 1부터 시작
                return (long) (i + 1);
            }
        }
        
        log.debug("Product not found in snapshot for productId: {}", productId);
        return null;
    }

    /**
     * Redis에서 랭킹 조회
     * @param snapshotTime Redis 조회 시는 null, 스냅샷 조회 시는 스냅샷 시간
     */
    private Page<RankingItem> getRankingsFromRedis(String key, Pageable pageable, LocalDateTime snapshotTime) {
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
        
        // Tier 2 캐시에서 조회 (Cache-Aside Pattern)
        Map<Long, ProductView> productViewMap = getProductViewsFromTier2Cache(productIds);
        
        List<RankingItem> rankingItems = new ArrayList<>();
        long rank = start + 1;
        for (Long productId : productIds) {
            ProductView productView = productViewMap.get(productId);
            if (productView != null) {
                rankingItems.add(new RankingItem(rank++, productId, productView, snapshotTime));
            }
        }
        
        Long totalCount = redisTemplate.opsForZSet().zCard(key);
        if (totalCount == null) {
            totalCount = 0L;
        }
        
        return new PageImpl<>(rankingItems, pageable, totalCount);
    }

    /**
     * Tier 2 캐시에서 ProductView 조회 (Cache-Aside Pattern)
     * Cache Miss인 항목은 DB에서 조회 후 캐시 저장
     */
    private Map<Long, ProductView> getProductViewsFromTier2Cache(List<Long> productIds) {
        Map<Long, ProductView> result = new HashMap<>();
        List<Long> missIds = new ArrayList<>();
        
        try {
            // Tier 2 캐시에서 조회
            for (Long productId : productIds) {
                String key = getProductDetailKey(productId);
                try {
                    String json = (String) redisTemplate.opsForHash().get(key, HASH_DATA_FIELD);
                    if (json != null) {
                        ProductView productView = objectMapper.readValue(json, ProductView.class);
                        result.put(productId, productView);
                    } else {
                        missIds.add(productId);
                    }
                } catch (Exception e) {
                    log.debug("Failed to get product from Tier 2 cache: productId={}", productId, e);
                    missIds.add(productId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get products from Tier 2 cache, falling back to DB", e);
            // Redis 장애 시 모든 상품을 DB에서 조회
            missIds = productIds;
            result.clear();
        }
        
        // Cache Miss 항목을 DB에서 조회
        if (!missIds.isEmpty()) {
            List<ProductView> dbViews = getProductViewsFromDb(missIds);
            for (ProductView productView : dbViews) {
                result.put(productView.getId(), productView);
                // 캐시 저장 (비동기 또는 동기, 실패해도 계속 진행)
                try {
                    saveProductViewToTier2Cache(productView);
                } catch (Exception e) {
                    log.debug("Failed to save product to Tier 2 cache: productId={}", productView.getId(), e);
                }
            }
        }
        
        return result;
    }

    /**
     * DB에서 ProductView 조회
     */
    private List<ProductView> getProductViewsFromDb(List<Long> productIds) {
        return productViewRepository.findByIds(productIds);
    }

    /**
     * Tier 2 캐시에 ProductView 저장
     */
    private void saveProductViewToTier2Cache(ProductView productView) {
        try {
            String key = getProductDetailKey(productView.getId());
            String json = objectMapper.writeValueAsString(productView);
            redisTemplate.opsForHash().put(key, HASH_DATA_FIELD, json);
            redisTemplate.expire(key, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to save product to Tier 2 cache: productId={}", productView.getId(), e);
        }
    }

    /**
     * ProductDetail 캐시 키 생성
     */
    private String getProductDetailKey(Long productId) {
        return PRODUCT_DETAIL_KEY_PREFIX + productId;
    }

    /**
     * 최신 Hourly 스냅샷에서 랭킹 조회 (최적화: 2단계 조회를 1단계로 통합)
     */
    private Page<RankingItem> getRankingsFromLatestHourlySnapshot(Pageable pageable) {
        List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyRepository.findLatestSnapshotOrderByProductRank();
        return convertSnapshotsToRankingPage(
            snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
            snapshots.isEmpty() ? null : snapshots.get(0).getSnapshotTime(),
            "hourly",
            pageable
        );
    }

    /**
     * 최신 Daily 스냅샷에서 랭킹 조회 (최적화: 2단계 조회를 1단계로 통합)
     */
    private Page<RankingItem> getRankingsFromLatestDailySnapshot(Pageable pageable) {
        List<RankingSnapshotDaily> snapshots = rankingSnapshotDailyRepository.findLatestSnapshotOrderByProductRank();
        return convertSnapshotsToRankingPage(
            snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
            snapshots.isEmpty() ? null : snapshots.get(0).getSnapshotTime(),
            "daily",
            pageable
        );
    }

    /**
     * 최신 Weekly 스냅샷에서 랭킹 조회 (최적화: 2단계 조회를 1단계로 통합)
     */
    private Page<RankingItem> getRankingsFromLatestWeeklySnapshot(Pageable pageable) {
        List<RankingSnapshotWeekly> snapshots = rankingSnapshotWeeklyRepository.findLatestSnapshotOrderByProductRank();
        return convertSnapshotsToRankingPage(
            snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
            snapshots.isEmpty() ? null : snapshots.get(0).getSnapshotTime(),
            "weekly",
            pageable
        );
    }

    /**
     * 최신 Monthly 스냅샷에서 랭킹 조회 (최적화: 2단계 조회를 1단계로 통합)
     */
    private Page<RankingItem> getRankingsFromLatestMonthlySnapshot(Pageable pageable) {
        List<RankingSnapshotMonthly> snapshots = rankingSnapshotMonthlyRepository.findLatestSnapshotOrderByProductRank();
        return convertSnapshotsToRankingPage(
            snapshots.stream().map(s -> new SnapshotItem(s.getProductId(), s.getTotalScore())).collect(Collectors.toList()),
            snapshots.isEmpty() ? null : snapshots.get(0).getSnapshotTime(),
            "monthly",
            pageable
        );
    }

    /**
     * 스냅샷 아이템 리스트를 RankingItem Page로 변환 (공통 로직)
     */
    private Page<RankingItem> convertSnapshotsToRankingPage(
            List<SnapshotItem> snapshotItems, LocalDateTime snapshotTime, String type, Pageable pageable) {
        if (snapshotItems.isEmpty()) {
            log.warn("No {} snapshot found, returning empty result", type);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        log.debug("Using latest {} snapshot: {} records", type, snapshotItems.size());
        
        return convertSnapshotsToRankingPageInternal(snapshotItems, pageable, snapshotTime);
    }

    /**
     * 스냅샷 데이터를 RankingItem Page로 변환 (내부 구현)
     */
    private Page<RankingItem> convertSnapshotsToRankingPageInternal(
            List<SnapshotItem> snapshotItems, Pageable pageable, LocalDateTime snapshotTime) {
        
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
        
        // Tier 2 캐시에서 조회 (Cache-Aside Pattern)
        Map<Long, ProductView> productViewMap = getProductViewsFromTier2Cache(productIds);
        
        // RankingItem 생성
        List<RankingItem> rankingItems = new ArrayList<>();
        long rank = start + 1;
        for (SnapshotItem snapshot : pagedSnapshots) {
            ProductView productView = productViewMap.get(snapshot.productId());
            if (productView != null) {
                rankingItems.add(new RankingItem(rank++, snapshot.productId(), productView, snapshotTime));
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
        ProductView productView,
        LocalDateTime snapshotTime
    ) {}
}

