package com.loopers.domain.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.embeded.BrandId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductService {

    public List<Long> toDistinctBrandIds(List<ProductModel> content) {
        return content.stream().map(ProductModel::getBrandId)
                .map(BrandId::getValue)
                .distinct()
                .toList();
    }
    public Map<Long, BrandModel> createBrandNameMap(List<BrandModel> brandModels, List<Long> requestedBrandIds) {
        if (brandModels.size() != requestedBrandIds.size()) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "상품에 포함된 브랜드 정보가 존재하지 않습니다.");
        }

        return brandModels.stream().collect(Collectors.toMap(
                BrandModel::getId,
                brandModel -> brandModel
        ));
    }
}
