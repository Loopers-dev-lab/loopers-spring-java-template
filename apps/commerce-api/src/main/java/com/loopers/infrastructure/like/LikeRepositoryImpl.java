package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
@Primary
public class LikeRepositoryImpl implements LikeRepository {
  private final LikeJpaRepository jpaRepository;

  @Override
  public Optional<Like> findById(Long userId, Long productId) {
    return jpaRepository.findByUserIdAndProductId(userId, productId);
  }

  @Override
  public int save(Long userId, Long productId) {
    return jpaRepository.save(userId, productId);
  }

  @Override
  public void remove(Long userId, Long productId) {
    jpaRepository.delete(userId, productId);
  }

  @Override
  public boolean isLiked(Long userId, Long productId) {
    return jpaRepository.existsByUserIdAndProductId(userId, productId);
  }

  @Override
  public long getLikeCount(Long productId) {
    return jpaRepository.countByProductId(productId);
  }

  @Override
  public Page<Like> getLikedProducts(Long userId, Pageable pageable) {
    return jpaRepository.getLikedProducts(userId, pageable);
  }

}
