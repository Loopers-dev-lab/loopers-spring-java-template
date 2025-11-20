package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 좋아요 API v1 컨트롤러.
 * <p>
 * 상품 좋아요 추가, 삭제, 목록 조회 유즈케이스를 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/like/products")
public class LikeV1Controller {

    private final LikeFacade likeFacade;

    /**
     * Adds a like from the specified user to the given product.
     *
     * @param userId    the user identifier extracted from the X-USER-ID request header
     * @param productId the identifier of the product to like
     * @return          an ApiResponse with no payload indicating the operation succeeded
     */
    @PostMapping("/{productId}")
    public ApiResponse<Void> addLike(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable Long productId
    ) {
        likeFacade.addLike(userId, productId);
        return ApiResponse.success(null);
    }

    /**
     * Remove a user's like from the specified product.
     *
     * @param userId   the user identifier extracted from the `X-USER-ID` request header
     * @param productId the identifier of the product to remove the like from
     * @return an ApiResponse with no payload indicating the operation succeeded
     */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeLike(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable Long productId
    ) {
        likeFacade.removeLike(userId, productId);
        return ApiResponse.success(null);
    }

    /**
     * Retrieves the list of products liked by the specified user.
     *
     * @param userId the user ID provided in the `X-USER-ID` request header
     * @return an ApiResponse containing the liked products payload
     */
    @GetMapping
    public ApiResponse<LikeV1Dto.LikedProductsResponse> getLikedProducts(
        @RequestHeader("X-USER-ID") String userId
    ) {
        var likedProducts = likeFacade.getLikedProducts(userId);
        return ApiResponse.success(LikeV1Dto.LikedProductsResponse.from(likedProducts));
    }
}
