package com.loopers.domain.like;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class ProductLikeServiceTest {

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("유저와 상품으로 좋아요를 등록할 수 있다")
    @Test
    @Transactional
    void addLike_success() {
        // given
        User user = createUser("user123", "user@test.com");
        Product product = createProduct("P001", "상품1");
        Long initialLikeCount = product.getLikeCount();

        // when
        ProductLike like = productLikeService.addLike(user, product);

        // then
        assertThat(like.getId()).isNotNull();
        assertThat(like.getLikeUser()).isEqualTo(user);
        assertThat(like.getLikeProduct()).isEqualTo(product);
        assertThat(like.getLikeAt()).isNotNull();
        assertThat(product.getLikeCount()).isEqualTo(initialLikeCount + 1);
    }

    @DisplayName("같은 유저가 같은 상품에 여러 번 좋아요를 등록해도 성공한다 (멱등성)")
    @Test
    @Transactional
    void addLike_idempotent_success() {
        // given
        User user = createUser("user123", "user@test.com");
        Product product = createProduct("P001", "상품1");
        Long initialLikeCount = product.getLikeCount();

        // when
        ProductLike firstLike = productLikeService.addLike(user, product);
        ProductLike secondLike = productLikeService.addLike(user, product);
        ProductLike thirdLike = productLikeService.addLike(user, product);

        // then
        assertThat(firstLike.getId()).isNotNull();
        assertThat(secondLike.getId()).isEqualTo(firstLike.getId());
        assertThat(thirdLike.getId()).isEqualTo(firstLike.getId());

        // 좋아요 수는 1번만 증가해야 함
        assertThat(product.getLikeCount()).isEqualTo(initialLikeCount + 1);

        // DB에는 1개의 좋아요만 존재해야 함
        boolean exists = productLikeRepository.existsByLikeUserAndLikeProduct(user, product);
        assertThat(exists).isTrue();
    }

    @DisplayName("등록된 좋아요를 취소할 수 있다")
    @Test
    @Transactional
    void cancelLike_success() {
        // given
        User user = createUser("user123", "user@test.com");
        Product product = createProduct("P001", "상품1");

        productLikeService.addLike(user, product);
        Long likeCountAfterAdd = product.getLikeCount();

        // when
        productLikeService.cancelLike(user, product);

        // then
        boolean exists = productLikeRepository.existsByLikeUserAndLikeProduct(user, product);
        assertThat(exists).isFalse();
        assertThat(product.getLikeCount()).isEqualTo(likeCountAfterAdd - 1);
    }

    @DisplayName("존재하지 않는 좋아요를 취소하려고 하면 예외가 발생한다")
    @Test
    @Transactional
    void cancelLike_notExists_throwException() {
        // given
        User user = createUser("user123", "user@test.com");
        Product product = createProduct("P001", "상품1");

        // when // then
        CoreException exception = assertThrows(CoreException.class, () -> {
            productLikeService.cancelLike(user, product);
        });

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(exception.getCustomMessage()).isEqualTo("좋아요가 존재하지 않습니다");
    }

    @DisplayName("좋아요를 등록하고 취소한 후 다시 등록할 수 있다")
    @Test
    @Transactional
    void addLike_afterCancel_success() {
        // given
        User user = createUser("user123", "user@test.com");
        Product product = createProduct("P001", "상품1");

        productLikeService.addLike(user, product);
        productLikeService.cancelLike(user, product);

        entityManager.flush();
        entityManager.clear();

        // when
        ProductLike newLike = productLikeService.addLike(user, product);

        // then
        assertThat(newLike.getId()).isNotNull();
        assertThat(newLike.getLikeUser().getId()).isEqualTo(user.getId());
        assertThat(newLike.getLikeProduct().getId()).isEqualTo(product.getId());
    }

    private User createUser(String userId, String email) {
        User user = User.createUser(userId, email, "1990-01-01", Gender.MALE);
        entityManager.persist(user);
        return user;
    }

    private Product createProduct(String productCode, String productName) {
        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct(
                productCode,
                productName,
                Money.of(10000),
                100,
                brand
        );
        entityManager.persist(product);
        return product;
    }
}
