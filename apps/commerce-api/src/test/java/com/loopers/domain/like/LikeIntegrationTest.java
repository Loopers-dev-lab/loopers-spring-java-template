package com.loopers.domain.like;

import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeIntegrationTest {

    @Autowired
    private LikeService likeService;
    @Autowired
    private LikeJpaRepository likeJpaRepository;
    @Autowired
    private ProductJpaRepository productJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Product newProduct() {
        return Product.builder()
                .brandId(1L)
                .name("P1")
                .price(new Money(BigDecimal.valueOf(1000)))
                .stockQuantity(new Stock(100))
                .build();
    }

    @Test
    @DisplayName("동일 상품에 대해 다수가 동시에 좋아요 요청해도 like_count가 정확히 증가한다")
    void concurrent_like_increments_like_count_exactly() throws InterruptedException {
        // given
        Product saved = productJpaRepository.save(newProduct());
        Long productId = saved.getId();

        int users = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(users);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            String userId = "u-" + i;
            tasks.add(() -> {
                await(start);
                likeService.likeProduct(userId, productId);
                done.countDown();
            });
        }
        tasks.forEach(pool::execute);

        // when
        start.countDown();
        done.await();
        pool.shutdown();

        // then
        Product reloaded = productJpaRepository.findById(productId).orElseThrow();
        Long countInTable = likeJpaRepository.countByProductId(productId);
        assertThat(reloaded.getLikeCount()).isEqualTo(users);
        assertThat(countInTable).isEqualTo((long) users);
    }

    @Test
    @DisplayName("동일 상품에 대해 다수가 동시에 좋아요 취소 요청해도 like_count가 정확히 감소한다")
    void concurrent_unlike_decrements_like_count_exactly() throws InterruptedException {
        // given: 선행 좋아요로 40건 누적
        Product saved = productJpaRepository.save(newProduct());
        Long productId = saved.getId();
        int users = 10;
        for (int i = 0; i < users; i++) {
            likeService.likeProduct("u-" + i, productId);
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(users);
        for (int i = 0; i < users; i++) {
            String userId = "u-" + i;
            pool.execute(() -> {
                await(start);
                likeService.cancleLikeProduct(userId, productId);
                done.countDown();
            });
        }

        // when
        start.countDown();
        done.await();
        pool.shutdown();

        // then
        Product reloaded = productJpaRepository.findById(productId).orElseThrow();
        Long countInTable = likeJpaRepository.countByProductId(productId);
        assertThat(reloaded.getLikeCount()).isEqualTo(0L);
        assertThat(countInTable).isEqualTo(0L);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


