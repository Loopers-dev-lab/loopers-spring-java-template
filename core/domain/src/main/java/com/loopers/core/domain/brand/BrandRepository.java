package com.loopers.core.domain.brand;

import com.loopers.core.domain.brand.vo.BrandId;

public interface BrandRepository {

    Brand getBrandById(BrandId brandId);

    Brand save(Brand brand);
}
