package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
    public List<Product> findProductsByLatestWithBrandName(Long brandId, int page, int size) {
        return null;
    }

    @Override
    public List<Product> findProductsByPriceAscWithBrandName(Long brandId, int page, int size) {
        return null;
    }

    @Override
    public List<Product> findProductsByLikesDescWithBrandName(Long brandId, int page, int size) {
        return null;
    }
}
