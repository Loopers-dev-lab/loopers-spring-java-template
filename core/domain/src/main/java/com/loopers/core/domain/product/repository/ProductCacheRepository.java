package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.ProductDetail;
import com.loopers.core.domain.product.vo.ProductId;

import java.util.Optional;

public interface ProductCacheRepository {

    Optional<ProductDetail> findDetailBy(ProductId productId);

    void save(ProductDetail productDetail);
}
