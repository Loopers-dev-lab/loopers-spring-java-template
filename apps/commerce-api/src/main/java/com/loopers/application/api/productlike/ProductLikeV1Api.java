package com.loopers.application.api.productlike;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.service.productlike.ProductLikeQueryService;
import com.loopers.core.service.productlike.ProductLikeService;
import com.loopers.core.service.productlike.command.ProductLikeCommand;
import com.loopers.core.service.productlike.command.ProductUnlikeCommand;
import com.loopers.core.service.productlike.query.GetLikeProductsListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.loopers.application.api.productlike.ProductLikeV1Dto.LikeProductsResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/like/products")
public class ProductLikeV1Api implements ProductLikeV1ApiSpec {

    private final ProductLikeService service;
    private final ProductLikeQueryService queryService;

    @Override
    @PostMapping("/{productId}")
    public ApiResponse<Void> likeProduct(
            @PathVariable String productId,
            @RequestHeader(name = "X-USER-ID") String userIdentifier
    ) {
        service.like(new ProductLikeCommand(userIdentifier, productId));
        return ApiResponse.success(null);
    }

    @Override
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> unlikeProduct(
            @PathVariable String productId,
            @RequestHeader(name = "X-USER-ID") String userIdentifier
    ) {
        service.unlike(new ProductUnlikeCommand(userIdentifier, productId));
        return ApiResponse.success(null);
    }

    @Override
    @GetMapping("/")
    public ApiResponse<LikeProductsResponse> getLikeProducts(
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestParam String brandId,
            @RequestParam String createdAtSort,
            @RequestParam String priceSort,
            @RequestParam String likeCountSort,
            @RequestParam int pageNo,
            @RequestParam int pageSize
    ) {
        LikeProductListView likeProductsListView = queryService.getLikeProductsListView(
                new GetLikeProductsListQuery(userId, brandId, createdAtSort, priceSort, likeCountSort, pageNo, pageSize)
        );
        return ApiResponse.success(LikeProductsResponse.from(likeProductsListView));
    }
}
