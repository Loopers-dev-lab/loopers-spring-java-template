package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findProductById(Long productId);

    void saveProduct(Product product);

    List<Product> findProductsByLatest(Long brandId, int page, int size);

    List<Product> findProductsByPriceAsc(Long brandId, int page, int size);

    List<Product> findProductsByLikesDesc(Long brandId, int page, int size);

}
