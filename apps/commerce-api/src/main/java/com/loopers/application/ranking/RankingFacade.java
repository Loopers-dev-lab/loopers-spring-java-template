package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.ranking.RankingEntry;
import com.loopers.infrastructure.ranking.RankingRedisService;
import com.loopers.interfaces.api.ranking.RankingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RankingRedisService rankingRedisService;
    private final ProductRepository productRepository;

    public RankingDto.RankingListResponse getRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        int offset = page * size;

        List<RankingEntry> entries = rankingRedisService.getTopProducts(date, offset, size);

        if (entries.isEmpty()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, 0);
        }

        long totalCount = rankingRedisService.getTotalCount(date);

        List<Long> productIds = entries.stream()
                .map(RankingEntry::productId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        int rank = offset + 1;
        for (RankingEntry entry : entries) {
            Product product = productMap.get(entry.productId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank++,
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        entry.score()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, totalCount);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
