package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixture.TestEventCaptor;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductLikeFacade의 이벤트 발행을 검증하는 테스트.
 *
 * ProductLikeDomainService에서 ProductLikedEvent를 발행하며,
 * 이 이벤트는 즉시 발행됨 (runAfterCommit 아님).
 */
@SpringBootTest
@Import(TestEventCaptor.class)
class ProductLikeFacadeEventPublishTest {

    @Autowired
    private ProductLikeFacade productLikeFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private TestEventCaptor eventCaptor;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        eventCaptor.clear();

        user = userRepository.save(
                User.create("eventuser1", "eventuser@test.com", "1990-01-01", Gender.MALE));

        Brand brand = brandJpaRepository.save(Brand.create("테스트브랜드"));
        product = productRepository.save(
                Product.create("테스트상품", "설명", 10000L, 100L, brand.getId()));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("ProductLikedEvent 발행 검증")
    class ProductLikedEventTest {

        @Test
        @DisplayName("좋아요 시 ProductLikedEvent(liked=true)가 발행된다")
        void productLikedEventTest1() {
            // act
            productLikeFacade.likeProduct(user.getUserId(), product.getId());

            // assert
            assertThat(eventCaptor.hasEventOfType(ProductLikedEvent.class)).isTrue();

            ProductLikedEvent event = eventCaptor.getFirstEventOfType(ProductLikedEvent.class);
            assertThat(event.getProductId()).isEqualTo(product.getId());
            assertThat(event.isLiked()).isTrue();
        }

        @Test
        @DisplayName("좋아요 취소 시 ProductLikedEvent(liked=false)가 발행된다")
        void productLikedEventTest2() {
            // arrange
            productLikeFacade.likeProduct(user.getUserId(), product.getId());
            eventCaptor.clear();

            // act
            productLikeFacade.unlikeProduct(user.getUserId(), product.getId());

            // assert
            assertThat(eventCaptor.hasEventOfType(ProductLikedEvent.class)).isTrue();

            ProductLikedEvent event = eventCaptor.getFirstEventOfType(ProductLikedEvent.class);
            assertThat(event.getProductId()).isEqualTo(product.getId());
            assertThat(event.isLiked()).isFalse();
        }

        @Test
        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면 이벤트가 발행되지 않는다")
        void productLikedEventTest3() {
            // arrange
            productLikeFacade.likeProduct(user.getUserId(), product.getId());
            eventCaptor.clear();

            // act - 중복 좋아요
            productLikeFacade.likeProduct(user.getUserId(), product.getId());

            // assert - 이벤트 발행 안 됨 (이미 좋아요 상태)
            assertThat(eventCaptor.hasEventOfType(ProductLikedEvent.class)).isFalse();
        }

        @Test
        @DisplayName("좋아요하지 않은 상품을 좋아요 취소하면 이벤트가 발행되지 않는다")
        void productLikedEventTest4() {
            // act - 좋아요 안 한 상태에서 취소
            productLikeFacade.unlikeProduct(user.getUserId(), product.getId());

            // assert - 이벤트 발행 안 됨 (좋아요 상태 아님)
            assertThat(eventCaptor.hasEventOfType(ProductLikedEvent.class)).isFalse();
        }

        @Test
        @DisplayName("이벤트에 올바른 userId와 productId가 포함된다")
        void productLikedEventTest5() {
            // act
            productLikeFacade.likeProduct(user.getUserId(), product.getId());

            // assert
            ProductLikedEvent event = eventCaptor.getFirstEventOfType(ProductLikedEvent.class);
            assertThat(event.getProductId()).isEqualTo(product.getId());
            assertThat(event.getUserId()).isEqualTo(user.getId());
        }
    }
}
