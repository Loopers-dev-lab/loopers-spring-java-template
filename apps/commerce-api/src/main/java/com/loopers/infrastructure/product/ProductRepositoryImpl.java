package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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

        return productJpaRepository.findAll(sort).stream()
                .filter(product -> product.getDeletedAt() == null) // 삭제된 상품 제외
                .collect(Collectors.toList());
    }

    private Sort getSortBySortType(ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case LIKES_DESC -> Sort.by(Sort.Direction.DESC, "likeCount");
        };
    }
}
