package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
  Page<Product> findByBrandId(Long brandId, Pageable pageable);

  List<Product> save(List<Product> product);

//  @Query(value = """
//          SELECT new com.loopers.api.product.dto.ProductView(
//              p,
//              COUNT(l.id)
//          )
//          FROM Product p
//          LEFT JOIN Like l ON l.product = p
//          GROUP BY p
//          ORDER BY p.id DESC
//      """,
//      countQuery = "SELECT COUNT(p.id) FROM Product p")
  // Page<Product> findAllWithLikeCount(Pageable pageable);
}
