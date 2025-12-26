package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductRepository productRepository;
    private final Clock clock;

    /**
     * 랭킹 페이지 조회
     */
    @Transactional(readOnly = true)
    public RankingPageInfo getRankingPage(RankingCommand command) {
        List<RankingEntry> entries = rankingService.getRankingPage(
                command.date(),
                command.page(),
                command.size()
        );

        if (entries.isEmpty()) {
            return RankingPageInfo.empty(command.date(), command.page(), command.size());
        }

        // 상품 정보 조회
        List<Long> productIds = entries.stream()
                .map(RankingEntry::productId)
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 랭킹 정보 조합
        List<RankingInfo> rankings = new ArrayList<>();
        long startRank = (long) command.page() * command.size() + 1;

        for (int i = 0; i < entries.size(); i++) {
            RankingEntry entry = entries.get(i);
            Product product = productMap.get(entry.productId());

            if (product != null) {
                rankings.add(RankingInfo.of(
                        product.getId(),
                        product.getName(),
                        product.getPriceValue(),
                        product.getBrand().getName(),
                        startRank + i,
                        entry.score()
                ));
            }
        }

        Long totalCount = rankingService.getRankingSize(command.date());

        return RankingPageInfo.of(
                rankings,
                command.date(),
                command.page(),
                command.size(),
                totalCount
        );
    }

    /**
     * Top-N 랭킹 조회
     */
    @Transactional(readOnly = true)
    public List<RankingInfo> getTopN(LocalDate date, int n) {
        List<RankingEntry> entries = rankingService.getTopNWithScores(date, n);

        if (entries.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = entries.stream()
                .map(RankingEntry::productId)
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingInfo> rankings = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            RankingEntry entry = entries.get(i);
            Product product = productMap.get(entry.productId());

            if (product != null) {
                rankings.add(RankingInfo.of(
                        product.getId(),
                        product.getName(),
                        product.getPriceValue(),
                        product.getBrand().getName(),
                        (long) (i + 1),
                        entry.score()
                ));
            }
        }

        return rankings;
    }

    /**
     * 특정 상품의 순위 조회
     */
    public Long getProductRank(Long productId, LocalDate date) {
        return rankingService.getRank(productId, date);
    }

    /**
     * 특정 상품의 순위 조회 (오늘 기준)
     */
    public Long getProductRankToday(Long productId) {
        return rankingService.getRank(productId, LocalDate.now(clock));
    }
}
