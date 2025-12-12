package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상품 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        ProductEntity entity;

        // ID가 있으면 기존 엔티티 업데이트, 없으면 새로 생성
        if (product.getId() != null) {
            entity = productJpaRepository.findById(product.getId())
                    .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다: " + product.getId()));
            entity.updateFrom(product);
        } else {
            entity = ProductEntity.from(product);
        }

        ProductEntity saved = productJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findById(productId)
                .map(ProductEntity::toDomain);
    }

    @Override
    public Optional<Product> findByIdWithLock(Long productId) {
        return productJpaRepository.findByIdWithLock(productId)
                .map(ProductEntity::toDomain);
    }

    @Override
    public List<Product> findByBrandId(Long brandId) {
        return productJpaRepository.findByBrandId(brandId).stream()
                .map(ProductEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        return productJpaRepository.findByStatus(status).stream()
                .map(ProductEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll().stream()
                .map(ProductEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findAllSorted(ProductSortType sortType) {
        List<ProductEntity> entities;

        switch (sortType) {
            case LIKES_DESC:
                entities = productJpaRepository.findAllOrderByLikeCountDesc();
                break;
            case PRICE_ASC:
                entities = productJpaRepository.findAllOrderByPriceAsc();
                break;
            case LATEST:
                entities = productJpaRepository.findAllOrderByCreatedAtDesc();
                break;
            default:
                entities = productJpaRepository.findAll();
        }

        return entities.stream()
                .map(ProductEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long productId) {
        return productJpaRepository.existsById(productId);
    }

    @Override
    public void deleteById(Long productId) {
        productJpaRepository.deleteById(productId);
    }
}
