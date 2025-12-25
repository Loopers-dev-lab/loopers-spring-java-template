package com.loopers.application.ranking;

import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final StringRedisTemplate redisTemplate;
    private final ProductViewRepository productViewRepository;
    
    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
     * 랭킹 아이템 DTO
     */
    public record RankingItem(
        Long rank,
        Long productId,
        ProductView productView
    ) {}
}

