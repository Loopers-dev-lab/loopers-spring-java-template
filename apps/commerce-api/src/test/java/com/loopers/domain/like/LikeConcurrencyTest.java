package com.loopers.domain.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    private LikeRepository likeRepository;

    @Autowired
    private TestFixture testFixture;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Product product;
    private List<User> users;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        TestFixture.ConcurrencyTestData data = testFixture.setupForConcurrency(100, 100, 1000L);
        users = data.users();
        product = data.product();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일 유저가 좋아요 100번 동시 클릭 시, 1개만 생성된다.")
    @Test
    void shouldCreateOnlyOneLike_whenSameUserClicksHundredTimesSimultaneously()
            throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        User sameUser = users.get(0);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    likeFacade.addLike(sameUser.getLoginIdValue(), product.getId());
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

        Long dbLikeCount = likeRepository.countByProduct(product);
        assertThat(dbLikeCount).isEqualTo(1L);
    }

    @DisplayName("100명의 유저가 동일한 상품에 동시에 '좋아요'를 누를때 정상 처리된다.")
    @Test
    void testLikeConcurrency() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();


        // act
        for (int i = 0; i < threadCount; i++) {
            User user = users.get(i);
            executor.submit(() -> {
                try {
                    likeFacade.addLike(user.getLoginIdValue(), product.getId());
                } catch (Exception e) {
                    System.err.println("좋아요 동시성 테스트 실패: " + e.getMessage());
                    exceptions.add(e);
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
        assertThat(exceptions).isEmpty();
    }
}
