package com.loopers.application.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.ProductModel;
import com.loopers.interfaces.api.product.ProductV1Dto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductAssembler {
    
    public List<ProductV1Dto.ProductItem> toListWithSingleBrand(
            List<ProductModel> productModelList, BrandModel brandModel) {
        Long brandId = brandModel.getId();
        String brandName = brandModel.getBrandNaem().getValue();
        
        return productModelList.stream()
                .map(product -> createProductItem(product, brandId, brandName))
                .toList();
    }

    public List<ProductV1Dto.ProductItem> toListWithBrands(
            List<ProductModel> productModelList, Map<Long, String> brandNameMap) {
        return productModelList.stream()
                .map(product -> {
                    Long brandId = product.getBrandId().getValue();
                    String brandName = brandNameMap.get(brandId);
                    return createProductItem(product, brandId, brandName);
                })
                .toList();
    }
    
    private ProductV1Dto.ProductItem createProductItem(
            ProductModel product, Long brandId, String brandName) {
        return new ProductV1Dto.ProductItem(
                product.getId(),
                product.getProductName().getValue(),
                product.getPrice().getValue(),
                brandId,
                brandName,
                product.getImgUrl().getValue(),
                product.getLikeCount().getValue(),
                product.getStatus().getValue(),
                product.getStock().getValue()
        );
    }
}
