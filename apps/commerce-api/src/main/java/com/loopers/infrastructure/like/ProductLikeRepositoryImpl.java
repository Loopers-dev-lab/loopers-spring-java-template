package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.ProductLikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductLikeRepositoryImpl implements ProductLikeRepository {

    private final ProductLikeJpaRepository productLikeJpaRepository;

    @Override
    public ProductLike save(ProductLike productLike) {
        return productLikeJpaRepository.save(productLike);
    }

    @Override
    public Optional<ProductLike> findByLikeUserAndLikeProduct(User user, Product product) {
        return productLikeJpaRepository.findByLikeUserAndLikeProduct(user, product);
    }

    @Override
    public void delete(ProductLike productLike) {
        productLikeJpaRepository.delete(productLike);
    }

    @Override
    public boolean existsByLikeUserAndLikeProduct(User user, Product product) {
        return productLikeJpaRepository.existsByLikeUserAndLikeProduct(user, product);
    }
}