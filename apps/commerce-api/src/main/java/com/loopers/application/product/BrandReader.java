package com.loopers.application.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BrandReader {
    private final BrandRepository brandRepository;

    public BrandReader(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }
    public BrandModel getByIdOrThrow(Long id) {
        if(id == null){
            return null;
        }
        return brandRepository.findById(id).orElseThrow(
                ()-> new CoreException(ErrorType.BAD_REQUEST,"존재하지 않는 브랜드 id")
        );
    }
    public Map<Long, String> getByIdsOrThrow(List<Long> brandIds) {
        if(brandIds == null || brandIds.isEmpty()) {
            return null;
        }
        List<BrandModel> brandModels = brandRepository.findByBrandIds(brandIds);
        if (brandModels.size() != brandIds.size()) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "상품에 포함된 브랜드 정보가 존재하지 않습니다.");
        }
        return brandModels.stream().collect(Collectors.toMap(
                BrandModel::getId,
                brand -> brand.getBrandNaem().getValue()
        ));
    }
}
