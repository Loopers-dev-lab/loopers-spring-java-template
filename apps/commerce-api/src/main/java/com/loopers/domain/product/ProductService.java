package com.loopers.domain.product;

import com.loopers.application.product.ProductCommand;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.embeded.BrandId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductService {
    
    public List<ProductCommand.ProductData.ProductItem> toListWithSingleBrand(
            List<ProductModel> productModelList, BrandModel brandModel) {
        Long brandId = brandModel.getId();
        String brandName = brandModel.getBrandNaem().getValue();
        
        return productModelList.stream()
                .map(product -> createProductItem(product, brandId, brandName))
                .toList();
    }

    public List<ProductCommand.ProductData.ProductItem> toListWithBrands(
            List<ProductModel> productModelList, Map<Long, String> brandNameMap) {
        return productModelList.stream()
                .map(product -> {
                    Long brandId = product.getBrandId().getValue();
                    String brandName = brandNameMap.get(brandId);
                    return createProductItem(product, brandId, brandName);
                })
                .toList();
    }
    
    public ProductCommand.ProductData.ProductItem createProductItem(
            ProductModel product, Long brandId, String brandName) {
        return new ProductCommand.ProductData.ProductItem(
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

    public List<Long> toDistinctBrandIds(List<ProductModel> content) {
        return content.stream().map(ProductModel::getBrandId)
                .map(BrandId::getValue)
                .distinct()
                .toList();
    }
    public Map<Long, String> createBrandNameMap(List<BrandModel> brandModels, List<Long> requestedBrandIds) {
        // 도메인 비즈니스 룰: 요청된 모든 브랜드가 존재해야 함
        if (brandModels.size() != requestedBrandIds.size()) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "상품에 포함된 브랜드 정보가 존재하지 않습니다.");
        }

        return brandModels.stream().collect(Collectors.toMap(
                BrandModel::getId,
                brand -> brand.getBrandNaem().getValue()
        ));
    }
    public ProductCommand.ProductData createProductData(
            Page<ProductModel> productModels, List<ProductCommand.ProductData.ProductItem> productItems) {
        return new ProductCommand.ProductData(productModels, productItems);
    }
}
