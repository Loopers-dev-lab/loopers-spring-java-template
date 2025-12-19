package com.loopers.domain.product.repository;

import com.loopers.domain.product.command.ProductSearchFilter;
import com.loopers.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    Optional<Product> findByIdForUpdate(Long id);

    List<Product> findByIdIn(List<Long> ids);

    Page<Product> findAll(ProductSearchFilter filter, Pageable pageable);

    List<Product> findAllByCursor(ProductSearchFilter filter, String cursor, int size);

    Product save(Product product);

    int decreaseStock(Long productId, int quantity);

    int increaseStock(Long productId, int quantity);

    int incrementLikeCount(Long productId);

    int decrementLikeCount(Long productId);

    int updateLikeCount(Long productId, long count);

    List<Product> findTopByLikeCount(int limit);

    List<Product> findTopByBrandIdAndLikeCount(Long brandId, int limit);
}
