package com.loopers.application.event.like;

import com.loopers.application.like.LikeCreateEvent;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LikeEventHandler {
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLikeCreated(LikeCreateEvent event) {
        Product product = productRepository.findById(event.productId()).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        );

        product.addLikeCount();
    }
}
