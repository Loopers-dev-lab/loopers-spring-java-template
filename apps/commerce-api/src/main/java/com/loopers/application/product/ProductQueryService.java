
package com.loopers.application.product;

import com.loopers.application.like.LikeCacheRepository;
import com.loopers.application.like.LikeInfo;
import com.loopers.application.ranking.RankInfo;
import com.loopers.application.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Service
public class ProductQueryService {

  private final ProductCacheService productCacheService;
  private final LikeCacheRepository likeCacheRepository;
  private final RankingService rankingService;

  public Page<ProductListItem> getProductList(Long userId,
                                              Long brandId,
                                              String sort,
                                              int page,
                                              int size) {
    return productCacheService.getProductList(userId, brandId, sort, page, size);
  }

  public List<ProductListItem> getProductListByProductIds(Long userId, List<Long> productIds) {
    List<ProductListItem> products = productCacheService.getProductListByProductIds(userId, productIds);
    return products;
  }

  public ProductDetailInfo getProductDetail(Long userId, Long productId) {
    ProductStock productStock = productCacheService.getProductStock(productId);
    LikeInfo likeInfo = likeCacheRepository.getLikeInfo(userId, productId);
    Integer rank = rankingService.getProductRank(productId);
    return ProductDetailInfo.from(productStock, likeInfo, rank);
  }

  public void evictListCache() {
    productCacheService.evictListCache();
  }

}
