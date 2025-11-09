package com.loopers.core.service.productlike;

import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
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
    private final ProductLikeRepository productLikeRepository;

    @Transactional
    public void like(ProductLikeCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.getUserIdentifier()));
        Product product = productRepository.getById(new ProductId(command.getProductId()));

        boolean isAlreadyLiked = productLikeRepository.findByUserIdAndProductId(user.getUserId(), product.getProductId())
                .isPresent();

        if (!isAlreadyLiked) {
            productLikeRepository.save(ProductLike.create(user.getUserId(), product.getProductId()));
            productRepository.save(product.increaseLikeCount());
        }
    }

    @Transactional
    public void unlike(ProductUnlikeCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.getUserIdentifier()));
        Product product = productRepository.getById(new ProductId(command.getProductId()));

        boolean isAlreadyLiked = productLikeRepository.findByUserIdAndProductId(user.getUserId(), product.getProductId())
                .isPresent();

        if (isAlreadyLiked) {
            productLikeRepository.deleteByUserIdAndProductId(user.getUserId(), product.getProductId());
            productRepository.save(product.decreaseLikeCount());
        }
    }
}
