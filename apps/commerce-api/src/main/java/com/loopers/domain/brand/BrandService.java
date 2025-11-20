package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class BrandService {
    private final BrandRepository brandRepository;

    public Brand getBrand(final Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + brandId + "] 브랜드를 찾을 수 없습니다."));
    }

    public List<Brand> getListByBrandIds(final Set<Long> brandIds) {
        return brandRepository.findAllById(brandIds);
    }
}
