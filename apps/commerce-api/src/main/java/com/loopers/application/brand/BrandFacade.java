package com.loopers.application.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class BrandFacade {
    private final BrandRepository brandRepository;

    public BrandFacade(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }
    public BrandModel getByIdOrThrow(Long brandId) {
        if(brandId == null){
            return null;
        }
        return brandRepository.findById(brandId).orElseThrow(
                ()-> new CoreException(ErrorType.BAD_REQUEST,"존재하지 않는 브랜드 id")
        );
    }
    public List<BrandModel> getByIds(List<Long> brandIds) {
        if (CollectionUtils.isEmpty(brandIds)) {
            return null;
        }

        return brandRepository.findByBrandIds(brandIds);
    }
}
