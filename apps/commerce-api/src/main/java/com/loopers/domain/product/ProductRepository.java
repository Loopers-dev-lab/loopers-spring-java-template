package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    void saveAll(List<Product> products);
    Optional<Product> findById(Long id);
    Page<Product> findAll(Pageable pageable);
    List<Product> findAllByIds(Collection<Long> ids);
    Page<Product> findProducts(Pageable pageable, Long brandId);
    long count();
    Optional<Product> findByIdWithPessimisticLock(Long id);
    List<Product> findAllByIdsWithPessimisticLock(Collection<Long> ids);
}
