package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public boolean addLike(User user, Product product) {
        try {
            Like like = Like.create(user, product);
            likeRepository.save(like);
            return true;
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateLikeException(e)) {
                return false;
            }
            throw e;
        }
    }

    private boolean isDuplicateLikeException(DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("Duplicate entry");
    }

    @Transactional
    public boolean removeLike(User user, Product product) {
        return likeRepository.findByUserAndProduct(user, product)
                .map(like -> {
                    likeRepository.delete(like);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Long getLikeCount(Product product) {
        return product.getLikeCount();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getLikeCounts(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Product::getLikeCount
                ));
    }
}
