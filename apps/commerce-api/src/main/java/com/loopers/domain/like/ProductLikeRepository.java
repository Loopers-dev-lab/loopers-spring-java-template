package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;

import java.util.Optional;

public interface ProductLikeRepository {
    ProductLike save(ProductLike productLike);

    Optional<ProductLike> findByLikeUserAndLikeProduct(User user, Product product);

    void delete(ProductLike productLike);

    boolean existsByLikeUserAndLikeProduct(User user, Product product);
}