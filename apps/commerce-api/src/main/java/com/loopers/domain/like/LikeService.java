package com.loopers.domain.like;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.product.ProductModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;

    // 좋아요 취소 또는 등록
    @Transactional
    public void toggleLike(UserModel user, ProductModel product) {
        var existing = likeRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
        } else {
            LikeModel newLike = new LikeModel(user, product);
            likeRepository.save(newLike);
        }
    }

    // 좋아요 등록: 좋아요가 없으면 추가, 있으면 취소
    @Transactional
    public void addLike(UserModel user, ProductModel product) {
        var existing = likeRepository.findByUserAndProduct(user, product);
        if (existing.isEmpty()) {
            LikeModel newLike = new LikeModel(user, product);
            likeRepository.save(newLike);
        } else {
            likeRepository.delete(existing.get());
        }
    }

    // 좋아요 취소: 좋아요가 있으면 취소, 없으면 추가
    @Transactional
    public void removeLike(UserModel user, ProductModel product) {
        var existing = likeRepository.findByUserAndProduct(user, product);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
        } else {
            LikeModel newLike = new LikeModel(user, product);
            likeRepository.save(newLike);
        }
    }

    // 좋아요 여부 확인
    @Transactional(readOnly = true)
    public boolean isLiked(UserModel user, ProductModel product) {
        return likeRepository.findByUserAndProduct(user, product).isPresent();
    }

    // 좋아요한 상품 목록 조회
    @Transactional(readOnly = true)
    public List<ProductModel> getLikedProducts(UserModel user) {
        return likeRepository.findLikedProductsByUser(user);
    }

    // 좋아요 수 조회
    @Transactional(readOnly = true)
    public long getLikeCount(ProductModel product) {
        return likeRepository.countByProductLiked(product);
    }

    // 좋아요 수 일괄 집계
    @Transactional(readOnly = true)
    public Map<Long, Long> getLikeCounts(List<ProductModel> products) {
        var ids = products.stream().map(ProductModel::getId).collect(Collectors.toSet());
        return likeRepository.countByProductIdsLiked(ids);
    }
}