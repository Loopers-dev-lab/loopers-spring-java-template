package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductCommand;
import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec{

    private final ProductFacade productFacade;

    public ProductV1Controller(ProductFacade productFacade) {
        this.productFacade = productFacade;
    }

    @Override
    @GetMapping
    public ApiResponse<ProductV1Dto.ListResponse> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        ProductCommand.Request.GetList request =
                new ProductCommand.Request.GetList(brandId, sort, page, size);
        
        ProductCommand.ProductData result = productFacade.getProductList(request);

        return ApiResponse.success(
                ProductV1Dto.ListResponse.of(
                        result.productItemList(),      // 실제 데이터 목록 (ProductItem 리스트)
                        result.productModels().getTotalElements(),     // 전체 개수
                        result.productModels().getNumber(),            // 현재 페이지 번호
                        result.productModels().getSize()               // 페이지 크기
                )
        );
    }
}

