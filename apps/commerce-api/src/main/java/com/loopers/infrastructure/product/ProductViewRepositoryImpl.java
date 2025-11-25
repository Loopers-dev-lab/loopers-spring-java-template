package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductView;
import com.loopers.domain.product.ProductViewRepository;
import com.loopers.domain.product.QProductView;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductViewRepositoryImpl implements ProductViewRepository {

    private final ProductViewJpaRepository productViewJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ProductView> save(ProductView productView) {
        ProductView saved = productViewJpaRepository.save(productView);
        return Optional.of(saved);
    }

    @Override
    public Optional<ProductView> findById(Long id) {
        return productViewJpaRepository.findById(id);
    }

    @Override
    public void updateLikeCount(Long id, Long count) {
        QProductView productView = QProductView.productView;
        queryFactory
                .update(productView)
                .set(productView.likeCount, count)
                .where(productView.id.eq(id))
                .execute();
    }

    @Override
    public void deleteById(Long id) {
        QProductView productView = QProductView.productView;
        queryFactory
                .delete(productView)
                .where(productView.id.eq(id))
                .execute();
    }
}

