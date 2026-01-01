package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductListItem;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductV1Controller implements ProductV1ApiSpec {

  private final ProductFacade productFacade;

  @Override
  public ApiResponse<ProductV1Dto.ProductListsResponse> getProducts(Long userId, Long brandId, String sortType, int page, int size) {
    Page<ProductListItem> productPage;
    if (brandId != null) {
      productPage = productFacade.getProductList(userId, brandId, sortType, page, size);
    } else {
      productPage = productFacade.getProductList(userId, null, sortType, page, size);
    }

    List<ProductV1Dto.ProductListResponse> products = productPage.getContent().stream()
        .map(ProductV1Dto.ProductListResponse::from)
        .toList();

    ProductV1Dto.ProductListsResponse response = new ProductV1Dto.ProductListsResponse(
        products,
        productPage.getTotalElements()
    );

    return ApiResponse.success(response);
  }

  @Override
  public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(Long userId, Long productId) {
    ProductDetailInfo product = productFacade.getProductDetail(userId, productId);
    return ApiResponse.success(ProductV1Dto.ProductDetailResponse.from(product));
  }

}
