package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll();
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        return productJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public List<ProductListView> findListViewByCondition(ProductSearchCondition condition) {
        int limit = condition.size();
        int offset = condition.page() * condition.size();
        Long brandId = condition.brandId();

        return switch (condition.sortType()) {
            case PRICE_ASC -> productJpaRepository.findListPriceAsc(brandId, limit, offset);
            case LIKE_DESC -> productJpaRepository.findListLikesDesc(brandId, limit, offset);
            case LATEST -> productJpaRepository.findListLatest(brandId, limit, offset);
        };
    }

    @Override
    public List<Product> findByIdIn(List<Long> ids) {
        return productJpaRepository.findByIdIn(ids);
    }
}
