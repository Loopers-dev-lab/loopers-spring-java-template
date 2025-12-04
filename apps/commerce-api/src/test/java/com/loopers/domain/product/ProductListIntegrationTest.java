package com.loopers.domain.product;

import com.loopers.application.product.ProductCacheService;
import com.loopers.application.product.ProductWithLikeCount;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
@Sql(scripts = "/product-list-data-ranged.sql")
class ProductListIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ProductCacheService productCacheService;

  @Test
  void testQueryPerformance() {
    long start = System.currentTimeMillis();
    Page<ProductWithLikeCount> page =
        productCacheService.getProductList(null, null, "likes_desc", 0, 20);
    long end = System.currentTimeMillis();

    System.out.println("Elapsed: " + (end - start) + " ms");
    System.out.println("Fetched: " + page.getContent().size());

    assertThat(page.getContent().size()).isEqualTo(20);
  }

  @Test
  void testCacheHitPerformance() {
    productCacheService.getProductList(null, null, "likes_desc", 0, 20);

    long start = System.currentTimeMillis();
    productCacheService.getProductList(null, null, "likes_desc", 0, 20);
    long end = System.currentTimeMillis();

    System.out.println("Cache Hit: " + (end - start) + " ms");
  }


}
