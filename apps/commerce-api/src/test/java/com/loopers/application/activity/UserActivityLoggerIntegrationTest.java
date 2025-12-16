package com.loopers.application.activity;

import com.loopers.domain.Money;
import com.loopers.domain.activity.event.UserActivityEvent;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class UserActivityLoggerIntegrationTest {

    @SpyBean
    private UserActivityLogger userActivityLogger;

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("좋아요 추가 시 UserActivityEvent가 발행된다")
    void addLike_publishesUserActivityEvent() throws InterruptedException {
        // given
        Brand brand = Brand.createBrand("테스트브랜드");
        Brand savedBrand = brandRepository.registerBrand(brand);

        Product product = Product.createProduct(
                "P001", "테스트상품", Money.of(10000), 100, savedBrand
        );
        Product savedProduct = productRepository.registerProduct(product);

        User user = User.createUser("testUser", "test@test.com", "1990-01-01", Gender.MALE);
        User savedUser = userRepository.save(user);

        // when
        productLikeService.addLike(savedUser, savedProduct);

        // then: 비동기 로깅을 위한 대기
        verify(userActivityLogger, timeout(1000).times(1))
                .logActivity(argThat(event ->
                        event.userId().equals("testUser") &&
                                event.action().equals("PRODUCT_LIKE_ADDED") &&
                                event.resourceType().equals("PRODUCT") &&
                                event.resourceId().equals(savedProduct.getId())
                ));
    }

    @Test
    @DisplayName("직접 이벤트를 발행하면 로거가 수신한다")
    void publishEvent_receivedByLogger() throws InterruptedException {
        // given
        UserActivityEvent event = UserActivityEvent.of(
                "testUser",
                "TEST_ACTION",
                "TEST_RESOURCE",
                123L
        );

        // when
        eventPublisher.publishEvent(event);

        // then: 비동기 로깅을 위한 대기
        verify(userActivityLogger, timeout(1000).times(1))
                .logActivity(argThat(e ->
                        e.userId().equals("testUser") &&
                                e.action().equals("TEST_ACTION") &&
                                e.resourceType().equals("TEST_RESOURCE") &&
                                e.resourceId().equals(123L)
                ));
    }
}
