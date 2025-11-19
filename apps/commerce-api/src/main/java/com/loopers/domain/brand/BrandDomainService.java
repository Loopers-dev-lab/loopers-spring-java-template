package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandDomainService {

    private final BrandRepository brandRepository;

    /**
     * 모든 브랜드 조회
     */
    @Transactional(readOnly = true)
    public Brand getBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 브랜드를 찾을 수 없습니다."
                ));
    }

    /**
     * 활성 브랜드 조회
     */
    @Transactional(readOnly = true)
    public Brand getActiveBrand(Long brandId) {
        Brand brand = getBrand(brandId);

        // 활성 상태 확인
        if (!brand.isActive()) {
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "해당 브랜드를 찾을 수 없습니다."
            );
        }

        return brand;
    }

    /**
     * 브랜드 목록
     */
    public Map<Long, Brand> getBrandMap(Set<Long> brandIds) {
        if (brandIds.isEmpty()) {
            return Map.of();
        }

        List<Brand> brands = brandRepository.findByIdIn(brandIds);

        return brands.stream()
                .collect(Collectors.toMap(Brand::getId, brand -> brand));
    }
}
