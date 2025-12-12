package com.loopers.domain.product;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductLikeStructureTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Product product;
    private List<User> users;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        Brand brand = brandRepository.save(Brand.create("TestBrand"));
        product = productRepository.save(Product.create("TestProduct", 10000L, 100, brand));

        users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String userId = "testUser" + i;
            users.add(userService.signUp(userId, userId + "@test.com", "1990-01-01", Gender.MALE));
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("상품 생성 시 좋아요 수는 0으로 초기화된다")
    void initializeLikeCountAsZero() {
        // given
        Brand brand = brandRepository.save(Brand.create("NewBrand"));

        // when
        Product newProduct = Product.create("NewProduct", 5000L, 50, brand);
        Product saved = productRepository.save(newProduct);

        // then
        assertThat(saved.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("좋아요 등록 시 상품의 좋아요 수가 증가한다")
    void incrementLikeCountWhenLikeAdded() {
        // given
        User user = users.get(0);
        Long initialLikeCount = product.getLikeCount();

        // when
        likeFacade.addLike(user.getLoginIdValue(), product.getId());

        // then
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(initialLikeCount + 1);
    }

    @Test
    @DisplayName("좋아요 취소 시 상품의 좋아요 수가 감소한다")
    void decrementLikeCountWhenLikeRemoved() {
        // given
        User user = users.get(0);
        likeFacade.addLike(user.getLoginIdValue(), product.getId());

        Product afterAdd = productRepository.findById(product.getId()).orElseThrow();
        Long likeCountAfterAdd = afterAdd.getLikeCount();

        // when
        likeFacade.removeLike(user.getLoginIdValue(), product.getId());

        // then
        Product afterRemove = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterRemove.getLikeCount()).isEqualTo(likeCountAfterAdd - 1);
    }

    @Test
    @DisplayName("여러 유저의 좋아요 등록 시 좋아요 수가 정확히 반영된다")
    void reflectMultipleLikes() {
        // given
        Long initialLikeCount = product.getLikeCount();

        // when
        for (User user : users) {
            likeFacade.addLike(user.getLoginIdValue(), product.getId());
        }

        // then
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(initialLikeCount + users.size());

        Long actualLikeCount = likeRepository.countByProduct(product);
        assertThat(updated.getLikeCount()).isEqualTo(actualLikeCount);
    }

    @Test
    @DisplayName("좋아요 등록과 취소를 반복해도 정합성이 유지된다")
    void maintainConsistencyWithRepeatedOperations() {
        // given
        User user = users.get(0);
        Long initialLikeCount = product.getLikeCount();

        // when
        for (int i = 0; i < 5; i++) {
            likeFacade.addLike(user.getLoginIdValue(), product.getId());
            likeFacade.removeLike(user.getLoginIdValue(), product.getId());
        }

        // then
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(initialLikeCount);

        Long actualLikeCount = likeRepository.countByProduct(product);
        assertThat(updated.getLikeCount()).isEqualTo(actualLikeCount);
    }

    @Test
    @DisplayName("상품 테이블과 좋아요 테이블의 카운트가 일치한다")
    void matchLikeCountBetweenTables() {
        // given
        users.forEach(user ->
                likeFacade.addLike(user.getLoginIdValue(), product.getId())
        );

        // when
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Long productLikeCount = updated.getLikeCount();
        Long actualLikeCount = likeRepository.countByProduct(product);

        // then
        assertThat(productLikeCount).isEqualTo(actualLikeCount);
        assertThat(productLikeCount).isEqualTo((long) users.size());
    }

    @Test
    @DisplayName("동시에 여러 유저가 좋아요를 등록해도 정합성이 유지된다")
    void maintainConsistencyUnderConcurrency() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Long initialLikeCount = product.getLikeCount();

        // when - 캐시 서비스 없이 직접 호출
        for (int i = 0; i < threadCount; i++) {
            User user = users.get(i);
            executor.submit(() -> {
                try {
                    boolean isNewLike = likeService.addLike(user, product);
                    if (isNewLike) {
                        productService.incrementLikeCount(product.getId());
                    }
                } catch (Exception e) {
                    System.err.println("좋아요 실패: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Long productLikeCount = updated.getLikeCount();
        Long actualLikeCount = likeRepository.countByProduct(product);

        System.out.println("=".repeat(80));
        System.out.println("초기 likeCount: " + initialLikeCount);
        System.out.println("최종 likeCount: " + productLikeCount);
        System.out.println("실제 Like 개수: " + actualLikeCount);
        System.out.println("=".repeat(80));

        assertThat(productLikeCount).isEqualTo(initialLikeCount + threadCount);
        assertThat(productLikeCount).isEqualTo(actualLikeCount);
    }

    @Test
    @DisplayName("비정규화 필드 사용으로 JOIN 없이 좋아요 수 조회가 가능하다")
    void queryLikeCountWithoutJoin() {
        // given
        users.forEach(user ->
                likeFacade.addLike(user.getLoginIdValue(), product.getId())
        );

        // when
        Product updated = productRepository.findById(product.getId()).orElseThrow();

        // then
        assertThat(updated.getLikeCount()).isEqualTo((long) users.size());
    }
}
