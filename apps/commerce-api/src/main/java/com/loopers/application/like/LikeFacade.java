package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeFacade {
  private final UserService userService;
  private final ProductService productService;
  private final LikeService likeService;

  public LikeInfo like(Long userId, Long productId) {
    likeService.save(userId, productId);
    long likeCnt = likeService.getLikeCount(productId);
    return LikeInfo.from(likeCnt, true);
  }

  public LikeInfo unlike(Long userId, Long productId) {
    likeService.remove(userId, productId);
    long likeCnt = likeService.getLikeCount(productId);
    return LikeInfo.from(likeCnt, false);
  }

}
