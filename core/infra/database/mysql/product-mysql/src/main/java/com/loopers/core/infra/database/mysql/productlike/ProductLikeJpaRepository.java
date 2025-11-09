package com.loopers.core.infra.database.mysql.productlike;

import com.loopers.core.infra.database.mysql.productlike.entity.ProductLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLikeEntity, Long> {

    Optional<ProductLikeEntity> findByUserIdAndProductId(Long userId, Long productId);
}
