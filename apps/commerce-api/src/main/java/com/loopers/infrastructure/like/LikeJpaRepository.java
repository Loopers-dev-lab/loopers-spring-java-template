package com.loopers.infrastructure.like;

import com.loopers.domain.like.LikeModel;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.product.ProductModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface LikeJpaRepository extends JpaRepository<LikeModel, Long> {

    Optional<LikeModel> findByUserAndProduct(UserModel user, ProductModel product);

    List<LikeModel> findByUser(UserModel user);

    long countByProduct(ProductModel product);

    @Query("SELECT l.product.id as productId, COUNT(l) as likeCount " +
           "FROM LikeModel l " +
           "WHERE l.product.id IN :productIds " +
           "GROUP BY l.product.id")
    Map<Long, Long> countByProductIds(@Param("productIds") Set<Long> productIds);
}