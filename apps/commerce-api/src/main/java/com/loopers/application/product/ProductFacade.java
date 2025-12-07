package com.loopers.application.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loopers.domain.product.ProductService;
import com.loopers.support.cache.CacheKeyService;
import com.loopers.support.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProductFacade {
    private final CacheKeyService cacheKeyService;
    private final CacheService cacheService;

    private final ProductService productService;
    private final ProductQueryService productQueryService;

    private static final Duration TTL_LIST = Duration.ofMinutes(10);
    private static final Duration TTL_DETAIL = Duration.ofMinutes(5);


    public Page<ProductInfo> getProducts(Long brandId, Pageable pageable, String sort) {
        String key = cacheKeyService.productListKey(brandId, pageable, sort);

        return cacheService.getOrLoad(
                key,
                () -> loadProducts(brandId, pageable, sort),
                TTL_LIST,
                new TypeReference<Page<ProductInfo>>() {}
        );
    }

    private Page<ProductInfo> loadProducts(Long brandId, Pageable pageable, String sort) {
        if("likes_desc".equals(sort)) {
            return productQueryService.getProductsSortedByLikes(brandId, pageable);
        } else {
            return productQueryService.getProducts(brandId, pageable, sort);
        }
    }


    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId) {
        String key = "product:v1:detail:" + productId;

        return cacheService.getOrLoad(
                key,
                () -> productService.getProductDetail(productId),
                TTL_DETAIL,
                ProductDetailInfo.class);
    }
}
