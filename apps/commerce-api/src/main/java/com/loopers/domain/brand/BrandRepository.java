package com.loopers.domain.brand;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.dto.BrandSearchFilter;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
public interface BrandRepository {
    BrandEntity save(BrandEntity brand);

    Page<BrandEntity> listBrands(Pageable pageable);

    Optional<BrandEntity> getBrandById(long id);

    Optional<BrandEntity> findByName(String name);

    Page<BrandEntity> searchBrands(BrandSearchFilter filter, Pageable pageable);
}
