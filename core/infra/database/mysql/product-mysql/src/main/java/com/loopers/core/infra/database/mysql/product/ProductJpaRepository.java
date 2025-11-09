package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>, ProductQuerydslRepository {


}
