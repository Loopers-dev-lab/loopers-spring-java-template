package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.domain.product.view.QProductView;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductViewRepositoryImpl implements ProductViewRepository {

    private final ProductViewJpaRepository productViewJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional
    public Optional<ProductView> save(ProductView productView) {
        ProductView saved = productViewJpaRepository.save(productView);
        return Optional.of(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductView> findById(Long id) {
        return productViewJpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductView> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        QProductView productView = QProductView.productView;
        return queryFactory
                .selectFrom(productView)
                .where(productView.id.in(ids))
                .fetch();
    }

    @Override
    @Transactional
    public void updateLikeCount(Long id, Long count) {
        QProductView productView = QProductView.productView;
        queryFactory
                .update(productView)
                .set(productView.likeCount, count)
                .where(productView.id.eq(id))
                .execute();
    }

    @Override
    @Transactional
    public void update(Long id, String name, BigDecimal price, Long brandId, String brandName, ProductStatus status) {
        QProductView productView = QProductView.productView;
        queryFactory
                .update(productView)
                .set(productView.name, name)
                .set(productView.price, price)
                .set(productView.brandId, brandId)
                .set(productView.brandName, brandName)
                .set(productView.status, status)
                .where(productView.id.eq(id))
                .execute();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        QProductView productView = QProductView.productView;
        queryFactory
                .delete(productView)
                .where(productView.id.eq(id))
                .execute();
    }
}

