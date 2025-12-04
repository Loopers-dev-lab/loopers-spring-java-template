package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public List<Product> findAll(Specification<Product> spec, int page, int size, ProductSortType sortType) {
        Sort sort = getSortBySortType(sortType);

        // Pageable에 페이징 + 정렬 정보 포함
        Pageable pageable = PageRequest.of(page, size, sort);

        // Page 결과에서 content(실제 데이터 리스트)만 추출하여 반환
        return productJpaRepository.findAll(spec, pageable).getContent();
    }

    @Override
    public Optional<Product> findByIdWithBrand(Long productId) {
        return productJpaRepository.findByIdWithBrand(productId);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public Optional<Product> findByIdWithLock(Long productId) {
        return productJpaRepository.findByIdWithLock(productId);
    }

    private Sort getSortBySortType(ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price.amount");  // @Embedded Money 타입의 중첩 경로
            case LIKES_DESC -> Sort.by(Sort.Direction.DESC, "likeCount");
            case BRAND -> Sort.by(Sort.Direction.DESC, "brand.brandName");
            case NAME -> Sort.by(Sort.Direction.ASC, "productName");
        };
    }
}
