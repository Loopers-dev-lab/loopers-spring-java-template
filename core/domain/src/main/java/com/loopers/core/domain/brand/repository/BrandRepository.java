package com.loopers.core.domain.brand.repository;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.vo.BrandId;

public interface BrandRepository {

    Brand getBrandById(BrandId brandId);

    Brand save(Brand brand);
}
