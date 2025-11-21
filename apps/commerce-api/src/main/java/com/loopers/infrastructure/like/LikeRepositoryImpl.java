package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository jpaRepository;

    @Override
    public Like save(Like like) {
        return jpaRepository.save(like);
    }

    @Override
    public void delete(Like like) {
        jpaRepository.delete(like);
    }

    @Override
    public Optional<Like> findByUserAndProduct(User user, Product product) {
        return jpaRepository.findByUserAndProduct(user, product);
    }

    @Override
    public boolean existsByUserAndProduct(User user, Product product) {
        return jpaRepository.existsByUserAndProduct(user, product);
    }

    @Override
    public Long countByProduct(Product product) {
        return jpaRepository.countByProduct(product);
    }

    @Override
    public Map<Long, Long> findLikeCounts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyMap();
        }

        return jpaRepository.findLikeCountsByProducts(products).stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("productId")).longValue(),
                        map -> ((Number) map.get("likeCount")).longValue()
                ));
    }
}
