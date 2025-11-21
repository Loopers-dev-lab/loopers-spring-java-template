package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * packageName : com.loopers.infrastructure.product
 * fileName     : ProductRepositoryImpl
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
@Component
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return productJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public void incrementLikeCount(Long productId) {
        Product product = productJpaRepository.findById(productId).get();
        product.increaseLikeCount();
    }

    @Override
    public void decrementLikeCount(Long productId) {
        Product product = productJpaRepository.findById(productId).get();
        product.decreaseLikeCount();
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findByIdWithPessimisticLock(Long id) {
        return productJpaRepository.findByIdWithPessimisticLock(id);
    }
}
