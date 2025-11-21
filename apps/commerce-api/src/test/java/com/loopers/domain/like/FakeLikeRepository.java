package com.loopers.domain.like;

import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class FakeLikeRepository implements LikeRepository {
  private final Map<Long, Like> list = new HashMap<>();

  @Override
  public Optional<Like> findById(Long userId, Long productId) {
    for (Long key : list.keySet()) {
      Like like = list.get(key);
      if (like.getUser().getId().equals(userId)
          && like.getProduct().getId().equals(productId)) {
        return Optional.of(like);
      }
    }
    return Optional.empty();
  }

  @Override
  public int save(Long userId, Long productId) {
    for (Long key : list.keySet()) {
      Like like = list.get(key);
      if (like.getUser().getId().equals(userId)
          && like.getProduct().getId().equals(productId)) {
        throw new CoreException(ErrorType.BAD_REQUEST, "중복 좋아요 요청입니다.");
      }
    }
    Long id = list.keySet().stream().max(Long::compareTo).orElse(0L) + 1;
    Like like = Like.create(UserFixture.createUser(), ProductFixture.createProduct());

    list.put(id, like);
    return 1;
  }

  @Override
  public void remove(Long userId, Long productId) {
    Iterator<Map.Entry<Long, Like>> iterator = list.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Long, Like> entry = iterator.next();
      Like like = entry.getValue();
      if (like.getUser().getId().equals(userId)
          && like.getProduct().getId().equals(productId)) {
        iterator.remove();
      }
    }
  }

  @Override
  public boolean isLiked(Long userId, Long productId) {
    for (Long key : list.keySet()) {
      Like like = list.get(key);
      if (like.getUser().getId().equals(userId)
          && like.getProduct().getId().equals(productId)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public long getLikeCount(Long productId) {
    long cnt = 0;
    for (Long key : list.keySet()) {
      Like like = list.get(key);
      if (like.getProduct().getId().equals(productId)) {
        cnt++;
      }
    }
    return cnt;
  }

  @Override
  public Page<Like> getLikedProducts(Long userId, Pageable pageable) {
    return new PageImpl<>(list.values().stream().filter(i -> i.getUser().getId().equals(userId))
        .collect(Collectors.toList()), pageable, list.size());
  }

  @Override
  public List<ProductIdAndLikeCount> getLikeCountWithProductId(List<Long> productIds) {
    Map<Long, Long> likeCountsByProduct = list.values().stream()
        .filter(like -> productIds.contains(like.getProduct().getId()))
        .collect(Collectors.groupingBy(
            like -> like.getProduct().getId(),
            Collectors.counting()
        ));

    return likeCountsByProduct.entrySet().stream()
        .map(entry -> new ProductIdAndLikeCount(
            entry.getKey(), // Product ID
            entry.getValue() // Like Count
        ))
        .collect(Collectors.toList());
  }
}
