package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeCommand;
import com.loopers.application.like.UserLikeProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/likes")
public class LikeV1Controller {
    private final UserLikeProductFacade userLikeProductFacade;

    @PostMapping
    public ResponseEntity<Void> likeProduct(@RequestBody LikeV1Dto.LikeRequest request) {
        LikeCommand.Like like = new LikeCommand.Like(request.userId(), request.productId());
        userLikeProductFacade.userLikeProduct(like);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unlikeProduct(@RequestBody LikeV1Dto.LikeRequest request) {
        LikeCommand.Like like = new LikeCommand.Like(request.userId(), request.productId());
        userLikeProductFacade.userUnlikeProduct(like);
        return ResponseEntity.ok().build();
    }
}
