
package com.loopers.application.product;

import com.loopers.application.like.LikeCacheRepository;
import com.loopers.application.like.LikeInfo;
import com.loopers.domain.view.ProductListViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class ProductQueryService {

  private final ProductCacheService productCacheService;
  private final LikeCacheRepository likeCacheRepository;

  public Page<ProductWithLikeCount> getProductList(Long userId,
                                                   Long brandId,
                                                   String sort,
                                                   int page,
                                                   int size) {
    return productCacheService.getProductList(userId, brandId, sort, page, size);
  }

  public ProductDetailInfo getProductDetail(Long userId, Long productId) {
    ProductStock productStock = productCacheService.getProductStock(productId);
    LikeInfo likeInfo = likeCacheRepository.getLikeInfo(userId, productId);

    return ProductDetailInfo.from(productStock, likeInfo);
  }

  public void evictListCache() {
    productCacheService.evictListCache();
  }

}
