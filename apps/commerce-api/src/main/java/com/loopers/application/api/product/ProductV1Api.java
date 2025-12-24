package com.loopers.application.api.product;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductDetail;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.domain.product.ProductRankingList;
import com.loopers.core.service.product.GetProductRankingService;
import com.loopers.core.service.product.ProductQueryService;
import com.loopers.core.service.product.query.GetProductDetailQuery;
import com.loopers.core.service.product.query.GetProductListQuery;
import com.loopers.core.service.product.query.GetProductQuery;
import com.loopers.core.service.product.query.GetProductRankingQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.loopers.application.api.product.ProductV1Dto.*;
import static com.loopers.application.api.product.ProductV1Dto.GetProductDetailResponse.GetProductRankingsResponse;
import static com.loopers.application.api.product.ProductV1Dto.GetProductDetailResponse.from;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductV1Api implements ProductV1ApiSpec {

    private final ProductQueryService queryService;
    private final GetProductRankingService getProductRankingService;

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<GetProductResponse> getProduct(@PathVariable String productId) {
        Product product = queryService.getProductBy(new GetProductQuery(productId));
        return ApiResponse.success(GetProductResponse.from(product));
    }

    @Override
    @GetMapping
    public ApiResponse<GetProductListResponse> getProductList(
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String createdAtSort,
            @RequestParam(required = false) String priceSort,
            @RequestParam(required = false) String likeCountSort,
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize
    ) {
        ProductListView productList = queryService.getProductList(new GetProductListQuery(
                brandId, createdAtSort, priceSort, likeCountSort, pageNo, pageSize
        ));
        return ApiResponse.success(GetProductListResponse.from(productList));
    }

    @Override
    @GetMapping("/{productId}/detail")
    public ApiResponse<GetProductDetailResponse> getProductDetail(
            @PathVariable String productId
    ) {
        ProductDetail productDetail = queryService.getProductDetail(new GetProductDetailQuery(productId));

        return ApiResponse.success(from(productDetail));
    }

    @Override
    @GetMapping("/rankings")
    public ApiResponse<GetProductRankingsResponse> getProductRankings(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize
    ) {
        ProductRankingList ranking = getProductRankingService.getRanking(new GetProductRankingQuery(date, pageNo, pageSize));

        return ApiResponse.success(GetProductRankingsResponse.from(ranking));
    }
}
