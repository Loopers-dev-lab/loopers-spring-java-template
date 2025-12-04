package com.loopers.application.product;

import com.loopers.application.like.LikeCacheRepository;
import com.loopers.application.like.LikeInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.view.ProductListView;
import com.loopers.domain.view.ProductListViewService;
import com.loopers.infrastructure.cache.RedisProductCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductCacheService {

  private final ProductListViewService productListViewService;
  private final ProductService productService;
  private final StockService stockService;
  private final LikeCacheRepository likeCacheRepository;
  private final RedisProductCacheRepository cacheRepository;

  private static final Duration LIST_TTL = Duration.ofMinutes(10);
  private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
  private static final String LIST_KEY_PREFIX = "product:list:";

  public Page<ProductWithLikeCount> getProductList(Long userId, Long brandId,
                                                   String sort, int page, int size) {
    String key = LIST_KEY_PREFIX + brandId + ":" + sort + ":" + page + ":" + size;
    Duration ttl = getListTtl(sort, page);

    // 1) ID 리스트 조회
    List<Long> productIds = cacheRepository.getIdList(key);
    if (productIds == null) {
      Page<ProductListView> pageData = productListViewService.getProducts(brandId, sort, page, size);
      productIds = pageData.getContent().stream().map(ProductListView::getProductId).toList();
      cacheRepository.saveIdList(key, productIds, ttl);
    }

    // 2) 상세 캐시 확인
    List<Long> missIds = productIds.stream()
        .filter(id -> cacheRepository.get(id) == null)
        .toList();

    if (!missIds.isEmpty()) {
      List<ProductStock> stocks = missIds.stream().map(id -> this.getProductStock(id)).toList();
      stocks.forEach(stock -> cacheRepository.save(stock, DETAIL_TTL));
    }

    // 3) 상세 캐시 조합 + 좋아요 조회
    List<ProductWithLikeCount> list = productIds.stream()
        .map(id -> {
          ProductStock stock = cacheRepository.get(id);
          LikeInfo like = likeCacheRepository.getLikeInfo(userId, id);
          return new ProductWithLikeCount(
              id,
              stock.product().getName(),
              stock.product().getPrice().getAmount(),
              like.likeCount()
          );
        }).toList();

    return new PageImpl<>(list, PageRequest.of(page, size), productIds.size());
  }

  public ProductStock getProductStock(Long productId) {
    ProductStock cached = cacheRepository.get(productId);
    if (cached != null) return cached;

    Product product = productService.getProduct(productId);
    Stock stock = stockService.getStock(productId);
    ProductStock productStock = ProductStock.from(product, stock);
    cacheRepository.save(productStock, DETAIL_TTL);
    return productStock;
  }

  public void evictListCache() {
    cacheRepository.evictByPrefix(LIST_KEY_PREFIX);
  }

  private Duration getListTtl(String sort, int page) {
    if ("latest".equals(sort)) {
      if (page == 0) return Duration.ofMinutes(1);        // 첫 페이지
      if (page >= 1 && page <= 4) return Duration.ofMinutes(2); // 2~5페이지
      return Duration.ofMinutes(5);                       // 6페이지 이후
    } else if ("likes_desc".equals(sort)) {
      if (page == 0) return Duration.ofMinutes(1);  // 1페이지 최신 좋아요 반영
      return Duration.ofMinutes(2);                 // 뒤 페이지
    }
    // 기본 TTL
    return LIST_TTL;
  }
}
