package com.loopers.core.service.productlike;

import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductLikeCacheRepository;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.productlike.command.ProductLikeCommand;
import com.loopers.core.service.productlike.command.ProductUnlikeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductLikeService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductLikeCacheRepository productLikeCacheRepository;

    @Transactional
    public void like(ProductLikeCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.getUserIdentifier()));
        Product product = productRepository.getById(new ProductId(command.getProductId()));

        long timestamp = System.currentTimeMillis();
        ProductLikeCache likeCache = new ProductLikeCache(product.getId(), user.getId(), timestamp);
        productLikeCacheRepository.saveLike(likeCache);
        productLikeCacheRepository.deleteUnlike(likeCache);
    }

    @Transactional
    public void unlike(ProductUnlikeCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.getUserIdentifier()));
        Product product = productRepository.getByIdWithLock(new ProductId(command.getProductId()));

        long timestamp = System.currentTimeMillis();
        ProductLikeCache unlikeCache = new ProductLikeCache(product.getId(), user.getId(), timestamp);
        productLikeCacheRepository.saveUnlike(unlikeCache);
        productLikeCacheRepository.deleteLike(unlikeCache);
    }
}
