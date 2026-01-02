package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 랭킹 Facade
 * <p>
 * 랭킹 조회 로직을 담당하며, ZSET에서 랭킹 정보를 조회하고
 * 상품 정보를 Aggregation하여 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductService productService;

    /**
     * 랭킹 페이지 조회
     * 1. ZSET에서 Top-N 상품 ID 조회
     * 2. 상품 정보 조회 및 Aggregation
     * 3. 순위 정보 포함하여 반환
     */
    @Transactional(readOnly = true)
    public RankingInfo.RankingsPageResponse getRankings(String date, Pageable pageable) {
        String rankingKey = rankingService.getRankingKey(date);

        // 페이지네이션 계산
        long start = pageable.getPageNumber() * pageable.getPageSize();
        long end = start + pageable.getPageSize() - 1;

        // ZSET에서 상품 ID와 점수 조회
        List<RankingService.RankingEntry> entries =
                rankingService.getTopNWithScores(rankingKey, start, end);

        if (entries.isEmpty()) {
            return RankingInfo.RankingsPageResponse.empty(pageable);
        }

        // 상품 ID 리스트 추출
        List<Long> productIds = entries.stream()
                .map(RankingService.RankingEntry::getProductId)
                .collect(Collectors.toList());

        // 상품 정보 조회
        Map<Long, Product> productMap = productService.getProductMapByIds(productIds);

        // 랭킹 정보와 상품 정보 결합
        List<RankingInfo.RankingItem> rankingItems = new ArrayList<>();
        long rank = start + 1; // 1부터 시작하는 순위

        for (RankingService.RankingEntry entry : entries) {
            Product product = productMap.get(entry.getProductId());
            if (product != null) {
                rankingItems.add(RankingInfo.RankingItem.of(
                        rank++,
                        entry.getProductId(),
                        entry.getScore(),
                        product
                ));
            }
        }

        // 전체 랭킹 수 조회 (총 페이지 수 계산용)
        Long totalSize = rankingService.getRankingSize(rankingKey);

        return RankingInfo.RankingsPageResponse.of(
                rankingItems,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                totalSize != null ? totalSize.intValue() : 0
        );
    }

    /**
     * 특정 상품의 랭킹 정보 조회
     */
    @Transactional(readOnly = true)
    public RankingInfo.ProductRankingInfo getProductRanking(Long productId, String date) {
        String rankingKey = rankingService.getRankingKey(date);
        Long rank = rankingService.getRank(rankingKey, productId);
        Double score = rankingService.getScore(rankingKey, productId);

        if (rank == null || score == null) {
            return null; // 랭킹에 없음
        }

        return RankingInfo.ProductRankingInfo.of(
                rank + 1, // 1부터 시작하는 순위로 변환
                score
        );
    }
}

