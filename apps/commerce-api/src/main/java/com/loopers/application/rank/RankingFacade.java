package com.loopers.application.rank;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.infrastructure.rank.RankingService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RankingFacade {

  private final RankingService rankingService;
  private final ProductService productService;

  public List<RankingInfo> getTopRankings(String date, int page, int size) {
    List<Long> productIds = rankingService.getTopRankingIds(date, page, size);

    if (productIds.isEmpty()) {
      return List.of();
    }

    List<Product> products = productService.getProducts(productIds);
    Map<Long, Product> productMap = products.stream()
        .collect(Collectors.toMap(Product::getId, p -> p));

    return IntStream.range(0, productIds.size())
        .mapToObj(i -> {
          Long productId = productIds.get(i);
          Product product = productMap.get(productId);

          int currentRank = ((page - 1) * size) + i + 1;

          return RankingInfo.of(product, currentRank);
        })
        .toList();
  }

  public List<RankingInfo> getRankings(String type, String date, int page, int size) {
    return switch (type.toUpperCase()) {
      case "WEEKLY" -> rankingService.getWeeklyRankings(date, page, size);
      case "MONTHLY" -> rankingService.getMonthlyRankings(date, page, size);
      default -> getTopRankings(date, page, size);
    };
  }
}
