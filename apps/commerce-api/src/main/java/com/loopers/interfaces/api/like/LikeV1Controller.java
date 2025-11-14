package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
public class LikeV1Controller implements LikeV1ApiSpec {
    private final LikeFacade likeFacade;

    @PostMapping
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> doLike(@RequestBody LikeV1Dto.LikeRequest request) {
        LikeInfo info = likeFacade.doLike(request);
        LikeV1Dto.LikeResponse response = LikeV1Dto.LikeResponse.from(info);

        return ApiResponse.success(response);
    }

    @DeleteMapping
    @Override
    public void doUnLike(@RequestParam Long userId, @RequestParam Long productId) {
        likeFacade.doUnlike(userId, productId);
    }
}
