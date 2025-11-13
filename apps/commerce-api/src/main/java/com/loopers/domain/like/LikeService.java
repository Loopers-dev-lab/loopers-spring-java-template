
package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class LikeService {

  private final LikeRepository likeRepository;

  @Transactional
  public Like save(Like like) {
    Long userId = like.getUser().getId();
    Long productId = like.getProduct().getId();
    Optional<Like> existingLike = likeRepository.findById(userId, productId);

    if (existingLike.isPresent()) {
      return existingLike.get();
    }
    return likeRepository.save(like);
  }


  @Transactional
  public long remove(Long userId, Long productId) {
    return likeRepository.remove(userId, productId);
  }

  @Transactional
  public boolean isLiked(Long userId, Long productId) {
    return likeRepository.isLiked(userId, productId);
  }

  public Page<Product> getLikedProducts(
      Long userId,
      String sortType,
      int page,
      int size
  ) {
    if (userId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    Sort sort = this.getSortBySortType(sortType);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Like> likePage = likeRepository.getLikedProducts(userId, pageable);
    List<Product> likedProducts = likePage.getContent().stream().map(Like::getProduct).toList();
    return new PageImpl<>(likedProducts, pageable, likePage.getTotalElements());
  }

  private Sort getSortBySortType(String sortType) {
    if (sortType == null) sortType = "latest";
    Sort latestSort = Sort.by("createdAt").descending();
    switch (sortType.toLowerCase()) {
      case "latest":
        return latestSort;
      case "price_asc":
        return Sort.by("price").ascending().and(latestSort);
      case "likes_desc":
        return Sort.by("likesCount").descending().and(latestSort);
      default:
        return latestSort;
    }
  }
}
