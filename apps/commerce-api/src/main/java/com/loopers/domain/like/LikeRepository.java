package com.loopers.domain.like;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LikeRepository {

  Optional<Like> findById(Long userId, Long productId);

  int save(Long userId, Long productId);

  void remove(Long userId, Long productId);

  boolean isLiked(Long userId, Long productId);

  long getLikeCount(Long productId);

  Page<Like> getLikedProducts(Long userId, Pageable pageable);
}
