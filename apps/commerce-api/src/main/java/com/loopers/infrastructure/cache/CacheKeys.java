package com.loopers.infrastructure.cache;

import com.loopers.cache.CacheKey;
import com.loopers.cache.CacheKeyType;
import com.loopers.cache.SimpleCacheKey;
import com.loopers.application.product.ProductListCache;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

public final class CacheKeys {

  private CacheKeys() {
  }

  public static CacheKey<Product> product(Long id) {
    return new SimpleCacheKey<>(
        CacheKeyType.PRODUCT.buildKey(id),
        CacheKeyType.PRODUCT.getTtl(),
        Product.class
    );
  }

  public static CacheKey<Brand> brand(Long id) {
    return new SimpleCacheKey<>(
        CacheKeyType.BRAND.buildKey(id),
        CacheKeyType.BRAND.getTtl(),
        Brand.class
    );
  }

  public static CacheKey<ProductListCache> productList(Long brandId) {
    return new SimpleCacheKey<>(
        CacheKeyType.PRODUCT_LIST.buildKey(brandId),
        CacheKeyType.PRODUCT_LIST.getTtl(),
        ProductListCache.class
    );
  }
}
