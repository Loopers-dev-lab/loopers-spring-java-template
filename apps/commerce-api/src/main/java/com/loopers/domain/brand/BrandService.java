package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class BrandService {
    private final BrandRepository brandRepository;

    public Brand getBrandById(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    public Map<Long, Brand> getBrandMapByBrandIds(Collection<Long> brandIds) {
        return brandRepository.findAllByIdIn(brandIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(Brand::getId, brand -> brand));
    }
}
