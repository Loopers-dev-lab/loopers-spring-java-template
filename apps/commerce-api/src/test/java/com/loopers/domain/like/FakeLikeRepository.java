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
    Long id = (long) 0;
    for (Long key : list.keySet()) {
      id = key;
      Like like = list.get(key);
      if (like.getUser().getId() == userId
          && like.getProduct().getId() == productId) {
        return Optional.of(like);
      }
    }
    return null;
  }

  @Override
  public Like save(Like entity) {
    Long id = (long) 0;
    for (Long key : list.keySet()) {
      id = key;
      Like like = list.get(key);
      if (like.getUser().getId() == entity.getUser().getId()
          && like.getProduct().getId() == entity.getUser().getId()) {
        throw new CoreException(ErrorType.BAD_REQUEST, "중복 좋아요 요청입니다.");
      }
    }
    id = id == (long) 0 ? 0 : id + 1;
    list.put(id, entity);
    return entity;
  }

  @Override
  public long remove(Long userId, Long productId) {
    long cnt = 0;
    for (Long key : list.keySet()) {
      Like like = list.get(key);
      if (like.getUser().getId() == userId
          && like.getProduct().getId() == productId) {
        list.remove(key);
        cnt++;
      }
    }
    return cnt;
  }

  @Override
  public boolean isLiked(Long userId, Long productId) {
    for (Long key : list.keySet()) {
      Like like = list.get(key);
      if (like.getUser().getId() == userId
          && like.getProduct().getId() == productId) {
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
      if (like.getProduct().getId() == productId) {
        cnt++;
      }
    }
    return cnt;
  }

  @Override
  public Page<Like> getLikedProducts(Long userId, Pageable pageable) {
    return new PageImpl<>(list.values().stream().filter(i -> i.getUser().getId() == userId)
        .collect(Collectors.toList()), pageable, list.size());
  }

}
