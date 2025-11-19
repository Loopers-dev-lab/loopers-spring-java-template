package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface ProductLikeRepository {

    Optional<ProductLike> findByUserIdAndProductId(Long userId, Long productId);

    void delete(ProductLike productLike);

    ProductLike save(ProductLike productLike);

    List<ProductLike> findByUserId(Long userId);

    List<Long> findProductIdsByUserId(Long userId);

}
