package com.loopers.domain.like;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class ProductLikeServiceTest {

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

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
    void addLike_success() {
        // given
        User savedUser = createUser("user123", "user1@test.com");
        Product savedProduct = createProduct("P001", "상품1");

        // Detached 엔티티를 managed 상태로 변경
        entityManager.clear();
        User user = entityManager.find(User.class, savedUser.getId());
        Product product = entityManager.find(Product.class, savedProduct.getId());
        Long initialLikeCount = product.getLikeCount();

        // when
        productLikeService.addLike(user, product);

        // then
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    entityManager.clear();
                    Product reloadedProduct = entityManager.find(Product.class, product.getId());
                    assertThat(reloadedProduct.getLikeCount()).isEqualTo(initialLikeCount + 1);
                });
    }

    @DisplayName("같은 유저가 같은 상품에 여러 번 좋아요를 등록해도 성공한다 (멱등성)")
    @Test
    void addLike_idempotent_success() {
        // given
        User savedUser = createUser("user456", "user2@test.com");
        Product savedProduct = createProduct("P002", "상품2");

        // Detached 엔티티를 managed 상태로 변경
        entityManager.clear();
        User user = entityManager.find(User.class, savedUser.getId());
        Product product = entityManager.find(Product.class, savedProduct.getId());
        Long initialLikeCount = product.getLikeCount();

        // when
        ProductLike firstLike = productLikeService.addLike(user, product);
        ProductLike secondLike = productLikeService.addLike(user, product);
        ProductLike thirdLike = productLikeService.addLike(user, product);

        // then
        assertThat(firstLike.getId()).isNotNull();
        assertThat(secondLike.getId()).isEqualTo(firstLike.getId());
        assertThat(thirdLike.getId()).isEqualTo(firstLike.getId());

        // 좋아요 수는 1번만 증가해야 함 (비동기 이벤트 완료 대기)
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    entityManager.clear();
                    Product reloadedProduct = entityManager.find(Product.class, product.getId());
                    assertThat(reloadedProduct.getLikeCount()).isEqualTo(initialLikeCount + 1);
                });

        // DB에는 1개의 좋아요만 존재해야 함
        boolean exists = productLikeRepository.existsByLikeUserAndLikeProduct(user, product);
        assertThat(exists).isTrue();
    }

    @DisplayName("등록된 좋아요를 취소할 수 있다")
    @Test
    void cancelLike_success() {
        // given
        User savedUser = createUser("user789", "user3@test.com");
        Product savedProduct = createProduct("P003", "상품3");

        // Detached 엔티티를 managed 상태로 변경
        entityManager.clear();
        User user = entityManager.find(User.class, savedUser.getId());
        Product product = entityManager.find(Product.class, savedProduct.getId());
        Long initialLikeCount = product.getLikeCount();

        productLikeService.addLike(user, product);

        // addLike 이벤트 완료 대기
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    entityManager.clear();
                    Product reloadedProduct = entityManager.find(Product.class, product.getId());
                    assertThat(reloadedProduct.getLikeCount()).isEqualTo(initialLikeCount + 1);
                });

        // when
        productLikeService.cancelLike(user, product);

        // then
        boolean exists = productLikeRepository.existsByLikeUserAndLikeProduct(user, product);
        assertThat(exists).isFalse();

        // cancelLike 이벤트 완료 대기
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    entityManager.clear();
                    Product reloadedProduct = entityManager.find(Product.class, product.getId());
                    assertThat(reloadedProduct.getLikeCount()).isEqualTo(initialLikeCount);
                });
    }

    @DisplayName("존재하지 않는 좋아요를 취소하려고 하면 예외가 발생한다")
    @Test
    void cancelLike_notExists_throwException() {
        // given
        User savedUser = createUser("user999", "user4@test.com");
        Product savedProduct = createProduct("P004", "상품4");

        // Detached 엔티티를 managed 상태로 변경
        entityManager.clear();
        User user = entityManager.find(User.class, savedUser.getId());
        Product product = entityManager.find(Product.class, savedProduct.getId());

        // when & then
        CoreException exception = assertThrows(CoreException.class,
                () -> productLikeService.cancelLike(user, product));

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(exception.getCustomMessage()).isEqualTo("좋아요가 존재하지 않습니다");
    }

    @DisplayName("좋아요를 등록하고 취소한 후 다시 등록할 수 있다")
    @Test
    void addLike_afterCancel_success() {
        // given
        User savedUser = createUser("user555", "user5@test.com");
        Product savedProduct = createProduct("P005", "상품5");

        // Detached 엔티티를 managed 상태로 변경
        entityManager.clear();
        User user = entityManager.find(User.class, savedUser.getId());
        Product product = entityManager.find(Product.class, savedProduct.getId());

        productLikeService.addLike(user, product);
        productLikeService.cancelLike(user, product);

        // when
        ProductLike newLike = productLikeService.addLike(user, product);

        // then
        assertThat(newLike.getId()).isNotNull();
        assertThat(newLike.getLikeUser().getId()).isEqualTo(user.getId());
        assertThat(newLike.getLikeProduct().getId()).isEqualTo(product.getId());
    }

    private User createUser(String userId, String email) {
        User user = User.createUser(userId, email, "1990-01-01", Gender.MALE);
        return userRepository.save(user);
    }

    private Product createProduct(String productCode, String productName) {
        Brand brand = Brand.createBrand("테스트브랜드");
        Brand savedBrand = brandRepository.registerBrand(brand);

        Product product = Product.createProduct(
                productCode,
                productName,
                Money.of(10000),
                100,
                savedBrand
        );
        return productRepository.registerProduct(product);
    }
}
