package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndProduct(User user, Product product);
    boolean existsByUserAndProduct(User user, Product product);
    Long countByProduct(Product product);

    @Query("SELECT l.product.id as productId, COUNT(l) as likeCount " +
            "FROM Like l WHERE l.product IN :products GROUP BY l.product.id")
    List<Map<String, Object>> findLikeCountsByProducts(@Param("products") List<Product> products);
}
