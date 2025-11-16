package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<Product, Long>, ProductRepository {

    @Override
    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
    Page<Product> findAll(Pageable pageable);

    @Override
    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId AND p.deletedAt IS NULL")
    Page<Product> findByBrandId(@Param("brandId") Long brandId, Pageable pageable);

    @Override
    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.deletedAt IS NULL")
    List<Product> findByIdIn(@Param("ids") List<Long> ids);

    @Override
    default Page<Product> findAllSorted(Pageable pageable, ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> findAllByLatest(pageable);
            case PRICE_ASC -> findAllByPriceAsc(pageable);
            case LIKES_DESC -> findAllByLikesDesc(pageable);
        };
    }

    @Override
    default Page<Product> findByBrandIdSorted(Long brandId, Pageable pageable, ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> findByBrandIdByLatest(brandId, pageable);
            case PRICE_ASC -> findByBrandIdByPriceAsc(brandId, pageable);
            case LIKES_DESC -> findByBrandIdByLikesDesc(brandId, pageable);
        };
    }

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<Product> findAllByLatest(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL ORDER BY p.price ASC")
    Page<Product> findAllByPriceAsc(Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN Like l ON p.id = l.productId AND l.deletedAt IS NULL WHERE p.deletedAt IS NULL GROUP BY p.id ORDER BY COUNT(l) DESC")
    Page<Product> findAllByLikesDesc(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<Product> findByBrandIdByLatest(@Param("brandId") Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId AND p.deletedAt IS NULL ORDER BY p.price ASC")
    Page<Product> findByBrandIdByPriceAsc(@Param("brandId") Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN Like l ON p.id = l.productId AND l.deletedAt IS NULL WHERE p.brand.id = :brandId AND p.deletedAt IS NULL GROUP BY p.id ORDER BY COUNT(l) DESC")
    Page<Product> findByBrandIdByLikesDesc(@Param("brandId") Long brandId, Pageable pageable);
}
