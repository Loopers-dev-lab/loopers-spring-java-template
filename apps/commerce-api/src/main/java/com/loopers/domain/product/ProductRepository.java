package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long id);

    /**
     * 동시성 제어를 위한 비관적 락을 사용하는 조회 메서드
     *
     * @param id Product ID
     * @return Product
     */
    Optional<Product> findByIdWithLock(Long id);

    Page<Product> findAll(Pageable pageable);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    List<Product> findByIdIn(List<Long> ids);

    Page<Product> findAllSorted(Pageable pageable, ProductSortType sortType);

    Page<Product> findByBrandIdSorted(Long brandId, Pageable pageable, ProductSortType sortType);
}
