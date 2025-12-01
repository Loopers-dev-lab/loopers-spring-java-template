package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    Product findByIdOrThrow(Long id);

    Page<Product> findAll(ProductSortType sortType, int page, int size);

    List<Product> findByBrandId(Long brandId);

    Page<Product> findByBrandId(Long brandId, ProductSortType sortType, int page, int size);

    Product save(Product product);

    List<Product> findAllByIdIn(List<Long> ids);

    Optional<Product> findByIdWithLock(Long id);

    void flush();

    // 비정규화: 좋아요 수 직접 업데이트
    @Modifying
    @Query("UPDATE Product p SET p.totalLikes = p.totalLikes + 1 WHERE p.id = :id")
    void incrementLikeCount(Long id);

    @Modifying
    @Query("UPDATE Product p SET p.totalLikes = p.totalLikes - 1 WHERE p.id = :id AND p.totalLikes > 0")
    void decrementLikeCount(Long id);

}
