package com.loopers.core.domain.productlike.repository;

import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.user.vo.UserId;

import java.util.Optional;

public interface ProductLikeRepository {

    void deleteByUserIdAndProductId(UserId userId, ProductId productId);

    ProductLike save(ProductLike productLike);

    Optional<ProductLike> findByUserIdAndProductId(UserId userId, ProductId productId);
}
