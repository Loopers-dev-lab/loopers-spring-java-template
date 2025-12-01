package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDetailProjection;
import com.loopers.domain.product.ProductListProjection;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public List<Product> findAllById(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public Page<ProductListProjection> findAllWithLikeCount(Pageable pageRequest) {
        return productJpaRepository.findAllWithLikeCount(pageRequest);
    }

    @Override
    public Page<ProductListProjection> findAllByBrandIdWithLikeCount(Long brandId, Pageable pageRequest) {
        return productJpaRepository.findAllByBrandIdWithLikeCount(brandId, pageRequest);
    }

    @Override
    public Optional<ProductDetailProjection> findByIdWithBrandAndLikeCount(Long id) {
        return productJpaRepository.findByIdWithBrandAndLikeCount(id);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Page<ProductListProjection> findAllByBrandIdWithLikeCountV1(Long brandId, Pageable pageRequest) {
        return productJpaRepository.findAllByBrandIdWithLikeCountV1(brandId, pageRequest);
    }

    @Override
    public Page<ProductListProjection> findAllWithLikeCountV1(Pageable pageRequest) {
        return productJpaRepository.findAllWithLikeCountV1(pageRequest);
    }
}
