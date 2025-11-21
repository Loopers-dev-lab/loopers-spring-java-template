package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
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
    public List<Product> findAllBySortType(ProductSortType sortType) {
        return switch (sortType) {
            case latest -> productJpaRepository.findAllByOrderByCreatedAtDesc();
            case price_asc -> productJpaRepository.findAllByOrderByPriceAmountAsc();
            case likes_desc -> productJpaRepository.findAllByOrderByLikeCountDesc(); //like entity 구현 후에 @Query 사용해서 정렬 진행해야 함.
        };
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        return productJpaRepository.findByIdForUpdate(id);
    }
}
