package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingPeriod;
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

    @Transactional(readOnly = true)
    public RankingPageInfo getRankingPage(RankingCommand command) {
        List<RankingEntry> entries;
        Long totalCount;

        switch (command.period()) {
            case WEEKLY -> {
                entries = rankingService.getWeeklyRankingPage(command.date(), command.page(), command.size());
                totalCount = rankingService.getWeeklyRankingSize(command.date());
            }
            case MONTHLY -> {
                entries = rankingService.getMonthlyRankingPage(command.date(), command.page(), command.size());
                totalCount = rankingService.getMonthlyRankingSize(command.date());
            }
            default -> {
                entries = rankingService.getRankingPage(command.date(), command.page(), command.size());
                totalCount = rankingService.getRankingSize(command.date());
            }
        }

        if (entries.isEmpty()) {
            return RankingPageInfo.empty(command.date(), command.period(), command.page(), command.size());
        }

        List<Long> productIds = entries.stream()
                .map(RankingEntry::productId)
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

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

        return RankingPageInfo.of(
                rankings,
                command.date(),
                command.period(),
                command.page(),
                command.size(),
                totalCount
        );
    }

    @Transactional(readOnly = true)
    public List<RankingInfo> getTopN(LocalDate date, RankingPeriod period, int n) {
        List<RankingEntry> entries = switch (period) {
            case WEEKLY -> rankingService.getWeeklyTopN(date, n);
            case MONTHLY -> rankingService.getMonthlyTopN(date, n);
            default -> rankingService.getTopNWithScores(date, n);
        };

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

    @Transactional(readOnly = true)
    public List<RankingInfo> getTopN(LocalDate date, int n) {
        return getTopN(date, RankingPeriod.DAILY, n);
    }

    public Long getProductRank(Long productId, LocalDate date, RankingPeriod period) {
        return switch (period) {
            case WEEKLY -> rankingService.getWeeklyRank(productId, date);
            case MONTHLY -> rankingService.getMonthlyRank(productId, date);
            default -> rankingService.getRank(productId, date);
        };
    }

    public Long getProductRank(Long productId, LocalDate date) {
        return rankingService.getRank(productId, date);
    }

    public Long getProductRankToday(Long productId) {
        return rankingService.getRank(productId, LocalDate.now(clock));
    }
}
