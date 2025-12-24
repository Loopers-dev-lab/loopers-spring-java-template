package com.loopers.core.infra.database.redis.product.impl;

import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductRanking;
import com.loopers.core.domain.product.vo.ProductRankings;
import com.loopers.core.infra.database.redis.product.ProductRankingRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class ProductRankingCacheRepositoryImpl implements ProductRankingCacheRepository {

    private final ProductRankingRedisRepository repository;

    @Override
    public void increaseDaily(ProductId productId, LocalDate date, Double score) {
        repository.increaseDaily(productId.value(), date, score);
    }

    @Override
    public ProductRankings getRankings(LocalDate date, int pageNo, int pageSize) {
        long start = (long) pageNo * pageSize;
        long stop = start + pageSize - 1;

        var topProducts = repository.getTopProductsWithScores(date, start, stop);
        Long totalElements = repository.countRankings(date);

        if (totalElements == null) {
            totalElements = 0L;
        }

        if (topProducts == null || topProducts.isEmpty()) {
            return new ProductRankings(List.of(), totalElements, 0, false, pageNo > 0);
        }

        List<TypedTuple<String>> tuples = topProducts.stream().toList();
        List<ProductRanking> rankings = IntStream.range(0, tuples.size())
                .mapToObj(index -> createProductRanking(tuples.get(index), pageNo, pageSize, index))
                .toList();

        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = pageNo < totalPages - 1;
        boolean hasPrevious = pageNo > 0;

        return new ProductRankings(rankings, totalElements, totalPages, hasNext, hasPrevious);
    }

    @Override
    public ProductRanking getDailyRankingBy(ProductId productId) {
        LocalDate today = LocalDate.now();
        Double score = repository.getRankScore(today, productId.value());
        Long ranking = repository.getRankingPosition(today, productId.value());

        if (Objects.isNull(score) || Objects.isNull(ranking)) {
            return new ProductRanking(productId, 0L, 0.0);
        }

        return new ProductRanking(productId, ranking, score);
    }

    private ProductRanking createProductRanking(TypedTuple<String> tuple, int pageNo, int pageSize, int index) {
        String productId = tuple.getValue();
        Double score = tuple.getScore();
        long ranking = (long) pageNo * pageSize + index + 1;

        return new ProductRanking(
                new ProductId(productId),
                ranking,
                score
        );
    }
}
