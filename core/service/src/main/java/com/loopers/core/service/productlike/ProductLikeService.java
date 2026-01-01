package com.loopers.core.service.productlike;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.event.ProductLikeEvent;
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
    private final EventOutboxRepository outboxRepository;

    @Transactional
    public void like(ProductLikeCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.getUserIdentifier()));
        Product product = productRepository.getById(new ProductId(command.getProductId()));

        long timestamp = System.currentTimeMillis();
        ProductLikeCache likeCache = new ProductLikeCache(product.getId(), user.getId(), timestamp);
        productLikeCacheRepository.saveLike(likeCache);
        productLikeCacheRepository.deleteUnlike(likeCache);

        EventId eventId = EventId.generate();
        outboxRepository.save(
                EventOutbox.create(
                        eventId,
                        AggregateType.PRODUCT,
                        product.getId().toAggregateId(),
                        EventType.LIKE_PRODUCT,
                        new EventPayload(JacksonUtil.convertToString(new ProductLikeEvent(eventId, product.getId())))
                )
        );
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
