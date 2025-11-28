package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findProductById(Long productId);

    void saveProduct(Product product);

    List<Product> findProductsByLatestWithBrandName(Long brandId, int page, int size);

    List<Product> findProductsByPriceAscWithBrandName(Long brandId, int page, int size);

    List<Product> findProductsByLikesDescWithBrandName(Long brandId, int page, int size);

}
