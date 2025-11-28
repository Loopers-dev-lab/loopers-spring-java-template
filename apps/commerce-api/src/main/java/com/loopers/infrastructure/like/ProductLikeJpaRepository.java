package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {
    Optional<ProductLike> findByLikeUserAndLikeProduct(User user, Product product);

    boolean existsByLikeUserAndLikeProduct(User user, Product product);
}