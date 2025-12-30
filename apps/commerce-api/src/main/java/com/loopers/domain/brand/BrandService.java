package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    /**
     * 브랜드 조회
     */
    @Transactional(readOnly = true)
    public Brand findById(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[brandId = " + brandId + "] Brand를 찾을 수 없습니다."
                ));
    }
}

