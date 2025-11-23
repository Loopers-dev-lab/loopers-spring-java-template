package com.loopers.domain.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.loopers.support.fixture.UserFixtures.createValidUser;
import static com.loopers.support.fixture.UserFixtures.createValidUsers;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LikeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Product createValidProduct() {
        return Product.create(1L, "상품이름1", Money.of(1000));
    }

    @DisplayName("한 사용자가 특정 상품에 좋아요를 100번 눌러도 카운트는 1 증가한다.")
    @Test
    public void concurrentLikeBySameUser_shouldBeIdempotent() throws InterruptedException {
        // arrange
        int threadCount = 100;

        // 100개의 스레드를 가질 수 있는 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 100개의 작업이 완료될 때까지 기다림
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        User user = userRepository.save(createValidUser());
        Product product = productRepository.save(
                createValidProduct()
        );

        // act
        IntStream.range(0, threadCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    // 같은 유저의 좋아요 서비스 호출
                    likeFacade.addLike(1L, 1L);
                } catch (OptimisticLockException | ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                    e.printStackTrace();
                    System.out.println("실패: " + e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        countDownLatch.await();

        // assert
        Product product2 = productRepository.findById(1L).orElseThrow();
        assertThat(product2.getLikeCount()).isEqualTo(1);
    }

    @DisplayName("5명의 사용자가 특정 상품에 각자 좋아요를 누르면 카운트는 5 증가한다.")
    @Test
    public void concurrentLikesFromDifferentUsers_shouldIncreaseCountCorrectly() throws InterruptedException {
        // arrange
        int threadCount = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        List<User> users = createValidUsers(threadCount);
        Product product = productRepository.save(
                createValidProduct()
        );

        IntStream.range(0, threadCount).forEach(i -> {
            userRepository.save(users.get(i));
        });

        // act
        IntStream.range(0, threadCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    // 각자 다른 유저의 동일 상품 좋아요 호출
                    likeFacade.addLike(users.get(i).getId(), product.getId());
                } catch (OptimisticLockingFailureException e) {
                    System.out.println("test 예외 타입 = " + e.getClass().getName());
                } finally {
                    countDownLatch.countDown();
                }
            });

        });
        countDownLatch.await();

        // assert
        Product product2 = productRepository.findById(1L).orElseThrow();
        assertThat(product2.getLikeCount()).isEqualTo(5);
    }


    @DisplayName("한 사용자가 좋아요한 상품에 좋아요 취소를 100번 눌러도 카운트는 1 감소한다.")
    @Test
    public void concurrentUnlikeBySameUser_shouldBeIdempotent() throws InterruptedException {
        // arrange
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        User user = userRepository.save(createValidUser());
        Product product = createValidProduct();
        product.increaseLikeCount();
        Product savedProduct = productRepository.save(product);

        likeFacade.addLike(user.getId(), savedProduct.getId());

        // act
        IntStream.range(0, threadCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    // 같은 유저의 좋아요 서비스 호출
                    likeFacade.removeLike(1L, 1L);
                } catch (OptimisticLockException | ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                    e.printStackTrace();
                    System.out.println("실패: " + e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        countDownLatch.await();

        // assert
        Product result = productRepository.findById(1L).orElseThrow();
        assertThat(result.getLikeCount()).isEqualTo(1);
    }


    @DisplayName("5명의 사용자가 좋아요 했던 한 상품에 각자 좋아요 취소를 누르면 카운트는 5 감소한다.")
    @Test
    public void concurrentUnlikesFromDifferentUsers_shouldDecreaseCountCorrectly() throws InterruptedException {
        // arrange
        int threadCount = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        Product product = createValidProduct();
        IntStream.range(0, threadCount).forEach(i -> {
            product.increaseLikeCount();
        });
        productRepository.save(product);

        List<User> users = createValidUsers(threadCount);
        IntStream.range(0, threadCount).forEach(i -> {
            userRepository.save(users.get(i));
            likeRepository.save(Like.create(users.get(i).getId(), product.getId()));
        });

        System.out.println(productRepository.findById(product.getId()).get().getLikeCount());

        // act
        IntStream.range(0, threadCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    // 각자 다른 유저의 동일 상품 좋아요 취소 호출
                    likeFacade.removeLike(users.get(i).getId(), product.getId());
                } catch (OptimisticLockingFailureException e) {
                    System.out.println("test 예외 타입 = " + e.getClass().getName());
                } finally {
                    countDownLatch.countDown();
                }
            });

        });
        countDownLatch.await();

        // assert
        Product product2 = productRepository.findById(1L).orElseThrow();
        assertThat(product2.getLikeCount()).isEqualTo(0);
    }
}
