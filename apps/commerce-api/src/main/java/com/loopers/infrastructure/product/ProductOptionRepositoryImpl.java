package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductOptionModel;
import com.loopers.domain.product.ProductOptionRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProductOptionRepositoryImpl implements ProductOptionRepository {
    private final JPAQueryFactory queryFactory;
    private final ProductOprtionJpaRepository productOprtionJpaRepository;

    public ProductOptionRepositoryImpl(JPAQueryFactory queryFactory, ProductOprtionJpaRepository productOprtionJpaRepository) {
        this.queryFactory = queryFactory;
        this.productOprtionJpaRepository = productOprtionJpaRepository;
    }

    @Override
    public ProductOptionModel save(ProductOptionModel productOptionModel) {
        return productOprtionJpaRepository.save(productOptionModel);
    }

    @Override
    public void deleteAll() {
        productOprtionJpaRepository.deleteAll();
    }

    @Override
    public Optional<ProductOptionModel> findById(Long optionId) {
        return productOprtionJpaRepository.findById(optionId);
    }

    @Override
    public boolean existsById(Long optionId) {
        return productOprtionJpaRepository.existsById(optionId);
    }
}
