package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findProductById(Long productId) {
        return productJpaRepository.findByIdWithLock(productId);
    }

    @Override
    public void saveProduct(Product product) {
        productJpaRepository.save(product);
    }

    @Override
    public List<Product> findProductsByLatest(Long brandId, int page, int size) {
        return productJpaRepository.findProductsByLatest(brandId, PageRequest.of(page, size));
    }

    @Override
    public List<Product> findProductsByPriceAsc(Long brandId, int page, int size) {
        return productJpaRepository.findProductsByPriceAsc(brandId, PageRequest.of(page, size));
    }

    @Override
    public List<Product> findProductsByLikesDesc(Long brandId, int page, int size) {
        return productJpaRepository.findProductsByLikesDesc(brandId, PageRequest.of(page, size));
    }

    @Override
    public List<Product> findProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productJpaRepository.findProductsByIds(productIds);
    }
}
