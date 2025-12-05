package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/like")
@RequiredArgsConstructor
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @Override
    public ApiResponse<Void> addLike(String userId, Long productId) {
        likeFacade.addLike(userId, productId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> removeLike(String userId, Long productId) {
        likeFacade.removeLike(userId, productId);
        return ApiResponse.success(null);
    }
}
