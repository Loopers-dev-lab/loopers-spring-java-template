package com.loopers.application.like;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 상품에 대해 여러 유저가 동시에 좋아요 요청을 보내도 likeCount는 정확히 1 증가한다.")
    @Test
    void should_handle_like_concurrency_correctly() throws Exception {

        Product product = Product.create(1L, "테스트상품", 10000L, 10L);
        productRepository.save(product);

        Long productId = product.getId();

        int threadCount = 20; // 20명이 동시에 좋아요
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i ->
                executor.submit(() -> {
                    try {
                        likeFacade.createLike("USER_" + i, productId);
                    } finally {
                        latch.countDown();
                    }
                })
        );

        latch.await();
        executor.shutdown();

        Product updated = productRepository.findById(productId).orElseThrow();
        long expected = 20;

        assertThat(updated.getLikeCount()).isEqualTo(expected);
        assertThat(likeRepository.countByProductId(productId)).isEqualTo(expected);
    }

    @DisplayName("동일한 상품에 대해 여러 유저가 동시에 좋아요 취소 요청을 보내도 likeCount는 정확히 감소된다.")
    @Test
    void should_handle_unlike_concurrency_correctly() throws Exception {
        Product product = Product.create(1L, "테스트상품", 10000L, 10L);
        productRepository.save(product);

        Long productId = product.getId();

        for (int i = 0; i < 20; i++) {
            likeFacade.createLike("USER_" + i, productId);
        }

        int threadCount = 20; // 20명이 동시에 취소 요청
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i ->
                executor.submit(() -> {
                    try {
                        likeFacade.deleteLike("USER_" + i, productId);
                    } finally {
                        latch.countDown();
                    }
                })
        );

        latch.await();
        executor.shutdown();

        Product updated = productRepository.findById(productId).orElseThrow();

        assertThat(updated.getLikeCount()).isEqualTo(0);
        assertThat(likeRepository.countByProductId(productId)).isEqualTo(0);
    }
}
