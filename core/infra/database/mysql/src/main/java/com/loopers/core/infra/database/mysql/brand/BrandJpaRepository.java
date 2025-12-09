package com.loopers.core.infra.database.mysql.brand;

import com.loopers.core.infra.database.mysql.brand.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {
}
