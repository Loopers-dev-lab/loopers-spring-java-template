package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    @Override
    public Optional<Product> findById(Long productId) {
        return Optional.empty();
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
