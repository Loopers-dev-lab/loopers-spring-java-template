package com.loopers.application.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.embeded.BrandId;
import com.loopers.interfaces.api.product.ProductV1Dto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductFacde {
    private final BrandReader brandReader;
    private final ProductRepository productRepository;
    private final ProductAssembler productAssembler;

    public ProductFacde(BrandReader brandReader, ProductRepository productRepository, ProductAssembler productAssembler) {
        this.brandReader = brandReader;
        this.productRepository = productRepository;
        this.productAssembler = productAssembler;
    }

    public ProductV1Dto.ListResponse getProductList(ProductCommand.Request.GetList request) {
        if (request.brandId() != null) {
            return getProductListByBrand(request);
        }
        return getAllProductList(request);
    }

    private ProductV1Dto.ListResponse getProductListByBrand(ProductCommand.Request.GetList request) {
        BrandModel brandModel = brandReader.getByIdOrThrow(request.brandId());
        Page<ProductModel> productModels = productRepository.search(
            brandModel.getId(), request.sort(), request.page(), request.size()
        );
        List<ProductV1Dto.ProductItem> productItems = 
            productAssembler.toListWithSingleBrand(productModels.getContent(), brandModel);
        
        return createListResponse(productModels, productItems);
    }

    private ProductV1Dto.ListResponse getAllProductList(ProductCommand.Request.GetList request) {
        Page<ProductModel> productModels = productRepository.search(
            null, request.sort(), request.page(), request.size()
        );
        
        Map<Long, String> brandMap = getBrandMap(productModels.getContent());
        List<ProductV1Dto.ProductItem> productItems = 
            productAssembler.toListWithBrands(productModels.getContent(), brandMap);
        
        return createListResponse(productModels, productItems);
    }

    private ProductV1Dto.ListResponse createListResponse(
            Page<ProductModel> productModels, List<ProductV1Dto.ProductItem> productItems) {
        return new ProductV1Dto.ListResponse(
            productItems,
            productModels.getTotalPages(),
            productModels.getNumber(),
            productModels.getSize()
        );
    }

    private Map<Long, String> getBrandMap(List<ProductModel> products) {
        List<Long> brandIds = products.stream()
                .map(ProductModel::getBrandId)
                .map(BrandId::getValue)
                .distinct()
                .toList();
        return brandReader.getByIdsOrThrow(brandIds);
    }
}
