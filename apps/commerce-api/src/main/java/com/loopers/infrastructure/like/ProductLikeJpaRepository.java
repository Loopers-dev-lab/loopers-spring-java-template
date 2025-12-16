package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {
    Optional<ProductLike> findByLikeUserAndLikeProduct(User user, Product product);

    boolean existsByLikeUserAndLikeProduct(User user, Product product);

    long countByLikeProduct(Product product);

    @Query("SELECT DISTINCT pl.likeProduct.id FROM ProductLike pl")
    List<Long> findDistinctProductIds();
}
