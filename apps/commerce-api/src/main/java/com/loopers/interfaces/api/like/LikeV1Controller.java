package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeCommand;
import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.application.like.LikeLockFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/like")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeLockFacade likeLockFacade;
    private final LikeFacade likeFacade;

    @PostMapping("/products/{productId}")
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> likeProduct(
            @RequestHeader("X-USER-ID") String loginId,
            @PathVariable Long productId
    ) {
        LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);
        LikeInfo likeInfo = likeLockFacade.recordLike(command);
        LikeV1Dto.LikeResponse response = LikeV1Dto.LikeResponse.from(likeInfo);

        return ApiResponse.success(response);
    }

    @DeleteMapping("/products/{productId}")
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> cancelLikeProduct(
            @RequestHeader("X-USER-ID") String loginId,
            @PathVariable Long productId
    ) {
        LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);
        LikeInfo likeInfo = likeLockFacade.cancelLike(command);
        LikeV1Dto.LikeResponse response = LikeV1Dto.LikeResponse.from(likeInfo);

        return ApiResponse.success(response);
    }

    @GetMapping("/products")
    @Override
    public ApiResponse<LikeV1Dto.LikedProductListResponse> getLikedProducts(
            @RequestHeader("X-USER-ID") String loginId
    ) {
        List<ProductInfo> productInfos = likeFacade.getLikedProducts(loginId);
        LikeV1Dto.LikedProductListResponse response = LikeV1Dto.LikedProductListResponse.from(productInfos);

        return ApiResponse.success(response);
    }
}

