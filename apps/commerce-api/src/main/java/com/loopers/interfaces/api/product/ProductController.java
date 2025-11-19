package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetail;
import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductDto.ProductListResponse;
import com.loopers.interfaces.api.product.ProductDto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApiSpec {

  private final ProductFacade productFacade;

  @Override
  @GetMapping
  public ApiResponse<ProductListResponse> searchProductDetails(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @RequestParam(required = false) Long brandId,
      @RequestParam(defaultValue = "lastest") ProductSortType sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size, sort.toSort());

    Page<ProductDetail> productPage = productFacade.searchProductDetails(brandId, userId,
        pageable);
    ProductListResponse response = ProductListResponse.from(productPage);

    return ApiResponse.success(response);
  }

  @Override
  @GetMapping("/{productId}")
  public ApiResponse<ProductResponse> retrieveProductDetail(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @PathVariable Long productId
  ) {
    ProductDetail detail = productFacade.retrieveProductDetail(productId, userId);
    ProductResponse response = ProductResponse.from(detail);

    return ApiResponse.success(response);
  }
}
