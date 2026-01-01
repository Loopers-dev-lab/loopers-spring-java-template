package com.loopers.application.ranking;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.ranking.Ranking;
import com.loopers.domain.ranking.RankingService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductService productService;
    private final BrandService brandService;

    public RankingInfo getRanking(RankingCommand.GetRankingCommand command) {
        List<Ranking> rankings;

        if (command.type() == RankingCommand.RankingType.DAILY) {
            rankings = rankingService.getDailyRanking(command.date(), command.page(), command.size());
        } else if (command.type() == RankingCommand.RankingType.WEEKLY) {
            rankings = rankingService.getWeeklyRanking(command.date(), command.page(), command.size());
        } else if (command.type() == RankingCommand.RankingType.MONTHLY) {
            rankings = rankingService.getMonthlyRanking(command.date(), command.page(), command.size());
        } else {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 랭킹 타입입니다: " + command.type());
        }

        if (rankings.isEmpty()) {
            return new RankingInfo(List.of());
        }

        List<Long> productIds = rankings.stream()
                .map(Ranking::productId)
                .collect(Collectors.toList());

        List<Product> products = productService.findProductsByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> brandNameMap = brandService.findBrandNamesByIds(brandIds);

        List<RankingInfo.RankingItemInfo> rankingItemInfos = rankings.stream()
                .map(ranking -> {
                    Product product = productMap.get(ranking.productId());
                    if (product == null) {
                        log.warn("랭킹에 있는 상품이 DB에 존재하지 않음: productId={}", ranking.productId());
                        return null;
                    }

                    String brandName = brandNameMap.getOrDefault(product.getBrandId(), "알 수 없음");

                    return new RankingInfo.RankingItemInfo(
                            ranking.productId(),
                            product.getName(),
                            brandName,
                            product.getPrice().getPrice(),
                            product.getLikeCount().getCount(),
                            ranking.rank(),
                            ranking.score()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new RankingInfo(rankingItemInfos);
    }
}
