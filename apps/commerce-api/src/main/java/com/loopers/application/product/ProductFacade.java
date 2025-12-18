package com.loopers.application.product;

import com.loopers.cache.CacheTemplate;
import com.loopers.cache.SimpleCacheKey;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.cache.CachePolicy;
import com.loopers.domain.common.event.DomainEventPublisher;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductFacade {

  private final ProductService productService;
  private final BrandService brandService;
  private final ProductLikeService productLikeService;
  private final CacheTemplate cacheTemplate;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Page<ProductDetail> searchProducts(Long brandId, ProductSearchCondition condition, Pageable pageable) {
    if (condition.isCacheable()) {
      return searchProductsWithCache(brandId, condition, pageable);
    }
    return fetchProductDetails(brandId, condition.userId(), pageable);
  }

  private Page<ProductDetail> fetchProductDetails(Long brandId, Long userId, Pageable pageable) {
    Page<Product> productPage = productService.findProducts(brandId, pageable);
    List<Product> products = productPage.getContent();

    Map<Long, Brand> brandById = getBrandById(products);
    Map<Long, Boolean> likeStatusByProductId = getLikeStatusByProductId(userId, products);

    return getProductDetails(productPage, brandById, likeStatusByProductId);
  }

  private Map<Long, Brand> getBrandById(List<Product> products) {
    List<Long> brandIds = products.stream()
        .map(Product::getBrandId)
        .distinct()
        .toList();

    return brandService.findByIdIn(brandIds).stream()
        .collect(Collectors.toMap(Brand::getId, Function.identity()));
  }

  private Map<Long, Boolean> getLikeStatusByProductId(Long userId, List<Product> products) {
    List<Long> productIds = products.stream()
        .map(Product::getId)
        .toList();

    return productLikeService.findLikeStatusByProductId(userId, productIds);
  }

  private Page<ProductDetail> getProductDetails(Page<Product> productPage, Map<Long, Brand> brandById,
      Map<Long, Boolean> likeStatusByProductId) {
    return productPage.map(product -> ProductDetail.of(
        product,
        brandById.get(product.getBrandId()),
        likeStatusByProductId.getOrDefault(product.getId(), false)
    ));
  }

  @Transactional
  public ProductDetail retrieveProductDetail(Long productId, Long userId) {
    Product product = productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    Brand brand = brandService.getById(product.getBrandId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    boolean isLiked = productLikeService.isLiked(userId, productId);

    eventPublisher.publish(ProductViewedEvent.of(productId, LocalDateTime.now(clock)));

    return ProductDetail.of(product, brand, isLiked);
  }

  private Page<ProductDetail> searchProductsWithCache(Long brandId, ProductSearchCondition condition, Pageable pageable) {
    String cacheKey = CachePolicy.PRODUCT_LIST.buildKey(brandId, condition.sortType().getCacheKey());

    ProductListCache cached = cacheTemplate.getOrLoad(
        SimpleCacheKey.of(cacheKey, CachePolicy.PRODUCT_LIST.getTtl(), ProductListCache.class),
        () -> ProductListCache.from(fetchProductDetails(brandId, null, pageable)));
    return cached.toPage();
  }
}
