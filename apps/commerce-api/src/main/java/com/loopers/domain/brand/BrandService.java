package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public Optional<Brand> findBrandById(Long brandId) {
        return brandRepository.findById(brandId);
    }

    public String findBrandNameById(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
        return brand.getName();
    }

    public Map<Long, String> findBrandNamesByIds(List<Long> brandIds) {
        return brandRepository.findBrandNamesByIds(brandIds);
    }
}
