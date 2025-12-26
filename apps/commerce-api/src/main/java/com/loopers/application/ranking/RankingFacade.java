package com.loopers.application.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RankingFacade {

  private final RankingService rankingService;
  private final ProductService productService;
  private final BrandService brandService;
  private final ProductLikeService productLikeService;

  @Transactional(readOnly = true)
  public RankingResult getDailyRanking(LocalDate date, int page, int size, Long userId) {
    List<RankingEntry> entries = rankingService.getTopN(date, page, size);

    if (entries.isEmpty()) {
      return RankingResult.empty(date, page, size);
    }

    List<Long> productIds = entries.stream().map(RankingEntry::productId).toList();

    List<Product> products = productService.findByIds(productIds);
    Map<Long, Product> productById = products.stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));

    List<Long> brandIds = products.stream()
        .map(Product::getBrandId)
        .distinct()
        .toList();
    Map<Long, Brand> brandById = brandService.findByIdIn(brandIds).stream()
        .collect(Collectors.toMap(Brand::getId, Function.identity()));

    Map<Long, Boolean> likeStatusByProductId = productLikeService.findLikeStatusByProductId(userId, productIds);

    List<RankingItemResult> items = entries.stream()
        .map(entry -> {
          Product product = productById.get(entry.productId());
          if (product == null) {
            return null;
          }
          Brand brand = brandById.get(product.getBrandId());
          boolean liked = likeStatusByProductId.getOrDefault(entry.productId(), false);
          return new RankingItemResult(
              entry.rank(),
              entry.score(),
              product.getId(),
              product.getName(),
              product.getPriceValue(),
              brand != null ? brand.getName() : null,
              liked
          );
        })
        .filter(Objects::nonNull)
        .toList();

    return new RankingResult(items, page, size, date);
  }

}
