package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product registerProduct(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public boolean existsProductCode(String productCode) {
        return productJpaRepository.existsByProductCode(productCode);
    }

    @Override
    public List<Product> findAllBySortType(ProductSortType sortType) {
        Sort sort = getSortBySortType(sortType);

        // Repository 레벨에서 deletedAt IS NULL 조건으로 필터링
        return productJpaRepository.findAllByDeletedAtIsNull(sort);
    }

    @Override
    public Optional<Product> findByIdWithBrand(Long productId) {
        return productJpaRepository.findByIdWithBrand(productId);
    }

    private Sort getSortBySortType(ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price.amount");  // @Embedded Money 타입의 중첩 경로
            case LIKES_DESC -> Sort.by(Sort.Direction.DESC, "likeCount");
        };
    }
}
