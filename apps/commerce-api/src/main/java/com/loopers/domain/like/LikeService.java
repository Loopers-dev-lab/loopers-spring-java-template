package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public void addLike(User user, Product product) {
        Like like = Like.create(user, product);
        // 중복 좋아요 방지를 위해 예외 무시
        try {
            likeRepository.save(like);
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    @Transactional
    public void removeLike(User user, Product product) {
        likeRepository.findByUserAndProduct(user, product)
                .ifPresent(likeRepository::delete);
    }

    public Long getLikeCount(Product product) {
        return likeRepository.countByProduct(product);
    }

    public Map<Long, Long> getLikeCounts(List<Product> products) {
        return likeRepository.findLikeCounts(products);
    }
}
