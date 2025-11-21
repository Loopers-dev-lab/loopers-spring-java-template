package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LikeRepository {
    Like save(Like like);
    void delete(Like like);
    Optional<Like> findByUserAndProduct(User user, Product product);
    boolean existsByUserAndProduct(User user, Product product);
    Long countByProduct(Product product);
    Map<Long, Long> findLikeCounts(List<Product> products);
}
