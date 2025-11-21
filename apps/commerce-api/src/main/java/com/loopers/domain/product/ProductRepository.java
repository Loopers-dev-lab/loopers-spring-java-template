package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * packageName : com.loopers.domain.product
 * fileName     : ProductRepositroy
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */
public interface ProductRepository {
    Page<Product> findAll(Pageable pageable);

    Optional<Product> findById(Long id);

    void incrementLikeCount(Long productId);

    void decrementLikeCount(Long productId);

    Product save(Product product);

    Optional<Product> findByIdWithPessimisticLock(Long id);
}
