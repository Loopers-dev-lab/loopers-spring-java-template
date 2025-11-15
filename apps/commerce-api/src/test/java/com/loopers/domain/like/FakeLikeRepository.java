package com.loopers.domain.like;

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
  public Like save(Like entity) {
    for (Long key : list.keySet()) {
      Like like = list.get(key);
      if (like.getUser().getId().equals(entity.getUser().getId())
          && like.getProduct().getId().equals(entity.getProduct().getId())) {
        throw new CoreException(ErrorType.BAD_REQUEST, "중복 좋아요 요청입니다.");
      }
    }
    Long id = list.keySet().stream().max(Long::compareTo).orElse(0L) + 1;
    list.put(id, entity);
    return entity;
  }

  @Override
  public long remove(Long userId, Long productId) {
    long cnt = 0;
    Iterator<Map.Entry<Long, Like>> iterator = list.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Long, Like> entry = iterator.next();
      Like like = entry.getValue();
      if (like.getUser().getId().equals(userId)
          && like.getProduct().getId().equals(productId)) {
        iterator.remove();
        cnt++;
      }
    }
    return cnt;
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

}
