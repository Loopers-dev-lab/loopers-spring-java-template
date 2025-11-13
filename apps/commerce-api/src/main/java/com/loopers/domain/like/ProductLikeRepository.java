package com.loopers.domain.like;

import java.util.Optional;

public interface ProductLikeRepository {

    Optional<ProductLike> findByUserIdAndProductId(Long userId, Long productId);

    void delete(ProductLike productLike);

    ProductLike save(ProductLike productLike);
}
