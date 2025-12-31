package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<RankingProductInfo> getDailyRanking(String yyyymmdd, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        long start = (long) (p - 1) * s;
        long end = start + s - 1;

        Set<ZSetOperations.TypedTuple<String>> tuples =
                rankingService.getDailyMembers(yyyymmdd, start, end);

        List<RankingProductInfo> result = new ArrayList<>();
        if (tuples == null || tuples.isEmpty()) {
            return result;
        }

        List<Long> productIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(v -> v != null && !v.isBlank())
                .map(Long::valueOf)
                .toList();

        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product pdt : products) {
            productMap.put(pdt.getId(), pdt);
        }

        for (ZSetOperations.TypedTuple<String> t : tuples) {
            String member = t.getValue();
            if (member == null || member.isBlank()) continue;
            Long productId = Long.valueOf(member);
            Product product = productMap.get(productId);
            if (product == null) continue;
            result.add(toInfo(productId, product));
        }
        return result;
    }

    private RankingProductInfo toInfo(Long productId, Product product) {
        return new RankingProductInfo(
                productId,
                product.getName(),
                product.getPrice() != null ? product.getPrice().getAmount() : BigDecimal.ZERO,
                product.getLikeCount() != null ? product.getLikeCount() : 0L
        );
    }

    
}


