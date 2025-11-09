package com.loopers.core.service.brand;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.service.brand.query.GetBrandQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandQueryService {

    private final BrandRepository brandRepository;

    public Brand getBrandBy(GetBrandQuery query) {
        return brandRepository.getBrandById(new BrandId(query.getBrandId()));
    }
}
