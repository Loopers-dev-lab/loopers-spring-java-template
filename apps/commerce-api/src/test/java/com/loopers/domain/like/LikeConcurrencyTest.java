package com.loopers.domain.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
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
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Product product;
    private List<User> users;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        Brand brand = brandRepository.save(Brand.create("Brand"));
        product = productRepository.save(Product.create("Product", 1000L, 100, brand));

        users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String userId = "likeUser" + String.format("%03d", i);
            users.add(userService.signUp(userId, userId + "@mail.com", "1990-01-01", Gender.MALE));
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일 유저가 좋아요 100번 동시 클릭 시, 1개만 생성된다.")
    @Test
    void shouldCreateOnlyOneLike_whenSameUserClicksHundredTimesSimultaneously()
            throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        User sameUser = users.get(0);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);

                    likeFacade.addLike(sameUser.getUserIdValue(), product.getId());

                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        Thread.sleep(100);
        startLatch.countDown();

        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // then
        Long likeCount = likeService.getLikeCount(product);
        Long dbLikeCount = likeRepository.countByProduct(product);

        assertThat(likeCount).isEqualTo(1L);
        assertThat(dbLikeCount).isEqualTo(1L);

        if (!exceptions.isEmpty()) {
            System.out.println("멱등성으로 무시된 요청 수: " + exceptions.size());
            exceptions.forEach(e -> {
                assertThat(e)
                        .satisfiesAnyOf(
                                ex -> assertThat(ex).isInstanceOf(
                                        org.springframework.dao.DataIntegrityViolationException.class),
                                ex -> assertThat(ex).isInstanceOf(
                                        org.springframework.transaction.UnexpectedRollbackException.class)
                        );
            });
        }
    }

    @DisplayName("100명의 유저가 동일한 상품에 동시에 '좋아요'를 누를때 정상 처리된다.")
    @Test
    void testLikeConcurrency() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // act
        for (int i = 0; i < threadCount; i++) {
            User user = users.get(i);
            executor.submit(() -> {
                try {
                    likeFacade.addLike(user.getUserIdValue(), product.getId());
                } catch (Exception e) {
                    System.err.println("좋아요 동시성 테스트 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // assert
        Long likeCount = likeService.getLikeCount(product);
        assertThat(likeCount).isEqualTo(100L);
        assertThat(likeRepository.countByProduct(product)).isEqualTo(100L);
    }
}
